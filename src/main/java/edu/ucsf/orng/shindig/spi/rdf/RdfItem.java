package edu.ucsf.orng.shindig.spi.rdf;

import java.io.ByteArrayOutputStream;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.json.JSONException;
import org.json.JSONObject;

import com.hp.hpl.jena.rdf.model.Model;

public class RdfItem {
	
	private Model model;	
	private String requestedUri;
	
	public RdfItem(Model model, String requestedUri) {
		super();
		this.model = model;
		this.requestedUri = requestedUri;
	}
	
	public Model getModel() {
		return model;
	}
	
	public String getRequestedUri() {
		return requestedUri;
	}

}
