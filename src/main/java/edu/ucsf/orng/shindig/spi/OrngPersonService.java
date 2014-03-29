package edu.ucsf.orng.shindig.spi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.core.model.ListFieldImpl;
import org.apache.shindig.social.opensocial.model.ListField;
import org.apache.shindig.social.opensocial.model.Name;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.R2RConstants;
import edu.ucsf.orng.shindig.config.OrngProperties;
import edu.ucsf.orng.shindig.model.OrngName;
import edu.ucsf.orng.shindig.model.OrngPerson;
import edu.ucsf.orng.shindig.spi.rdf.JsonLDService;
import edu.ucsf.orng.shindig.spi.rdf.RdfService;

/**
 * Implementation of supported services backed by a JSON DB.
 */
@Singleton
public class OrngPersonService implements PersonService, OrngProperties, R2RConstants {

	private static final Logger LOG = Logger.getLogger(OrngPersonService.class.getName());	
	
	// ontlogy items
	
	
	private String read_sp;
	private final RdfService rdfService;
	private final JsonLDService jsonldService;
	private final OrngDBUtil dbUtil;
	private final Cache<String, Person> cache; 
	private Map<String, Method> personSetMethods = new HashMap<String, Method>();

	@Inject
	public OrngPersonService(@Named("orng.system") String system, RdfService rdfService, JsonLDService jsonldService, OrngDBUtil dbUtil, CacheProvider cacheProvider) {
		this.rdfService = rdfService;
		this.jsonldService = jsonldService;
		this.dbUtil = dbUtil;
		if (SYS_PROFILES.equalsIgnoreCase(system)) {
			this.read_sp = "[ORNG.].[ReadPerson]";
		}
		else {
			//this.table = "orng_appdata";
		}
		// wire up set methods
		for (Method method : OrngPerson.class.getDeclaredMethods()) {
			if (method.getName().startsWith("set")) {
				// should also test for accessibility and argument types, but wait on that
				personSetMethods.put(method.getName().toLowerCase(), method);
			}
		}
		// set up the cache
		cache = cacheProvider.createCache("orngPerson");
	}

	public Future<RestfulCollection<Person>> getPeople(Set<UserId> userIds,
			GroupId groupId, CollectionOptions options, Set<String> fields,
			SecurityToken token) throws ProtocolException {
		// TODO collection options
		// TODO fields
		List<Person> result = Lists.newArrayList();

		if (userIds.size() == 0) {
			return Futures.immediateFuture(null);
		}

		int firstResult = 0;
		if (options != null) {
			firstResult = options.getFirst();
		}
		return Futures.immediateFuture(new RestfulCollection<Person>(
				result, firstResult, result.size()));
	}

	public Future<Person> getPerson(UserId id, Set<String> fields,
			SecurityToken token) throws ProtocolException {
		String strId = id.getUserId(token);
		LOG.log(Level.INFO, "getPerson uri=" + strId);
		
		Person personObj = cache.getElement(strId);
		if (personObj != null) {
			return Futures.immediateFuture(personObj);
		}
		
		try {
			// There can be only one!
			if (strId != null) {				
				JSONObject personJSON = jsonldService.getJSONObject(rdfService.getRDF(strId, false, null, null, null, token));
				personObj = parsePerson(strId, personJSON);
				cache.addElement(strId, personObj);
				return Futures.immediateFuture(personObj);
			}
		} catch (MalformedURLException e) {
			throw new ProtocolException(0, e.getMessage(), e);
		} catch (IOException e) {
			throw new ProtocolException(0, e.getMessage(), e);
		} catch (JSONException e) {
			throw new ProtocolException(0, e.getMessage(), e);
		} catch (Exception e) {
			throw new ProtocolException(0, e.getMessage(), e);
		}
		return Futures.immediateFuture(null);
	}
	
	private Person parsePerson(String strId, JSONObject json) throws JSONException {
		OrngPerson retVal = new OrngPerson();
		retVal.setId(strId);
		if (json == null) {
			return retVal;
		}

		retVal.setProfileUrl(strId);
		if (json.has(PRNS + "mainImage") && json.getString(PRNS + "mainImage") != null) {
			retVal.setThumbnailUrl(json.getString(PRNS + "mainImage"));
		}
		if (json.has(RDFS + "label") && json.getString(RDFS + "label") != null) {
			retVal.setDisplayName(json.getString(RDFS + "label"));
		}
		if (json.has(VIVO + "email") && json.getString(VIVO + "email") != null) {
			List<ListField> emails = new ArrayList<ListField>();
			emails.add( new ListFieldImpl(null, json.getString(VIVO + "email")) );
			retVal.setEmails(emails);
		}
		else if (json.has(VIVO + "primaryEmail") && json.getString(VIVO + "primaryEmail") != null) {
			List<ListField> emails = new ArrayList<ListField>();
			emails.add( new ListFieldImpl(null, json.getString(VIVO + "primaryEmail")) );
			retVal.setEmails(emails);
		}
		Name name = new OrngName();
		if (json.has(FOAF + "firstName") && json.getString(FOAF + "firstName") != null) {				
			name.setGivenName(json.getString(FOAF + "firstName"));
		}
		if (json.has(FOAF + "lastName") && json.getString(FOAF + "lastName") != null) {				
			name.setFamilyName(json.getString(FOAF + "lastName"));
		}
		retVal.setName(name);
		
		addAdditionalInformation(retVal);

		return retVal;
	}
	
	// should we cache people?  This might be slow.
	private void addAdditionalInformation(OrngPerson orngPerson) {
        Connection conn = dbUtil.getConnection();
		
		try { 
	        CallableStatement cs = conn
			        .prepareCall("{ call " + read_sp + "(?)}");
	        cs.setString(1, orngPerson.getId());
	        ResultSet rs = cs.executeQuery();
	        
	        if (rs.next()) {
	    		ResultSetMetaData rsmd = rs.getMetaData();
	    		for (int idx = 1; idx <= rsmd.getColumnCount(); idx++) {
	    			LOG.info(rsmd.getColumnLabel(idx) + " : " + rsmd.getColumnClassName(idx));
	    			Method method = personSetMethods.get("set" + rsmd.getColumnLabel(idx).toLowerCase());
	    			if (method != null && method.getParameterTypes().length == 1) {
		    			LOG.info(method.getParameterTypes()[0].getName());
	    			}
	    			if (method != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().equals(rsmd.getColumnClassName(idx))) { 
	    				try {
							method.invoke(orngPerson, rs.getObject(idx));
						} catch (IllegalAccessException e) {
							LOG.log(Level.SEVERE, e.getMessage(), e);
						} catch (IllegalArgumentException e) {
							LOG.log(Level.SEVERE, e.getMessage(), e);
						} catch (InvocationTargetException e) {
							LOG.log(Level.SEVERE, e.getMessage(), e);
						}
	    			}
	    		}
	        }
        } 
		catch (SQLException se) {
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

	public Future<Person> updatePerson(UserId id, Person person,
			SecurityToken token) throws ProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

}
