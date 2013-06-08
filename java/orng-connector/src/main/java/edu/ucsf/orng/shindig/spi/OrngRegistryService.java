package edu.ucsf.orng.shindig.spi;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.orng.shindig.config.OrngProperties;


public class OrngRegistryService implements OrngProperties {
	
	private static final Logger LOG = Logger.getLogger(OrngRegistryService.class.getName());	
	
	private String table;
	private String upsert_sp;
	private OrngDBUtil dbUtil;
	
	@Inject
	public OrngRegistryService(
			@Named("orng.system") String system, OrngDBUtil dbUtil)
			throws Exception {
		if (PROFILES.equalsIgnoreCase(system)) {
			this.table = "[ORNG].[AppRegistry]";
			this.upsert_sp = "[ORNG].[RegisterAppPerson]";
		}
		else {
			// TODO
			//this.table = "orng_appdata";
			//this.upsert_sp = "orng_upsertAppData";
		}
		this.dbUtil = dbUtil;
	}

	public Future<Map<String, String>> getVisibility(Set<UserId> userIds, GroupId groupId,
    	      String appId, SecurityToken token) throws ProtocolException {    
		appId = dbUtil.getAppId(token.getAppUrl());
        Connection conn = dbUtil.getConnection();
        try {
            Map<String, String> idToData = Maps.newHashMap();
            Set<String> idSet = dbUtil.getIdSet(userIds, groupId, token);
            for (String id : idSet) {
            	if (id == null || id.isEmpty()) {
            		break;
            	}
            	LOG.log(Level.INFO, "getRegistry " +id + " "
                         + groupId.getType() + " " + appId);
            	idToData.put(id, getVisibility(conn, id, appId));
            }
			conn.close();
            return ImmediateFuture.newInstance(idToData);
        } catch (SQLException je) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je
                            .getMessage(), je);
        }
    }

    public Future<Void> setVisibility(UserId userId, GroupId groupId,
            String appId, SecurityToken token, String visibility) throws ProtocolException {
		appId = dbUtil.getAppId(token.getAppUrl());
        Connection conn = dbUtil.getConnection();
        String id = userId.getUserId(token);
        try {
        	setVisibility(conn, id, appId, visibility);
			conn.close();
            return ImmediateFuture.newInstance(null);
        } catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
    }

    private String getVisibility(Connection conn, String id, String appId)
            throws SQLException {
        PreparedStatement ps = conn
                .prepareStatement("select visibility from " + table + " where appId=? AND uri = ?");
        ps.setString(1, appId);
        ps.setString(2, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString(1);
        }
        return null;
    }

    private void setVisibility(Connection conn, String id, String appId, String visibility)
            throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + upsert_sp + "(?, ?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		cs.setString(3, visibility);
        cs.execute();
    }

}
