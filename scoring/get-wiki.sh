#!/bin/bash
mkdir -p build/tomcat/webapps/fll-sw/wiki


wget \
  -I /apps/trac/fll-sw/wiki \
  --page-requisites \
  --html-extension \
  --restrict-file-names=windows \
  -P web/wiki \
  -nH \
  --cut-dirs=3 \
  -k \
  --mirror \
  http://sourceforge.net/apps/trac/fll-sw/wiki 
