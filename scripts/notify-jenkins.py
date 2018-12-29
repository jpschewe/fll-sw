#!/usr/bin/env python

import warnings
with warnings.catch_warnings():
    import re
    import sys
    from optparse import OptionParser
    import urllib

def main(argv=None):
    if argv is None:
        argv = sys.argv

    parser = OptionParser()

    (options, args) = parser.parse_args(argv)

    opener = urllib.FancyURLopener({})
    stream = opener.open('http://mtu.net/jenkins/git/notifyCommit?url=http://git.code.sf.net/p/fll-sw/code')
    text = stream.read()
    print(text)
        
if __name__ == "__main__":
    sys.exit(main())
    
