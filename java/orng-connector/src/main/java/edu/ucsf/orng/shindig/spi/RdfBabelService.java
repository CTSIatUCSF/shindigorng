package edu.ucsf.orng.shindig.spi;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.babel.Babel;
import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.BabelWriter;
import edu.mit.simile.babel.TranslatorServlet;

public class RdfBabelService extends RdfService {

	private static final Logger LOG = Logger.getLogger(RdfBabelService.class.getName());	
	
	public JSONObject getRDF(String uri, String output) throws Exception {
		BabelReader babelReader = Babel.getReader("rdf-xml"); 
		BabelWriter babelWriter = Babel.getWriter("exhibit-json"); 
		Locale locale = Locale.getDefault();
		Properties readerProperties = new Properties();
		Properties writerProperties = new Properties();

        readerProperties.setProperty("namespace", TranslatorServlet.makeIntoNamespace(uri));
        // trick to get rdfxml out of the URI as needed for Babel
        readerProperties.setProperty("url", uri+"?format=rdfxml");
        
		URLConnection connection = new URL(uri+"?format=rdfxml").openConnection();
		connection.setConnectTimeout(5000);
		connection.connect();

        InputStream inputStream = connection.getInputStream();
        StringWriter writer = new StringWriter();
        
		MemoryStore store = new MemoryStore();
        try {
        	store.initialize();
            try {
				String encoding = connection.getContentEncoding();
				
				Reader reader = new InputStreamReader(
					inputStream, (encoding == null) ? "ISO-8859-1" : encoding);
							
				babelReader.read(reader, store, readerProperties, locale);
            } finally {
    			inputStream.close();
            }
            
    		babelWriter.write(writer, store, writerProperties, locale);
        }
        finally {
        	store.shutDown();
        }
        
        JSONObject retval = new JSONObject(writer.toString());
        
        // return the matching item only
        if (MINIMAL.equals(output)) {
        	if (retval.has("items")) {
        		JSONArray items = retval.getJSONArray("items");
        		for (int i = 0; i < items.length(); i++) {
        			JSONObject obj = items.getJSONObject(i);
        			if (obj.has("uri") && uri.equalsIgnoreCase(obj.getString("uri"))) {
        				LOG.info(obj.toString());
        				return obj;
        			}
        		}
        		
        	}
        }
		return retval;
	}
}
