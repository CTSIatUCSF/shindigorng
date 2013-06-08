package edu.ucsf.orng.shindig.spi;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.json.JSONArray;
import org.json.JSONObject;

import com.epimorphics.jsonrdf.Encoder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;


public class RdfEldaService implements RdfService {

	private static final Logger LOG = Logger.getLogger(RdfEldaService.class.getName());
	
	private String system;
	private String systemDomain;
	
	@Inject
	public RdfEldaService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain) {
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
			else if (PROFILES.equalsIgnoreCase(system)) {  // need to rethink this
				if (!url.toLowerCase().endsWith(".rdf") && url.indexOf('?') == -1) {
					url +=  url.substring(url.lastIndexOf('/')) + ".rdf";
				}		
				// add in SessionID so that we can take advantage of Profiles security settings
				if (containerSessionId != null)
				{
					url += (url.indexOf('?') == -1 ? "?" : "&") + "ContainerSessionID=" + containerSessionId + "&Viewer=" + URLEncoder.encode(token.getViewerId(), "UTF-8");					
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
        boolean fromRoot = src.contains(root, null, (RDFNode)null);
        
        StringWriter out = new StringWriter();
        boolean prettyPrint = false;
        
        if (fromRoot) {
            Encoder.get().encodeRecursive(src, roots, out, prettyPrint);
        } else {
            Encoder.get().encode(src, out, prettyPrint);
        }

        out.flush();

        JSONObject retval = new JSONObject(out.toString());
        
        // return the matching item only
        if (MINIMAL.equals(output)) {
        	if (retval.has("results")) {
        		JSONArray items = retval.getJSONArray("results");
        		for (int i = 0; i < items.length(); i++) {
        			JSONObject obj = items.getJSONObject(i);
        			if (obj.has("_about") && uri.toLowerCase().equalsIgnoreCase(obj.getString("_about"))) {
        				LOG.info(obj.toString());
        				return obj;
        			}
        		}
        		
        	}
        }
		return retval;
	}
}
