@echo off

rem this silly expression emulates the unix dirname command
set BATPATH=%~dp0

set fll_java=%BATPATH%\..\tools\jdk-windows
if not exist "%fll_java%" goto end
set JAVA_HOME=%fll_java%

:end
