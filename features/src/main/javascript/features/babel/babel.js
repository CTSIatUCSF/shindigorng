
var babel = babel || {};

babel.translate = function(rdfurl, callback) {
	
		var postData = ""; 
		if (rdfurl instanceof Array) {
			for (var i = 0; i < rdfurl.length; i++) {
				postData += "url=" + rdfurl[i] + ( i < rdfurl.length - 1 ? "&" : "");
			}
		}
		else {
			postData = "url=" + rdfurl;
		}
		
    	var data = {
    		reader : 'rdf-xml',
    		writer : 'exhibit-json',
    		mimetype : 'default'
    	};
    	
    	var makeRequestParams = {
    		'POST_DATA' : postData,    			
		    'CONTENT_TYPE' : 'JSON',
		    'METHOD' : 'POST'
		};

    	gadgets.io.makeNonProxiedRequest('translator?' + gadgets.io.encodeValues(data),
    		      callback,
    		      makeRequestParams,
    		      'application/javascript'
    	);    	
};

babel.people = babel.people || {};

babel.people.get = function(person, callback) {
	if (person.urls) {
		  for (var i = 0; i < person.urls.length; i++) {
			if (person.urls[i].type == "RDF") {
				babel.translate(person.urls[i].value, callback)
			}
		  }
     }
};	

babel.people.getViewer = function(callback) {
	osapi.people.getViewer().execute(function(result) {
		babel.people.get(result, callback);
	});
};	

babel.people.getOwner = function(callback) {
	osapi.people.getOwner().execute(function(result) {
		babel.people.get(result, callback);
	});
};	
