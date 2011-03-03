package com.nirima;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;


public class AdaptivePluginPublisher extends Recorder implements Serializable {

    @DataBoundConstructor
    public AdaptivePluginPublisher()
    {}

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build,
                           Launcher launcher, final BuildListener listener)
        throws InterruptedException {

        AdaptivePluginProperty hbp = (AdaptivePluginProperty) build.getProject().getProperty(AdaptivePluginProperty.class);
        AdaptivePluginRun hr = hbp.getAdaptivePluginRun(build);
        return hr.runPublish(build, launcher, listener);





    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "AdaptivePlugin";  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
