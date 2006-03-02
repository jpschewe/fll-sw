@echo off

rem this silly expression emulates the unix dirname command
set BATPATH=%~dp0

rem go there, then to the parent, so all paths are relative now
rem cd /D "%BATPATH%\.."

rem run the application
java @JAVA_ARGS@ -classpath "@CLASSPATH@" @CLASSNAME@ %1 %2 %3 %4 %5 %6 %7 %8 %9
