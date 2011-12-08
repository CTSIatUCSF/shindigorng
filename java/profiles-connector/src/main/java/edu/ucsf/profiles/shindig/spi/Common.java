package edu.ucsf.profiles.shindig.spi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Common {

	@Inject
	@Named("dbURL") private static String dbUrl;

	@Inject
	@Named("dbUser") private static String dbUser;

	@Inject
	@Named("dbPassword") private static String dbPassword;

    public Common() throws ClassNotFoundException {
    	//Class.forName("org.apache.derby.jdbc.ClientDriver");
    	Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }
    
    /**
     * Get the set of user id's from a user and group
     */
    public static Set<String> getIdSet(UserId user, GroupId group, SecurityToken token) {
        String userId = user.getUserId(token);

        System.out.println(userId);
        if (group == null) {
            return ImmutableSortedSet.of(userId);
        }

        Set<String> returnVal = Sets.newLinkedHashSet();
        switch (group.getType()) {
        case all:
        case friends:
        case groupId:
            break;
        case self:
            returnVal.add(userId);
            break;
        case deleted:
            break;
        }
        return returnVal;
    }

    /**
     * Get the set of user id's for a set of users and a group
     */
    public static Set<String> getIdSet(Set<UserId> users, GroupId group,
            SecurityToken token) {
        Set<String> ids = Sets.newLinkedHashSet();
        for (UserId user : users) {
            ids.addAll(getIdSet(user, group, token));
        }
        return ids;
    }
    
    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(dbUrl, dbUser,
                    dbPassword);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Connection getConnection(boolean create) {
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
