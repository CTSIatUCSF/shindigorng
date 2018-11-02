shindigorng
===========

Version of Shindig that connects to Profiles and VIVO.  Based on shindig-2.5.2



To install the jdbc driver needed for SQL Server and other jars, go into the jars directory and run the following commands:
>mvn install:install-file -Dfile=jdbc4-6.0.jar -DgroupId=com.microsoft.jdbc -DartifactId=jdbc4 -Dversion=6.0 -Dpackaging=jar
>mvn install:install-file -Dfile=caja-r5054.jar -DgroupId=caja -DartifactId=caja -Dversion=r5054 -Dpackaging=jar
>mvn install:install-file -Dfile=htmlparser-r4209.jar -DgroupId=caja -DartifactId=htmlparser -Dversion=r4209 -Dpackaging=jar
>mvn install:install-file -Dfile=diff_match_patch-current.jar -DgroupId=diff_match_patch -DartifactId=diff_match_patch -Dversion=current -Dpackaging=jar

Eric Meeks
