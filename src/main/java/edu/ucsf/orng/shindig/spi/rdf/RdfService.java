package edu.ucsf.orng.shindig.spi.rdf;

import java.util.Set;

import org.apache.shindig.auth.SecurityToken;
import org.json.JSONObject;

import edu.ucsf.orng.shindig.config.OrngProperties;

public interface RdfService extends OrngProperties {

	JSONObject getRDF(String uri, boolean nocache, Set<String> fields, String containerSessionId, SecurityToken token) throws Exception;

}
