package edu.ucsf.orng.shindig.spi;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openrdf.sail.memory.MemoryStore;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.mit.simile.babel.Babel;
import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.BabelWriter;
import edu.mit.simile.babel.TranslatorServlet;

public class RdfBabelService implements RdfService {

	private static final Logger LOG = Logger.getLogger(RdfBabelService.class.getName());	
	
	private String system;
	private String systemDomain;
	
	@Inject
	public RdfBabelService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain) {
		this.system = system;
		this.systemDomain = systemDomain;
	}
	
	
	public JSONObject getRDF(String uri, String output, String containerSessionId, SecurityToken token) throws Exception {
		BabelReader babelReader = Babel.getReader("rdf-xml"); 
		BabelWriter babelWriter = Babel.getWriter("exhibit-json"); 
		Locale locale = Locale.getDefault();
		Properties readerProperties = new Properties();
		Properties writerProperties = new Properties();

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
				// add in SessionID so that we can take advantage of Profiles security settings
				if (containerSessionId != null)
				{
					url += (url.indexOf('?') == -1 ? "?" : "&") + "ContainerSessionID=" + containerSessionId + "&Viewer=" + URLEncoder.encode(token.getViewerId(), "UTF-8");					
				}
			}
		}

		readerProperties.setProperty("namespace", TranslatorServlet.makeIntoNamespace(uri));
        // trick to get rdfxml out of the URI as needed for Babel
        readerProperties.setProperty("url", url);
        
		URLConnection connection = new URL(url).openConnection();
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
