
gadgets.orng = gadgets.orng || {};

/**
* Make an RPC call that the container will pick up
*/

gadgets.orng.reportEvent = function (event) {
	gadgets.rpc.call('..', 'orng_ga_reportEvent', event);
};
