/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.IOException;
import java.io.InputStream;

import org.apache.xerces.parsers.DOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parse challenge description and generate script/text for scoreEntry page.
 * 
 * @version $Revision$
 */
public final class ChallengeParser {

  /**
   * The expected namespace for FLL documents
   */
  public static final String FLL_NAMESPACE = "http://www.hightechkids.org";

  /**
   * Just for debugging.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge-region-2003.xml"));
      //final Document challengeDocument = ChallengeParser.parse(new java.io.FileInputStream("/home/jpschewe/projects/fll-sw/code/scoring/src/resources/challenge-region-2003.xml"));
      if(null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }

      System.out.println("Title: " + challengeDocument.getDocumentElement().getAttribute("title"));
      
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private  ChallengeParser() {
  }

  /**
   * Parse the challenge document from the given stream.  The document will be
   * validated and must be in the fll namespace.  Does not close the stream
   * after reading.
   *
   * @param stream a stream containing document
   * @return the challengeDocument, null on an error
   */
  public static Document parse(final InputStream stream) {
    try {
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      
      final DOMParser parser = new DOMParser();
      parser.setFeature("http://xml.org/sax/features/validation", true);
      parser.setFeature("http://apache.org/xml/features/validation/schema", true);
      
      parser.setFeature("http://xml.org/sax/features/namespaces", true);
      
      parser.setErrorHandler(new ErrorHandler() {
        public void error(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }
        public void fatalError(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }
        public void warning(final SAXParseException spe) throws SAXParseException {
          System.err.println(spe.getMessage());
        }
      });
      
      
      parser.setEntityResolver(new EntityResolver() {
        public InputSource resolveEntity(final String publicID,
                                         final String systemID)
          throws SAXException, IOException {
//           System.out.println("resolveEntity(" + publicID + ", " + systemID + ")"
//                              + " packageName: " + packageName
//                              );
          if(systemID.endsWith("fll.xsd")) {
            //just use the one we store internally
            //final int slashidx = systemID.lastIndexOf("/") + 1;
            return new InputSource(classLoader.getResourceAsStream("resources/fll.xsd")); // + systemID.substring(slashidx)));
          } else {
            return null;
          }
        }
      });

      parser.parse(new InputSource(stream));
      final Document document = parser.getDocument();
      final Element rootElement = document.getDocumentElement();
      if(!"fll".equals(rootElement.getTagName())) {
        throw new RuntimeException("Not a fll challenge description file");
      }
      return document;
    } catch(final SAXException se) {
      throw new RuntimeException(se);
    } catch(final IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

}
