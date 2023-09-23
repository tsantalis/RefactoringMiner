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

package com.liferay.gradle.plugins.poshi.runner.util;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;

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
		Project project, String configurationName, String group, String name,
		String version, String classifier) {

		DependencyHandler dependencyHandler = project.getDependencies();

		Map<String, Object> dependencyNotation = new HashMap<>();

		dependencyNotation.put("group", group);
		dependencyNotation.put("name", name);
		dependencyNotation.put("version", version);

		if (Validator.isNotNull(classifier)) {
			dependencyNotation.put("classifier", classifier);
		}

		return dependencyHandler.add(configurationName, dependencyNotation);
	}

	public static <T> T addExtension(
		Project project, String name, Class<T> clazz) {

		ExtensionContainer extensionContainer = project.getExtensions();

		return extensionContainer.create(name, clazz, project);
	}

	public static <T extends Task> T addTask(
		Project project, String name, Class<T> clazz) {

		TaskContainer taskContainer = project.getTasks();

		return taskContainer.create(name, clazz);
	}

	public static Configuration getConfiguration(Project project, String name) {
		ConfigurationContainer configurationContainer =
			project.getConfigurations();

		return configurationContainer.getByName(name);
	}

	public static Task getTask(Project project, String name) {
		TaskContainer taskContainer = project.getTasks();

		return taskContainer.getByName(name);
	}

}