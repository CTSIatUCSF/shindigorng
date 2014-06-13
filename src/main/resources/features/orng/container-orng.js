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

//This allows us to use Google Analytics when present, but not throw errors when not.
var _gaq = _gaq || {};

// create dummy function if necessary so google analytics does not break for institutions who do not use it
if (typeof _gaq.push != 'function') {
    _gaq.push = function (data) { };
}
// Google Analytics END

osapi.container.Container.addMixin('orng', function(container) {

	container.rpcRegister('orng_containerRpc', function (rpc, channel, opt_params) {
    	// send an ajax command to the server letting them know we need data
    	// since this is directly into Profiles and has nothing to do with Shindig, we just use jquery
    	var data = { "guid": my.guid, "request": channel };

    	$.ajax({
    		type: "POST",
    		url: my.orngRPCEndpoint,
    		data: gadgets.json.stringify(data),
    		contentType: "application/json; charset=utf-8",
    		dataType: "json",
    		async: true,
    		success: function (msg) {
    			rpc.callback(msg.d);
    		}
    	});
    });	
    
    container.rpcRegister('orng_reportGoogleAnalyticsEvent', function (rpc, event, opt_params) {
    	_gaq.push(['_trackEvent', rpc.gs.getTitle(), event.action, event.label, event.value]);
    });	

});

