package edu.ucsf.orng.shindig.spi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.DBUtil;
import edu.ucsf.orng.shindig.config.OrngProperties;

@Singleton
public class OrngDBUtil extends DBUtil implements OrngProperties {
	
	private static final Logger LOG = Logger.getLogger(OrngDBUtil.class.getName());		

	private String apps_table;
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	
	private Map<String, String> appIds = new HashMap<String, String>();

	@Inject
	public OrngDBUtil(
			@Named("orng.system") String system,
			@Named("orng.dbURL") String dbUrl,
			@Named("orng.dbUser") String dbUser,
			@Named("orng.dbPassword") String dbPassword) throws ClassNotFoundException {
		super(dbUrl, dbUser, dbPassword);
		this.apps_table = PROFILES.equalsIgnoreCase(system) ? "[ORNG.].[Apps]" : "orng_apps";
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		String sql = "select AppID, Url from " + apps_table;
		Connection conn = getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				addToAppIdsMap(rs.getString("Url").toLowerCase(), rs.getString("AppID"));
			}
		}
		catch (SQLException se) {
	        LOG.log(Level.SEVERE, "Error reading " + apps_table, se);
		}
		finally {
			try { conn.close(); } catch (SQLException se) {
		        LOG.log(Level.SEVERE, "Error closing connection", se);
			}
		}
	}
	
	String getAppId(String url) {
		// first look for match, if none found look for match based on name alone
		// if none found look up in db, if still none found then add a random one
		if (url == null) {
			return null;
		}
		url = url.toLowerCase();
		String id = appIds.get(url);
		if (id != null) {
			return id;
		}		
		String[] urlbits = url.split("/");
		id = appIds.get(urlbits[urlbits.length - 1]);
		if (id != null) {
			return id;
		}
		// see if its already numeric
		if (StringUtils.isNumeric(url)) {
			addToAppIdsMap(url, url);
			return url;			
		}

		// check DB
		String sql = "select AppID from " + apps_table + " where Url = ?";
		Connection conn = getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, url);		
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				addToAppIdsMap(url, rs.getString("AppID"));
				return rs.getString("AppID");
			}
		}
		catch (SQLException se) {
	        LOG.log(Level.SEVERE, "Error reading AppID", se);
		}
		finally {
			try { conn.close(); } catch (SQLException se) {
		        LOG.log(Level.SEVERE, "Error closing connection", se);
			}
		}
		// must be new
		addToAppIdsMap(url, "" + Math.abs(url.hashCode()));
		return appIds.get(url);
	}
	
	// allow to pull up by full URL, name or ID
	private void addToAppIdsMap(String url, String id) {
		appIds.put(url, id);
		String[] urlbits = url.split("/");
		appIds.put(urlbits[urlbits.length - 1], id);
		appIds.put(id,  id);
	}
	
    /**
     * Get the set of user id's from a user and group
     */
    Set<String> getIdSet(UserId user, GroupId group, SecurityToken token) {
        String userId = user.getUserId(token);

        LOG.log(Level.INFO, userId);
        if (group == null) {
            return ImmutableSortedSet.of(userId);
        }

        Set<String> returnVal = Sets.newLinkedHashSet();
        switch (group.getType()) {
        case all:
        case friends:
        case self:
            returnVal.add(userId);
            break;
        }
        return returnVal;
    }

    /**
     * Get the set of user id's for a set of users and a group
     */
    Set<String> getIdSet(Set<UserId> users, GroupId group,
            SecurityToken token) {
        Set<String> ids = Sets.newLinkedHashSet();
        for (UserId user : users) {
            ids.addAll(getIdSet(user, group, token));
        }
        return ids;
    }
    
    Connection getConnection(boolean create) {
        try {
            Properties props = new Properties();
            props.put("user", dbUser);
            props.put("password", dbPassword);
            if (create) {
                props.put("create", "true");
            }
            Connection conn = DriverManager.getConnection(dbUrl, props);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
       
}
