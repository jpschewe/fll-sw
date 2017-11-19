rem this silly expression emulates the unix dirname command
set BATPATH=%~dp0

%BATPATH%\tools\ant\bin\ant %1 %2 %3 %4 %5 %6 %7 %8 %9
