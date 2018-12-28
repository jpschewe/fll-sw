#!/bin/sh

OPWD="${PWD}"
mypath=`dirname $0`
cd "${mypath}"
mypath="${PWD}"
cd "${OPWD}"

fll_java="${mypath}/../tools/jdk-linux"
if [ -d "${fll_java}" ]; then
  JAVA_HOME="${fll_java}"
  export JAVA_HOME
  
  PATH="${JAVA_HOME}/bin:${PATH}"
  export PATH
fi
