package com.nirima;

import hudson.model.User;
import hudson.scm.ChangeLogSet;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 22/12/2010
 * Time: 11:02
 * To change this template use File | Settings | File Templates.
 */
public class AdaptivePluginSCMChangeSet extends ChangeLogSet.Entry {
    @Override
    public String getMsg() {
        return "msg";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public User getAuthor() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getAffectedPaths() {
        return Collections.EMPTY_SET;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
