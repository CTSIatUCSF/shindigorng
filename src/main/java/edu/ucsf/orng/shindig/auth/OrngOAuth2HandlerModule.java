package edu.ucsf.orng.shindig.auth;

import java.util.List;

import org.apache.shindig.gadgets.oauth2.handler.AuthorizationEndpointResponseHandler;
import org.apache.shindig.gadgets.oauth2.handler.BasicAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.BearerTokenHandler;
import org.apache.shindig.gadgets.oauth2.handler.ClientAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.ClientCredentialsGrantTypeHandler;
import org.apache.shindig.gadgets.oauth2.handler.CodeAuthorizationResponseHandler;
import org.apache.shindig.gadgets.oauth2.handler.CodeGrantTypeHandler;
import org.apache.shindig.gadgets.oauth2.handler.GrantRequestHandler;
import org.apache.shindig.gadgets.oauth2.handler.MacTokenHandler;
import org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerModule;
import org.apache.shindig.gadgets.oauth2.handler.ResourceRequestHandler;
import org.apache.shindig.gadgets.oauth2.handler.StandardAuthenticationHandler;
import org.apache.shindig.gadgets.oauth2.handler.TokenAuthorizationResponseHandler;
import org.apache.shindig.gadgets.oauth2.handler.TokenEndpointResponseHandler;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

// copy from OAuth2HandlerModule with JSTGrantTypeHandler added
public class OrngOAuth2HandlerModule extends AbstractModule {

	  private static final FilteredLogger LOG = FilteredLogger
	          .getFilteredLogger(OAuth2HandlerModule.class.getName());

	  @Override
	  protected void configure() {
	    if (OrngOAuth2HandlerModule.LOG.isLoggable()) {
	    	OrngOAuth2HandlerModule.LOG.entering(OAuth2HandlerModule.class.getName(), "configure");
	    }
	  }

	  @Provides
	  @Singleton
	  public static List<AuthorizationEndpointResponseHandler> provideAuthorizationEndpointResponseHandlers(
	          final CodeAuthorizationResponseHandler codeAuthorizationResponseHandler) {
	    return ImmutableList
	            .of((AuthorizationEndpointResponseHandler) codeAuthorizationResponseHandler);
	  }

	  @Provides
	  @Singleton
	  public static List<ClientAuthenticationHandler> provideClientAuthenticationHandlers(
	          final BasicAuthenticationHandler basicAuthenticationHandler,
	          final StandardAuthenticationHandler standardAuthenticationHandler) {
	    return ImmutableList.of(basicAuthenticationHandler, standardAuthenticationHandler);
	  }

	  @Provides
	  @Singleton
	  public static List<GrantRequestHandler> provideGrantRequestHandlers(
	          final ClientCredentialsGrantTypeHandler clientCredentialsGrantTypeHandler,
	          final CodeGrantTypeHandler codeGrantTypeHandler,
	          final JWTGrantTypeHandler jwtGrantTypeHandler) {
	    return ImmutableList.of(clientCredentialsGrantTypeHandler, codeGrantTypeHandler, jwtGrantTypeHandler);
	  }

	  @Provides
	  @Singleton
	  public static List<TokenEndpointResponseHandler> provideTokenEndpointResponseHandlers(
	          final TokenAuthorizationResponseHandler tokenAuthorizationResponseHandler) {
	    return ImmutableList.of((TokenEndpointResponseHandler) tokenAuthorizationResponseHandler);
	  }

	  @Provides
	  @Singleton
	  public static List<ResourceRequestHandler> provideTokenHandlers(
	          final BearerTokenHandler bearerTokenHandler, final MacTokenHandler macTokenHandler) {
	    return ImmutableList.of(bearerTokenHandler, macTokenHandler);
	  }
	
}
