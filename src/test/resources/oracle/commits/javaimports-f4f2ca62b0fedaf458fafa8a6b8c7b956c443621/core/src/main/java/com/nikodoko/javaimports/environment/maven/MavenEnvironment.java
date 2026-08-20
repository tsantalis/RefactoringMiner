package com.nikodoko.javaimports.environment.maven;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.JavaProject;
import com.nikodoko.javaimports.environment.PackageDistance;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a Maven project environment, scanning project files and dependencies for importable
 * symbols.
 */
public class MavenEnvironment implements Environment {
  private static Logger log = Logger.getLogger(MavenEnvironment.class.getName());
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");
  private static final Clock clock = Clock.systemDefaultZone();

  private final Path root;
  private final Path fileBeingResolved;
  private final Options options;
  private final PackageDistance distance;
  private final MavenDependencyResolver resolver;

  private Map<String, Import> bestAvailableImports = new HashMap<>();
  private JavaProject project;
  private boolean projectIsParsed = false;
  private boolean isInitialized = false;

  public MavenEnvironment(
      Path root, Path fileBeingResolved, String pkgBeingResolved, Options options) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
    this.options = options;
    this.distance = PackageDistance.from(pkgBeingResolved);
    var repository =
        options.repository().isPresent() ? options.repository().get() : DEFAULT_REPOSITORY;
    this.resolver = MavenDependencyResolver.withRepository(repository);
  }

  @Override
  public Set<ParsedFile> filesInPackage(String packageName) {
    parseProjectIfNeeded();
    return Sets.newHashSet(project.filesInPackage(packageName));
  }

  @Override
  public Optional<Import> search(String identifier) {
    if (!isInitialized) {
      init();
    }

    return Optional.ofNullable(bestAvailableImports.get(identifier));
  }

  @Override
  public Collection<com.nikodoko.javaimports.common.Import> findImports(Identifier i) {
    if (!isInitialized) {
      init();
    }

    var best = bestAvailableImports.get(i.toString());
    if (best == null) {
      return List.of();
    }

    return List.of(best.toNew());
  }

  private void init() {
    parseProjectIfNeeded();

    long start = clock.millis();
    List<Import> imports = extractImportsInDependencies();
    for (ParsedFile file : project.allFiles()) {
      imports.addAll(extractImports(file));
    }

    Collections.sort(imports, (a, b) -> distance.to(a.qualifier()) - distance.to(b.qualifier()));
    for (Import i : imports) {
      if (bestAvailableImports.containsKey(i.name())) {
        continue;
      }

      bestAvailableImports.put(i.name(), i);
    }

    isInitialized = true;
    log.log(Level.INFO, String.format("init completed in %d ms", clock.millis() - start));
  }

  private void parseProjectIfNeeded() {
    if (projectIsParsed) {
      return;
    }
    long start = clock.millis();

    MavenProjectParser parser = new MavenProjectParser(root, options).excluding(fileBeingResolved);
    MavenProjectParser.Result parsed = parser.parseAll();
    if (options.debug()) {
      log.info(
          String.format(
              "parsed project in %d ms (total of %d files)",
              clock.millis() - start, Iterables.size(parsed.project.allFiles())));

      parsed.errors.forEach(e -> log.log(Level.WARNING, "error parsing project", e));
    }

    project = parsed.project;
    projectIsParsed = true;
  }

  private List<Import> extractImportsInDependencies() {
    MavenDependencyFinder.Result direct = new MavenDependencyFinder().findAll(root);

    var versionlessDirectDependencies =
        direct.dependencies.stream().map(d -> d.hideVersion()).collect(Collectors.toSet());
    var loadedDirect = resolveAndLoad(direct.dependencies);
    var indirectDependencies =
        loadedDirect.stream()
            // Limit to empty dependencies, and get their dependencies
            // This is to better handle cases like org.junit.jupiter.junit-jupiter, that point to
            // an empty jar and a pom which in turns points to the actual API
            //
            // Of course, we could get ALL transitives dependencies and this recursively until we
            // get the full dependency graph (minus version conflicts), but this is not viable on
            // big projects that quickly have a LOT of transitive dependencies. Since relying on
            // these non-explicit dependencies is a bad practice anyway, we explicitely choose to
            // not support it here.
            .filter(d -> d.importables.isEmpty())
            .flatMap(d -> d.dependencies.stream())
            .map(d -> d.hideVersion())
            .filter(d -> !versionlessDirectDependencies.contains(d))
            .distinct()
            .map(d -> d.showVersion())
            .collect(Collectors.toList());
    var loadedIndirect = resolveAndLoad(indirectDependencies);
    if (options.debug()) {
      log.info(
          String.format("found %d direct dependencies: %s", direct.dependencies.size(), direct));
      log.info(
          String.format(
              "found %d indirect dependencies: %s",
              indirectDependencies.size(), indirectDependencies));
    }
    return Stream.concat(loadedDirect.stream(), loadedIndirect.stream())
        .flatMap(d -> d.importables.stream())
        .collect(Collectors.toList());
  }

  private static class LoadedDependency {
    final List<Import> importables;
    final List<MavenDependency> dependencies;

    LoadedDependency(List<Import> importables, List<MavenDependency> dependencies) {
      this.importables = importables;
      this.dependencies = dependencies;
    }
  }

  private List<LoadedDependency> resolveAndLoad(List<MavenDependency> dependencies) {
    var futures =
        dependencies.stream()
            .map(d -> CompletableFuture.supplyAsync(() -> resolveAndLoad(d), options.executor()))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.stream().toArray(CompletableFuture[]::new)).join();
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  private LoadedDependency resolveAndLoad(MavenDependency dependency) {
    LoadedDependency loaded = new LoadedDependency(List.of(), List.of());
    long start = clock.millis();
    try {
      var location = resolver.resolve(dependency);
      if (options.debug()) {
        log.info(String.format("looking for dependency %s at %s", dependency, location));
      }

      var importables = new MavenDependencyLoader().load(location.jar);
      var dependencies = new MavenPomLoader().load(location.pom).dependencies;
      loaded = new LoadedDependency(importables, dependencies);
    } catch (Exception e) {
      // No matter what happens, we don't want to fail the whole importing process just for that.
      if (options.debug()) {
        log.log(Level.WARNING, String.format("could not resolve dependency %s", dependency), e);
      }
    } finally {
      if (options.debug()) {
        log.log(
            Level.INFO,
            String.format(
                "loaded %d imports and %d additional dependencies in %d ms (%s)",
                loaded.importables.size(),
                loaded.dependencies.size(),
                clock.millis() - start,
                dependency));
      }
    }

    return loaded;
  }

  private List<Import> extractImports(ParsedFile file) {
    List<Import> imports = new ArrayList<>();
    for (String identifier : file.topLevelDeclarations()) {
      imports.add(new Import(identifier, file.packageName(), false));
    }

    return imports;
  }
}
