package edu.ucsf.orng.shindig.spi.rdf;

import com.hp.hpl.jena.rdf.model.Model;

import edu.ucsf.orng.shindig.config.OrngProperties;

public interface JenaModelService extends OrngProperties {
	
	Model getModel(String uri) throws Exception;		
}
