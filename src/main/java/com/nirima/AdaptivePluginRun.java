package com.nirima;

import com.nirima.jenkins.dsl.JenkinsDSL;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 22/12/2010
 * Time: 11:15
 * To change this template use File | Settings | File Templates.
 */
public class AdaptivePluginRun implements Action {

    transient JenkinsDSL dsl;
    String script;


    public AdaptivePluginRun()
    {

    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    protected JenkinsDSL getDSL(TaskListener listener)
    {
        if( dsl == null )
        {
            dsl = new JenkinsDSL(listener);
            dsl.initScript(script);
        }

        return dsl;
    }

    public String getIconFileName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDisplayName() {
        return "AdaptivePlugin";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUrlName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean runBuilder(AbstractBuild build, Launcher launcher, BuildListener listener) {
        JenkinsDSL dsl = getDSL(listener);
        return dsl.build(build, launcher);
    }

    public boolean runPublish(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        JenkinsDSL dsl = getDSL(listener);
        return dsl.publish(build, launcher);
    }


    public boolean runCheckout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) {
        JenkinsDSL dsl = getDSL(listener);
        return dsl.runCheckout(build, launcher, workspace, changelogFile);
    }

    public SCMRevisionState runCalcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) {
        JenkinsDSL dsl = getDSL(listener);
        return dsl.runCalcRevisionsFromBuild(build, launcher);
    }

    public PollingResult runCompareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) {
        JenkinsDSL dsl = getDSL(listener);
        return dsl.runCompareRemoteRevisionWith(project, launcher, workspace);
    }
}
