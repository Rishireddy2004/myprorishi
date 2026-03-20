@echo off
REM Apache Maven Wrapper startup batch script (patched for paths with spaces)

SET "BASE_DIR=%~dps0"
SET "MAVEN_PROJECTBASEDIR=%BASE_DIR%"
SET "MAVEN_WRAPPER_JAR=%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
SET "MAVEN_WRAPPER_PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_WRAPPER_PROPERTIES%") DO (
    IF "%%A"=="distributionUrl" SET "DISTRIBUTION_URL=%%B"
)

echo [DEBUG] BASE_DIR=%BASE_DIR%
echo [DEBUG] JAR=%MAVEN_WRAPPER_JAR%
echo [DEBUG] PROPS=%MAVEN_WRAPPER_PROPERTIES%

java "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -cp "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
