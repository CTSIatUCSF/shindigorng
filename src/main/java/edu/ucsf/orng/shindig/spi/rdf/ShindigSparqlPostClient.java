package edu.ucsf.orng.shindig.spi.rdf;

import java.util.logging.Logger;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import edu.ucsf.ctsi.r2r.jena.SparqlPostClient;

// make this an interface that can work without httpfetcher
public class ShindigSparqlPostClient extends SparqlPostClient {

	private static final Logger LOG = Logger.getLogger(ShindigSparqlPostClient.class.getName());
	
	private Uri sparqlPost = Uri.parse("http://localhost:3030/ds/data?default");
	private HttpFetcher fetcher;	
	
	public ShindigSparqlPostClient(String sparqlUpdate, String sparqlPost, HttpFetcher fetcher) {
		super(sparqlUpdate, sparqlPost);
		this.sparqlPost = Uri.parse(getSparqlPostEndpoint());
		this.fetcher = fetcher;
	}

	@Override
	public int add(byte[] body) throws GadgetException {
	    HttpRequest request = new HttpRequest(sparqlPost)
				    .setMethod("POST")
				    .setPostBody(body)
				    .addHeader("content-type", ADD_CONTENT_TYPE);
	    HttpResponse response = fetcher.fetch(request);		
		return response.getHttpStatusCode();
	}

}
