package edu.ucsf.orng.shindig.service;

import static org.apache.shindig.auth.AbstractSecurityToken.Keys.APP_URL;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.CONTAINER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.OWNER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.VIEWER;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;

import edu.ucsf.orng.shindig.auth.OrngSecurityTokenService;
import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.rdf.RdfService;

/**
 * RPC/REST handler for all /orng requests
 */
@Service(name = "orng", path = "/{userId}+/{groupId}/{personId}+")
public class OrngAppHandler implements OrngProperties {

	private final RdfService rdfService;
	private OrngSecurityTokenService securityTokenService = null;
	
	@Inject
	public OrngAppHandler(RdfService rdfService, OrngSecurityTokenService securityTokenService) {
		this.rdfService = rdfService;
		this.securityTokenService = securityTokenService;
	}

	/**
	 * Allowed end-points /jsonld/{userId}+/{groupId}
	 * /people/{userId}/{groupId}/{optionalPersonId}+
	 * 
	 * examples: /jsonld/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
	 */
	@Operation(httpMethods = "GET")
	public Future<String> get(SocialRequestItem request) throws ProtocolException {
	    Set<UserId> userIds = request.getUsers();

	    // Preconditions
	    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
	    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

	    return Futures.immediateFuture(rdfService.getAppInstance(userIds.iterator().next(), request.getAppId(), request.getToken()));
	}

	/**
	 * Allowed end-points /jsonld/{userId}+/{groupId}
	 * /people/{userId}/{groupId}/{optionalPersonId}+
	 * 
	 * examples: /jsonld/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
	 */
	@Operation(httpMethods = "POST")
	public Future<Boolean> add(SocialRequestItem request) throws ProtocolException {
	    Set<UserId> userIds = request.getUsers();

	    // Preconditions
	    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
	    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

	    return Futures.immediateFuture(rdfService.addAppToPerson(userIds.iterator().next(), request.getAppId(), request.getToken()));
	}

	/**
	 * Allowed end-points /jsonld/{userId}+/{groupId}
	 * /people/{userId}/{groupId}/{optionalPersonId}+
	 * 
	 * examples: /jsonld/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
	 */
	@Operation(httpMethods = "DELETE")
	public Future<Boolean> delete(SocialRequestItem request) throws ProtocolException {
	    Set<UserId> userIds = request.getUsers();

	    // Preconditions
	    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
	    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

	    return Futures.immediateFuture(rdfService.removeAppFromPerson(userIds.iterator().next(), request.getAppId(), request.getParameter("deleteType"), request.getToken()));
	}

	@Operation(httpMethods = "GET")
	public Future<String> refreshContainerToken(SocialRequestItem request) throws ProtocolException, SecurityTokenException {
	    Set<UserId> userIds = request.getUsers();

	    // Preconditions
	    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
	    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");
	    
	    //request.getToken().
		Map<String, String> tokenParameters = Maps.newHashMap();
		tokenParameters.put(CONTAINER.getKey(), request.getToken().getContainer());
		tokenParameters.put(OWNER.getKey(), request.getToken().getOwnerId());
		tokenParameters.put(VIEWER.getKey(), request.getToken().getViewerId());

	    //return Futures.immediateFuture(rdfService.getAppInstance(userIds.iterator().next(), request.getAppId(), request.getToken()));
	    return Futures.immediateFuture(securityTokenService.convert(tokenParameters));
	}
}
