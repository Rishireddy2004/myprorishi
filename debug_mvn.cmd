@echo off
SET "BASE_DIR=%~dps0"
echo BASE_DIR=[%BASE_DIR%]
SET "MAVEN_WRAPPER_JAR=%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
echo JAR=[%MAVEN_WRAPPER_JAR%]
IF EXIST "%MAVEN_WRAPPER_JAR%" (echo JAR EXISTS) ELSE (echo JAR MISSING)
SET "JAVA_CMD=java"
echo CMD=[%JAVA_CMD%]
echo Full command:
echo "%JAVA_CMD%" "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" -cp "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain
