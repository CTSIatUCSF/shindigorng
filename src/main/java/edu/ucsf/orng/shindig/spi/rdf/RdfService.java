package edu.ucsf.orng.shindig.spi.rdf;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.fuseki.embedded.FusekiEmbeddedServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupCapable;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupHandler;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.joda.time.DateTime;
import org.joda.time.Period;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.jena.DbService;
import edu.ucsf.ctsi.r2r.jena.FusekiCache;
import edu.ucsf.ctsi.r2r.jena.LODService;
import edu.ucsf.ctsi.r2r.jena.ResourceService;
import edu.ucsf.ctsi.r2r.jena.SparqlQueryClient;
import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.OrngDBUtil;

@Singleton
public class RdfService implements OrngProperties, CleanupCapable {

	private static final Logger LOG = Logger.getLogger(RdfService.class.getName());
	
	private String system;
	private String systemDomain;
	private String systemBase;
	private String get_sp;
	private String add_sp;
	private String delete_sp;
	private OrngDBUtil dbUtil;
	private Cache<String, Model> modelCache;
	private Cache<String, String> appInstanceCache;
	
	private FusekiCache userCache;
	private FusekiCache anonymousCache;
	private FusekiEmbeddedServer fusekiServer = null;
	private ScheduledExecutorService executorService;
	
