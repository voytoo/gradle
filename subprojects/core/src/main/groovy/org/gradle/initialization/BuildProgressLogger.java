/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization;

import org.gradle.BuildAdapter;
import org.gradle.BuildResult;
import org.gradle.api.Project;
import org.gradle.api.ProjectEvaluationListener;
import org.gradle.api.ProjectState;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.execution.TaskExecutionGraphListener;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskState;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;

class BuildProgressLogger extends BuildAdapter implements TaskExecutionGraphListener, TaskExecutionListener, ProjectEvaluationListener {
    private ProgressLogger progressLogger;
    private final ProgressLoggerFactory progressLoggerFactory;
    private Gradle gradle;
    private BuildPhaseProgress buildProgress;
    private int configuredProjects = 0;
    private ProgressLogger confLogger;
    private int totalProjects;
    private ProgressLogger currentProjectLogger;

    public BuildProgressLogger(ProgressLoggerFactory progressLoggerFactory) {
        this.progressLoggerFactory = progressLoggerFactory;
    }

    @Override
    public void buildStarted(Gradle gradle) {
        if (gradle.getParent() == null) {
            progressLogger = progressLoggerFactory.newOperation(BuildProgressLogger.class);
            progressLogger.setDescription("Initialize build");
            progressLogger.setShortDescription("Loading");
            progressLogger.started();
            this.gradle = gradle;
        }
    }

    @Override
    public void projectsLoaded(Gradle gradle) {
        if (gradle.getParent() == null) {
            totalProjects = gradle.getRootProject().getAllprojects().size();
            confLogger = progressLoggerFactory.newOperation(BuildProgressLogger.class);
            confLogger.setDescription("Configure projects");
            confLogger.setShortDescription("0/" + totalProjects + " projects");
            confLogger.started();
        }
    }

    public void graphPopulated(TaskExecutionGraph graph) {
        if (graph == gradle.getTaskGraph()) {
            confLogger.completed();
            confLogger = null;
            progressLogger.completed("Task graph ready");
            progressLogger = progressLoggerFactory.newOperation(BuildProgressLogger.class);
            progressLogger.setDescription("Execute tasks");
            String desc = "Building";
            progressLogger.setShortDescription(desc + " 0%");
            buildProgress = new BuildPhaseProgress(desc, graph.getAllTasks().size());
            progressLogger.started();
        }
    }

    @Override
    public void buildFinished(BuildResult result) {
        if (result.getGradle() == gradle) {
            progressLogger.completed();
            progressLogger = null;
            gradle = null;
            buildProgress = null;
            confLogger = null;
        }
    }

    public void beforeExecute(Task task) {}

    public void afterExecute(Task task, TaskState state) {
        if (task.getProject().getGradle() == gradle) {
            progressLogger.progress(buildProgress.progress());
        }
    }

    public void beforeEvaluate(Project project) {
        if (project.getGradle() == gradle && confLogger != null) {
            currentProjectLogger = progressLoggerFactory.newOperation(BuildProgressLogger.class);
            currentProjectLogger.setDescription("Configuring " + project);
            currentProjectLogger.setShortDescription("Configuring " + (project.getPath().equals(":")? "root project" : project.getPath()));
            currentProjectLogger.started();
        }
    }

    public void afterEvaluate(Project project, ProjectState state) {
        if (project.getGradle() == gradle && confLogger != null) {
            currentProjectLogger.completed();
            currentProjectLogger = null;
            confLogger.progress(++configuredProjects + "/" + totalProjects + " projects");
        }
    }
}
