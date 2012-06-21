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
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.json.JSONObject;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Interface that defines how shindig gathers people information.
 */
public abstract class RdfService {

	public static final String FULL = "full";
	public static final String MINIMAL = "minimal";

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
	public Future<RestfulCollection<JSONObject>> getItems(Set<String> uris,
			String output, GroupId groupId,
			CollectionOptions collectionOptions, SecurityToken token)
			throws ProtocolException {
		// TODO Auto-generated method stub
		List<JSONObject> result = Lists.newArrayList();

		if (uris.size() == 0) {
			return ImmediateFuture.newInstance(null);
		}
		for (String uri : uris) {
			try {
				result.add(getRDF(uri, output));
			} catch (Exception e) {
				throw new ProtocolException(0, e.getMessage(), e);
			}
		}
		int firstResult = 0;
		if (collectionOptions != null) {
			firstResult = collectionOptions.getFirst();
		}
		return ImmediateFuture.newInstance(new RestfulCollection<JSONObject>(
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
	public Future<JSONObject> getItem(String uri, String output)
			throws ProtocolException {
		try {
			return ImmediateFuture.newInstance(getRDF(uri, output));
		} catch (Exception e) {
			throw new ProtocolException(0, e.getMessage(), e);
		}
	}

	abstract JSONObject getRDF(String uri, String output) throws Exception;
}
