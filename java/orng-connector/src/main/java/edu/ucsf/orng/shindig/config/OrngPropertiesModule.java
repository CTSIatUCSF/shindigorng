package edu.ucsf.orng.shindig.config;

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

import edu.ucsf.orng.shindig.service.SecureTokenGeneratorService;
import edu.ucsf.orng.shindig.spi.OrngUtil;
import edu.ucsf.orng.shindig.spi.OrngActivityService;
import edu.ucsf.orng.shindig.spi.OrngMessageService;
import edu.ucsf.orng.shindig.spi.OrngAppDataService;
import edu.ucsf.orng.shindig.spi.RdfBabelService;
import edu.ucsf.orng.shindig.spi.RdfService;
import edu.ucsf.orng.shindig.spi.vivo.VIVOPersonService;
import edu.ucsf.orng.shindig.spi.profiles.ProfilesPersonService;

public class OrngPropertiesModule extends PropertiesModule {//SocialApiGuiceModule {
	
	private final static String DEFAULT_PROPERTIES = "shindig.orng.properties";
	
	SecureTokenGeneratorService service = null;
	
	public OrngPropertiesModule() {
		super(DEFAULT_PROPERTIES);
	}

	@Override
	protected void configure() {
		super.configure();
	    
		bind(String.class).annotatedWith(Names.named("shindig.canonical.json.db"))
			.toInstance("sampledata/canonicaldb.json");
	    try {
	    	Class.forName(getProperties().getProperty("orng.dbDriver"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	    requestStaticInjection(OrngUtil.class);

	    bind(ActivityService.class).to(OrngActivityService.class);
        bind(MessageService.class).to(OrngMessageService.class);
	    bind(AppDataService.class).to(OrngAppDataService.class);
	    bind(RdfService.class).to(RdfBabelService.class);

		String orngSystem = getProperties().getProperty("orng.system");		
		if ("Profiles".equalsIgnoreCase(orngSystem)) {
			bind(PersonService.class).to(ProfilesPersonService.class);
		}
		else if ("VIVO".equalsIgnoreCase(orngSystem)) {
			bind(PersonService.class).to(VIVOPersonService.class);
		}
		else {
			throw new RuntimeException("orng.system not set properly. Needs to be Profiles or VIVO, is :" + orngSystem);
		}

        // Note from Eric Meeks.  We do not have this yet
		bind(MediaItemService.class).to(JsonDbOpensocialService.class);
        bind(OAuthDataStore.class).to(SampleOAuthDataStore.class);
        
        // start token service thread
        // pass in port now because waiting for injection would be too late
		service = new SecureTokenGeneratorService(Integer.parseInt(getProperties().getProperty("orng.tokenservice.port")));
		Thread thread = new Thread(service);
		thread.setDaemon(true);
		thread.start();
		requestInjection(service);
	}

	SecureTokenGeneratorService getSecureTokenGeneratorService(SecureTokenGeneratorService service) {
		return service;
	}
	
}
