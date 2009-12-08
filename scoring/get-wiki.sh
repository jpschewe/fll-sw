#!/bin/bash
mkdir -p build/tomcat/webapps/fll-sw/wiki
wget -P web/wiki -nH --cut-dirs=3 -k -I /apps/trac/fll-sw/wiki --mirror http://sourceforge.net/apps/trac/fll-sw/wiki 
