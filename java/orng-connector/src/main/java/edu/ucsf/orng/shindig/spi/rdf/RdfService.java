package edu.ucsf.orng.shindig.spi.rdf;

import org.apache.shindig.auth.SecurityToken;
import org.json.JSONObject;

import edu.ucsf.orng.shindig.config.OrngProperties;

public interface RdfService extends OrngProperties {

	public static final String FULL = "full";
	public static final String MINIMAL = "minimal";

	JSONObject getRDF(String uri, String output, String containerSessionId, SecurityToken token) throws Exception;

}
