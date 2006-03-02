#!/bin/sh
#
# Based on the script used to start ant.  Original copyright:
# Copyright (c) 2001-2002 The Apache Software Foundation.  All rights reserved.

## this may not be portable:
# mypath=`dirname $0`
## this sed expression emulates the dirname command
mypath="`echo $0 | sed -e 's,[^/]*$,,;s,/$,,;s,^$,.,'`"

#cd ${mypath}/..

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home   
           fi
           ;;
esac

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
    JAVACMD=java
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

# Jon and Eric disagree about this - for now, Jon wins the argument.
# Eric thinks the classpath should be built from ALL jars in the lib dir.
# Jon thinks that it should be explicit.
LOCALCLASSPATH="@CLASSPATH@"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
  mypath=`cygpath --windows -a "$mypath"`
fi

"$JAVACMD" @JAVA_ARGS@ -classpath "$LOCALCLASSPATH" @CLASSNAME@ $*
