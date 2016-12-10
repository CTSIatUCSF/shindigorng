package edu.ucsf.orng.shindig.spi.rdf;

import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.jena.LODService;
import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.OrngDBUtil;

@Singleton
public class RdfService implements OrngProperties {

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
	
	@Inject
	public RdfService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain, 
							  OrngDBUtil dbUtil, CacheProvider cacheProvider, HttpFetcher fetcher) throws Exception {
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
		String viewerId =  token.getViewerId();
		boolean anonymous = viewerId == null || "-1".equals(viewerId);
		
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			// not sure where this logic belongs, but this seems as good as any
			if (SYS_PROFILES.equalsIgnoreCase(system)) {
				expand |= url.contains("ShowDetails=true");
			}
		}
		
		String cacheKey = uri + anonymous + expand;
		Model model = getFromModelCache(cacheKey);
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
	
	public boolean removeAppFromPerson(UserId userId, String appId, String deleteType, SecurityToken token) {
		String id = userId.getUserId(token);
		appId = dbUtil.getAppId(appId);
		// clear cache
		removeFromAppInstanceCache(id, appId);
        Connection conn = dbUtil.getConnection();
        try {
            CallableStatement cs = conn
    		        .prepareCall("{ call " + delete_sp + "(?, ?, ?, ?)}");
            cs.setNull("SubjectID", java.sql.Types.BIGINT);
    		cs.setString("SubjectURI", id);
    		cs.setInt("AppID", Integer.parseInt(appId));
    		cs.setInt("DeleteType", Integer.parseInt(deleteType));
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
				
}
