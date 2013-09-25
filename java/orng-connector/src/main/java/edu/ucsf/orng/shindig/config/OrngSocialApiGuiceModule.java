package edu.ucsf.orng.shindig.config;

import java.util.Set;

import org.apache.shindig.social.core.config.SocialApiGuiceModule;
import org.apache.shindig.social.opensocial.service.ActivityHandler;
import org.apache.shindig.social.opensocial.service.AppDataHandler;
import org.apache.shindig.social.opensocial.service.MessageHandler;
import org.apache.shindig.social.opensocial.service.PersonHandler;
import edu.ucsf.orng.shindig.service.RdfHandler;

import com.google.common.collect.ImmutableSet;

public class OrngSocialApiGuiceModule extends SocialApiGuiceModule {

	  /**
	   * Hook to provide a Set of request handlers.  Subclasses may override
	   * to add or replace additional handlers.
	   */
	  protected Set<Class<?>> getHandlers() {
	    return ImmutableSet.<Class<?>>of(ActivityHandler.class, AppDataHandler.class,
	        PersonHandler.class, MessageHandler.class, RdfHandler.class);
	  }
}
