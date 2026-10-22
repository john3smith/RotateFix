@echo off
setlocal

if not defined JAVA_HOME if exist "C:\ytdownforand\.tools\jdk-17.0.20.1+1\bin\java.exe" set "JAVA_HOME=C:\ytdownforand\.tools\jdk-17.0.20.1+1"

call "%~dp0gradlew.bat" --no-daemon testDebugUnitTest lintRelease assembleRelease
if errorlevel 1 exit /b %errorlevel%

echo Built UNSIGNED release input: %~dp0app\build\outputs\apk\release\app-release-unsigned.apk
echo Not for installation or distribution. Follow docs\RELEASE_SECURITY.md to sign and verify.
