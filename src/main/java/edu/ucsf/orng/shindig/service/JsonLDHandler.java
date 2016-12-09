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
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;

import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.rdf.OrngJsonLDService;
import edu.ucsf.orng.shindig.spi.rdf.RdfItem;
import edu.ucsf.orng.shindig.spi.rdf.RdfService;

/**
 * RPC/REST handler for all /jsonld requests
 */
@Service(name = "jsonld", path = "/{userId}+/{groupId}/{personId}+")
public class JsonLDHandler implements OrngProperties {

	private final RdfService rdfService;
	private final OrngJsonLDService jsonldService;
	
	private static final Logger LOG = Logger.getLogger(JsonLDHandler.class.getName());		
	
	
	@Inject
	public JsonLDHandler(RdfService rdfService, OrngJsonLDService jsonldService) {
		this.rdfService = rdfService;
		this.jsonldService = jsonldService;
	}

	/**
	 * Allowed end-points /jsonld/{userId}+/{groupId}
	 * /people/{userId}/{groupId}/{optionalPersonId}+
	 * 
	 * examples: /jsonld/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
	 */
	@Operation(httpMethods = "GET")
	public Future<?> getJsonLDData(SocialRequestItem request) throws ProtocolException {
		GroupId groupId = request.getGroup();
		Set<String> optionalURLs = ImmutableSet.copyOf(request
				.getListParameter("url"));
		String containerSessionId = request.getParameter("containerSessionId");
		boolean nocache = "true".equalsIgnoreCase(request.getParameter("nocache"));
		String output = request.getParameter("output");

		Set<String> urls = Sets.newLinkedHashSet();
		urls.addAll(makeIdsIntoURLs(request.getUsers(), request.getToken()));
		urls.addAll(optionalURLs);

		// Preconditions
		HandlerPreconditions.requireNotEmpty(urls, "No URL's specified");

		CollectionOptions options = new CollectionOptions(request);

		Set<String> fields = request.getFields();

		return getJSONItems(urls, nocache, output, fields, containerSessionId, groupId, options, request.getToken());
	}

	public Set<String> makeIdsIntoURLs(Set<UserId> userIds, SecurityToken token) {
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
	private Future<JSONObject> getJSONItems(Set<String> urls, boolean nocache, String output, Set<String> fields,
			String containerSessionId, GroupId groupId,
			CollectionOptions collectionOptions, SecurityToken token)
			throws ProtocolException {
		try {
			Model model = null;
			Set<String> uris = Sets.newLinkedHashSet();
			for (String url : urls) {
				RdfItem item = rdfService.getRDF(url, nocache, "full".equalsIgnoreCase(output), fields, containerSessionId, token);
				model = model == null ? item.getModel() : model.add(item.getModel());
				uris.add(item.getRequestedUri());
			}
			JSONObject jsonld = jsonldService.getJSONObject(model);
			// add the URI's
			JSONObject retval = new JSONObject().put("base", jsonldService.getBase()).put("uris", uris).put("jsonld", jsonld);
			return Futures.immediateFuture(retval);
		} 
		catch (Exception e) {
	        LOG.log(Level.SEVERE, "Error in getJSONItems", e);
			throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
		} 
	}

}
