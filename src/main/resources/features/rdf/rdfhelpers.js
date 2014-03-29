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
 * The default behavior is to allow for cached data to be returned.
 * You can change this by setting options.nocache = 'true'
 */
gadgets.util.registerOnLoadHandler(function() {

  // No point defining these if osapi.rdf.get doesn't exist
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
      options.containerSessionId = parent.my.containerSessionId; 
      options.userId = '@owner';
      options.groupId = '@self';
      return osapi.rdf.get(options);
    };

    /**
     * Function to get any RDF converted to JSON
     * @param {String} The url for the specific request.
     */
    osapi.rdf.getRDF = function(url, options) {
        options = options || {};
        // for security reasons only send sessionId if going back to host!
        if (url.indexOf(parent.location.hostname) !== -1) { 
        	options.containerSessionId = parent.my.containerSessionId;
        }
        options.userId = '@userId';
        options.groupId = '@self';
        options.url = url;
        return new osapi.rdf.get(options);
      };
      
      osapi.rdf.getOwnerProperties = function(properties, options) {
          options = options || {};
          options.containerSessionId = parent.my.containerSessionId; 
          options.userId = '@owner';
          options.groupId = '@self';
          options.fields = properties || {};
          return new osapi.rdf.get(options);
      };
      
      osapi.rdf.getViewerProperties = function(properties, options) {
          options = options || {};
          options.containerSessionId = parent.my.containerSessionId; 
          options.userId = '@viewer';
          options.groupId = '@self';
          options.fields = properties || {};
          return new osapi.rdf.get(options);
      };

      osapi.rdf.getProperties = function(url, properties, options) {
          options = options || {};
          // for security reasons only send sessionId if going back to host!
          if (url && url.indexOf(parent.location.hostname) !== -1) { 
          	options.containerSessionId = parent.my.containerSessionId;
          }
          options.userId = '@userId';
          options.groupId = '@self';
          options.url = url;
          options.fields = properties || {};
          return new osapi.rdf.get(options);
        };
        
        /*
         * Return a map of objects keyed by uri
         */
        osapi.rdf.parse = function(data) {
            var jsonld = data.jsonld;

            var retval = {};
            // when this happens, just return the jsonld as the only entry
            if (!jsonld.hasOwnProperty('@graph')) {
                retval[data.uris[0]] = jsonld;
                return retval;
            }
            
            // put everything in a map keyed by ID
            var fatObjMap = {};
            for (var i = 0; i < jsonld['@graph'].length; i++) {
                var item = jsonld['@graph'][i];
                fatObjMap[item['@id']] = item;
            }

            // now go through map and swap out thin object with the fat one from the map
            for (var key in fatObjMap) {
            	var obj = fatObjMap[key];
            	for (var prop in obj) {
            		if (obj[prop] instanceof Array) {
                		for (var i = 0; i < obj[prop].length; i++) {
    	            		var thin = obj[prop][i];
    	            		if (typeof(thin) == 'object' && thin.hasOwnProperty('@id')) {
    	            			// replace the thin one with the fat one
    	            			obj[prop][i] = fatObjMap[thin['@id']];
    	            		}
                		}
            		}
            		else {
	            		var thin = obj[prop];
	            		if (typeof(thin) == 'object' && thin.hasOwnProperty('@id')) {
	            			// replace the thin one with the fat one
	            			obj[prop] = fatObjMap[thin['@id']];
	            		}
            		}
            	}
            }
            
            // there are many items in the fatObjMap all wired together, return the ones they 
            // specifically asked for aka. data.uris in a map keyed by uri 
    	    for (var i = 0; i < data.uris.length; i++) {
    	    	var uri = data.uris[i];
    		    retval[uri] = fatObjMap[uri];
    	    }
    	    return retval;
        };

     }
});
