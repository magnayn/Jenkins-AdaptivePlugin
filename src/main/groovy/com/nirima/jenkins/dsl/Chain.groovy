package com.nirima.jenkins.dsl

import hudson.model.AbstractProject

class Chain {
  def listener;
  def projects = [];
  Chain next;


  Chain(AbstractProject p, listener) {
    projects = [p];
    this.listener = listener;
  }

  Chain(ArrayList p, listener) {
    projects = p;
    this.listener = listener;
  }

  def rightShift(next) {
    if (!(next instanceof Chain))
      next = new Chain(next, listener);

    if (this.next != null) {
      this.next.rightShift(next);
    }
    else {
      this.next = next;
    }
    return this;
  }

  def buildAndWait(cause) {
    def futures = [];
    def results = [];
    projects.each() {proj ->
      futures += proj.scheduleBuild2(0, cause, []);
    }

    futures.each() { future ->
      results += future.get();
    }

    return results;
  }

  def poll() {

    def results = projects.collect() { project ->
      project.poll(listener);
    }

    if (next != null)
    results += next.poll(listener);

    // Order by severity
    results.sort() { item -> item.change.ordinal  }

    // Return worst..
    return results.last();

  }

  def hasChanges() {
    boolean success = false;

    poll(listener).each() { res -> if (res.hasChanges) success = true; }

    return success;
  }

  def String toString() {
    "$projects >> $next"
  }

}