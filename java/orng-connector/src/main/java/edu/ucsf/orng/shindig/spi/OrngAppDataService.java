package edu.ucsf.orng.shindig.spi;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;


public class OrngAppDataService implements AppDataService {
	
	private static final Logger LOG = Logger.getLogger(OrngAppDataService.class.getName());	
	
    public Future<DataCollection> getPersonData(Set<UserId> userIds, GroupId groupId,
    	      String appId, Set<String> fields, SecurityToken token) throws ProtocolException {    
        Connection conn = OrngUtil.getConnection();
        try {
            Map<String, Map<String, String>> idToData = Maps.newHashMap();
            Set<String> idSet = OrngUtil.getIdSet(userIds, groupId, token);
            for (String id : idSet) {
            	if (id == null || id.isEmpty()) {
            		break;
            	}
            	LOG.log(Level.INFO, "getPersonData " +id + " "
                         + groupId.getType() + " " + appId);
                Map<String, String> data = Maps.newHashMap();
                for (String key : fields) {
                    String value = getData(conn, id, appId, key );
                    LOG.log(Level.INFO, "  "+key+" "+ value);
                    data.put(key, value);
                }
                idToData.put(id, data);
            }
			conn.close();
            return ImmediateFuture.newInstance(new DataCollection(idToData));
        } catch (SQLException je) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je
                            .getMessage(), je);
        }
    }

    public Future<Void> deletePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, SecurityToken token)
            throws ProtocolException {
        
        Connection conn = OrngUtil.getConnection();
        String id = userId.getUserId(token);

        try {
            for (String key : fields) {
                deleteData(conn, id, appId, key);
            }
			conn.close();
            return ImmediateFuture.newInstance(null);
        } catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
    }

    public Future<Void> updatePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, Map<String, String> values,
            SecurityToken token) throws ProtocolException {
        Connection conn = OrngUtil.getConnection();
        String id = userId.getUserId(token);
        try {
            for (String key : values.keySet()) {
            	String value = values.get(key);
                upsertData(conn, id, appId, key, value);
            }
			conn.close();
            return ImmediateFuture.newInstance(null);
        } catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
    }

    private String getData(Connection conn, String id, String appId, String key)
            throws SQLException {
        PreparedStatement ps = conn
                .prepareStatement("select value from shindig_appdata where appId=? AND userId = ? AND keyName = ?");
        ps.setString(1, appId);
        ps.setString(2, id);
        ps.setString(3, key);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("value");
        }
        return null;
    }

    private void deleteData(Connection conn, String id, String appId, String key)
            throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call shindig_deleteAppData(?, ?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		cs.setString(3, key);
        cs.execute();
    }

    private void upsertData(Connection conn, String id, String appId,
            String key, String value) throws SQLException {
        CallableStatement cs = conn
                .prepareCall("{ call shindig_upsertAppData(?, ?, ?, ?)}");
        cs.setString(1,id);
        cs.setInt(2, Integer.parseInt(appId));
        cs.setString(3, key);
        cs.setString(4, value);
        cs.execute();
    }

}
