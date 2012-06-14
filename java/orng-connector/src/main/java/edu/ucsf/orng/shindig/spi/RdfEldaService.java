package edu.ucsf.orng.shindig.spi;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.json.JSONArray;
import org.json.JSONObject;

import com.epimorphics.jsonrdf.Encoder;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

public class RdfEldaService implements RdfService {

	private static final Logger LOG = Logger.getLogger(RdfEldaService.class.getName());	
	
	public Future<RestfulCollection<JSONObject>> getItems(Set<String> uris, String output,
			GroupId groupId, CollectionOptions collectionOptions,
			SecurityToken token) throws ProtocolException {
		// TODO Auto-generated method stub
		List<JSONObject> result = Lists.newArrayList();

		if (uris.size() == 0) {
			return ImmediateFuture.newInstance(null);
		}
		for (String uri : uris) {
			try {
				result.add(getRDF(uri, output));
			}
			catch (Exception e) {
				throw new ProtocolException(0, e.getMessage(), e);
			}
		}
		int firstResult = 0;
		if (collectionOptions != null) {
			firstResult = collectionOptions.getFirst();
		}
		return ImmediateFuture.newInstance(new RestfulCollection<JSONObject>(
				result, firstResult, result.size()));
	}

	public Future<JSONObject> getItem(String uri, String output) throws ProtocolException {
		try {
			return ImmediateFuture.newInstance(getRDF(uri, output));
		}
		catch (Exception e) {
			throw new ProtocolException(0, e.getMessage(), e);
		}
	}

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
        			// changed from equalsIgnoreCase to startsWith to catch /foo/foo.rdf cases
        			if (obj.has("_about") && uri.toLowerCase().startsWith(obj.getString("_about").toLowerCase())) {
        				LOG.info(obj.toString());
        				return obj;
        			}
        		}
        		
        	}
        }
		return retval;
	}
}
