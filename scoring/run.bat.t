@echo off

rem this silly expression emulates the unix dirname command
set BATPATH=%~dp0

if exist "%BATPATH%\setenv.bat" call "%BATPATH%\setenv.bat"

rem run the application
java @JAVA_ARGS@ -classpath "@CLASSPATH@" @CLASSNAME@ %1 %2 %3 %4 %5 %6 %7 %8 %9
