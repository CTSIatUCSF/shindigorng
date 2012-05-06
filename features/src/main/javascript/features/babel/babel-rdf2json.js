
var babel = babel || {};

babel.translate = function(rdfurl, callback) {
	
		var urls = [];
		if (rdfurl instanceof Array) {
			urls.concat(rdfurl);
		}
		else {
			urls[0] = rdfurl;
		}
		
		var multipart = ""; 
        var boundary = Math.random().toString().substr(2);
        for (var i = 0; i < urls.length; i++) {
        	multipart += "--" + boundary
                     + "\r\nContent-Disposition: form-data; name=url"
                     + "\r\nContent-type: application/octet-stream"
                     + "\r\n\r\n" + urls[i] + "?format=rdfxml" + "\r\n";
        }
        multipart += "--"+boundary+"--\r\n";
		
    	var data = {
    		reader : 'rdf-xml',
    		writer : 'exhibit-json',
    		mimetype : 'default'
    	};
    	
    	var makeRequestParams = {
    		'POST_DATA' : multipart,    			
		    'CONTENT_TYPE' : 'JSON',
		    'METHOD' : 'POST'
		};

    	gadgets.io.makeNonProxiedRequest('/babel/translator?' + gadgets.io.encodeValues(data),
    		      callback,
    		      makeRequestParams,
    		      'multipart/form-data; charset=utf-8; boundary=' + boundary
    	);    	
};

babel.people = babel.people || {};

babel.people.get = function(person, callback) {
	if (person.profileUrl) {
		babel.translate(person.profileUrl, callback)
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
