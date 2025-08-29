Add these to the local maven repository as needed when running mvn install fails for shindigorng.war

mvn install:install-file -Dfile=<path-to-file> -DgroupId=<group-id> -DartifactId=<artifact-id> -Dversion=<version> -Dpackaging=<packaging>

<dependency>
    <groupId>caja</groupId>
    <artifactId>caja</artifactId>
    <version>r5054</version>
</dependency>

<dependency>
    <groupId>caja</groupId>
    <artifactId>htmlparser</artifactId>
    <version>r4209</version>
</dependency>

<dependency>
    <groupId>com.microsoft.jdbc</groupId>
    <artifactId>jdbc4</artifactId>
    <version>6.0</version>
</dependency>