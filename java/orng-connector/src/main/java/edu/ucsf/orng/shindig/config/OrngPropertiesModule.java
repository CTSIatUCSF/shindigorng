package edu.ucsf.orng.shindig.config;

import org.apache.shindig.auth.SecurityTokenCodec;
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

import edu.ucsf.orng.shindig.auth.OrngCrypterSecurityTokenCodec;
import edu.ucsf.orng.shindig.spi.OrngPersonService;
import edu.ucsf.orng.shindig.spi.OrngDBUtil;
import edu.ucsf.orng.shindig.spi.OrngActivityService;
import edu.ucsf.orng.shindig.spi.OrngMessageService;
import edu.ucsf.orng.shindig.spi.OrngAppDataService;
import edu.ucsf.orng.shindig.spi.RdfBabelService;
import edu.ucsf.orng.shindig.spi.RdfEldaService;
import edu.ucsf.orng.shindig.spi.RdfService;

public class OrngPropertiesModule extends PropertiesModule implements OrngProperties {//SocialApiGuiceModule {
	
	private final static String DEFAULT_PROPERTIES = "shindigorng.properties";	
	
	public OrngPropertiesModule() {
		super(DEFAULT_PROPERTIES);
		getProperties().setProperty("shindig.containers.default", "res://" + this.getProperties().getProperty("orng.system").toLowerCase() + "-container.js");		
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
	    
		String orngSystem = getProperties().getProperty("orng.system");		
		if (!PROFILES.equalsIgnoreCase(orngSystem) && !VIVO.equalsIgnoreCase(orngSystem)) {
			throw new RuntimeException("orng.system not set properly. Needs to be Profiles or VIVO, is :" + orngSystem);
		}
	    
	    bind(SecurityTokenCodec.class).to(OrngCrypterSecurityTokenCodec.class);
	    bind(ActivityService.class).to(OrngActivityService.class);
        bind(MessageService.class).to(OrngMessageService.class);
	    bind(AppDataService.class).to(OrngAppDataService.class);
	    
		String rdfConverter = getProperties().getProperty("orng.RDFConverter");		
	    if ("babel".equalsIgnoreCase(rdfConverter)) {
	    	bind(RdfService.class).to(RdfBabelService.class);
	    } 
	    else if ("elda".equalsIgnoreCase(rdfConverter)) {
	    	bind(RdfService.class).to(RdfEldaService.class);	    	
	    }
		else {
			throw new RuntimeException("orng.RDFConverter not set properly. Needs to be babel or elda, is :" + rdfConverter);
		}
		bind(PersonService.class).to(OrngPersonService.class);

        // Note from Eric Meeks.  We do not use this yet
		bind(MediaItemService.class).to(JsonDbOpensocialService.class);
        bind(OAuthDataStore.class).to(SampleOAuthDataStore.class);
        
        // start token service thread
        // pass in port now because waiting for injection would be too late
        /*
		service = new SecureTokenGeneratorService(Integer.parseInt(getProperties().getProperty("orng.tokenservice.port")));
		Thread thread = new Thread(service);
		thread.setDaemon(true);
		thread.start();
		requestInjection(service); */
	}

}
