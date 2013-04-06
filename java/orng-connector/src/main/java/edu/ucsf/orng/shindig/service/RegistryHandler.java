/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package edu.ucsf.orng.shindig.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.inject.Inject;

import edu.ucsf.orng.shindig.spi.OrngRegistryService;

/**
 * RPC/REST handler for all /people requests
 */
@Service(name = "registry", path = "/{userId}+/{groupId}/{personId}+")
public class RegistryHandler {

	private final OrngRegistryService service;

	@Inject
	public RegistryHandler(OrngRegistryService registryService, ContainerConfig config) {
		this.service = registryService;
	}

	  @Operation(httpMethods = "DELETE")
	  public Future<?> delete(SocialRequestItem request)
	      throws ProtocolException {

	    Set<UserId> userIds = request.getUsers();

	    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
	    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

	    return service.registerAppPerson(userIds.iterator().next(), request.getGroup(),
	        request.getAppId(), request.getToken(), false);
	  }

	  /**
	   * Allowed endpoints /appdata/{userId}/{groupId}/{appId} - fields={field1, field2}
	   *
	   * examples: /appdata/john.doe/@friends/app?fields=count /appdata/john.doe/@self/app
	   *
	   * The post data should be a regular json object. All of the fields vars will be pulled from the
	   * values and set on the person object. If there are no fields vars then all of the data will be
	   * overridden.
	   */
	  @Operation(httpMethods = "PUT", bodyParam = "visible")
	  public Future<?> update(SocialRequestItem request) throws ProtocolException {
	    return create(request);
	  }

	  /**
	   * /appdata/{userId}/{groupId}/{appId} - fields={field1, field2}
	   *
	   * examples: /appdata/john.doe/@friends/app?fields=count /appdata/john.doe/@self/app
	   *
	   * The post data should be a regular json object. All of the fields vars will be pulled from the
	   * values and set. If there are no fields vars then all of the data will be overridden.
	   */
	  @Operation(httpMethods = "POST", bodyParam = "visible")
	  public Future<?> create(SocialRequestItem request) throws ProtocolException {
	    Set<UserId> userIds = request.getUsers();

	    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
	    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

	    return service.registerAppPerson(userIds.iterator().next(), request.getGroup(),
	        request.getAppId(), request.getToken(), Boolean.valueOf(request.getParameter("visible")));
	  }

	/**
	 * Allowed end-points /rdf/{userId}+/{groupId}
	 * /people/{userId}/{groupId}/{optionalPersonId}+
	 * 
	 * examples: /rdf/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
	 */
	@Operation(httpMethods = "GET")
	public Future<?> get(SocialRequestItem request) throws ProtocolException {
		GroupId groupId = request.getGroup();
		return service.getRegistry(request.getUsers(), groupId, request.getAppId(), request.getToken());
	}

}
