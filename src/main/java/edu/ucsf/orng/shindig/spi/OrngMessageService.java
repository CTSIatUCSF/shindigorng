package edu.ucsf.orng.shindig.spi;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.model.OrngMessage;
import edu.ucsf.orng.shindig.model.OrngMessageCollection;

/*
 * TODO multi-recipient messages, multi collection messages, delete, groups
 */
@Singleton
public class OrngMessageService implements MessageService, OrngProperties {

	private String read_sp;
	private String readCollections_sp;
	private String insert_sp;
	private OrngDBUtil dbUtil;

	@Inject
	public OrngMessageService(
			@Named("orng.system") String system, OrngDBUtil dbUtil)
			throws Exception {
		if (PROFILES.equalsIgnoreCase(system)) {
			this.read_sp = "[ORNG.].[ReadMessages]";
			this.readCollections_sp = "[ORNG.].[ReadMessageCollections]";
			this.insert_sp = "[ORNG.].[InsertMessage]";
		}
		this.dbUtil = dbUtil;
	}
	
	/**
     * Post a message for a set of users.
     * 
     * @param userId
     *            The user sending the message.
     * @param appId
     *            The application sending the message.
     * @param msgCollId
     * @param message
     *            The message to post.
     */
    public Future<Void> createMessage(UserId userId, String appId, String msgCollId, Message message,
            SecurityToken token) throws ProtocolException {
		appId = dbUtil.getAppId(appId);
        Connection conn = dbUtil.getConnection();
        String from = userId.getUserId(token);
        try {
            for (String recipient : message.getRecipients()) {
                addMessage(conn, from, recipient, msgCollId, message);
            }
        } catch (SQLException se) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}

        return Futures.immediateFuture(null);
    }

    public Future<RestfulCollection<MessageCollection>> getMessageCollections(UserId userId, Set<String> fields,
            CollectionOptions options, SecurityToken token) throws ProtocolException {
        Connection conn = dbUtil.getConnection();
        try {
            List<MessageCollection> result = getMessageCollections(conn, userId.getUserId(token));
            return Futures.immediateFuture(new RestfulCollection<MessageCollection>(result));
        } catch (SQLException se) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
    }

    public Future<Void> deleteMessages(UserId userId, String msgCollId, List<String> ids, SecurityToken token)
            throws ProtocolException {
        throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED, "this functionality is not yet available");
    }

    /**
     * Gets the messsages in an user's queue.
     */
    public Future<RestfulCollection<Message>> getMessages(UserId userId, String msgCollId, Set<String> fields,
            List<String> msgIds, CollectionOptions options, SecurityToken token) throws ProtocolException {
        Connection conn = dbUtil.getConnection();
        try {
            List<Message> result = getMessages(conn, userId.getUserId(token), msgCollId, msgIds);
            return Futures.immediateFuture(new RestfulCollection<Message>(result));
        } catch (SQLException se) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
    }

    public Future<MessageCollection> createMessageCollection(UserId userId, MessageCollection msgCollection,
            SecurityToken token) throws ProtocolException {
        throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED, "this functionality is not yet available");
    }

    public Future<Void> modifyMessage(UserId userId, String msgCollId, String messageId, Message message,
            SecurityToken token) throws ProtocolException {
        throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED, "this functionality is not yet available");
    }

    public Future<Void> modifyMessageCollection(UserId userId, MessageCollection msgCollection, SecurityToken token)
            throws ProtocolException {
        throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED, "this functionality is not yet available");
    }

    public Future<Void> deleteMessageCollection(UserId userId, String msgCollId, SecurityToken token)
            throws ProtocolException {
        throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED, "this functionality is not yet available");
    }

    private List<Message> getMessages(Connection conn, String user, String coll, List<String> msgIds)
            throws SQLException {
        List<Message> retVal = Lists.newArrayList();
        CallableStatement cs = conn
		        .prepareCall("{ call " + read_sp + "(?, ?, ?)}");
		cs.setString(1, user);
		cs.setString(2, coll);
        if (msgIds != null && msgIds.size() > 0) {
    		String ids = "(";
            for (String msgId : msgIds) {
                ids += msgId + ",";
            }
            ids = ids.substring(0, ids.length()-1) + ")";
            cs.setString(3, ids);            
        }
        else {
        	cs.setString(3, null);
        }
		ResultSet rs = cs.executeQuery();
        while (rs.next()) {
            OrngMessage pm = new OrngMessage();
            pm.setId(rs.getString("msgId"));
            List<String> colls = Lists.newArrayList();
            colls.add(rs.getString("coll"));
            pm.setCollectionIds(colls);
            pm.setBody(rs.getString("body"));
            pm.setTitle(rs.getString("title"));
            pm.setSenderId(rs.getString("senderUri"));
            List<String> recipients = Lists.newArrayList();
            colls.add(rs.getString("recipientUri"));
            pm.setRecipients(recipients);
            retVal.add(pm);
        }

        return retVal;
    }
    

    private void addMessage(Connection conn, String from, String to, String coll, Message msg)
            throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + insert_sp + "(?, ?, ?, ?, ?, ?)}");
        cs.setString(0, msg.getId());
        cs.setString(1, coll);
        cs.setString(2, msg.getTitle());
        cs.setString(3, msg.getBody());
        cs.setString(4, from);
        cs.setString(5, to);
        cs.execute();
    }

    private List<MessageCollection> getMessageCollections(Connection conn, String user)
            throws SQLException {
        List<MessageCollection> retVal = Lists.newArrayList();
        CallableStatement cs = conn
		        .prepareCall("{ call " + readCollections_sp + "(?)}");
		cs.setString(1, user);
        ResultSet rs = cs.executeQuery();
        while (rs.next()) {
            OrngMessageCollection pmc = new OrngMessageCollection();
            pmc.setId(rs.getString("coll"));
            retVal.add(pmc);
        }

        return retVal;
    }

}
