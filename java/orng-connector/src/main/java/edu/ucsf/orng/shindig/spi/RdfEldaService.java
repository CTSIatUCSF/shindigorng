package edu.ucsf.orng.shindig.spi;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.epimorphics.jsonrdf.Encoder;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;


public class RdfEldaService extends RdfService {

	private static final Logger LOG = Logger.getLogger(RdfEldaService.class.getName());	
	
	public JSONObject getRDF(String uri, String output) throws Exception {
        Model src = FileManager.get().loadModel(uri);
        Resource root = src.getResource(uri);
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
