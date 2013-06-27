package edu.ucsf.orng.shindig.spi;

import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.impl.JenaRDFParser;
import com.github.jsonldjava.utils.JSONUtils;

public class RdfJsonLDService implements RdfService {

	private static final Logger LOG = Logger.getLogger(RdfJsonLDService.class.getName());
	
	private static final String RDFXML = "application/rdf+xml";
	
	private String system;
	private String systemDomain;
	private String systemBase;
	
	@Inject
	public RdfJsonLDService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain) {
		this.system = system;
		this.systemDomain = systemDomain;
		this.systemBase = systemDomain;
		if (PROFILES.equalsIgnoreCase(system)) {
			systemBase += "/profile/";
		}		
    	JSONLD.registerRDFParser(RDFXML, new JenaRDFParser());
	}
	
	public JSONObject getRDF(String uri, String output, String containerSessionId, SecurityToken token) throws Exception {
		
		String url = uri;
		boolean local = false;
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			local = true;
			if (VIVO.equalsIgnoreCase(system)) {
				url += (url.indexOf('?') == -1 ? "?" : "&") + "format=rdfxml";
			}
			else if (PROFILES.equalsIgnoreCase(system)) {
				// we know how to parse out the URL with profiles to take advantage of a direct request
				// where we can avoid the redirect and add helpful query args
				// we also know how to grab the nodeID and build a clean URI
				int ndx = -1;  
				String nodeId = null;
				if ((ndx = uri.indexOf("Subject=")) != -1) {
					nodeId = uri.indexOf('&', ndx) != -1 ? uri.substring(ndx + "Subject=".length(), uri.indexOf('&', ndx)) : uri.substring(ndx + "Subject=".length());					
				}
				else {
					// if it is a GET style URL then the first numeric item in the path is likely it
					String[] items = uri.substring(systemDomain.length() + 1, uri.indexOf('?') == -1 ? uri.length() : uri.indexOf('?')).split("/");
					for (String item : items) {
						if (StringUtils.isNumeric(item)) {
							nodeId = item;
							break;
						}
					}
				}
				if (nodeId != null) {
					uri = systemBase + nodeId;
				}
				
				if (!url.toLowerCase().endsWith(".rdf") && url.indexOf('?') == -1) {
					url = systemDomain + "/Profile/Profile.aspx?Subject=" + nodeId;
					// add in SessionID so that we can take advantage of Profiles security settings
					if ("full".equalsIgnoreCase(output)) {
						url += "&Expand=true&ShowDetails=true";
					}
					if (containerSessionId != null)
					{
						url += "&ContainerSessionID=" + containerSessionId + "&Viewer=" + URLEncoder.encode(token.getViewerId(), "UTF-8");					
					}
				}		
			}
		}
    	LOG.log(Level.INFO, "getRDF :" + url );
        final Options opts = new Options(local ? systemBase : "");
        opts.format = RDFXML;
        opts.outputForm = "compacted";  // [compacted|expanded|flattened]
        Model model = FileManager.get().loadModel(url);
        Object obj = JSONLD.fromRDF(model, opts);        
        // simplify
        obj = JSONLD.simplify(obj, opts);
        String str = JSONUtils.toString(obj);
        JSONObject jsonld = new JSONObject(str);
        // we put the JSON-LD in one item, the requested URI in another        
        return new JSONObject().put("uri", uri).put("jsonld", jsonld).put("base", opts.base);
	}
}
