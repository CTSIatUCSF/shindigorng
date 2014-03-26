package edu.ucsf.orng.shindig.spi.rdf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.fuseki.server.FusekiConfig;
import org.apache.jena.fuseki.server.SPARQLServer;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupCapable;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupHandler;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaRDFParser;
import com.github.jsonldjava.utils.JSONUtils;

import edu.ucsf.ctsi.r2r.jena.DbService;
import edu.ucsf.ctsi.r2r.jena.FusekiCache;
import edu.ucsf.ctsi.r2r.jena.LODService;
import edu.ucsf.ctsi.r2r.jena.ResourceService;
import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.OrngDBUtil;

@Singleton
public class JsonLDService implements RdfService, OrngProperties, CleanupCapable {

	private static final Logger LOG = Logger.getLogger(JsonLDService.class.getName());
	
	private static final String RDFXML = "application/rdf+xml";
	
	private String system;
	private String systemDomain;
	private String systemBase;
	private OrngDBUtil dbUtil;
	private Cache<String, JSONObject> generalCache;
	
	private SPARQLServer fusekiServer;
	private FusekiCache userCache;
	private FusekiCache anonymousCache;
	private ScheduledExecutorService executorService;
	
	@Inject
	public JsonLDService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain, 
							  @Named("orng.fuseki") String fuseki, @Named("orng.fusekiDBUser") String orngUser, 
							  @Named("orng.fusekiFetchIntervalMinutes") String fetchInterval, @Named("orng.fusekiEagerRunLimitMinutes") String eagerRunLimit,
							  OrngDBUtil dbUtil, CacheProvider cacheProvider, HttpFetcher fetcher,
							  CleanupHandler cleanup) throws SQLException, IOException {
		this.system = system;
		this.systemDomain = systemDomain;
		this.systemBase = systemDomain;
		if (PROFILES.equalsIgnoreCase(system)) {
			systemBase += "/profile/";
		}		
    	JsonLdProcessor.registerRDFParser(RDFXML, new JenaRDFParser());
		// set up the cache
    	this.dbUtil = dbUtil;
    	
    	if (cacheProvider != null) {
    		generalCache = cacheProvider.createCache("orngRdf");
    	}
    	if (cleanup != null) {
    		cleanup.register(this);
    	}

    	if (fuseki != null) {
    		String fusekiURL = fuseki;
    		if ("internal".equalsIgnoreCase(fuseki)) {
    			fusekiURL = "http://localhost:3030/profiles";
    			fusekiServer = new SPARQLServer(FusekiConfig.configure(JsonLDService.class.getResource("/fuseki-shindigorng.ttl").getFile()));
    			fusekiServer.start();
    		}
	    	
	    	userCache = new FusekiCache(new ShindigFusekiClient(fusekiURL, fetcher), new DbService(systemDomain, orngUser, dbUtil));
	    	//userService = new TDBCacheResourceService(system, systemDomain, tdbBaseDir + orngUser, tdbCacheExpire, new DbModelService(systemDomain, orngUser, dbUtil));
	    	//anonymousService = new TDBCacheResourceService(system, systemDomain, tdbBaseDir + ANONYMOUS, tdbCacheExpire, new DbModelService(systemDomain, null, dbUtil));
	    	
	    	// pass into some scheduled loader
	    	executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
	    		   public Thread newThread(Runnable runnable) {
	    		      Thread thread = Executors.defaultThreadFactory().newThread(runnable);
	    		      thread.setDaemon(true);
	    		      return thread;
	    		   }
	    		});
	    	// start with a delay equal to the run limit.  Shindig is busy at startup, so best to wait 
	    	executorService.scheduleAtFixedRate(new EagerFetcher(userCache, Integer.parseInt(eagerRunLimit)), 
	    			Integer.parseInt(eagerRunLimit), Integer.parseInt(fetchInterval), TimeUnit.MINUTES);
	    	}
	}
	
	private JSONObject getFromCache(String key) {
		return generalCache != null ? generalCache.getElement(key) : null;
	}
	
	private void addToCache(String key, JSONObject value) {
		if (generalCache != null) {
			generalCache.addElement(key, value);
		}
	}
	
	// maybe have some option to force fresh or allow cache, etc.
	public JSONObject getRDF(String url, Set<String> fields, String sessionId, SecurityToken token) throws Exception {		
		String uri = getURI(url);
		boolean local = false;
		String viewerId =  token.getViewerId();
		boolean anonymous = viewerId == null || "-1".equals(viewerId);
		
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			local = true;
		}
		
		// need to think about and fix this
		boolean putInCache = false;
		
		String cacheKey = uri + anonymous;
		JSONObject retval = getFromCache(cacheKey);
		if (retval != null) {
			return retval;
		}
    	Object resourceOrModel = null;
		// we get non URI's coming into here, need to find a better way to work with those.
		if (local && uri.indexOf('?') == -1) {
			FusekiCache cache = anonymous ? anonymousCache : userCache;
			if (cache != null) {
				if (fields == null || fields.size() == 0) {
					resourceOrModel = cache.getResource(uri);
				}
				else {
					resourceOrModel = cache.getModel(uri, fields);
				}
			}
		}
		
		if (resourceOrModel == null) {
			// this can grab anything, and knows how to directly grab data from a local Profiles
			LODService service = new LODService(systemDomain, sessionId, viewerId, true, false);
			resourceOrModel = service.getModel(uri);
			// this is the only one worth caching, others are fast enough as is
			putInCache = true;
		}

		if (resourceOrModel != null) {
	        final JsonLdOptions opts = new JsonLdOptions(local ? systemBase : "");
	        opts.format = RDFXML;
	        opts.outputForm = "compacted";
	    	// do we need to simplify?
	    	Object obj = JsonLdProcessor.fromRDF(resourceOrModel, opts); 
	        // simplify
//    	        obj = JSONLD.simplify(obj, opts);
	        String str = JSONUtils.toString(obj);
	        JSONObject jsonld = new JSONObject(str);
	        // we put the JSON-LD in one item, the requested URI in another    
	        retval = new JSONObject().put("uri", uri).put("jsonld", jsonld).put("base", opts.getBase());
	        if (putInCache) {
	        	addToCache(cacheKey, retval);
	        }
	        return retval;
		}
    	return null;
	}

	private String getURI(String url) throws UnsupportedEncodingException {
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		String uri = url;
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			if (PROFILES.equalsIgnoreCase(system)) {
				// we know how to parse out the URL with profiles to take advantage of a direct request
				// where we can avoid the redirect and add helpful query args
				// we also know how to grab the nodeID and build a clean URI
				int ndx = -1;  
				String nodeId = null;
				if ((ndx = url.indexOf("Subject=")) != -1) {
					nodeId = url.indexOf('&', ndx) != -1 ? url.substring(ndx + "Subject=".length(), url.indexOf('&', ndx)) : url.substring(ndx + "Subject=".length());					
				}
				else {
					// if it is a GET style URL then the first numeric item in the path is likely it
					String[] items = url.substring(systemDomain.length() + 1, url.indexOf('?') == -1 ? url.length() : url.indexOf('?')).split("/");
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
			}
		}
    	LOG.log(Level.INFO, "URL to URI -> " + url +  " : " + uri);
		return uri;
	}	
	
	// need to make this smarter!
	protected Integer getNodeID(String uri) {
		if (uri.toLowerCase().startsWith(systemBase)) {
			return Integer.parseInt(uri.split(systemBase)[1]);
		}
		return null;
	}
		
	// important to put this in only 1 thread!
	private class EagerFetcher implements Runnable {

		private ResourceService service;
		private List<String> currentURIs;
		private int runTimeInMinutes; // how long to run before giving the machine a needed break....
		
		private EagerFetcher(ResourceService service, int runTimeInMinutes) {
			this.service = service;
			this.runTimeInMinutes = runTimeInMinutes;
			this.currentURIs = new ArrayList<String>();
		}
		
		private synchronized void loadProfileURIs() throws SQLException {
			LOG.info("Loading ProfileURI's to fetch");
	        Connection conn = dbUtil.getConnection();        
			try {
		        ResultSet rs = conn.createStatement().executeQuery("SELECT nodeId FROM [ORNG.].[vwPerson] WHERE isActive = 1");        
		        while (rs.next()) {
		        	currentURIs.add(systemBase + rs.getInt(1));
		        	LOG.log(Level.FINE, "Adding " + rs.getInt(1) + " to eager load list");
	        	}
		        rs.close();	        
			}
			finally {
				conn.close();
			}
		}
		
		public synchronized void run() {
			List<String> processedURIs = new ArrayList<String>();
			try {
				if (currentURIs.isEmpty()) {
					loadProfileURIs();
				}
				// load these things!
				DateTime start = new DateTime();
				for (String uri : currentURIs) {
					processedURIs.add(uri);
					service.getResource(uri);
					if (new Period(start, new DateTime()).getMinutes() > runTimeInMinutes) {
						LOG.info("Halting, out of time");
						break;
					}
				}
			}
			catch (Exception e) {
				LOG.log(Level.WARNING, e.getMessage(), e);
			}
			finally {
				// we at least TRIED to get these....
				LOG.info("Processed " + processedURIs.size() + " URI's");
				currentURIs.removeAll(processedURIs);				
			}
		}		
	}
	
	public void cleanup() {
		if (fusekiServer != null) {
			fusekiServer.stop();
		}
		// since this is running in a daemon thread, this really isn't necessary
		if (executorService != null) {
			executorService.shutdown();
		}
	}
	
    public static void main(String[] args) {
    	try {
    		OrngDBUtil dbUtil = new OrngDBUtil("Profiles", 
    									"jdbc:sqlserver://stage-sql-ctsi.ucsf.edu;instanceName=default;portNumber=1433;databaseName=profiles_200", 
    									"App_Profiles10", "Password1234");
    		new JsonLDService("Profiles", "http://stage-profiles.ucsf.edu/profiles200", 
    									"/shindig/Jena/", "025693078", "60", "5", dbUtil, null, null, null);
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }

}
