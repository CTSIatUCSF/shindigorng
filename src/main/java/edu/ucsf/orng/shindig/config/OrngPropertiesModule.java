package edu.ucsf.orng.shindig.config;

import java.net.MalformedURLException;

import org.apache.shindig.common.PropertiesModule;

public class OrngPropertiesModule extends PropertiesModule implements OrngProperties {
	
	public OrngPropertiesModule() {
		super(System.getProperty("shindig.contextroot").length() > 1 ? System.getProperty("shindig.contextroot").substring(1) + ".properties" : "shindigorng.properties");
		java.net.URL hostName;
		try {
			hostName = new java.net.URL(getProperties().getProperty("orng.systemDomain"));
		} catch (MalformedURLException e1) {
			throw new RuntimeException(e1);
		}
		System.setProperty("shindig.host", hostName.getHost());
	}
	
	@Override
	protected void configure() {
		super.configure();
		// hope this is soon enough
		// This allows us to host shindigorng on a server with multiple host names (eg. profiles.ucsf.edu, profiles.usc.edu) 
		
	    try {
	    	Class.forName(getProperties().getProperty("orng.dbDriver"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		String orngSystem = getProperties().getProperty("orng.system");		
		if (!PROFILES.equalsIgnoreCase(orngSystem) && !VIVO.equalsIgnoreCase(orngSystem)) {
			throw new RuntimeException("orng.system not set properly. Needs to be Profiles or VIVO, is :" + orngSystem);
		}
	}

}
