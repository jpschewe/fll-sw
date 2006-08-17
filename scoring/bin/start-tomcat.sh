#!/bin/sh

## this sed expression emulates the dirname command
mypath="`echo $0 | sed -e 's,[^/]*$,,;s,/$,,;s,^$,.,'`"

if [ -e "${mypath}/setenv.sh" ]; then
  . "${mypath}/setenv.sh"
fi

"${mypath}/../tomcat/bin/startup.sh"
