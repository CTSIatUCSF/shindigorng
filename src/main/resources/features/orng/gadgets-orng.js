
gadgets.orng = gadgets.orng || {};

/**
* Make an RPC call that the container will pick up
*/
gadgets.orng.getPeopleListMetadata = function (callback) {
	gadgets.rpc.call('..', 'orng_container_rpc', callback, "REQUEST_PEOPLE_LIST_METADATA");
};

gadgets.orng.getPeopleList = function (callback) {
	gadgets.rpc.call('..', 'orng_container_rpc', callback, "REQUEST_PEOPLE_LIST");
};

gadgets.orng.hideGadget = function (callback) {
	gadgets.rpc.call('..', 'orng_hide_show', callback, "hide");
};

gadgets.orng.showGadget = function (callback) {
	gadgets.rpc.call('..', 'orng_hide_show', callback, "show");
};
