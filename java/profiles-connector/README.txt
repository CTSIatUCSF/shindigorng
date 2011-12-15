Setting up in Eclipse/Local Testing
	Clear out your old repository by deleting C:\Documents and Settings\meekse\.m2\repository or equivalent
	Follow the instruction in SetupInstrucionsforShindig.pdf.  Use the best branch you can find (currently shindig-project-2.0.0-RC2_
	Make sure that shindig by itself builds in Eclipse and that you can run it in the debugger on Tomcat.  Test.
	From eclipse in the workspace you just created for shindig, go to the https://svn.ctsi.ucsf.edu/ctsi/ctsi-profiles-shindig/trunk
	repository location and check out as a Maven Project
	Accept defaults.  You should see in the file browser that shindig-profiles has been added as a subfolder in your workspace.
	Install SQLServer JDBC Driver (see below) if you have not done so before.
	
Integrating profiles-shindig into shindig
	Check pom.xml in shindig-profiles and make sure the parent info is correct.  Be sure to check that version is correct.
	Add shindig-profiles to the dependencyManagement section of the parent shindig-project/pom.xml.  Make it like the others.
	In the 'all' profile of shindig-project/pom.xml, add ../shindig-profiles as a module immediately after java/social-api.    
	Add shindig-profiles as a dependency to shindig-server/pom.xml.  Make it like the others
	Edit the web.xml.  
		Remove org.apache.shindig.common.PropertiesModule:
		Replace org.apache.shindig.social.sample.SampleModule with edu.ucsf.profiles.shindig.config.ProfilesModule
		Make sure shingid.host and shindig.port are empty, as we need to pass these in via JVM args to Tomcat
		Optionally remove or comment out the other sample and extras modules
		
	In eclipse right click the shindig-profiles project.  Properties->Builders.  Make sure Maven Project Builder is before Validation
	Look at shindig-social-api and make it like that.
	
	Compare profiles.shindig.poperties with conf/shindig.properites in shindig-common
	Compare profiles-container.js with config/container.js in shindig-project
	
	Make sure that profiles.shindig.properties is in the Tomcat class path. 
	Also add appropriate values for the shindig.port and shindig.host 
	(ex -Dshindig.port=80 -Dshindig.host=localhost) to the VM Arguments
	In eclipse do this by editing the launch configuration of Tomcat
	Outside of eclipse this can be done in the Tomcat monitor
	
	If using IIS to front end Tomcat, make sure that you can handle long URL's!

Install SQLServer JDBC Driver into you repository if you have not already done so.
Download jdbc4.jar
C:\>mvn install:install-file -Dfile=jdbc4.jar -DgroupId=com.microsoft.jdbc -DartifactId=jdbc4 -Dversion=3.0 -Dpackaging=jar
	