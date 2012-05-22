package edu.ucsf.orng.shindig.spi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Lists;

import edu.ucsf.orng.shindig.model.OrngMessage;
import edu.ucsf.orng.shindig.model.OrngMessageCollection;

/*
 * TODO multi-recipient messages, multi collection messages, delete, groups
 */
public class OrngMessageService implements MessageService {

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
        Connection conn = OrngUtil.getConnection();
        String from = userId.getUserId(token);
        try {
            for (String recipient : message.getRecipients()) {
                addMessage(conn, from, recipient, msgCollId, message);
            }
        } catch (SQLException se) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
        }

        return ImmediateFuture.newInstance(null);
    }

    public Future<RestfulCollection<MessageCollection>> getMessageCollections(UserId userId, Set<String> fields,
            CollectionOptions options, SecurityToken token) throws ProtocolException {
        Connection conn = OrngUtil.getConnection();
        try {
            List<MessageCollection> result = getMessageCollections(conn, userId.getUserId(token));
            return ImmediateFuture.newInstance(new RestfulCollection<MessageCollection>(result));
        } catch (SQLException se) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
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
        Connection conn = OrngUtil.getConnection();
        try {
            List<Message> result = getMessages(conn, userId.getUserId(token), msgCollId, msgIds);
            return ImmediateFuture.newInstance(new RestfulCollection<Message>(result));
        } catch (SQLException se) {
            throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se.getMessage(), se);
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
        String sql = "select * from shindig_messages where recipient = ? ";
        if (coll != null && !coll.trim().equals("")) {
            sql += "AND collection = ? ";
        }
        if (msgIds != null && msgIds.size() > 0) {
            sql += "AND msgId in ( ?";
            for (int i = 2; i <= msgIds.size(); i++) {
                sql += ", ?";
            }
            sql += " )";
        }
        PreparedStatement ps = conn.prepareCall(sql);
        int index = 0;
        ps.setString(index++, user);
        if (coll != null && !coll.trim().equals("")) {
            ps.setString(index++, coll);
        }
        if (msgIds != null && msgIds.size() > 0) {
            for (int i = 0; i < msgIds.size(); i++) {
                ps.setString(index++, msgIds.get(i));
            }
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            OrngMessage pm = new OrngMessage();
            pm.setId(rs.getString("msgId"));
            List<String> colls = Lists.newArrayList();
            colls.add(rs.getString("coll"));
            pm.setCollectionIds(colls);
            pm.setBody(rs.getString("body"));
            pm.setTitle(rs.getString("title"));
            pm.setSenderId(rs.getString("senderId"));
            List<String> recipients = Lists.newArrayList();
            colls.add(rs.getString("recipient"));
            pm.setRecipients(recipients);
            retVal.add(pm);
        }

        return retVal;
    }
    

    private void addMessage(Connection conn, String from, String to, String coll, Message msg)
            throws SQLException {
        String sql = "insert into shindig_messages (msgId, coll, title, body, senderId, recipientId) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareCall(sql);
        ps.setString(0, msg.getId());
        ps.setString(1, coll);
        ps.setString(2, msg.getTitle());
        ps.setString(3, msg.getBody());
        ps.setString(4, from);
        ps.setString(5, to);
        ps.execute();
    }

    private List<MessageCollection> getMessageCollections(Connection conn, String user)
            throws SQLException {
        List<MessageCollection> retVal = Lists.newArrayList();
        String sql = "select distinct(coll) from shindig_messages where recipient = ? ";
        
        PreparedStatement ps = conn.prepareCall(sql);
        int index = 0;
        ps.setString(index++, user);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            OrngMessageCollection pmc = new OrngMessageCollection();
            pmc.setId(rs.getString("coll"));
            retVal.add(pmc);
        }

        return retVal;
    }

    public static void init() {
        try {
            Connection conn = OrngUtil.getConnection(true);
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE shindig_messages(msgId varchar(255),senderId varchar(255),recipientId varchar(255),coll varchar(255),title varchar(4095),body varchar(4095))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
