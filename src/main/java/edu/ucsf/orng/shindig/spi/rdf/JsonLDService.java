package edu.ucsf.orng.shindig.spi.rdf;

import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaRDFParser;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.rdf.model.Model;

import edu.ucsf.orng.shindig.config.OrngProperties;

@Singleton
public class JsonLDService implements OrngProperties {

	private static final Logger LOG = Logger.getLogger(JsonLDService.class.getName());

	private static final String RDFXML = "application/rdf+xml";

	// only use this if the client JSON can know this and take advantage of it easily
	private String systemBase;
	
	@Inject
	public JsonLDService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain) {
		if (SYS_PROFILES.equalsIgnoreCase(system)) {
			systemBase = systemDomain + "/profile/";
		}		
    	JsonLdProcessor.registerRDFParser(RDFXML, new JenaRDFParser());
	}
	

	public JSONObject getJSONObject(Model model) throws JSONException, JsonLdError {
        final JsonLdOptions opts = new JsonLdOptions();//new JsonLdOptions(systemBase);
        opts.format = RDFXML;
        // maybe have outputForm be configurable?
        opts.outputForm = "compacted";
    	// do we need to simplify?
    	Object obj = JsonLdProcessor.fromRDF(model, opts); 
        String str = JSONUtils.toString(obj);
        return new JSONObject(str);
	}
}
