package edu.ucsf.orng.shindig.spi.rdf;

import java.util.logging.Logger;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.jena.SparqlPostClient;

// make this an interface that can work without httpfetcher
public class ShindigSparqlClient extends SparqlPostClient {

	private static final Logger LOG = Logger.getLogger(ShindigSparqlClient.class.getName());
	
	private Uri fusekiPost = Uri.parse("http://localhost:3030/ds/data?default");
	private HttpFetcher fetcher;	
	
	@Inject
	public ShindigSparqlClient(@Named("orng.fuseki") String fusekiURL, HttpFetcher fetcher) {
		super(fusekiURL + "/sparql", fusekiURL + "/update");
		this.fusekiPost = Uri.parse(fusekiURL + "/data?default");
		this.fetcher = fetcher;
	}

	public int add(byte[] body) throws GadgetException {
	    HttpRequest request = new HttpRequest(fusekiPost)
				    .setMethod("POST")
				    .setPostBody(body)
				    .addHeader("content-type", ADD_CONTENT_TYPE);
	    HttpResponse response = fetcher.fetch(request);		
		return response.getHttpStatusCode();
	}

}
