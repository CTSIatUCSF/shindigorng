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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONObject;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.ucsf.orng.shindig.spi.rdf.RdfService;

/**
 * RPC/REST handler for all /people requests
 */
@Service(name = "rdf", path = "/{userId}+/{groupId}/{personId}+")
public class RdfHandler {

	private final RdfService rdfService;
	private final ContainerConfig config;

	@Inject
	public RdfHandler(RdfService rdfService, ContainerConfig config) {
		this.rdfService = rdfService;
		this.config = config;
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
		Set<String> optionalURI = ImmutableSet.copyOf(request
				.getListParameter("uri"));
		String containerSessionId = request.getParameter("containerSessionId");

		Set<String> uris = new HashSet<String>();
		uris.addAll(makeIdsIntoURIs(request.getUsers(), request.getToken()));
		uris.addAll(optionalURI);

		// Preconditions
		HandlerPreconditions.requireNotEmpty(uris, "No URI's specified");

		CollectionOptions options = new CollectionOptions(request);

		Set<String> fields = request.getFields();

		if (uris.size() == 1) {
			return getJSONItem(uris.iterator().next(), fields, containerSessionId, request.getToken());
		} 
		else {
			return getJSONItems(uris, fields, containerSessionId, groupId, options, request.getToken());
		}
	}

	@Operation(httpMethods = "GET", path = "/@supportedOntologies")
	public List<Object> supportedOntologies(RequestItem request) {
		// TODO: Would be nice if name in config matched name of service.
		String container = Objects.firstNonNull(request.getToken()
				.getContainer(), "default");
		return config.getList(container,
				"${Cur['gadgets.features'].opensocial.supportedOntologies}");
	}

	public Set<String> makeIdsIntoURIs(Set<UserId> userIds, SecurityToken token) {
		Set<String> urls = new HashSet<String>();
		for (UserId id : userIds) {
			String strId = id.getUserId(token);
			if (strId != null) {
				urls.add(strId);
			}
		}
		return urls;
	}
	
	/**
	 * Returns a list of people that correspond to the passed in person ids.
	 * 
	 * @param userIds
	 *            A set of users
	 * @param groupId
	 *            The group
	 * @param collectionOptions
	 *            How to filter, sort and paginate the collection being fetched
	 * @param fields
	 *            The profile details to fetch. Empty set implies all
	 * @param token
	 *            The gadget token @return a list of people.
	 * @return Future that returns a RestfulCollection of Person
	 */
	private Future<RestfulCollection<JSONObject>> getJSONItems(Set<String> uris, Set<String> fields,
			String containerSessionId, GroupId groupId,
			CollectionOptions collectionOptions, SecurityToken token)
			throws ProtocolException {
		// TODO Auto-generated method stub
		List<JSONObject> result = Lists.newArrayList();

		if (uris.size() == 0) {
			return Futures.immediateFuture(null);
		}
		for (String uri : uris) {
			try {
				result.add(rdfService.getRDF(uri, fields, containerSessionId, token));
			} catch (Exception e) {
				throw new ProtocolException(0, e.getMessage(), e);
			}
		}
		int firstResult = 0;
		if (collectionOptions != null) {
			firstResult = collectionOptions.getFirst();
		}
		return Futures.immediateFuture(new RestfulCollection<JSONObject>(
				result, firstResult, result.size()));
	}

	/**
	 * Returns a JSON object that corresponds to the passed in URI.
	 * 
	 * @param id
	 *            The id of the person to fetch.
	 * @param fields
	 *            The fields to fetch.
	 * @param token
	 *            The gadget token
	 * @return a list of people.
	 */
	private Future<JSONObject> getJSONItem(String uri, Set<String> fields, String containerSessionId, SecurityToken token)
			throws ProtocolException {
		try {
			return Futures.immediateFuture(rdfService.getRDF(uri, fields, containerSessionId, token));
		} catch (Exception e) {
			throw new ProtocolException(0, e.getMessage(), e);
		}
	}

}
