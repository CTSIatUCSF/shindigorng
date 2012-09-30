package edu.ucsf.profiles.shindig.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

public class ProfilesActivityService implements ActivityService {

	  /**
	   * The XML<->Bean converter
	   */
	  private BeanConverter converter;
	  private Common common;

	  @Inject
	  public ProfilesActivityService(@Named("shindig.bean.converter.xml")
	  BeanConverter converter, Common common) throws Exception {
	    this.converter = converter;
	    this.common = common;
	  }
	  
	  public Future<RestfulCollection<Activity>> getActivities(
			Set<UserId> userIds, GroupId groupId, String appId,
			Set<String> fields, CollectionOptions options, SecurityToken token)
			throws ProtocolException {
		appId = common.getAppId(appId);
		List<Activity> result = Lists.newArrayList();
		Connection conn = common.getConnection();
		try {
			Set<String> idSet = common.getIdSet(userIds, groupId, token);
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
	}

	public Future<RestfulCollection<Activity>> getActivities(UserId userId,
			GroupId groupId, String appId, Set<String> fields,
			CollectionOptions options, Set<String> activityIds,
			SecurityToken token) throws ProtocolException {
		appId = common.getAppId(appId);
		List<Activity> result = Lists.newArrayList();
		String user = userId.getUserId(token);
		Connection conn = common.getConnection();
		try {
			for (String strActivityId : activityIds) {

				int activityId = convertId(strActivityId);
				getActivity(conn, user, appId, activityId);
			}
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
		return ImmediateFuture.newInstance(new RestfulCollection<Activity>(
				result));	}

	public Future<Activity> getActivity(UserId userId, GroupId groupId,
			String appId, Set<String> fields, String activityId,
			SecurityToken token) throws ProtocolException {
		appId = common.getAppId(appId);
		String user = userId.getUserId(token);
		Connection conn = common.getConnection();
		try {
			int id = convertId(activityId);
			Activity act = getActivity(conn, user, appId, id);
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
		appId = common.getAppId(appId);
		String user = userId.getUserId(token);
		Connection conn = common.getConnection();
		try {
			for (String strActivityId : activityIds) {
				int activityId = convertId(strActivityId);
				deleteActivity(conn, user, appId, activityId);
			}
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
		return ImmediateFuture.newInstance(null);
	}

	public Future<Void> createActivity(UserId userId, GroupId groupId,
			String appId, Set<String> fields, Activity activity,
			SecurityToken token) throws ProtocolException {
		appId = common.getAppId(appId);
		String strId = userId.getUserId(token);
		Connection conn = common.getConnection();

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
		String sql = "select activity from shindig_activity where userId = ?" + (appId != null ? " AND appId=" + appId: "");
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setString(1, id);		
		ResultSet rs = ps.executeQuery();
		List<Activity> retVal = Lists.newArrayList();
		while (rs.next()) {
			retVal.add( converter.convertToObject( rs.getString("activity"), Activity.class)) ;
		}
		return retVal;
	}

	private Activity getActivity(Connection conn, String id, String appId,
			int activityId) throws SQLException {
		String sql = "select activity from shindig_activity where userId = ? AND activityId = ?" + (appId != null ? " AND appId=" + appId: "");
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setString(1, id);
		ps.setInt(2, activityId);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			return converter.convertToObject( rs.getString("activity"), Activity.class) ;
		}
		return null;
	}

	private void deleteActivity(Connection conn, String id, String appId,
			int activityId) throws SQLException {
		PreparedStatement ps = conn
				.prepareStatement("delete from shindig_activity where appId=? AND userId = ? AND activityId = ?");
		ps.setString(1, appId);
		ps.setString(2, id);
		ps.setInt(3, activityId);
		ps.execute();
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
		
		String sql = "insert into shindig_activity (appId, userId, activity" + (
				activityId > 0 ? ", activityId) VALUES (?,?,?,?)" : ") VALUES (?,?,?)");
		PreparedStatement ps = conn
				.prepareStatement(sql);
		ps.setString(1, appId);
		ps.setString(2, id);
		ps.setSQLXML(3, sqlXML);
		if (activityId > 0) {
			ps.setInt(4, activityId);
		}
		ps.execute();
	}

}
