package edu.ucsf.profiles.shindig.config;

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.MediaItemService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.sample.oauth.SampleOAuthDataStore;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.name.Names;

import edu.ucsf.profiles.shindig.service.SecureTokenGeneratorService;
import edu.ucsf.profiles.shindig.spi.Common;
import edu.ucsf.profiles.shindig.spi.ProfilesActivityService;
import edu.ucsf.profiles.shindig.spi.ProfilesAppDataService;
import edu.ucsf.profiles.shindig.spi.ProfilesMessageService;
import edu.ucsf.profiles.shindig.spi.ProfilesPersonService;

public class ProfilesModule extends PropertiesModule {//SocialApiGuiceModule {
	
	private final static String DEFAULT_PROPERTIES = "profiles.shindig.properties";
	
	SecureTokenGeneratorService service = null;
	
	public ProfilesModule() {
		super(DEFAULT_PROPERTIES);
	}

	@Override
	protected void configure() {
		super.configure();
	    
		bind(String.class).annotatedWith(Names.named("shindig.canonical.json.db"))
	        .toInstance("sampledata/canonicaldb.json");

	    bind(ActivityService.class).to(ProfilesActivityService.class);
	    bind(AppDataService.class).to(ProfilesAppDataService.class);
		bind(PersonService.class).to(ProfilesPersonService.class);
        bind(MessageService.class).to(ProfilesMessageService.class);

        bind(MediaItemService.class).to(JsonDbOpensocialService.class);
        bind(OAuthDataStore.class).to(SampleOAuthDataStore.class);
        
        // start token service thread
        // pass in port now because waiting for injection would be too late
		service = new SecureTokenGeneratorService(Integer.parseInt(getProperties().getProperty("profiles.tokenservice.port")));
		Thread thread = new Thread(service);
		thread.setDaemon(true);
		thread.start();
		requestInjection(service);
	}

	SecureTokenGeneratorService getSecureTokenGeneratorService(SecureTokenGeneratorService service) {
		return service;
	}
	
}
