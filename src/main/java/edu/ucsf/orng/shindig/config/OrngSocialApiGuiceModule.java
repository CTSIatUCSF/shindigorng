package edu.ucsf.orng.shindig.config;

import java.util.Set;

import org.apache.shindig.social.core.config.SocialApiGuiceModule;
import org.apache.shindig.social.core.oauth2.OAuth2DataService;
import org.apache.shindig.social.core.oauth2.OAuth2DataServiceImpl;
import org.apache.shindig.social.core.oauth2.OAuth2Service;
import org.apache.shindig.social.core.oauth2.OAuth2ServiceImpl;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.service.ActivityHandler;
import org.apache.shindig.social.opensocial.service.AppDataHandler;
import org.apache.shindig.social.opensocial.service.MessageHandler;
import org.apache.shindig.social.opensocial.service.PersonHandler;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.ActivityStreamService;
import org.apache.shindig.social.opensocial.spi.AlbumService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupService;
import org.apache.shindig.social.opensocial.spi.MediaItemService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.sample.oauth.SampleOAuthDataStore;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import edu.ucsf.orng.shindig.auth.OrngSecurityTokenService;
import edu.ucsf.orng.shindig.service.RdfHandler;
import edu.ucsf.orng.shindig.spi.OrngActivityService;
import edu.ucsf.orng.shindig.spi.OrngAppDataService;
import edu.ucsf.orng.shindig.spi.OrngMessageService;
import edu.ucsf.orng.shindig.spi.OrngPersonService;
import edu.ucsf.orng.shindig.spi.rdf.JsonLDService;
import edu.ucsf.orng.shindig.spi.rdf.RdfService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.name.Names;

public class OrngSocialApiGuiceModule extends SocialApiGuiceModule {

	@Override
	protected void configure() {
		super.configure();
		bind(ActivityService.class).to(OrngActivityService.class);
		bind(MessageService.class).to(OrngMessageService.class);
		bind(AppDataService.class).to(OrngAppDataService.class);
		bind(RdfService.class).to(JsonLDService.class);
		bind(PersonService.class).to(OrngPersonService.class);

		bind(String.class).annotatedWith(
				Names.named("shindig.canonical.json.db")).toInstance(
				"sampledata/canonicaldb.json");
		bind(ActivityStreamService.class).to(JsonDbOpensocialService.class);
		bind(AlbumService.class).to(JsonDbOpensocialService.class);
		bind(MediaItemService.class).to(JsonDbOpensocialService.class);
//		bind(AppDataService.class).to(JsonDbOpensocialService.class);
//		bind(PersonService.class).to(JsonDbOpensocialService.class);
//		bind(MessageService.class).to(JsonDbOpensocialService.class);
		bind(GroupService.class).to(JsonDbOpensocialService.class);
		bind(OAuthDataStore.class).to(SampleOAuthDataStore.class);
		bind(OAuth2Service.class).to(OAuth2ServiceImpl.class);
		bind(OAuth2DataService.class).to(OAuth2DataServiceImpl.class);
		
		bind(OrngSecurityTokenService.class);
	}

	/**
	 * Hook to provide a Set of request handlers. Subclasses may override to add
	 * or replace additional handlers.
	 */
	protected Set<Class<?>> getHandlers() {
		return new ImmutableSet.Builder<Class<?>>().addAll(super.getHandlers())
				.add(RdfHandler.class).build();
	}
}
