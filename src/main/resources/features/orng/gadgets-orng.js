
gadgets.orng = gadgets.orng || {};

/**
* Make an RPC call that the container will pick up
*/
gadgets.orng.getCurrentPageItemsMetadata = function (callback) {
	gadgets.rpc.call('..', 'orng_responder', callback, "currentPageItemsMetadata");
};

gadgets.orng.getCurrentPageItems = function (callback) {
	gadgets.rpc.call('..', 'orng_responder', callback, "currentPageItems");
};

gadgets.orng.hideGadget = function (callback) {
	gadgets.rpc.call('..', 'orng_hide_show', callback, "hide");
};

gadgets.orng.showGadget = function (callback) {
	gadgets.rpc.call('..', 'orng_hide_show', callback, "show");
};
