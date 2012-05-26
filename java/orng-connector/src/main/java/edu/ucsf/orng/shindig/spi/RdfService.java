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
package edu.ucsf.orng.shindig.spi;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * Interface that defines how shindig gathers people information.
 */
public interface RdfService {

	public static final String FULL = "full";
	public static final String MINIMAL = "minimal";
	  /**
	   * Returns a list of people that correspond to the passed in person ids.
	   *
	   * @param userIds A set of users
	   * @param groupId The group
	   * @param collectionOptions How to filter, sort and paginate the collection being fetched
	   * @param fields The profile details to fetch. Empty set implies all
	   * @param token The gadget token @return a list of people.
	   * @return Future that returns a RestfulCollection of Person
	   */
	  Future<RestfulCollection<JSONObject>> getItems(Set<String> uris, String output, GroupId groupId,
	      CollectionOptions collectionOptions, SecurityToken token)
	      throws ProtocolException;


  /**
   * Returns a JSON object that corresponds to the passed in URI.
   *
   * @param id The id of the person to fetch.
   * @param fields The fields to fetch.
   * @param token The gadget token
   * @return a list of people.
   */
  Future<JSONObject> getItem(String uri, String output)
      throws ProtocolException;
  
  JSONObject getRDF(String uri, String output) throws Exception;
}
