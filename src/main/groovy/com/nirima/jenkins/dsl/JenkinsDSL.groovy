package com.nirima.jenkins.dsl

import hudson.model.AbstractBuild

import hudson.Launcher

import hudson.model.Hudson
import hudson.tasks.Publisher

import net.sf.json.JSONObject
import org.kohsuke.stapler.StaplerRequest

import com.nirima.stapler.FakeStapler
import hudson.tasks.BuildStep

import hudson.tasks.Builder
import hudson.model.AbstractProject

import hudson.scm.SCM
import hudson.scm.PollingResult
import hudson.scm.SCMRevisionState

import com.nirima.AdaptivePluginBuilder

import net.sf.json.JsonConfig
import com.nirima.json.JsonMapProcessor

class JenkinsDSL implements Serializable {
  ScriptDelegate sd;
  def scriptRun;


  public JenkinsDSL(listener) {

    sd = new ScriptDelegate(listener);
  }

  public void initScript(String script) {

    Binding binding = new Binding();

    ClassLoader parent = AdaptivePluginBuilder.class.getClassLoader();

    if (sd.listener != null) {
      binding.setProperty("out", new PrintWriter(sd.listener.getLogger(), true));
      binding.setProperty("pluginClassLoader", parent);
      binding.setProperty("ant", new AntBuilder());
    }

    Script dslScript = new GroovyShell(parent, binding).parse(script);

    dslScript.metaClass = createEMC(dslScript.class,
            {
              ExpandoMetaClass emc ->

              emc.jenkins = {
                Closure cl ->

                cl.delegate = sd
                cl.resolveStrategy = Closure.DELEGATE_FIRST

                cl()

                return sd;
              }

              emc.getProperty = { String name ->

                if (sd.build != null) {
                  return sd.build.getEnvironment().get(name);
                }
                else {
                  return name;
                }
              }


            })





    scriptRun = dslScript.run();
  }

  static ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
    ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false);
    cl(emc)
    emc.initialize()
    return emc
  }

  public boolean build(AbstractBuild build, Launcher launcher) {
    sd.build = build;
    sd.launcher = launcher;
    return scriptRun.runBuild();
  }

  public boolean publish(AbstractBuild<?, ?> build, Launcher launcher) {
    sd.build = build;
    sd.launcher = launcher;
    return scriptRun.runPublish();
  }

  // SCM:

  public SCMRevisionState runCalcRevisionsFromBuild(build, launcher) {
    sd.build = build;
    sd.launcher = launcher;

    scriptRun.calcRevisions();
  }

  public PollingResult runCompareRemoteRevisionWith(project, launcher, workspace) {
    sd.project = project;
    sd.launcher = launcher;
    sd.workspace = workspace;

    return scriptRun.compareRemoteRevision();
  }

  public boolean runCheckout(build, launcher, workspace, changelogFile) {
    sd.build = build;
    sd.launcher = launcher;
    sd.workspace = workspace;
    sd.changelogFile = changelogFile;
    return scriptRun.runCheckout();
  }

}

class ScriptDelegate implements Serializable {
  /** Context  **/
  AbstractBuild build;
  def listener;
  Launcher launcher;
  def project;
  def workspace;
  def changelogFile;

  def pollAction;
  def checkoutAction;
  def calcRevisionsAction;
  def buildActions = [];
  def publishActions = [];

  public ScriptDelegate(listener) {
    this.listener = listener;

    AbstractProject.metaClass.rightShift << { b ->  new Chain(delegate, listener) >> b }
  }

  // Setup ---------------------------------------

  def scm(Closure c) {
    c.getMetaClass().poll << { __it -> pollAction = __it; }
    c.getMetaClass().checkout << { __it -> checkoutAction = __it; }
    c.getMetaClass().calcRevisions << { __it -> calcRevisionsAction = __it; }

    c();
  }

  def build(Closure c) {
    buildActions += c;
  }

  def publish(Closure c) {
    publishActions += c;
  }

  // Runners ---------------------------------------

  def runBuild() {
    for (__it in buildActions) {
      try {
        if (__it() == false)
        return false;
      }
      catch (Exception ex) {
        println ex;
        return false;
      }
    }

    return true;
  }

  def runPublish() {
    publishActions.each() { __it -> __it(); }
    return true;
  }

