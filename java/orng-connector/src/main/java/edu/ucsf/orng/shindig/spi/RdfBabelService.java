package edu.ucsf.orng.shindig.spi;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONObject;
import org.openrdf.sail.memory.MemoryStore;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.mit.simile.babel.Babel;
import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.BabelWriter;
import edu.mit.simile.babel.TranslatorServlet;
import edu.ucsf.orng.shindig.spi.vivo.VIVOPersonService;

public class RdfBabelService implements RdfService {

	private static final Logger LOG = Logger.getLogger(RdfBabelService.class.getName());	
	
	public Future<RestfulCollection<JSONObject>> getItems(Set<String> urls,
			GroupId groupId, CollectionOptions collectionOptions,
			SecurityToken token) throws ProtocolException {
		// TODO Auto-generated method stub
		List<JSONObject> result = Lists.newArrayList();

		if (urls.size() == 0) {
			return ImmediateFuture.newInstance(null);
		}
		for (String url : urls) {
			try {
				result.add(getRDF(url));
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

	public Future<JSONObject> getItem(String url) throws ProtocolException {
		try {
			return ImmediateFuture.newInstance(getRDF(url));
		}
		catch (Exception e) {
			throw new ProtocolException(0, e.getMessage(), e);
		}
	}

	public JSONObject getRDF(String url) throws Exception {
		BabelReader babelReader = Babel.getReader("rdf-xml"); 
		BabelWriter babelWriter = Babel.getWriter("exhibit-json"); 
		Locale locale = Locale.getDefault();
		Properties readerProperties = new Properties();
		Properties writerProperties = new Properties();

        readerProperties.setProperty("namespace", TranslatorServlet.makeIntoNamespace(url));
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
    			if (babelReader.takesReader()) {
    				String encoding = connection.getContentEncoding();
    				
    				Reader reader = new InputStreamReader(
    					inputStream, (encoding == null) ? "ISO-8859-1" : encoding);
    							
    				babelReader.read(reader, store, readerProperties, locale);
    			} else {
    				babelReader.read(inputStream, store, readerProperties, locale);
    			}
            } finally {
    			inputStream.close();
            }
            
    		babelWriter.write(writer, store, writerProperties, locale);
        }
        finally {
        	store.shutDown();
        }
        
		return new JSONObject(writer.toString());
	}
}
