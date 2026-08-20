/*
 * The MIT License
 *
 * Copyright 2015 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.workflow.multibranch.template;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.slaves.WorkspaceList;
import jenkins.branch.Branch;
import jenkins.model.Jenkins;
import jenkins.scm.api.*;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinitionDescriptor;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jenkinsci.plugins.workflow.multibranch.template.finder.ConfigurationValueFinder;
import org.jenkinsci.plugins.workflow.steps.scm.GenericSCMStep;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.jenkinsci.plugins.workflow.multibranch.template.ConfigDrivenWorkflowBranchProjectFactory.PIPELINE_TEMPLATE;
import static org.jenkinsci.plugins.workflow.multibranch.template.ConfigDrivenWorkflowBranchProjectFactory.USER_DEFINITION_PATH;

/**
 * Checks out the desired version of {@link ConfigDrivenWorkflowBranchProjectFactory#USER_DEFINITION_PATH}.
 */
class ConfigFileSCMBinder extends FlowDefinition {

    private String scriptPath;
    private SCM jenkinsFileScm;

    public Object readResolve() {
        if (this.scriptPath == null) {
            this.scriptPath = USER_DEFINITION_PATH;
        }
        return this;
    }

    @DataBoundConstructor public ConfigFileSCMBinder(String scriptPath, SCM jenkinsFileScm) {
        this.scriptPath = scriptPath;
        this.jenkinsFileScm = jenkinsFileScm;
    }

    public static final String INAPPROPRIATE_CONTEXT = "inappropriate context";

    @Override public FlowExecution create(FlowExecutionOwner handle, TaskListener listener, List<? extends Action> actions) throws Exception {
        Queue.Executable exec = handle.getExecutable();
        if (!(exec instanceof WorkflowRun)) {
            throw new IllegalStateException(INAPPROPRIATE_CONTEXT);
        }
        WorkflowRun build = (WorkflowRun) exec;
        WorkflowJob job = build.getParent();
        BranchJobProperty property = job.getProperty(BranchJobProperty.class);
        if (property == null) {
            throw new IllegalStateException(INAPPROPRIATE_CONTEXT);
        }
        Branch branch = property.getBranch();
        ItemGroup<?> parent = job.getParent();
        if (!(parent instanceof WorkflowMultiBranchProject)) {
            throw new IllegalStateException(INAPPROPRIATE_CONTEXT);
        }
        SCMSource scmSource = ((WorkflowMultiBranchProject) parent).getSCMSource(branch.getSourceId());
        if (scmSource == null) {
            throw new IllegalStateException(branch.getSourceId() + " not found");
        }
        SCMHead head = branch.getHead();
        SCMRevision tip = scmSource.fetch(head, listener);
        String script = null;
        String configContents;
        if (tip != null) {
            // TODO are we getting an extra "Could not update commit status." from here?
            build.addAction(new SCMRevisionAction(scmSource, tip));
            if (head instanceof ChangeRequestSCMHead) {
                // TODO evaluate if there's a better way to snag pull requests
                // Keep in mind that the pull request could be:
                // a) coming from the same repo (easy)
                // b) coming from a fork (why we defaulted to this...)
                SCM scm = scmSource.build(head, tip);
                listener.getLogger().println("Checking out " + scm.getKey() + " to read " + scriptPath);
                FilePath dir;
                Node node = Jenkins.getInstanceOrNull();
                if (node == null) {
                    throw new IOException("Unable to communicate with Jenkins node");
                }
                FilePath baseWorkspace = node.getWorkspaceFor(build.getParent());
                if (baseWorkspace == null) {
                    throw new IOException(node.getDisplayName() + " may be offline");
                }
                dir = baseWorkspace.withSuffix(
                        System.getProperty(WorkspaceList.class.getName(), "@") + "script");
                Computer computer = node.toComputer();
                if (computer == null) {
                    throw new IOException(node.getDisplayName() + " may be offline");
                }
                SCMStep delegate = new GenericSCMStep(scm);
                delegate.setPoll(true);
                delegate.setChangelog(true);
                try (WorkspaceList.Lease lease = computer.getWorkspaceList().acquire(dir)) {
                    delegate.checkout(build, dir, listener, node.createLauncher(listener));
                    FilePath scriptFile = dir.child(scriptPath);
                    if (!scriptFile.absolutize().getRemote().replace('\\', '/').startsWith(dir.absolutize().getRemote().replace('\\', '/') + '/')) { // TODO JENKINS-26838
                        throw new IOException(scriptFile + " is not inside " + dir);
                    }
                    if (!scriptFile.exists()) {
                        throw new AbortException(scriptFile + " not found");
                    }
                    configContents = scriptFile.readToString();
                }
            } else {
                try (SCMFileSystem fs = SCMFileSystem.of(scmSource, head, tip)) {
                    if (fs != null) { // JENKINS-33273
                        try {
                            configContents = fs.child(scriptPath).contentAsString();
                            listener.getLogger().println("Obtained " + scriptPath);
                        } catch (IOException | InterruptedException x) {
                            throw new AbortException(String.format("Could not do lightweight checkout, %n%s",
                                    Functions.printThrowable(x).trim()));
                        }

                    } else {
                        // TODO: Evaluate if this is necessary...
                        // PRs were getting here because they're not trusted revisions but we're checking
                        // for PRs above... Are there other possible scenarios or should we just bail?
                        throw new AbortException("Could not do a lightweight checkout and retrieve an SCMFileSystem");
                    }
                }
            }
            if (configContents == null) {
                String pipelineTemplateNotFound =
                        String.format("Could not find a value for %s in %s", PIPELINE_TEMPLATE, scriptPath);
                throw new AbortException(pipelineTemplateNotFound);
            } else {
                String jenkinsfilePathString =
                        ConfigurationValueFinder.findFirstConfigurationValue(configContents,
                                ConfigDrivenWorkflowBranchProjectFactory.PIPELINE_TEMPLATE);

                build.addAction(new ConfigFileEnvironmentContributingAction(configContents));

                try (SCMFileSystem scriptFileSystem = SCMFileSystem.of(job, jenkinsFileScm)) {
                    if (scriptFileSystem != null) {
                        script = scriptFileSystem.child(jenkinsfilePathString).contentAsString();
                        listener.getLogger().println("Obtained " + jenkinsfilePathString);

                    }

                } catch (FileNotFoundException exception) {
                    throw new AbortException(String.format("Could not find file %s", jenkinsfilePathString));
                }
            }

            if (script != null) {
                return new CpsFlowDefinition(script, true).create(handle, listener, actions);
            }
        }
        // TODO evaluate if there's something else we should be looking at for no `tip` or `script`
        throw new AbortException("Unable to properly load a script file.");
    }

    @Extension
    public static class DescriptorImpl extends FlowDefinitionDescriptor {

        @Override public String getDisplayName() {
            return "Pipeline script from " + Messages.ProjectRecognizer_DisplayName();
        }

    }

    /** Want to display this in the r/o configuration for a branch project, but not offer it on standalone jobs or in any other context. */
    @Extension
    public static class HideMeElsewhere extends DescriptorVisibilityFilter {

        @Override public boolean filter(Object context, Descriptor descriptor) {
            if (descriptor instanceof DescriptorImpl) {
                return context instanceof WorkflowJob && ((WorkflowJob) context).getParent() instanceof WorkflowMultiBranchProject;
            }
            return true;
        }

    }

}
