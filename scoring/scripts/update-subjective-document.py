#!/usr/bin/env python

import warnings
with warnings.catch_warnings():
    import re
    import sys
    from optparse import OptionParser
    from xml.dom import minidom, Node
    from xml.dom.minidom import getDOMImplementation

def transform(doc, out):
    inScores = doc.documentElement
    if None == inScores:
        print >>sys.stderr, "Cannot find root node!"
        return 1

    impl = getDOMImplementation()
    outDoc = impl.createDocument(None, "scores", None)
    outScores = outDoc.documentElement

    # walk child elements and convert to "subjectiveCategory"
    for child in inScores.childNodes:
        if child.nodeType == Node.ELEMENT_NODE:
            categoryName = child.nodeName
            outCategory = outDoc.createElement("subjectiveCategory")
            outCategory.setAttribute("name", categoryName)
            for oldScore in child.childNodes:
                if oldScore.nodeType == Node.ELEMENT_NODE:
                    outScore = outDoc.createElement("score")
                    for attr in oldScore.attributes.keys():
                        if attr == "NoShow" or attr == "division" or attr == "teamNumber" or attr == "judge" or attr == "organization" or attr == "teamName":
                            outScore.setAttribute(attr, oldScore.getAttribute(attr))
                        else:
                            # create subscore element
                            outSubscore = outDoc.createElement("subscore")
                            outSubscore.setAttribute("name", attr)
                            outSubscore.setAttribute("value", oldScore.getAttribute(attr))
                            outScore.appendChild(outSubscore)
                    if not outScore.hasAttribute("judging_station"):
                        outScore.setAttribute("judging_station", outScore.getAttribute("division"))
                    outCategory.appendChild(outScore)
            outScores.appendChild(outCategory)
            
    outDoc.writexml(out)

    
def main(argv=None):
    if argv is None:
        argv = sys.argv

    parser = OptionParser()

    (options, args) = parser.parse_args(argv)
    for filename in args[1:]:
        doc = minidom.parse(filename)
        out = file(filename + ".new.xml", "w")
        transform(doc, out)
        
if __name__ == "__main__":
    sys.exit(main())
    
