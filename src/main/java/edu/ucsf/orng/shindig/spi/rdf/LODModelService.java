package edu.ucsf.orng.shindig.spi.rdf;

import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

public class LODModelService implements JenaModelService {

	private static final Logger LOG = Logger.getLogger(LODModelService.class.getName());
	
	private String system;
	private String systemDomain;
	private String systemBase;
	private String sessionId;
	private String viewerId;
	private boolean showDetails = true;
	private boolean expand = false;
	
	@Inject
	public LODModelService(@Named("orng.system") String system, 
						  @Named("orng.systemDomain") String systemDomain,
						  String sessionId, String viewerId) {
		this.system = system;
		this.systemDomain = systemDomain;
		this.systemBase = systemDomain;
		if (PROFILES.equalsIgnoreCase(system)) {
			systemBase += "/profile/";
		}	
		this.sessionId = sessionId;
		this.viewerId = viewerId;
	}

	public void setProfilesOptions(boolean showDetails, boolean expand) {
		this.showDetails = showDetails;
		this.expand = expand;
	}
		
	public Model getModel(String uri) throws Exception {		
		String url = uri;
		// custom way to convert URI to URL in case standard LOD mechanisms will not work
		if (systemDomain != null && url.toLowerCase().startsWith(systemDomain.toLowerCase())) {
			if (VIVO.equalsIgnoreCase(system)) {
				url += (url.indexOf('?') == -1 ? "?" : "&") + "format=rdfxml";
			}
			else if (PROFILES.equalsIgnoreCase(system)) {
				// we know how to parse out the URL with profiles to take advantage of a direct request
				// where we can avoid the redirect and add helpful query args
				// we also know how to grab the nodeID and build a clean URI
				int ndx = -1;  
				String nodeId = null;
				if ((ndx = uri.indexOf("Subject=")) != -1) {
					nodeId = uri.indexOf('&', ndx) != -1 ? uri.substring(ndx + "Subject=".length(), uri.indexOf('&', ndx)) : uri.substring(ndx + "Subject=".length());					
				}
				else {
					// if it is a GET style URL then the first numeric item in the path is likely it
					String[] items = uri.substring(systemDomain.length() + 1, uri.indexOf('?') == -1 ? uri.length() : uri.indexOf('?')).split("/");
					for (String item : items) {
						if (StringUtils.isNumeric(item)) {
							nodeId = item;
							break;
						}
					}
				}
				if (nodeId != null) {
					uri = systemBase + nodeId;
				}
				
				if (!url.toLowerCase().endsWith(".rdf") && url.indexOf('?') == -1) {
					url = systemDomain + "/Profile/Profile.aspx?Subject=" + nodeId;
					// add in SessionID so that we can take advantage of Profiles security settings
					url += "&ShowDetails=" + showDetails + "&Expand=" + expand;
					if (sessionId != null)
					{
						url += "&ContainerSessionID=" + sessionId;					
					}
					if (viewerId != null)
					{
						url += "&Viewer=" + URLEncoder.encode(viewerId, "UTF-8");					
					}
				}		
			}
		}
    	LOG.log(Level.INFO, "getRDF :" + url );

    	return FileManager.get().loadModel(url);
	}
}
