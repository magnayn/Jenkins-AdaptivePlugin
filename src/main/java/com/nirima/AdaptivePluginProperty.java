package com.nirima;

import hudson.Extension;
import hudson.model.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 22/12/2010
 * Time: 11:55
 * To change this template use File | Settings | File Templates.
 */
@ExportedBean(defaultVisibility=2)
public class AdaptivePluginProperty extends JobProperty<AbstractProject<?, ?>> implements Action
{
    private final String script;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public AdaptivePluginProperty(String script) {
        this.script = script;
    }

    public String getScript() {
        return script;
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

    public AdaptivePluginRun getAdaptivePluginRun(AbstractBuild build)
    {
        AdaptivePluginRun hbRun = null;

        if( build != null )
            build.getAction(AdaptivePluginRun.class);

        if( hbRun == null )
        {
            hbRun = new AdaptivePluginRun();
            hbRun.setScript(script);

            if( build != null )
                build.addAction(hbRun);
        }
        return hbRun;
    }

     @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
//		@Override
//		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
//            formData = formData.getJSONObject("useProjectSecurity");
//            if (formData.isNullObject())
//                return null;
//
//			return new AdaptivePluginProperty();
//		}

//		@Override
//		public boolean isApplicable(Class<? extends Job> jobType) {
//            // only applicable when ProjectMatrixAuthorizationStrategy is in charge
//            return Hudson.getInstance().getAuthorizationStrategy() instanceof ProjectMatrixAuthorizationStrategy;
//		}

		@Override
		public String getDisplayName() {
			return "AdaptivePlugin";
		}


    }
}
