@echo off

rem this silly expression emulates the unix dirname command
set BATPATH=%~dp0

if exist "%BATPATH%\setenv.bat" call "%BATPATH%\setenv.bat"

cd /D "%BATPATH%\..\tomcat\bin"
shutdown.bat