	@Inject
	public RdfService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain, 
							  @Named("orng.fuseki") String fuseki, @Named("orng.fusekiDBUser") String orngUser, 
							  @Named("orng.fusekiFetchIntervalMinutes") String fetchInterval, @Named("orng.fusekiEagerRunLimitMinutes") String eagerRunLimit,
							  OrngDBUtil dbUtil, CacheProvider cacheProvider, HttpFetcher fetcher,
							  CleanupHandler cleanup) throws Exception {
		this.system = system;
		this.systemDomain = systemDomain;
		this.systemBase = systemDomain;
		if (SYS_PROFILES.equalsIgnoreCase(system)) {
			systemBase += "/profile/";
			this.get_sp = "[ORNG.].[GetAppInstance]";
			this.add_sp = "[ORNG.].[AddAppToPerson]";
			this.delete_sp = "[ORNG.].[RemoveAppFromPerson]";
		}		

		// set up the cache
    	this.dbUtil = dbUtil;
    	
    	if (cacheProvider != null) {
    		modelCache = cacheProvider.createCache("orngRdf");
    		appInstanceCache = cacheProvider.createCache("orgnAppInstance");
    	}
    	if (cleanup != null) {
    		cleanup.register(this);
    	}

    	if (StringUtils.isNotBlank(fuseki)) {
    		String fusekiURL = null;
    		if (fuseki.startsWith("embedded")) {
    			int port = fuseki.contains(":") ? Integer.parseInt(fuseki.split(":")[1]) : 3330;
    			String dir = System.getProperty("java.io.tmpdir") + "/RDF/" + port;
    			LOG.info("Using RDF directory :" + dir);
    			new File(dir).mkdirs();    			
    			Dataset pds = TDBFactory.createDataset(dir);//DatasetFactory.assemble("profiles.ttl");
    			fusekiServer = FusekiEmbeddedServer.create().add("/profiles", pds).setPort(port).build();
    			fusekiServer.start();
    			fusekiURL = "http://localhost:" + port + "/profiles";
    		}
    		else {
    			fusekiURL = fuseki;
    		}
    		
    		DbService dbService = new DbService(systemDomain, orngUser, dbUtil);
	    	userCache = new FusekiCache( new SparqlQueryClient(fusekiURL + "/query"),
	    								new ShindigSparqlPostClient(fusekiURL + "/update", fusekiURL + "/data?default", fetcher), 
	    								dbService, dbService);
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
	
	private Model getFromModelCache(String key) {
		return modelCache != null ? modelCache.getElement(key) : null;
	}
	
	private void addToModelCache(String key, Model value) {
		if (modelCache != null) {
			modelCache.addElement(key, value);
		}
	}
	
	// This cache is OK because Profiles does not actually mess with the appInstance
	// actually need to think about that....
	private String getKey(String id, String appId) {
		return id + "-" + appId;
	}
	
	private String getFromAppInstanceCache(String id, String appId) {
		return appInstanceCache != null ? appInstanceCache.getElement(getKey(id, appId)) : null;
	}
	
	private void addToAppInstanceCache(String id, String appId, String value) {
		if (appInstanceCache != null) {
			appInstanceCache.addElement(getKey(id, appId), value);
		}
	}

	private String removeFromAppInstanceCache(String id, String appId) {
		return appInstanceCache != null ? appInstanceCache.removeElement(getKey(id, appId)) : null;
	}

	// maybe have some option to force fresh or allow cache, etc.
	public RdfItem getRDF(String url, boolean nocache, boolean expand, Set<String> fields, String sessionId, SecurityToken token) throws Exception {		
		String uri = getURI(url);
		boolean local = false;
		String viewerId =  token.getViewerId();
		boolean anonymous = viewerId == null || "-1".equals(viewerId);
		
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			local = true;
			// not sure where this logic belongs, but this seems as good as any
			if (SYS_PROFILES.equalsIgnoreCase(system)) {
				expand |= url.contains("ShowDetails=true");
			}
		}
		
		String cacheKey = uri + anonymous + expand;
		Model model = getFromModelCache(cacheKey);
		if (model != null) {
			return new RdfItem(model, uri);
		}
		// we get non URI's coming into here, need to find a better way to work with those.
		if (local && uri.indexOf('?') == -1 && !nocache && !expand) {
			FusekiCache cache = anonymous ? anonymousCache : userCache;
			if (cache != null) {
				model = (fields == null || fields.size() == 0) ? cache.getModel(uri) : cache.getModel(uri, fields);
			}
		}
		
		if (model == null) {
			// this can grab anything, and knows how to directly grab data from a local Profiles
			LODService service = new LODService(systemDomain, sessionId, viewerId, true, expand);
			model = service.getModel(uri);
			// this is the only one worth caching, others are fast enough as is
			if (model != null) {
	        	addToModelCache(cacheKey, model);
			}
		}
		return new RdfItem(model, uri);

	}
	
	private String getURI(String url) throws UnsupportedEncodingException {
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		String uri = url;
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			if (SYS_PROFILES.equalsIgnoreCase(system)) {
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
	
	public String getAppInstance(UserId userId, String appId, SecurityToken token) {
		String id = userId.getUserId(token);
		appId = dbUtil.getAppId(appId);
		String retval = getFromAppInstanceCache(id, appId);
		if (retval != null) {
			// cheap trick to return null when we've checked before and have no entry
			return retval != "" ? retval : null; 
		}
        Connection conn = dbUtil.getConnection();
        try {
            CallableStatement cs = conn
    		        .prepareCall("{ call " + get_sp + "(?, ?, ?, ?)}");
            cs.setNull("SubjectID", java.sql.Types.BIGINT);
    		cs.setString("SubjectURI", id);
    		cs.setInt("AppID", Integer.parseInt(appId));
            cs.setNull("SessionID", java.sql.Types.VARCHAR);
        	ResultSet rs = cs.executeQuery();
            if (rs.next()) {
            	retval = rs.getString(1);
            	addToAppInstanceCache(id, appId, retval != null ? retval : "");
            }
            return retval; 
        } 
        catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
	}
	
	public boolean addAppToPerson(UserId userId, String appId, SecurityToken token) {
		String id = userId.getUserId(token);
		appId = dbUtil.getAppId(appId);
		// clear cache
		removeFromAppInstanceCache(id, appId);
		Connection conn = dbUtil.getConnection();
        try {
            CallableStatement cs = conn
    		        .prepareCall("{ call " + add_sp + "(?, ?, ?)}");
            cs.setNull("SubjectID", java.sql.Types.BIGINT);
    		cs.setString("SubjectURI", id);
    		cs.setInt("AppID", Integer.parseInt(appId));
            return cs.execute();
        } 
        catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
	}
	
	public boolean removeAppFromPerson(UserId userId, String appId, SecurityToken token) {
		String id = userId.getUserId(token);
		appId = dbUtil.getAppId(appId);
		// clear cache
		removeFromAppInstanceCache(id, appId);
        Connection conn = dbUtil.getConnection();
        try {
            CallableStatement cs = conn
    		        .prepareCall("{ call " + delete_sp + "(?, ?, ?)}");
            cs.setNull("SubjectID", java.sql.Types.BIGINT);
    		cs.setString("SubjectURI", id);
    		cs.setInt("AppID", Integer.parseInt(appId));
            return cs.execute();
        } 
        catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
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
					try {
						service.getResource(uri);
					}
					catch (Exception e) {
						LOG.log(Level.WARNING, "Unable to process URI: " + uri + ", " + e.getMessage(), e);
					}					
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
		// since this is running in a daemon thread, this really isn't necessary
		if (executorService != null) {
			executorService.shutdown();
		}
		if (fusekiServer != null) {
			fusekiServer.stop();
			fusekiServer.join();
		}
	}
	

}
