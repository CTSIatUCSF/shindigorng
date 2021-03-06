package edu.ucsf.orng.shindig.spi.rdf;


import org.apache.jena.rdf.model.Model;

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