  def runCheckout() {
    if (checkoutAction != null)
    return checkoutAction();

    return true;
  }

  SCMRevisionState calcRevisions() {
    if (calcRevisionsAction != null)
    return calcRevisionsAction();

    return SCMRevisionState.NONE;
  }

  PollingResult compareRemoteRevision() {
    if (pollAction != null)
    return pollAction();

    return PollingResult.NO_CHANGES;
  }

  // Helpers ---------------------------------------

  def getJenkins() {
    return Hudson.getInstance();
  }

  /*
  def getCall() {
    def cd = new CallDelegate(this);
    return cd;
  }
  */

  def project(String name) {
    Hudson.getInstance().getAllItems(AbstractProject.class).find() { proj -> proj.name.equals(name)  }
  }

  def build(Object o) {
    return buildProcess(o).buildAndWait();
  }

  def poll(Object o) {
    if (o instanceof SCM) {
      return o.pollChanges(project, launcher, workspace, listener);
    }
    else {
      return buildProcess(o).poll();
    }
  }

  def checkout(Object o) {
    if (o instanceof SCM) {
      return ((SCM) o).checkout(build, launcher, workspace, listener, changelogFile);
    }
  }

  def buildProcess(o) {
    if (o instanceof AbstractProject) {
      o = new Chain(o, listener);
    }

    BuildProcess bp = new BuildProcess(build, o);
  }

  def inTransaction(Closure c) {
    println("hello inTXN");

    c();
  }

  def invokeMethod(String name, args) {
    println "create object $name";

    name = name.toLowerCase();

    def list = [];

    list.addAll(Hudson.getInstance().getDescriptorList(Publisher));
    list.addAll(Hudson.getInstance().getDescriptorList(Builder));
    list.addAll(Hudson.getInstance().getDescriptorList(SCM));

    def best;

    list.each() { it -> String cname = it.getClass().getName().toLowerCase(); if (cname.contains(name)) best = it; };

    if (best == null) {
      println "No object found for $name";
      return;
    }

    StaplerRequest req = new FakeStapler(args[0], Hudson.getInstance().getPluginManager().uberClassLoader);
    JSONObject formData = new JSONObject();
    JsonConfig jsonConfig = new JsonConfig();

    jsonConfig.typeMap.put(LinkedHashMap, new JsonMapProcessor());

    formData.putAll(args[0], jsonConfig);

    try
    {
        def inst = best.newInstance(req, formData);
        return inst;
    }
    catch(RuntimeException ex)
    {
        println "Failed to create object $name";
        ex.printStackTrace();
        throw ex;
    }

  }

  def invoke(inst) {

    if (inst instanceof BuildStep) {
      return inst.perform((AbstractBuild) build, launcher, listener);
    }
    else {
      return inst;
    }
  }

}

/*
class CallDelegate
{
  ScriptDelegate scriptDelegate;

  CallDelegate(ScriptDelegate sd)
  {
    this.scriptDelegate = sd;
  }

   def createInstance(String name, args) {
    println "create object $name";

    name = name.toLowerCase();

    def list = [];

    list.addAll( Hudson.getInstance().getDescriptorList(Publisher) );
    list.addAll( Hudson.getInstance().getDescriptorList(Builder) );
    list.addAll( Hudson.getInstance().getDescriptorList(SCM) );

    def best;

    list.each() { it -> String cname = it.getClass().getName().toLowerCase(); if (cname.contains(name)) best = it; };

    if( best == null )
    {
      println "No object found for $name";
      return;
    }

    StaplerRequest req = new FakeStapler(args[0]);
    JSONObject formData = new JSONObject();
    JsonConfig jsonConfig = new JsonConfig();

    jsonConfig.typeMap.put(LinkedHashMap, new JsonMapProcessor() );

    formData.putAll(args[0],jsonConfig);

    def inst = best.newInstance(req, formData);

    return inst;
  }

  def invokeMethod(String name, args) {
    println "call function $name";

    def best = createInstance(name, args);

    if( inst instanceof BuildStep )
    {
      return inst.perform( (AbstractBuild)scriptDelegate.build, scriptDelegate.launcher, scriptDelegate.listener);
    }
    else
    {
      return inst;
    }
  }


}
*/