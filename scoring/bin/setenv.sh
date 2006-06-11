#!/bin/sh

## this sed expression emulates the dirname command
mypath="`echo $0 | sed -e 's,[^/]*$,,;s,/$,,;s,^$,.,'`"
cd ${mypath}
mypath=`pwd`

fll_java=${mypath}/../tools/jdk-linux
if [ -d ${fll_java} ]; then
  JAVA_HOME=${fll_java}
  export JAVA_HOME
fi
