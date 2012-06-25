package edu.ucsf.orng.shindig.spi;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
	
	public JSONObject getRDF(String uri, String output) throws Exception {
		
		String url = uri;
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			if (VIVO.equalsIgnoreCase(system)) {
				url += (url.indexOf('?') == -1 ? "?" : "&") + "format=rdfxml";
			}
			else if (PROFILES.equalsIgnoreCase(system)) {
				if (!url.toLowerCase().endsWith(".rdf")) {
					url +=  url.substring(url.lastIndexOf('/')) + ".rdf";
				}		
			}
		}
        Model src = FileManager.get().loadModel(url);
        Resource root = src.getResource(url);
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
