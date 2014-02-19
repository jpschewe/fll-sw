#!/usr/bin/env python

import warnings
with warnings.catch_warnings():
    import re
    import sys
    from optparse import OptionParser
    import markdown
    import os.path

def rewrite_links(page_source):
    '''
    Convert references to page.md to page.html.
    @return the modified source
    '''

    result = page_source
    result = re.sub(r'\((.*)\.md\)', r'(\1.html)', result)

    # [page] without ()'s
    result = re.sub(r'\[(.*)\.md\]([^\(])', r'[\1](\1.html)', result)

    return result

def main(argv=None):
    if argv is None:
        argv = sys.argv

    parser = OptionParser()

    (options, args) = parser.parse_args(argv)

    for page in args[1:]:
        (page_dir, page_file) = os.path.split(page)
        match = re.match(r'^(.*)\.md$', page_file)
        if not match:
            raise "Filename does not end in 'md': {0}".format(page)
        page_base = match.group(1)
        with open(page, 'r') as markdown_file:
            source = markdown_file.read()
            resolved = rewrite_links(source)
            html = markdown.markdown(resolved)
            with open('%s.html' % (os.path.join(page_dir, page_base)), 'w') as html_file:
                html_file.write(html)
        
if __name__ == "__main__":
    sys.exit(main())
    
