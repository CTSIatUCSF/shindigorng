
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

