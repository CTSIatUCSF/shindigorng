package edu.ucsf.orng.shindig.spi.rdf;

import java.util.logging.Logger;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;

import edu.ucsf.ctsi.r2r.jena.FusekiService;

// make this an interface that can work without httpfetcher
public class ShindigFusekiService implements FusekiService {

	private static final Logger LOG = Logger.getLogger(ShindigFusekiService.class.getName());
	
	private Uri fusekiPost = Uri.parse("http://localhost:3030/ds/data?default");
	private String fusekiQuery = "http://localhost:3030/ds/query";
	private Uri fusekiUpdate = Uri.parse("http://localhost:3030/ds/update");
	private HttpFetcher fetcher;
	
	
	@Inject
	public ShindigFusekiService(@Named("orng.fuseki") String fusekiURL, HttpFetcher fetcher) {
		this.fusekiPost = Uri.parse(fusekiURL + "/data?default");
		this.fusekiQuery = fusekiURL + "/query";
		this.fusekiUpdate = Uri.parse(fusekiURL + "/update");
		this.fetcher = fetcher;
	}
		
	public Model get(String uri) {
		QueryExecution qe = QueryExecutionFactory.sparqlService(fusekiQuery, "DESCRIBE <" + uri + ">");
		Model model = qe.execDescribe();
		return model;
	}
	
	public int delete(String uri) throws GadgetException {
	    HttpRequest request = new HttpRequest(fusekiUpdate)
			        .setMethod("POST")
			        .setPostBody(("DELETE WHERE { <" + uri + ">  ?p ?o }").getBytes())
			        .addHeader("content-type", "application/sparql-update");
    	HttpResponse response = fetcher.fetch(request);
    	return response.getHttpStatusCode();
	}

	public int add(byte[] body) throws GadgetException {
	    HttpRequest request = new HttpRequest(fusekiPost)
				    .setMethod("POST")
				    .setPostBody(body)
				    .addHeader("content-type", "application/rdf+xml");
	    HttpResponse response = fetcher.fetch(request);		
		return response.getHttpStatusCode();
	}

	public int update(String sparql) throws GadgetException {
		HttpRequest request = new HttpRequest(fusekiUpdate)
			        .setMethod("POST")
			        .setPostBody(sparql.getBytes())
			        .addHeader("content-type", "application/sparql-update");
		
	    HttpResponse response = fetcher.fetch(request);		
		return response.getHttpStatusCode();
	}
}
