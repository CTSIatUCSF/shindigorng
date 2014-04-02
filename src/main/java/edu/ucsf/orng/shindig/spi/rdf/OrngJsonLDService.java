package edu.ucsf.orng.shindig.spi.rdf;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edu.ucsf.ctsi.r2r.jena.JsonLDService;
import edu.ucsf.orng.shindig.config.OrngProperties;

@Singleton
public class OrngJsonLDService extends JsonLDService implements OrngProperties {

	@Inject
	public OrngJsonLDService(@Named("orng.system") String system, @Named("orng.systemDomain") String systemDomain) {
		super(SYS_PROFILES.equalsIgnoreCase(system) ? systemDomain + "/profile/" : null);
	}
	
}
