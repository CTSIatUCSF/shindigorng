package edu.ucsf.orng.shindig.spi.rdf;

import java.util.logging.Logger;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.jena.FusekiClient;

// make this an interface that can work without httpfetcher
public class ShindigFusekiClient extends FusekiClient {

	private static final Logger LOG = Logger.getLogger(ShindigFusekiClient.class.getName());
	
	private Uri fusekiPost = Uri.parse("http://localhost:3030/ds/data?default");
	private Uri fusekiUpdate = Uri.parse("http://localhost:3030/ds/update");
	private HttpFetcher fetcher;	
	
	@Inject
	public ShindigFusekiClient(@Named("orng.fuseki") String fusekiURL, HttpFetcher fetcher) {
		super(fusekiURL);
		this.fusekiPost = Uri.parse(fusekiURL + "/data?default");
		this.fusekiUpdate = Uri.parse(fusekiURL + "/update");
		this.fetcher = fetcher;
	}

	public int deleteSubject(String uri) throws GadgetException {
	    HttpRequest request = new HttpRequest(fusekiUpdate)
			        .setMethod("POST")
			        .setPostBody(("DELETE WHERE { <" + uri + ">  ?p ?o }").getBytes())
			        .addHeader("content-type", UPDATE_CONTENT_TYPE);
    	HttpResponse response = fetcher.fetch(request);
    	return response.getHttpStatusCode();
	}

	public int add(byte[] body) throws GadgetException {
	    HttpRequest request = new HttpRequest(fusekiPost)
				    .setMethod("POST")
				    .setPostBody(body)
				    .addHeader("content-type", ADD_CONTENT_TYPE);
	    HttpResponse response = fetcher.fetch(request);		
		return response.getHttpStatusCode();
	}

	public int update(String sparql) throws GadgetException {
		HttpRequest request = new HttpRequest(fusekiUpdate)
			        .setMethod("POST")
			        .setPostBody(sparql.getBytes())
			        .addHeader("content-type", UPDATE_CONTENT_TYPE);
		
	    HttpResponse response = fetcher.fetch(request);		
		return response.getHttpStatusCode();
	}
}
