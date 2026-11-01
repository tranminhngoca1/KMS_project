@ECHO OFF
SETLOCAL
SET MAVEN_VERSION=3.9.9
SET MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
    ECHO Downloading Apache Maven %MAVEN_VERSION%...
    powershell -NoProfile -Command "$ProgressPreference = 'SilentlyContinue'; New-Item -ItemType Directory -Force -Path '%USERPROFILE%\.m2\wrapper\dists' | Out-Null; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%USERPROFILE%\.m2\wrapper\dists\maven.zip'; Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
)
CALL "%MAVEN_HOME%\bin\mvn.cmd" %*

