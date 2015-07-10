#!/bin/sh

debug() { ! "${log_debug-false}" || log "DEBUG: $*" >&2; }
log() { printf '%s\n' "$*"; }
warn() { log "WARNING: $*" >&2; }
error() { log "ERROR: $*" >&2; }
fatal() { error "$*"; exit 1; }
try() { "$@" || fatal "'$@' failed"; }

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
MYDIR=$(cd "$(dirname "$0")" && pwd -L) || fatal "Unable to determine script directory"

# Run _our_ ant, properly passing all arguments
exec "${MYDIR}"/tools/ant/bin/ant --noconfig "$@"
