package edu.ucsf.orng.shindig.config;

import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.sample.oauth.SampleOAuthDataStore;

import com.google.inject.name.Names;

import edu.ucsf.orng.shindig.auth.OrngCrypterSecurityTokenCodec;
import edu.ucsf.orng.shindig.spi.OrngPersonService;
import edu.ucsf.orng.shindig.spi.OrngActivityService;
import edu.ucsf.orng.shindig.spi.OrngMessageService;
import edu.ucsf.orng.shindig.spi.OrngAppDataService;
import edu.ucsf.orng.shindig.spi.RdfJsonLDService;
import edu.ucsf.orng.shindig.spi.RdfService;

public class OrngPropertiesModule extends PropertiesModule implements OrngProperties {//SocialApiGuiceModule {
	
	private final static String PROPERTIES_SUFFIX = ".properties";	
	
	public OrngPropertiesModule() {
		super(System.getProperty(JsonContainerConfig.SHINDIGORNG_PATH) + PROPERTIES_SUFFIX);
		getProperties().setProperty("shindig.containers.default", "res://" + 
		this.getProperties().getProperty("orng.system").toLowerCase() + "-container" +
		(this.getProperties().getProperty("orng.systemDomain").toLowerCase().startsWith("https") ? "-https.js" : ".js"));		
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
	    bind(RdfService.class).to(RdfJsonLDService.class);	    	
		bind(PersonService.class).to(OrngPersonService.class);

        bind(OAuthDataStore.class).to(SampleOAuthDataStore.class);
	}
}
