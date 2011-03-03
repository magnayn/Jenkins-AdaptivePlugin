package com.nirima;

import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 22/12/2010
 * Time: 11:00
 * To change this template use File | Settings | File Templates.
 */
public class AdaptivePluginSCMBrowser extends RepositoryBrowser<AdaptivePluginSCMChangeSet> {
    @Override
    public URL getChangeSetLink(AdaptivePluginSCMChangeSet changeSet) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
