package edu.ucsf.orng.shindig.spi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
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
import com.google.inject.Inject;

import edu.ucsf.orng.shindig.model.OrngName;
import edu.ucsf.orng.shindig.model.OrngPerson;

/**
 * Implementation of supported services backed by a JSON DB.
 */
public class OrngPersonService implements PersonService {

	private static final Logger LOG = Logger.getLogger(OrngPersonService.class.getName());	
	
	private final RdfService rdfService;

	@Inject
	public OrngPersonService(RdfService rdfService)
	{
		this.rdfService = rdfService;
	}

	public Future<RestfulCollection<Person>> getPeople(Set<UserId> userIds,
			GroupId groupId, CollectionOptions options, Set<String> fields,
			SecurityToken token) throws ProtocolException {
		// TODO collection options
		// TODO fields
		List<Person> result = Lists.newArrayList();

		if (userIds.size() == 0) {
			return ImmediateFuture.newInstance(null);
		}

		int firstResult = 0;
		if (options != null) {
			firstResult = options.getFirst();
		}
		return ImmediateFuture.newInstance(new RestfulCollection<Person>(
				result, firstResult, result.size()));
	}

	public Future<Person> getPerson(UserId id, Set<String> fields,
			SecurityToken token) throws ProtocolException {
		String strId = id.getUserId(token);
		LOG.log(Level.INFO, "getPerson id=" + strId);
		
		try {
			// There can be only one!
			if (strId != null) {
				JSONObject personJSON =  rdfService.getRDF(strId, RdfService.MINIMAL);;
				Person personObj = parsePerson(strId, personJSON);
				return ImmediateFuture.newInstance(personObj);
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
		return ImmediateFuture.newInstance(null);
	}
	
	private Person parsePerson(String strId, JSONObject json) throws JSONException {
		Person retVal = new OrngPerson();
		retVal.setId(strId);
		if (json == null) {
			return retVal;
		}

		retVal.setProfileUrl(strId);
		if (json.has("mainImage") && json.getString("mainImage") != null) {
			retVal.setThumbnailUrl(json.getString("mainImage"));
		}
		if (json.has("label") && json.getString("label") != null) {
			retVal.setDisplayName(json.getString("label"));
		}
		if (json.has("primaryEmail") && json.getString("primaryEmail") != null) {
			List<ListField> emails = new ArrayList<ListField>();
			emails.add( new ListFieldImpl(null, json.getString("primaryEmail")) );
			retVal.setEmails(emails);
		}
		Name name = new OrngName();
		if (json.has("firstName") && json.getString("firstName") != null) {				
			name.setGivenName(json.getString("firstName"));
		}
		if (json.has("lastName") && json.getString("lastName") != null) {				
			name.setFamilyName(json.getString("lastName"));
		}
		retVal.setName(name);

		return retVal;
	}

}
