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
import org.apache.shindig.common.util.FutureUtil;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.service.SocialRequestItem;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONObject;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.orng.shindig.spi.RdfService;

/**
 * RPC/REST handler for all /people requests
 */
@Service(name = "rdf", path = "/{userId}+/{groupId}/{personId}+")
public class RdfHandler {
  private final RdfService rdfService;
  private final ContainerConfig config;
  private String orngURL;

  @Inject
  public RdfHandler(RdfService rdfService, ContainerConfig config, @Named("orng.url") String orngURL) {
    this.rdfService = rdfService;
    this.config = config;
    this.orngURL = orngURL;
  }

  /**
   * Allowed end-points /rdf/{userId}+/{groupId} /people/{userId}/{groupId}/{optionalPersonId}+
   *
   * examples: /rdf/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
   */
  @Operation(httpMethods = "GET")
  public Future<?> get(SocialRequestItem request) throws ProtocolException {
    GroupId groupId = request.getGroup();
    Set<String> optionalPersonId = ImmutableSet.copyOf(request.getListParameter("personId"));
    Set<String> optionalRDFUrl = ImmutableSet.copyOf(request.getListParameter("url"));
    
    Set<String> urls = new HashSet<String>();
    urls.addAll(makeIdsIntoURLs(request.getUsers(), request.getToken()));
    urls.addAll(optionalRDFUrl);

    // Preconditions
    HandlerPreconditions.requireNotEmpty(urls, "No URI's specified");

    CollectionOptions options = new CollectionOptions(request);

    if (urls.size() == 1) {
    	return rdfService.getItem(urls.iterator().next());
    } else {
        return rdfService.getItems(urls, groupId, options, request.getToken());
    }
  }

  @Operation(httpMethods = "GET", path="/@supportedOntologies")
  public List<Object> supportedOntologies(RequestItem request) {
    // TODO: Would be nice if name in config matched name of service.
    String container = Objects.firstNonNull(request.getToken().getContainer(), "default");
    return config.getList(container,
        "${Cur['gadgets.features'].opensocial.supportedOntologies}");
  }

	public Set<String> makeIdsIntoURLs(Set<UserId> userIds, SecurityToken token) {
		Set<String> urls = new HashSet<String>();
		for (UserId id : userIds) {
			String strId = id.getUserId(token);
			if (strId != null) {
				urls.add( makeIdIntoURL(strId) );
			}
		}
		return urls;
	}
	
	public String makeIdIntoURL(String strId) {
		return orngURL + "/display/n" + strId + "?format=rdfxml";
	}


}
