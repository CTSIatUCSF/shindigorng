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

/**
 * Service to retrieve JSON-LD via JSON RPC opensocial calls.
 * Called in onLoad handler as osapi.rdf.get could be defined by
 * the container over the gadgets.rpc transport.
 * 
 * Options for output are "minimal" or "full", default is "minimal"
 */
gadgets.util.registerOnLoadHandler(function() {

  // No point defining these if osapi.people.get doesn't exist
  if (osapi && osapi.rdf && osapi.rdf.get) {
	  
	  /**
    * Helper functions to get People.
    * Options specifies parameters to the call as outlined in the
    * JSON RPC Opensocial Spec
    * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
    * @param {object.<JSON>} The JSON object of parameters for the specific request.
    */
    /**
      * Function to get Viewer profile.
      * Options specifies parameters to the call as outlined in the
      * JSON RPC Opensocial Spec
      * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
      * @param {object.<JSON>} The JSON object of parameters for the specific request.
      */
    osapi.rdf.getViewer = function(options) {
      options = options || {};
      options.output = options.output || "minimal";
      options.containerSessionId = parent.my.containerSessionId; 
      options.userId = '@viewer';
      options.groupId = '@self';
      return new osapi.rdf.get(options);
    };

    /**
      * Function to get Owner profile.
      * Options specifies parameters to the call as outlined in the
      * JSON RPC Opensocial Spec
      * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
      * @param {object.<JSON>} The JSON object of parameters for the specific request.
      */
    osapi.rdf.getOwner = function(options) {
      options = options || {};
      options.output = options.output || "minimal";
      options.containerSessionId = parent.my.containerSessionId; 
      options.userId = '@owner';
      options.groupId = '@self';
      return osapi.rdf.get(options);
    };

    /**
     * Function to get any RDF converted to JSON
     * @param {String} The uri for the specific request.
     */
    osapi.rdf.getRDF = function(uri, options) {
        var options = options || {};
        options.output = options.output || "minimal";
        // for security reasons only send sessionId if going back to host!
        if (uri.indexOf(parent.location.hostname) !== -1) { 
        	options.containerSessionId = parent.my.containerSessionId;
        }
        options.userId = '@userId';
        options.groupId = '@self';
        options.uri = uri;
        var foo = osapi.rdf.get(options);
        foo.oldexecute = foo.execute;
        foo.execute = function(callback) {
        	oldexecute(callback);
        }
        return new osapi.rdf.get(options);
      };
      
  }
});
