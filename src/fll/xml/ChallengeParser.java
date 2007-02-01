/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
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

  private static final Logger LOG = Logger.getLogger(ChallengeParser.class);

  /**
   * Just for debugging.
   * 
   * @param args ignored
   */
  /**
   * @param args
   */
  public static void main(final String[] args) {
    try {
      // final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final java.io.FileReader input = new java.io.FileReader("/home/jpschewe/projects/fll-sw/working-dir/challenge-descriptors/challenge-hsr-2006.xml");
      final Document challengeDocument = ChallengeParser.parse(input);
      if (null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }

      LOG.info("Title: " + challengeDocument.getDocumentElement().getAttribute("title"));
      final org.w3c.dom.Element rootElement = challengeDocument.getDocumentElement();
      final org.w3c.dom.Element performanceElement = (org.w3c.dom.Element) rootElement
      .getElementsByTagName("Performance").item(0);
      final org.w3c.dom.NodeList goals = performanceElement.getElementsByTagName("goal");
      for (int i = 0; i < goals.getLength(); i++) {
        final Element element = (org.w3c.dom.Element) goals .item(i);
        final String name = element.getAttribute("name");
        LOG.info("The min value for goal " + name + " is " + element.getAttribute("min"));
      }

    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private ChallengeParser() {
  }

  /**
   * Parse the challenge document from the given stream. The document will be
   * validated and must be in the fll namespace. Does not close the stream after
   * reading.
   * 
   * @param stream a stream containing document
   * @return the challengeDocument, null on an error
   */
  public static Document parse(final Reader stream) {
    try {
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final Source schemaFile = new StreamSource(classLoader.getResourceAsStream("fll/resources/fll.xsd"));
      final Schema schema = factory.newSchema(schemaFile);

      final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(true);
      builderFactory.setSchema(schema);
      final DocumentBuilder parser = builderFactory.newDocumentBuilder();

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
        public InputSource resolveEntity(final String publicID, final String systemID) throws SAXException, IOException {
          if (LOG.isDebugEnabled()) {
            LOG.debug("resolveEntity(" + publicID + ", " + systemID + ")");
          }
          if (systemID.endsWith("fll.xsd")) {
            // just use the one we store internally
            // final int slashidx = systemID.lastIndexOf("/") + 1;
            return new InputSource(classLoader.getResourceAsStream("fll/resources/fll.xsd")); // +
            // systemID.substring(slashidx)));
          } else {
            return null;
          }
        }
      });

      // pull the whole stream into a string
      final StringWriter writer = new StringWriter();
      final char[] buffer = new char[1024];
      int bytesRead;
      while ((bytesRead = stream.read(buffer)) != -1) {
        writer.write(buffer, 0, bytesRead);
      }

      
      final Document document = parser.parse(new InputSource(new StringReader(writer.toString())));
      
      final Element rootElement = document.getDocumentElement();
      if (!"fll".equals(rootElement.getTagName())) {
        throw new RuntimeException("Not a fll challenge description file");
      }
      return document;
    } catch (final SAXParseException spe) {
      throw new RuntimeException("Error parsing file line: " + spe.getLineNumber() + " column: " + spe.getColumnNumber() + " " + spe.getMessage());
    } catch (final SAXException se) {
      throw new RuntimeException(se);
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }
  }

}
