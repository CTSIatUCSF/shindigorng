package edu.ucsf.orng.shindig.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.orng.shindig.config.OrngProperties;


public class OrngActivityService implements ActivityService, OrngProperties {

	/**
	 * The XML<->Bean converter
	 */
	private BeanConverter converter;
	private String read_sp;
	private String readAll_sp;
	private String delete_sp;
	private String insert_sp;
	private OrngDBUtil dbUtil;

	@Inject
	public OrngActivityService(
			@Named("orng.system") String system,
			@Named("shindig.bean.converter.xml") BeanConverter converter, OrngDBUtil dbUtil)
			throws Exception {
		this.converter = converter;
		if (PROFILES.equalsIgnoreCase(system)) {
			this.read_sp = "[ORNG.].[ReadActivity]";
			this.readAll_sp = "[ORNG.].[ReadAllActivities]";
			this.delete_sp = "[ORNG.].[DeleteActivity]";
			this.insert_sp = "[ORNG.].[InsertActivity]";
		}
		this.dbUtil = dbUtil;
	}

	public Future<RestfulCollection<Activity>> getActivities(
			Set<UserId> userIds, GroupId groupId, String appId,
			Set<String> fields, CollectionOptions options, SecurityToken token)
			throws ProtocolException {
		appId = dbUtil.getAppId(appId);
		List<Activity> result = Lists.newArrayList();
		Connection conn = dbUtil.getConnection();
		try {
			Set<String> idSet = dbUtil.getIdSet(userIds, groupId, token);
			for (String id : idSet) {
				result.addAll(getAllActivities(conn, id, appId));
			}
			return ImmediateFuture.newInstance(new RestfulCollection<Activity>(
					result));
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

	public Future<RestfulCollection<Activity>> getActivities(UserId userId,
			GroupId groupId, String appId, Set<String> fields,
			CollectionOptions options, Set<String> activityIds,
			SecurityToken token) throws ProtocolException {
		appId = dbUtil.getAppId(appId);
		List<Activity> result = Lists.newArrayList();
		String user = userId.getUserId(token);
		Connection conn = dbUtil.getConnection();
		for (String strActivityId : activityIds) {
			try {

				int activityId = convertId(strActivityId);
				getActivity(conn, user, appId, activityId);
				conn.close();
			} catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
		try { conn.close(); } catch (SQLException se) {
			throw new ProtocolException(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
							.getMessage(), se);
		}
		return ImmediateFuture.newInstance(new RestfulCollection<Activity>(
				result));	}

	public Future<Activity> getActivity(UserId userId, GroupId groupId,
			String appId, Set<String> fields, String activityId,
			SecurityToken token) throws ProtocolException {
		appId = dbUtil.getAppId(appId);
		Connection conn = dbUtil.getConnection();
		try {
			String user = userId.getUserId(token);
			int id = convertId(activityId);
			Activity act = getActivity(conn, user, appId, id);
			conn.close();
			if (act != null) {
				return ImmediateFuture.newInstance(act);
			}

			throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
					"Activity not found");
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

	public Future<Void> deleteActivities(UserId userId, GroupId groupId,
			String appId, Set<String> activityIds, SecurityToken token)
			throws ProtocolException {
		appId = dbUtil.getAppId(appId);
		String user = userId.getUserId(token);
		Connection conn = dbUtil.getConnection();
		for (String strActivityId : activityIds) {
			try {

				int activityId = convertId(strActivityId);
				deleteActivity(conn, user, appId, activityId);
			} catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
		try { conn.close(); } catch (SQLException se) {
			throw new ProtocolException(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
							.getMessage(), se);
		}
		return ImmediateFuture.newInstance(null);
	}

	public Future<Void> createActivity(UserId userId, GroupId groupId,
			String appId, Set<String> fields, Activity activity,
			SecurityToken token) throws ProtocolException {
		appId = dbUtil.getAppId(appId);
		String strId = userId.getUserId(token);
		Connection conn = dbUtil.getConnection();

		// Are fields really needed here?
		try {
			int activityId = convertId(activity.getId());
			
			insertActivity(conn, strId, appId, activityId, activity);
			return ImmediateFuture.newInstance(null);
		} catch (SQLException se) {
			throw new ProtocolException(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
							.getMessage(), se);
		} catch (Exception ex) {
			throw new ProtocolException(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex
							.getMessage(), ex);
		}
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
	}

	private int convertId(String activityId) {
		int id = 0;
		try {
			id = Integer.parseInt(activityId);
		} catch (Exception e) {
			id = -1;
		}
		return id;
	}


	private List<Activity> getAllActivities(Connection conn, String id, String appId) throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + readAll_sp + "(?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		ResultSet rs = cs.executeQuery();
		List<Activity> retVal = Lists.newArrayList();
		while (rs.next()) {
			retVal.add( converter.convertToObject( rs.getString("activity"), Activity.class)) ;
		}
		return retVal;
	}

	private Activity getActivity(Connection conn, String id, String appId,
			int activityId) throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + read_sp + "(?, ?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		cs.setInt(3, activityId);
		ResultSet rs = cs.executeQuery();
		if (rs.next()) {
			return converter.convertToObject( rs.getString("activity"), Activity.class) ;
		}
		return null;
	}

	private void deleteActivity(Connection conn, String id, String appId,
			int activityId) throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call " + delete_sp + "(?, ?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		cs.setInt(3, activityId);
        cs.execute();
	}

	private void insertActivity(Connection conn, String id, String appId,
			int activityId, Activity act) throws SQLException, TransformerException, IOException, SAXException, ParserConfigurationException {

		String activityStr = converter.convertToString(act);
		// the converter adds a parent node that we need to remove, big pain in the ar**
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();			
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(activityStr.getBytes()));
        
        // Use a Transformer for output
        TransformerFactory tFactory =
          TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        
        //grab child node and put into SQLXML object
        SQLXML sqlXML = conn.createSQLXML();
        DOMSource source = new DOMSource(doc.getElementsByTagName("activity").item(0));
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
        transformer.transform(source, new StreamResult(sqlXML.setCharacterStream()));
        
        //baos now has the content we want to persist
		// To TEST that we can rebuild object
        //Activity foo = converter.convertToObject(baos.toString(), Activity.class);
        CallableStatement cs = conn
		        .prepareCall("{ call " + insert_sp + "(?, ?, ?, ?)}");
		cs.setString(1, id);
		cs.setInt(2, Integer.parseInt(appId));
		cs.setInt(3, activityId);
		cs.setSQLXML(4, sqlXML);
        cs.execute();
	}

}
