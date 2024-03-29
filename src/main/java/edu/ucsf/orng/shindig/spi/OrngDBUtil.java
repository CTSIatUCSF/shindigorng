package edu.ucsf.orng.shindig.spi;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupCapable;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupHandler;
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
public class OrngDBUtil extends DBUtil implements OrngProperties, CleanupCapable {
	
	private static final Logger LOG = Logger.getLogger(OrngDBUtil.class.getName());		

	private String apps_table;
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	
	private String orngUserURI = null;
	
	private Map<String, String> appIds = new HashMap<String, String>();

	@Inject
	public OrngDBUtil(
			@Named("orng.system") String system,
			@Named("orng.dbDriver") String dbDriver,
			@Named("orng.dbURL") String dbUrl,
			@Named("orng.dbUser") String dbUser,
			@Named("orng.dbPassword") String dbPassword,
			@Named("orng.user") String orngUser,
			CleanupHandler cleanup) throws ClassNotFoundException {
		super(dbUrl, dbUser, dbPassword);

		// load the DB Driver
		Class.forName(dbDriver);
		if (cleanup != null) {
			cleanup.register(this);
		}

		this.apps_table = SYS_PROFILES.equalsIgnoreCase(system) ? "[ORNG.].[Apps]" : "orng_apps";
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		Connection conn = getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement("select AppID, Url from " + apps_table);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				addToAppIdsMap(rs.getString("Url").toLowerCase(), rs.getString("AppID"));
			}
			ps.close();
			
			// load ORNG user
			if (orngUser != null) {
				ps = conn.prepareStatement("SELECT p.Value + CAST(m.NodeID AS VARCHAR(50)) " + 
						"FROM [RDF.Stage].InternalNodeMap m, [Framework.].[Parameter] p " + 
						"WHERE m.InternalID = (select UserID from [User.Account].[User]  where InternalUserName = '" + orngUser + "') " + 
						"AND m.InternalType = 'User'\n" + 
						"AND m.Class = 'http://profiles.catalyst.harvard.edu/ontology/prns#User' " + 
						"AND p.ParameterID = 'baseURI'");
				rs = ps.executeQuery();
				if (rs.next()) {
					orngUserURI = rs.getString(1);
				}	
				ps.close();
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
	
	public String getAppId(String url) {
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
		String sql = "select AppID from " + apps_table + " where Url = ? UNION " +
				"select AppID from [UCSF.ORNG].[InstitutionalizedApps] where Url = ?";
		Connection conn = getConnection();
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setString(1, url);		
			ps.setString(2, url);		
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
	
	public String getORNGUserURI() {
		return orngUserURI;
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
		default:
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
    
    public Connection getConnection(boolean create) {
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

	public void cleanup() {
        // This manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks wrto this class
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                LOG.log(Level.INFO, String.format("deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, String.format("Error deregistering driver %s", driver), e);
            }

        }
    }
       
}
