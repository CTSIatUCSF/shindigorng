package edu.ucsf.orng.shindig.config;

import org.apache.shindig.common.PropertiesModule;

public class OrngPropertiesModule extends PropertiesModule implements OrngProperties {
	
	public OrngPropertiesModule() {
		super(System.getProperty("shindig.contextroot").length() > 1 ? System.getProperty("shindig.contextroot").substring(1) + ".properties" : "shindigorng.properties");
	}
	
	@Override
	protected void configure() {
		super.configure();

		try {
	    	Class.forName(getProperties().getProperty("orng.dbDriver"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		String orngSystem = getProperties().getProperty("orng.system");		
		if (!SYS_PROFILES.equalsIgnoreCase(orngSystem) && !SYS_VIVO.equalsIgnoreCase(orngSystem)) {
			throw new RuntimeException("orng.system not set properly. Needs to be Profiles or VIVO, is :" + orngSystem);
		}
	}

}
