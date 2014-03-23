package edu.ucsf.orng.shindig.spi.rdf;

import java.util.Set;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.json.JSONObject;

import edu.ucsf.orng.shindig.config.OrngProperties;

public interface RdfService extends OrngProperties {

	JSONObject getRDF(String uri, String containerSessionId, SecurityToken token) throws Exception;
	DataCollection getData(String uri, Set<String> fields, String containerSessionId, SecurityToken token) throws ProtocolException;
}
