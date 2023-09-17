/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.gradle.util;

import groovy.lang.Closure;

import java.io.File;

import java.net.URL;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.util.PatternFilterable;

/**
 * @author Andrea Di Giorgi
 */
public class GradleUtil {

	public static Configuration addConfiguration(Project project, String name) {
		ConfigurationContainer configurationContainer =
			project.getConfigurations();

		return configurationContainer.create(name);
	}

	public static Dependency addDependency(
		Project project, String configurationName, File file) {

		return _addDependency(project, configurationName, project.files(file));
	}

	public static Dependency addDependency(
		Project project, String configurationName,
		FileCollection fileCollection) {

		return _addDependency(project, configurationName, fileCollection);
	}

	public static Dependency addDependency(
		Project project, String configurationName, String dependencyNotation) {

		return _addDependency(project, configurationName, dependencyNotation);
	}

	public static Dependency addDependency(
		Project project, String configurationName, String group, String name,
		String version) {

		return addDependency(
			project, configurationName, group, name, version, true);
	}

	public static Dependency addDependency(
		Project project, String configurationName, String group, String name,
		String version, boolean transitive) {

		Map<String, Object> dependencyNotation = new HashMap<>();

		dependencyNotation.put("group", group);
		dependencyNotation.put("name", name);
		dependencyNotation.put("transitive", transitive);
		dependencyNotation.put("version", version);

		return _addDependency(project, configurationName, dependencyNotation);
	}

	public static <T> T addExtension(
		Project project, String name, Class<T> clazz) {

		ExtensionContainer extensionContainer = project.getExtensions();

		return extensionContainer.create(name, clazz, project);
	}

	public static SourceSet addSourceSet(Project project, String name) {
		JavaPluginConvention javaPluginConvention = getConvention(
			project, JavaPluginConvention.class);

		SourceSetContainer sourceSetContainer =
			javaPluginConvention.getSourceSets();

		return sourceSetContainer.create(name);
	}

	public static <T extends Task> T addTask(
		Project project, String name, Class<T> clazz) {

		Map<String, Class<T>> args = Collections.singletonMap(
			Task.TASK_TYPE, clazz);

		return (T)project.task(args, name);
	}

	public static <T extends Plugin<? extends Project>> void applyPlugin(
		Project project, Class<T> clazz) {

		Map<String, Class<T>> args = Collections.singletonMap("plugin", clazz);

		project.apply(args);
	}

	public static void applyScript(Project project, String name, Object obj) {
		Map<String, Object> args = new HashMap<>();

		ClassLoader classLoader = GradleUtil.class.getClassLoader();

		URL url = classLoader.getResource(name);

		if (url == null) {
			throw new GradleException("Unable to apply script " + name);
		}

		args.put("from", url);

		if (obj != null) {
			args.put("to", obj);
		}

		project.apply(args);
	}

	public static void executeIfEmpty(
		final Configuration configuration, final Action<Configuration> action) {

		ResolvableDependencies resolvableDependencies =
			configuration.getIncoming();

		resolvableDependencies.beforeResolve(
			new Action<ResolvableDependencies>() {

				@Override
				public void execute(
					ResolvableDependencies resolvableDependencies) {

					Set<Dependency> dependencies =
						configuration.getDependencies();
					Set<Configuration> parentConfigurations =
						configuration.getExtendsFrom();

					if (dependencies.isEmpty() &&
						parentConfigurations.isEmpty()) {

						action.execute(configuration);
					}
				}

			});
	}

	public static Configuration getConfiguration(Project project, String name) {
		ConfigurationContainer configurationContainer =
			project.getConfigurations();

		return configurationContainer.getByName(name);
	}

	public static <T> T getConvention(Project project, Class<T> clazz) {
		Convention convention = project.getConvention();

		return convention.getPlugin(clazz);
	}

	public static <T> T getExtension(Project project, Class<T> clazz) {
		ExtensionContainer extensionContainer = project.getExtensions();

		return extensionContainer.getByType(clazz);
	}

	public static FileTree getFilteredFileTree(
		FileTree fileTree, final String[] excludes, final String[] includes) {

		Closure<Void> closure = new Closure<Void>(null) {

			@SuppressWarnings("unused")
			public void doCall(PatternFilterable patternFilterable) {
				if (ArrayUtil.isNotEmpty(excludes)) {
					patternFilterable.setExcludes(Arrays.asList(excludes));
				}

				if (ArrayUtil.isNotEmpty(includes)) {
					patternFilterable.setIncludes(Arrays.asList(includes));
				}
			}

		};

		return fileTree.matching(closure);
	}

	public static Project getProject(Project rootProject, File projectDir) {
		for (Project project : rootProject.getAllprojects()) {
			if (projectDir.equals(project.getProjectDir())) {
				return project;
			}
		}

		return null;
	}

	public static SourceSet getSourceSet(Project project, String name) {
		JavaPluginConvention javaPluginConvention = getConvention(
			project, JavaPluginConvention.class);

		SourceSetContainer sourceSetContainer =
			javaPluginConvention.getSourceSets();

		return sourceSetContainer.getByName(name);
	}

	public static Task getTask(Project project, String name) {
		TaskContainer taskContainer = project.getTasks();

		return taskContainer.getByName(name);
	}

	public static String getTaskName(String prefix, File file) {
		String fileName = FileUtil.stripExtension(file.getName());

		fileName = fileName.replaceAll("\\W", "");

		return prefix + StringUtil.capitalize(fileName);
	}

	public static void removeDependencies(
		Project project, String configurationName,
		String[] dependencyNotations) {

		Configuration configuration = getConfiguration(
			project, configurationName);

		Set<Dependency> dependencies = configuration.getDependencies();

		Iterator<Dependency> iterator = dependencies.iterator();

		while (iterator.hasNext()) {
			Dependency dependency = iterator.next();

			String dependencyNotation = _getDependencyNotation(dependency);

			if (ArrayUtil.contains(dependencyNotations, dependencyNotation)) {
				iterator.remove();
			}
		}
	}

	private static Dependency _addDependency(
		Project project, String configurationName, Object dependencyNotation) {

		DependencyHandler dependencyHandler = project.getDependencies();

		return dependencyHandler.add(configurationName, dependencyNotation);
	}

	private static String _getDependencyNotation(Dependency dependency) {
		StringBuilder sb = new StringBuilder();

		if (Validator.isNotNull(dependency.getGroup())) {
			sb.append(dependency.getGroup());
			sb.append(":");
		}

		sb.append(dependency.getName());

		if (Validator.isNotNull(dependency.getVersion())) {
			sb.append(":");
			sb.append(dependency.getVersion());
		}

		return sb.toString();
	}

}