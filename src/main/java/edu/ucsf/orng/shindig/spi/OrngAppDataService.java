package edu.ucsf.orng.shindig.spi;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.orng.shindig.config.OrngProperties;

@Singleton
public class OrngAppDataService implements AppDataService, OrngProperties {
	
	private static final Logger LOG = Logger.getLogger(OrngAppDataService.class.getName());	
	
	private String read_sp;
	private String delete_sp;
	private String upsert_sp;
	private int appDataValueLimit;
	private OrngDBUtil dbUtil;
	private final Cache<String, Object> cache; 
	
	public final static String CHUNKED_MARKER = "---DATA CHUNKED BY ORNG SYSTEM---";
	public final static String CHUNKED_COUNT_SUFFIX = ".count";
	
	@Inject
	public OrngAppDataService(
			@Named("orng.system") String system, @Named("orng.appDataValueLimit") String appDataValueLimit, 
					OrngDBUtil dbUtil, CacheProvider cacheProvider)
			throws Exception {
		if (SYS_PROFILES.equalsIgnoreCase(system)) {
			this.read_sp = "[ORNG.].[ReadAppData]";
			this.delete_sp = "[ORNG.].[DeleteAppData]";
			this.upsert_sp = "[ORNG.].[UpsertAppData]";
		}
		else {
			//this.table = "orng_appdata";
			this.delete_sp = "orng_deleteAppData";
			this.upsert_sp = "orng_upsertAppData";
		}
		this.appDataValueLimit = Integer.parseInt(appDataValueLimit);
		this.dbUtil = dbUtil;
		// set up the cache
		cache = cacheProvider.createCache("orngAppData");   
	}
	
	private static String getCacheKey(String appId, String id, String key) {
		return appId + ":" + id + ":" + key;
	}

	public Future<DataCollection> getPersonData(Set<UserId> userIds, GroupId groupId,
    	      String appId, Set<String> fields, SecurityToken token) throws ProtocolException {    
		appId = dbUtil.getAppId(appId);
        Connection conn = null;
        try {
            Map<String, Map<String, Object>> idToData = Maps.newHashMap();
            Set<String> idSet = dbUtil.getIdSet(userIds, groupId, token);
            for (String id : idSet) {
            	if (id == null || id.isEmpty()) {
            		break;
            	}
            	Map<String, Object> data = Maps.newHashMap();
            	// but does data have the value we need?  Still need to check
                for (String key : fields) {
                	String cacheKey = getCacheKey(appId, id, key);
                	Object value = cache.getElement(cacheKey);
                	if (value == null) {
                		if (conn == null) {
                			// don't get one till we need it
                			conn = dbUtil.getConnection();            			
                		}
                        value = getData(conn, id, appId, key );
                        LOG.log(Level.INFO, "  "+key+" "+ value);
                        cache.addElement(cacheKey, value);
                	}
                    data.put(key, value);
                }
                idToData.put(id, data);
            }
            return Futures.immediateFuture(new DataCollection(idToData));
        } 
        catch (SQLException je) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je
                            .getMessage(), je);
        }
		finally {
			try {             
				if (conn != null) {
					conn.close();
				}
			} 
			catch (SQLException se) {
				throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
			}
		}
    }

    public Future<Void> deletePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, SecurityToken token)
            throws ProtocolException {
		appId = dbUtil.getAppId(appId);        
        Connection conn = dbUtil.getConnection();
        String id = userId.getUserId(token);

        try {
            for (String key : fields) {
                deleteData(conn, id, appId, key);
    			cache.removeElement(getCacheKey(appId, id, key));
            }
			conn.close();
            return Futures.immediateFuture(null);
        } catch (SQLException se) {
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

	public Future<Void> updatePersonData(UserId userId, GroupId groupId,
			String appId, Set<String> fields, Map<String, Object> values,
			SecurityToken token) throws ProtocolException {
		appId = dbUtil.getAppId(appId);
        Connection conn = dbUtil.getConnection();
        String id = userId.getUserId(token);
        try {
            for (String key : values.keySet()) {
            	Object value = values.get(key);  // somehow this can be an int
    			cache.addElement(getCacheKey(appId, id, key), value);
                upsertData(conn, id, appId, key, value != null ? value.toString() : "");
            }
            return Futures.immediateFuture(null);
        } catch (SQLException se) {
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

    private String getData(Connection conn, String id, String appId, Object key)
            throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + read_sp + "(?, ?, ?)}");
        cs.setString(1,id);
        cs.setInt(2, Integer.parseInt(appId));
        cs.setString(3, key != null ? key.toString() : null);
        ResultSet rs = cs.executeQuery();
        String value = null;
        if (rs.next()) {
            value = rs.getString("value");
        }
        if (CHUNKED_MARKER.equals(value)) {
        	value = "";
        	int count = Integer.parseInt(getData(conn, id, appId, key + CHUNKED_COUNT_SUFFIX));
        	for (int i = 0; i < count; i++) {
        		value += getData(conn, id, appId, key + "." + i);
        	}        		
        }
        return value;
    }

    private void deleteData(Connection conn, String id, String appId, String key)
            throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + delete_sp + "(?, ?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		cs.setString(3, key);
        cs.execute();
    }

    private void upsertData(Connection conn, String id, String appId,
            String key, String value) throws SQLException {
        CallableStatement cs = conn
                .prepareCall("{ call " + upsert_sp + "(?, ?, ?, ?)}");
        cs.setString(1,id);
        cs.setInt(2, Integer.parseInt(appId));
        if (value.length() > appDataValueLimit) {
        	// break it into pieces
            cs.setString(3, key);
            cs.setString(4, CHUNKED_MARKER);
            cs.execute();
            int numChunks = 0;
            for (String chunk : Splitter.fixedLength(appDataValueLimit).split(value)) {
                cs.setString(3, key + "." + numChunks++);
                cs.setString(4, chunk);            	
                cs.execute();            	            	
            }
            cs.setString(3, key + CHUNKED_COUNT_SUFFIX);
            cs.setString(4, "" + numChunks);
            cs.execute();
        }
        else {
	        cs.setString(3, key);
	        cs.setString(4, value);
	        cs.execute();
        }
    }

}
