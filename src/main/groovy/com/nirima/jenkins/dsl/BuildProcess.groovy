package com.nirima.jenkins.dsl

import hudson.model.Cause
import hudson.model.AbstractBuild
import hudson.model.Run

import hudson.model.Result;

class BuildProcess
{
  Chain chain;
  AbstractBuild build;

  public BuildProcess(build, Chain c)
  {
    this.chain = c;
    this.build = build;
  }

  def buildAndWait()
  {
     def cause = new Cause.UpstreamCause((Run)build);
     def result = new BuildResult();

     while(chain != null)
     {
       def results = chain.buildAndWait(cause);
       // results some collection of AbstractBuild

       result.results += results;

       // Early escape..
       if( !result.getResult() != Result.SUCCESS )
          return result;

       chain = chain.next;
     }

    return result;
  }

  def poll()
  {
    return chain.poll();
  }

}