package edu.ucsf.orng.shindig.spi.rdf;

import com.hp.hpl.jena.rdf.model.Resource;

import edu.ucsf.orng.shindig.config.OrngProperties;

public interface JenaResourceService extends OrngProperties {
	
	Resource getResourceContaining(String uri) throws Exception;		
}
