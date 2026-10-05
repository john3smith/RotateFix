@echo off
setlocal

if not defined JAVA_HOME if exist "C:\ytdownforand\.tools\jdk-17.0.20.1+1\bin\java.exe" set "JAVA_HOME=C:\ytdownforand\.tools\jdk-17.0.20.1+1"

call "%~dp0gradlew.bat" --no-daemon clean testDebugUnitTest lintDebug assembleDebug
if errorlevel 1 exit /b %errorlevel%

copy /Y "%~dp0app\build\outputs\apk\debug\app-debug.apk" "%~dp0RotateFix-v1.0.1-debug.apk" >nul
echo Built: %~dp0RotateFix-v1.0.1-debug.apk
