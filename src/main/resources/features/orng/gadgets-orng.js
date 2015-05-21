
gadgets.orng = gadgets.orng || {};

/**
* Make an RPC call that the container will pick up
*/
gadgets.orng.getPeopleListMetadata = function (callback) {
	gadgets.rpc.call('..', 'orng_containerRpc', callback, "REQUEST_PEOPLE_LIST_METADATA");
};

gadgets.orng.getPeopleList = function (callback) {
	gadgets.rpc.call('..', 'orng_containerRpc', callback, "REQUEST_PEOPLE_LIST");
};

gadgets.orng.reportGoogleAnalyticsEvent = function (event) {
	gadgets.rpc.call('..', 'orng_reportGoogleAnalyticsEvent', function() {}, event);
};

gadgets.orng.hideGadget = function (callback) {
	gadgets.rpc.call('..', 'orng_hideShow', callback, "hide");
};

gadgets.orng.showGadget = function (callback) {
	gadgets.rpc.call('..', 'orng_hideShow', callback, "show");
};


gadgets.util.registerOnLoadHandler(function() {

	// No point defining these if jsonld and osapi.jsonld.getJsonLDData doesn't exist
	if (osapi && osapi.orng && osapi.orng.get && osapi.orng.add && osapi.orng["delete"]) {
		osapi.orng.addAppToOwner = function(options) {
			options = options || {};
			options.containerSessionId = parent.my.containerSessionId; 
			options.userId = '@owner';
			options.groupId = '@self';
			options.appId = '@app';
			return osapi.orng.add(options);
		};

		osapi.orng.getAppInstance = function(options) {
			options = options || {};
			options.containerSessionId = parent.my.containerSessionId; 
			options.userId = '@owner';
			options.groupId = '@self';
			options.appId = '@app';
			return osapi.orng.get(options);
		};

		osapi.orng.removeAppFromOwner = function(options) {
			options = options || {};
			options.containerSessionId = parent.my.containerSessionId; 
			options.userId = '@owner';
			options.groupId = '@self';
			options.appId = '@app';
			options.deleteType = 0; // hard delete
			return osapi.orng["delete"](options);
		};		
	}
});
