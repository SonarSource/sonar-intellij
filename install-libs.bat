echo off

set /p intellij_home="IntelliJ home path: "

set /p intellij_version="IntelliJ version: "


IF NOT EXIST %intellij_home% GOTO NOOP

call mvn install:install-file -Dfile="%intellij_home%"/lib/annotations.jar -DgroupId=com.intellij -DartifactId=annotations -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/openapi.jar -DgroupId=com.intellij -DartifactId=openapi -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/util.jar -DgroupId=com.intellij -DartifactId=util -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/idea.jar -DgroupId=com.intellij -DartifactId=idea -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/extensions.jar -DgroupId=com.intellij -DartifactId=extensions -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/javac2.jar -DgroupId=com.intellij -DartifactId=javac2 -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/asm4-all.jar -DgroupId=com.intellij -DartifactId=asm4-all -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/lib/forms_rt.jar -DgroupId=com.intellij -DartifactId=forms_rt -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/plugins/maven/lib/maven.jar -DgroupId=com.intellij.plugins.maven -DartifactId=maven -Dversion=%intellij_version% -Dpackaging=jar
call mvn install:install-file -Dfile="%intellij_home%"/plugins/maven/lib/maven-server-api.jar -DgroupId=com.intellij.plugins.maven -DartifactId=maven-server-api -Dversion=%intellij_version% -Dpackaging=jar

:NOOP