
var jsonldHelper = {};

// Basically from rdfhelpers.js in Shindigorng
/**
* Function to rebuild references in the graph and return the item originally requested.
*/


/*
 * Returns an array of URI's
 */
jsonldHelper.getObjectUris = function(data, property) {
	var retval = [];
	if (data['@graph'] instanceof Array) {
		for (var i = 0; i < data['@graph'].length; i++) {
			retval = retval.concat(jsonldHelper.getObjectUris(data['@graph'][i], property));
		}
	}
	else if (data[property] instanceof Array) {
		for (var i = 0; i < data[property].length; i++) {
			retval[i] = data[property][i]['@id'];
		}
	}
	else if (data[property]) {
		retval[0] = data[property]['@id'];
	}
	return retval;
};

/*
 * Returns a map of all the subjects, keyed by their URI
 */
jsonldHelper.getSubjects = function(data) {
	var retval = {};
	if (data.list instanceof Array) {
		for (var i = 0; i < data.list.length; i++) {
			retval[data.list[i].uri] = jsonldHelper.getSubject(data.list[i]);			
		}
	}
	else if (data.list) {
		retval[data.list[0].uri] = jsonldHelper.getSubject(data.list[0]);					
	}
	else if (data) {
		retval[data.uri] = jsonldHelper.getSubject(data);
	}
	return retval;
};

/*
 * Returns the object specified by data.uri
 */
jsonldHelper.getSubject = function(data) {
    var jsonld = data.jsonld;
    var map = {};
    if (!jsonld.hasOwnProperty('@graph')) {
        return jsonld;
    }
    // put everything in a map keyed by ID
    for (var i = 0; i < jsonld['@graph'].length; i++) {
        var item = jsonld['@graph'][i];
        map[item['@id']] = item;
    }
    // now go through map and swap out URI's with actual objects
    for (var key in map) {
        jsonldHelper._swap(map, map, key);
    }

    // finally, pull out the one item they want
    return jsonldHelper.findByUri(map, data.base, data.uri);
};

jsonldHelper._swap = function (map, base, key) {
    if (key.charAt(0) == '@') {
        return;
    }
    var obj = base[key];
    if (typeof (obj) == 'string') {
        if (map[obj]) {
            base[key] = map[obj];
        }
    }
    else if (typeof (obj) == 'object') {
        for (var key in obj) {
            jsonldHelper._swap(map, obj, key)
        }
    }
};

jsonldHelper.findByUri = function (map, base, uri) {
    // drop any query args
    uri = uri.split('?')[0];
    // in some cases, the map is the item
    if (typeof map == 'object' && map.hasOwnProperty('@id') && uri == base + map['@id']) {
        return map;
    }
    for (var key in map) {
        var item = map[key];
        if (typeof item == 'object' && item.hasOwnProperty('@id') && uri == base + item['@id']) {
            return item;
        }
    }
};

jsonldHelper.findByPersonId = function (map, personId) {
    // in some cases, the map is the item
    if (typeof map == 'object' && map.hasOwnProperty('personId') && personId == map['personId']) {
        return map;
    }
    for (var key in map) {
        item = map[key];
        if (typeof item == 'object' && item.hasOwnProperty('personId') && personId == item['personId']) {
            return item;
        }
    }
};

