package edu.ucsf.orng.shindig.spi.rdf;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.logging.Logger;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.spi.OrngDBUtil;

public class DbModelService implements JenaModelService, OrngProperties {

	private static final Logger LOG = Logger.getLogger(DbModelService.class.getName());
	
	private String systemBase;
	private String orngUser;
	private OrngDBUtil dbUtil;
	private boolean showDetails = true;
	private boolean expand = false;
	
	@Inject
	public DbModelService(@Named("orng.systemDomain") String systemDomain,  @Named("orng.user") String orngUser, OrngDBUtil dbUtil) {
    	this.systemBase = systemDomain += "/profile/";
		this.orngUser = orngUser;
		// set up the cache
    	this.dbUtil = dbUtil;    	
	}
	
	// inject someday
	public void setProfilesOptions(boolean showDetails, boolean expand) {
		this.showDetails = showDetails;
		this.expand = expand;
	}
	
	public Model getModel(String uri) throws Exception {
		Model model = ModelFactory.createDefaultModel();
		Integer nodeId = getNodeID(uri);
		if (nodeId ==  null) {
			return null;
		}
		Connection conn = dbUtil.getConnection();        
		try {
			loadIntoModel(model, getSessionId(conn), conn, nodeId, showDetails, expand);
		}
		finally {
			conn.close();
		}
		return model;
	}
	
	// need to make this smarter!
	private Integer getNodeID(String uri) {
		if (uri.toLowerCase().startsWith(systemBase)) {
			return Integer.parseInt(uri.split(systemBase)[1]);
		}
		return null;
	}
	
	private String getSessionId(Connection conn) throws SQLException {
		// log in, maybe check if username is null and do anonymous TDB in that case
		if (orngUser != null) {
	        CallableStatement cs = conn.prepareCall("{ call [User.Account].[Authenticate](?, ?, ?)}");
	        cs.setString(1, orngUser);
	        cs.setString(2, orngUser);
	        cs.registerOutParameter(3, java.sql.Types.INTEGER);
	        cs.executeUpdate();
	        int userId = cs.getInt(3);
	        cs.close();
	        
	        cs = conn.prepareCall("{ call [User.Session].[CreateSession](?, ?, ?)}");
	        cs.setString(1, "127.0.0.1");
	        cs.setString(2, "ORNG");
	        cs.setInt(3, userId);
	        ResultSet rs = cs.executeQuery();
	        if (rs.next()) {
	        	return rs.getString(1);
	        }
		}
		return null;
	}
	
	private void loadIntoModel(Model model, String sessionId, Connection conn, int nodeId, boolean showDetails, boolean expand) throws SQLException {
        CallableStatement cs = conn.prepareCall("{ call [RDF.].[GetDataRDF](?, ?, ?, ?, ? ,? ,? ,?)}");
        cs.setInt(1, nodeId);
        cs.setNull(2, java.sql.Types.BIGINT);
        cs.setNull(3, java.sql.Types.BIGINT);
        cs.setNull(4, java.sql.Types.BIGINT);
        cs.setNull(5, java.sql.Types.BIGINT);
        cs.setBoolean(6, showDetails);
        cs.setBoolean(7, expand);
        cs.setString(8, sessionId);

        ResultSet rs = cs.executeQuery();
        if (rs.next()) {
        	SQLXML sx = rs.getSQLXML(1);
            model.read(sx.getCharacterStream(), null);
        }
    	LOG.info("Just read " + nodeId + " from the database");	        	
	}
	
}
