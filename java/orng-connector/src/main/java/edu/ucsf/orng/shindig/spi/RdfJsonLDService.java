package edu.ucsf.orng.shindig.spi;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLD;
import de.dfki.km.json.jsonld.impl.JenaJSONLDSerializer;


public class RdfJsonLDService implements RdfService {

	private static final Logger LOG = Logger.getLogger(RdfJsonLDService.class.getName());
	
	private String system;
	private String systemDomain;
	
	@Inject
	public RdfJsonLDService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain) {
		this.system = system;
		this.systemDomain = systemDomain;
	}
	
	public JSONObject getRDF(String uri, String output, String containerSessionId, SecurityToken token) throws Exception {
		
		String url = uri;
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			if (VIVO.equalsIgnoreCase(system)) {
				url += (url.indexOf('?') == -1 ? "?" : "&") + "format=rdfxml";
			}
			else if (PROFILES.equalsIgnoreCase(system)) {
				// we know how to parse out the URL with profiles to take advantage of a direct request
				// where we can avoid the redirect and add helpful query args
				if (!url.toLowerCase().endsWith(".rdf") && url.indexOf('?') == -1) {
					url = systemDomain + "/Profile/Profile.aspx?Subject=" + url.substring(url.lastIndexOf('/') + 1);
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
		Model src = null;
		Resource root = null;
		try {
	        src = FileManager.get().loadModel(url);
	        root = src.getResource(url);
		}
		catch (Exception e) {
			if (systemDomain != null && VIVO.equalsIgnoreCase(system)) {
				// worth a try. Handles the case where VIVO has consumed data from another instance
				url = systemDomain + "/display?uri=" + uri + "&format=rdfxml";
		        src = FileManager.get().loadModel(url);
		        root = src.getResource(url);
			}
			else {
				throw e;
			}
		}
        List<Resource> roots = new ArrayList<Resource>();
        roots.add( root );
        
        JenaJSONLDSerializer serializer = new JenaJSONLDSerializer();
        
        // expand and simplify
        return new JSONObject(JSONUtils.toString(JSONLD.simplify(JSONLD.expand(JSONLD.fromRDF(src, serializer)))));
	}
}
