@ECHO OFF
SETLOCAL
SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_HOME=%DIRNAME%
SET APP_BASE_NAME=%~n0
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%CLASSPATH%" (
  ECHO gradle-wrapper.jar missing. Open this project in Android Studio to generate the wrapper.
  EXIT /B 1
)

java -jar "%CLASSPATH%" %*
