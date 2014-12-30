#!/bin/bash

# commented out as it's providing some bad JSP libraries
#ant --noconfig -lib lib -lib lib/ant -lib lib/test $*
#ant --noconfig $*

# unset some variables to ensure that we don't have problems with an existing tomcat
unset JASPER_HOME
unset TOMCAT_USER
unset CATALINA_OPTS
unset CATALINA_PID
unset CATALINA_TMPDIR
unset TOMCAT_HOME
unset CATALINA_HOME
unset CATALINA_BASE

#Make sure that the right ant is being used
unset ANT_HOME

# Set ANT options
#ANT_OPTS="-XX:MaxPermSize=128m"
#export ANT_OPTS

# Get the current working directory of this script
MYDIR=$(cd ${0%/*} && builtin pwd)

### this may not be portable:
#pushd . > /dev/null 2>&1
#mypath=`dirname $0`
#cd "${mypath}"
#mypath=`pwd`
#cd "${mypath}"
#popd > /dev/null 2>&1

# Run _our_ ant, properly passing all arguments
exec ${MYDIR}/tools/ant/bin/ant --noconfig "$@"
