package edu.ucsf.profiles.shindig.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.ucsf.profiles.shindig.model.ProfilesName;
import edu.ucsf.profiles.shindig.model.ProfilesOrganization;
import edu.ucsf.profiles.shindig.model.ProfilesPerson;

/**
 * Implementation of supported services backed by a JSON DB.
 */
public class ProfilesPersonService implements PersonService {
	private String endPoint;

	@Inject
	public ProfilesPersonService(@Named("ProfilesEndpoint") String endpoint)
			throws Exception {
		this.endPoint = endpoint;
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
		
		for (UserId id : userIds) {
			String strId = id.getUserId(token);
			System.out.println("getting people, id=" + strId);

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
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
			}/*
			 * catch (InterruptedException e) { throw new ProtocolException(0,
			 * e.getMessage(), e); } catch (ExecutionException e) { throw new
			 * ProtocolException(0, e.getMessage(), e); }
			 */
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
		System.out.println("getPerson id=" + strId);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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

			Element options = doc.createElement("OutputOptions");
			options.setAttribute("SortType", "QueryRelevance");
			options.setAttribute("StartRecord", "0");
			//options.setAttribute("MaxRecords", "100");
			root.appendChild(options);

			Document outDoc = contactEndPoint(builder, doc);

			NodeList outList = outDoc.getElementsByTagName("Person");
			// There can be only one!
			Person personObj = parsePerson(outList.item(0));
			return ImmediateFuture.newInstance(personObj);

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
	}
	
	private Document contactEndPoint(DocumentBuilder builder, Document doc)
			throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, TransformerException,
			MalformedURLException, IOException, SAXException {
		// transform the Document into a String
		DOMSource domSource = new DOMSource(doc);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		String xml = sw.toString();

		URL u = new URL(endPoint);
		HttpURLConnection uc = (HttpURLConnection) u.openConnection();
		uc.setDoOutput(true);
		uc.setDoInput(true);
		uc.setRequestProperty("Content-type", "text/xml");
		OutputStream os = uc.getOutputStream();
		for (int i = 0; i < xml.length(); i++) {
			os.write(xml.charAt(i));
		}
		os.flush();
		os.close();

		Document outDoc = builder.parse(uc.getInputStream());
		return outDoc;
	}

	private Person parsePerson(Node curNode) {
		// TODO there is no check for the visible attribute
		Person retVal = new ProfilesPerson();
		if (curNode == null) {
			return retVal;
		}
		NodeList list = curNode.getChildNodes();
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
			} /*
			 * else { //TODO InternalIDList, BasicStatistics, EmaiImageUrl,
			 * AwardList, Publications System.out.print(tempNode.getNodeName() +
			 * " : "); System.out.println(tempNode.getTextContent()); }
			 */
		}
		
		return retVal;
	}

	private Organization parseOrg(Node curNode) {
		Organization retVal = new ProfilesOrganization();
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
			 * System.out.print(tempNode.getNodeName() + " : ");
			 * System.out.println(tempNode.getTextContent()); }
			 */
		}
		return retVal;
	}

}
