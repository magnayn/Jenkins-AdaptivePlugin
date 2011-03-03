package com.nirima.jenkins.dsl

import hudson.model.Result


class BuildResult {
  def results = [];

  def getResult()
  {
    def result = Result.SUCCESS;

    results.each() { res -> if( res.getResult() != Result.SUCCESS) result = res; }

    return result;
  }

}
