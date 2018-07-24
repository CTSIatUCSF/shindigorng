shindigorng
===========

Version of Shindig that connects to Profiles and VIVO.  Based on the shindig-project-2.5.0-update1 tag

You can find the shinidig dependencies at http://svn.apache.org/repos/asf/shindig

The json-ld dependencies can be found at http://github.com/jsonld-java, and you sill need to 
The one change that is required is to update the top jsonld-java pom.xml file to use jena.version 2.11.1 instead of 2.11.0
This is required for the TDB items.

To install the jdbc driver needed for SQL Server, go into the jars directory and run the following command:
>mvn install:install-file -Dfile=jdbc4-6.0.jar -DgroupId=com.microsoft.jdbc -DartifactId=jdbc4 -Dversion=6.0 -Dpackaging=jar

Eric Meeks
