#!/bin/sh
#
# Based on the script used to start ant.  Original copyright:
# Copyright (c) 2001-2002 The Apache Software Foundation.  All rights reserved.

OPWD="${PWD}"
mypath=`dirname $0`
cd "${mypath}"
mypath="${PWD}"
cd "${OPWD}"
debug() { ! "${log_debug-false}" || log "DEBUG: $*" >&2; }
log() { printf '%s\n' "$*"; }
warn() { log "WARNING: $*" >&2; }
error() { log "ERROR: $*" >&2; }
fatal() { error "$*"; exit 1; }
try() { "$@" || fatal "'$@' failed"; }

mypath=$(cd "$(dirname "$0")" && pwd -L) || fatal "Unable to determine script directory"



# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  Linux*) linux=true ;;
esac

if $linux ; then
    # check for bundled JDK
    for dir in "${mypath}"/jdk*; do
        if [ -e "${dir}/bin/java" ]; then
            JAVA_HOME=${dir}
        fi
    done
fi


if $darwin ; then
    # check for bundled JDK
    for dir in "${mypath}"/jdk*/Contents/Home; do
        if [ -e "${dir}/bin/java" ]; then
            JAVA_HOME=${dir}
        fi
    done
    
    if [ -z "${JAVA_HOME}" ] ; then
        # best option is first
        for java_dir in \
            $(/usr/libexec/java_home) \
                "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" \
                "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Home" \
                "/System/Library/Frameworks/JavaVM.framework/Home" \
                "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home"
        do
            if [ -z "${JAVA_HOME}" -a -e "${java_dir}"/bin/java ]; then
                JAVA_HOME=${java_dir}
            fi
        done
    fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=$(command -v java)
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined and java is not in your path"
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  mypath=`cygpath --windows -a "$mypath"`
fi

# Add the JAVA 9 specific start-up parameters required by Tomcat
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.io=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.util=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED"
export JDK_JAVA_OPTIONS


cd "${mypath}"
CLASSPATH='classes/:lib/*'
export CLASSPATH
exec "$JAVACMD" fll.Launcher "$@"
