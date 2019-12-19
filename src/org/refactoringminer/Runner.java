package org.refactoringminer;

import com.jasongoodwin.monads.Try;
import gr.uom.java.xmi.TypeFactMiner.Models.TypeGraphOuterClass;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import io.vavr.Tuple3;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.api.TypeRelatedRefactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gr.uom.java.xmi.TypeFactMiner.TypeGraphUtil.pretty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Runner {

    public final static String path = "D:\\MyProjects\\";

    public static void main(String a[]) throws IOException {
        Map<String, String> ps = readProjects(path +"mavenProjectsAll.csv");

        List<String> commitsToAnalyze = Files.readAllLines(Paths.get("D:\\MyProjects\\refactoringsAnalysis.txt"))
                .stream()
                .map(row -> row.split(",")[1])
                .collect(toList());
        GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
        for(Map.Entry<String,String> p :ps.entrySet()) {
            final String projectPath = path + "Corpus\\Project_" + p.getKey() + "\\";
            final Try<Git> gitRepo = tryCloningRepo(p.getKey(), p.getValue(), projectPath);
                   // .onFailure(Throwable::printStackTrace);
             if(gitRepo.isSuccess()) {
                List<RevCommit> cs = getCommits(gitRepo.getUnchecked(), RevSort.COMMIT_TIME_DESC);
                for (RevCommit c : cs) {
                    if( c.getId().getName().equals("e4128e95053a333a1a2bf8db03ca5e0f5b0b836f")
//                            ( c.getId().getName().equals("e57c689626c93f4b2243e11e1e1d243abde7e248")
//                    || c.getId().getName().equals("fc6dabc6a07eee76dcce7b3124a69d69306f1102"))
                    &&
                    commitsToAnalyze.contains(c.getId().getName())){
                        System.out.println(c.getId().getName());
                        final ChangeTypeMiner ctm = new ChangeTypeMiner(gitRepo.getUnchecked().getRepository(), c);

                        gitHistoryRefactoringMiner
                                .detectAtCommit(gitRepo.getUnchecked().getRepository(), c.getId().getName(), ctm, 50);
                    }
                }
            }
        }

    }


    public static class ChangeTypeMiner extends RefactoringHandler {

        private Repository repo;
        private RevCommit rc;

        public ChangeTypeMiner(Repository repo, RevCommit rc) {
            this.repo = repo;
            this.rc = rc;
        }

        @Override
        public void handle(String c, List<Refactoring> refactorings) {

            System.out.println("Refactorings detected");
            if (refactorings.isEmpty() || refactorings.stream().noneMatch(r -> r.getRefactoringType().equals(RefactoringType.CHANGE_ATTRIBUTE_TYPE)
                    || r.getRefactoringType().equals(RefactoringType.CHANGE_PARAMETER_TYPE) || r.getRefactoringType().equals(RefactoringType.CHANGE_VARIABLE_TYPE)
                    || r.getRefactoringType().equals(RefactoringType.CHANGE_RETURN_TYPE))) {
                System.out.println("No CTT");
                return;
            }

            List<TypeRelatedRefactoring> typeRelatedRefactorings = refactorings.stream().filter(x -> x instanceof TypeRelatedRefactoring)
                    .map(x -> (TypeRelatedRefactoring) x)
                    .collect(toList());
            try {
                System.out.println("Resolved");
                typeRelatedRefactorings.stream().filter(x->x.isResolved()).forEach(x -> {

                    Path resolved = Paths.get("D:/MyProjects/resolved.txt");
                    if(!resolved.toFile().exists()){
                        Try.ofFailable(()->Files.createFile(resolved));
                    }

                    String tc = "\n" + ((Refactoring) x).getName() + "  " + x.getTypeB4().getTypeStr() + "  ---->   " + x.getTypeAfter().getTypeStr();
                    Try.ofFailable(() -> Files.write(resolved,tc.getBytes(), StandardOpenOption.APPEND));
                    System.out.print(tc);
                    if(x.getRealTypeChanges()!=null && x.getRealTypeChanges().get(0)!=null) {
                        Tuple3<TypeGraphOuterClass.TypeGraph, TypeGraphOuterClass.TypeGraph, List<String>> t = x.getRealTypeChanges().get(0);

                        if (t != null) {
                            String s = "\n" +  pretty(t._1()) + " to " + pretty(t._2()) + " Info: " + t._3().stream().collect(Collectors.joining(","));
                            System.out.print(s);
                            Try.ofFailable(() -> Files.write(resolved,s.getBytes(), StandardOpenOption.APPEND));
                        }
                        x.getRealTypeChanges().stream().skip(1).forEach(tcc -> {
                            String x1 = "\n" +  "       " + pretty(tcc._1()) + " to " + pretty(tcc._2()) + " Info: " + tcc._3().stream().collect(Collectors.joining(","));
                            Try.ofFailable(() -> Files.write(resolved,x1.getBytes(), StandardOpenOption.APPEND));
                            System.out.print(x1);
                        });
                    }
                });

                System.out.println();
                System.out.println("Un resolved");
                typeRelatedRefactorings.stream().filter(x->!x.isResolved()).forEach(x -> {
                    Path unResolved = Paths.get("D:/MyProjects/unresolved.txt");
                    if(!unResolved.toFile().exists()){
                        Try.ofFailable(()->Files.createFile(unResolved));
                    }
                    String x1 = c + ((Refactoring) x).getName() + "  "
                            + x.getTypeB4().getTypeStr() + "  ---->   " + x.getTypeAfter().getTypeStr() + "\n";
                    Try.ofFailable(() -> Files.write(unResolved,x1.getBytes(), StandardOpenOption.APPEND));
                });

            }catch (Exception e){
                System.out.println("Some exception!!!");
            }
            System.out.println("-------------------------------");

        }

    }

    public static Map<String, String> readProjects(String path){
        try {
            return Files.readAllLines(Paths.get(path)).parallelStream()
                    .map(e -> Tuple.of(e.split(",")[0], e.split(",")[1]))
                    .peek(e -> System.out.println(e._1()))
                    .collect(toMap(Tuple2::_1, Tuple2::_2, (a, b)->a));
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Could not read projects");
            throw new RuntimeException("Could not read projects");
        }
    }

    public static Try<Git> tryCloningRepo(String projectName, String cloneLink, String path) {
        return Try.ofFailable(() -> Git.open(new File(path + projectName)))
                .onFailure(e -> {
               //     e.printStackTrace();
                    System.out.println("Did not find " + projectName + " at" + path);
                })
//                .orElseTry(() ->
//                        Git.cloneRepository().setURI(cloneLink).setDirectory(new File(path + projectName)).call())
                .onFailure(e -> System.out.println("Could not clone " + projectName));

    }

    public static List<RevCommit> getCommits(Git git, RevSort order) {
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
        String input = "2015-01-01" ;
        return Try.ofFailable(() -> {
            RevWalk walk = new RevWalk(git.getRepository());
            walk.markStart(walk.parseCommit(git.getRepository().resolve("HEAD")));
            walk.sort(order);
            walk.setRevFilter(RevFilter.NO_MERGES);
            walk.setRevFilter(CommitTimeRevFilter.after(ft.parse(input)));
            return walk;
        })
                .map(walk -> {
                    Iterator<RevCommit> iter = walk.iterator();
                    List<RevCommit> l = new ArrayList<>();
                    while(iter.hasNext()){
                        l.add(iter.next()); }
                    walk.dispose();
                    return l;
                })
                .onSuccess(l -> System.out.println("Total number of commits found : " + l.size()))
                .onFailure(Throwable::printStackTrace)

                .orElse(new ArrayList<>());
    }

}
