package edu.ucsf.orng.shindig.service;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;

import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.rdf.RdfService;

/**
 * RPC/REST handler for all /orng requests
 */
@Service(name = "orng", path = "/{userId}+/{groupId}/{personId}+")
public class OrngAppHandler implements OrngProperties {

	private final RdfService rdfService;
	
	@Inject
	public OrngAppHandler(RdfService rdfService) {
		this.rdfService = rdfService;
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

}
