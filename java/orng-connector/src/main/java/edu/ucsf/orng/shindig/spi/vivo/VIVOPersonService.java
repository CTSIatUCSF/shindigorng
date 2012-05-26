package edu.ucsf.orng.shindig.spi.vivo;

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
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import edu.ucsf.orng.shindig.model.OrngName;
import edu.ucsf.orng.shindig.model.OrngOrganization;
import edu.ucsf.orng.shindig.model.OrngPerson;
import edu.ucsf.orng.shindig.spi.RdfService;

/**
 * Implementation of supported services backed by a JSON DB.
 */
public class VIVOPersonService implements PersonService {

	private static final Logger LOG = Logger.getLogger(VIVOPersonService.class.getName());	
	
	private final RdfService rdfService;

	@Inject
	public VIVOPersonService(RdfService rdfService)
			throws Exception {
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
		/**
		for (UserId id : userIds) {
			String strId = id.getUserId(token);
			LOG.log(Level.INFO, "getting people, id=" + strId);

			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				DOMImplementation impl = builder.getDOMImplementation();
				Document doc = impl.createDocument(null, null, null);
				Element root = doc.createElement("Profiles");
				root.setAttribute("xmlns",
						"http://connects.profiles.schema/profiles/query");
				root.setAttribute("Operation", "GetPersonList");
				root.setAttribute("Version", "2");
				doc.appendChild(root);

				Element def = doc.createElement("QueryDefinition");
				root.appendChild(def);

				Element personId = doc.createElement("PersonID");
				personId.setTextContent(strId);
				def.appendChild(personId);

				Element outputOptions = doc.createElement("OutputOptions");
				outputOptions.setAttribute("SortType", "LastFirstName");
				outputOptions.setAttribute("StartRecord", options.getFirst()+"");
				outputOptions.setAttribute("MaxRecords", options.getMax()+"");

				String groupName = null;
				
				if (groupId.getType().equals(GroupId.Type.groupId)) {
					Element filterList = doc.createElement("OutputFilterList");
					outputOptions.appendChild(filterList);
					
					// we currently support only SimilarPersonList, CoAuthorList,
					// NeighborList
					groupName = groupId.getGroupId();
					Element filter = doc.createElement("OutputFilter");
					filter.setTextContent(groupName);
					filterList.appendChild(filter);
				}
				if (!ImmutableSet.of(GroupId.Type.groupId, GroupId.Type.self).contains(groupId.getType())) {
					// todo handle friends and all
					return ImmediateFuture.newInstance(null);
				}

				root.appendChild(outputOptions);

				Document outDoc = contactEndPoint(builder, doc);

				if (groupName != null) {
					// there is only 1 such list
					NodeList outList = outDoc.getElementsByTagName(groupName);
					if (outList == null) {
						return ImmediateFuture.newInstance(null);
					}
					if (outList.getLength() == 0) {
					    continue;
					}
					NodeList outChildren = outList.item(0).getChildNodes();
					for (int i = 0; i < outChildren.getLength(); i++) {
						Node outChild = outChildren.item(i);
						ProfilesPerson pp = new ProfilesPerson();
						pp.setId(outChild.getAttributes().getNamedItem("PersonID")
								.getNodeValue());
						pp.setDisplayName(outChild.getTextContent());
						// UserId userId = new UserId(UserId.Type.userId, childID);
						// result.add(getPerson(userId, fields, token).get());
						result.add(pp);
					}
				}
				else {
					NodeList outList = outDoc.getElementsByTagName("Person");
					// There can be only one!
					Person personObj = parsePerson(outList.item(0));
					result.add(personObj);
				}

			} catch (ParserConfigurationException pce) {
				throw new ProtocolException(0, pce.getMessage(), pce);
			} catch (TransformerConfigurationException e) {
				throw new ProtocolException(0, e.getMessage(), e);
			} catch (TransformerException e) {
				throw new ProtocolException(0, e.getMessage(), e);
			} catch (IOException e) {
				throw new ProtocolException(0, e.getMessage(), e);
			} catch (SAXException e) {
				throw new ProtocolException(0, e.getMessage(), e);
			}
		}  */
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
				Person personObj = parsePerson(strId, rdfService.getRDF(strId, RdfService.MINIMAL));				
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
	
	/*
	private JSONObject contactEndPoint(String strId) throws MalformedURLException, 
	IOException, JSONException {
		// transform the Document into a String
		URL u = new URL("http://localhost:8080/shindigorng/babel?reader=rdf-xml&writer=exhibit-json&mimetype=default");
		HttpURLConnection uc = (HttpURLConnection) u.openConnection();
		uc.setDoOutput(true);
		uc.setDoInput(true);
        String boundary = ("" + Math.random()).substring(2);
		uc.setRequestProperty("Content-type", "multipart/form-data; charset=utf-8; boundary=" + boundary);
		OutputStream os = uc.getOutputStream();
		
        String multipart = "--" + boundary
                     + "\r\nContent-Disposition: form-data; name=url"
                     + "\r\nContent-type: application/octet-stream"
                     + "\r\n\r\n" + orngURL + "/display/n" + strId + "?format=rdfxml" + "\r\n" 
                     + "--"+boundary+"--\r\n";
		
		
		for (int i = 0; i < multipart.length(); i++) {
			os.write(multipart.charAt(i));
		}
		os.flush();
		os.close();

		// Receive the encoded content.
		int bytes = 0;
		String page = "";
		byte[] bytesReceived = new byte[4096];

		// The following will block until the page is transmitted.
		while ((bytes = uc.getInputStream().read(bytesReceived)) > 0) {
			page += new String(bytesReceived, 0, bytes);
		};
		
		return new JSONObject(page);
	}*/

	private Person parsePerson(String strId, JSONObject json) throws JSONException {
		// TODO there is no check for the visible attribute
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
		if (json.has("fistName") && json.getString("firstName") != null) {				
			name.setGivenName(json.getString("firstName"));
		}
		if (json.has("lastName") & json.getString("lastName") != null) {				
			name.setFamilyName(json.getString("lastName"));
		}
		retVal.setName(name);
		
/**		NodeList list = curNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node tempNode = list.item(i);
			if (tempNode.getNodeName().equals("PersonID")) {
				retVal.setId(tempNode.getTextContent());
			} else if (tempNode.getNodeName().equals("ProfileURL")) {
				retVal.setProfileUrl(tempNode.getTextContent());
			} else if (tempNode.getNodeName().equals("EmailImageUrl")) {
				List<ListField> emails = new ArrayList<ListField>();
				emails.add( new ListFieldImpl(null, tempNode.getTextContent()) );
				retVal.setEmails(emails);
			} else if (tempNode.getNodeName().equals("PhotoUrl")) {
				retVal.setThumbnailUrl(tempNode.getTextContent());
			} else if (tempNode.getNodeName().equals("Name")) {
				NodeList inner = tempNode.getChildNodes();
				Name name = new ProfilesName();
				for (int j = 0; j < inner.getLength(); j++) {
					// TODO this needs to be refactored, talk to HMS people
					Node innerNode = inner.item(j);
					if (innerNode.getNodeName().equals("FullName")) {
						retVal.setDisplayName(innerNode.getTextContent());
						name.setFormatted(tempNode.getTextContent());
					} else if (innerNode.getNodeName().equals("FirstName")) {
						name.setGivenName(innerNode.getTextContent());
					} else if (innerNode.getNodeName().equals("LastName")) {
						name.setFamilyName(innerNode.getTextContent());
					}
				}
				retVal.setName(name);
			} else if (tempNode.getNodeName().equals("AffiliationList")) {
				List<Organization> orgs = new ArrayList<Organization>();
				NodeList inner = tempNode.getChildNodes();
				for (int j = 0; j < inner.getLength(); j++) {
					orgs.add(parseOrg(inner.item(j)));
				}
				retVal.setOrganizations(orgs);
			} 
		}**/
		
		return retVal;
	}

	private Organization parseOrg(Node curNode) {
		Organization retVal = new OrngOrganization();
		NodeList list = curNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node tempNode = list.item(i);
			if (tempNode.getNodeName().equals("InstitutionName")) {
				retVal.setName(tempNode.getTextContent());
			} else if (tempNode.getNodeName().equals("DepartmentName")) {
				retVal.setField(tempNode.getTextContent());
			} else if (tempNode.getNodeName().equals("DivisionName")) {
				retVal.setSubField(tempNode.getTextContent());
			} else if (tempNode.getNodeName().equals("JobTitle")) {
				retVal.setTitle(tempNode.getTextContent());
			}/*
			 * else { //TODO abbreviation and facultytype
			 * LOG.log(Level.INFO, tempNode.getNodeName() + " : ");
			 * LOG.log(Level.INFO, tempNode.getTextContent()); }
			 */
		}
		return retVal;
	}
}
