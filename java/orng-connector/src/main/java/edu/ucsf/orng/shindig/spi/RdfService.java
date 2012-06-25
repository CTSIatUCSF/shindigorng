package edu.ucsf.orng.shindig.spi;

import org.json.JSONObject;

import edu.ucsf.orng.shindig.config.OrngProperties;

public interface RdfService extends OrngProperties {

	public static final String FULL = "full";
	public static final String MINIMAL = "minimal";

	JSONObject getRDF(String uri, String output) throws Exception;

}
