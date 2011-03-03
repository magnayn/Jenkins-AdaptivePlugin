package com.nirima;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class AdaptivePluginSCM extends SCM implements Serializable {



    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return getAdaptivePluginRun(build).runCalcRevisionsFromBuild(build, launcher, listener);
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {

        return getAdaptivePluginRun(project).runCompareRemoteRevisionWith(project, launcher, workspace, listener, baseline);
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {

        return getAdaptivePluginRun(build).runCheckout(build, launcher, workspace, listener, changelogFile);
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new NullChangeLogParser();  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected AdaptivePluginRun getAdaptivePluginRun(AbstractBuild<?, ?> build) {
        AdaptivePluginProperty hbp = build.getProject().getProperty(AdaptivePluginProperty.class);
        AdaptivePluginRun hr = hbp.getAdaptivePluginRun(build);
        return hr;
    }

    protected AdaptivePluginRun getAdaptivePluginRun(AbstractProject<?, ?> project) {
        AdaptivePluginProperty hbp = project.getProperty(AdaptivePluginProperty.class);
        AdaptivePluginRun hr = hbp.getAdaptivePluginRun(null);
        return hr;
    }


    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<AdaptivePluginSCM> {
        public DescriptorImpl() {
            super(AdaptivePluginSCM.class, AdaptivePluginSCMBrowser.class);
        }

        @Override
        public String getDisplayName() {
            return "AdaptivePlugin";
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new AdaptivePluginSCM();
        }

    }

}
