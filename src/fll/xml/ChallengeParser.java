/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import fll.Utilities;
import fll.util.FP;

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
   * Parse the specified XML document and report errors.
   * <ul>
   * <li>arg[0] - the location of the document to parse
   * </ul>
   */
  public static void main(final String[] args) {
    if (args.length < 1) {
      LOG.fatal("Usage: ChallengeParser <xml file>");
      System.exit(1);
    }
    final File challengeFile = new File(args[0]);
    if (!challengeFile.exists()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " doesn't exist");
      System.exit(1);
    }
    if (!challengeFile.canRead()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " is not readable");
      System.exit(1);
    }
    if (!challengeFile.isFile()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " is not a file");
      System.exit(1);
    }
    try {
      final FileReader input = new FileReader(challengeFile);
      final Document challengeDocument = ChallengeParser.parse(input);
      if (null == challengeDocument) {
        LOG.fatal("Error parsing challenge descriptor");
        System.exit(1);
      }

      LOG.info("Title: "
          + challengeDocument.getDocumentElement().getAttribute("title"));
      final Element rootElement = challengeDocument.getDocumentElement();
      final Element performanceElement = (org.w3c.dom.Element) rootElement.getElementsByTagName("Performance").item(0);
      final List<Element> goals = XMLUtils.filterToElements(performanceElement.getElementsByTagName("goal"));
      LOG.info("The performance goals are");
      for (final Element element : goals) {
        final String name = element.getAttribute("name");
        LOG.info(name);
      }
    } catch (final Exception e) {
      LOG.fatal(e, e);
      System.exit(1);
    }
  }

  /**
   * Used to compare the initial value against enum values and min/maxes.
   */
  public static final double INITIAL_VALUE_TOLERANCE = 1E-4;

  private ChallengeParser() {
    // no instances
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
      builderFactory.setIgnoringComments(true);
      builderFactory.setIgnoringElementContentWhitespace(true);
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
            LOG.debug("resolveEntity("
                + publicID + ", " + systemID + ")");
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
      validateDocument(document);
      return document;
    } catch (final SAXParseException spe) {
      throw new RuntimeException("Error parsing file line: "
          + spe.getLineNumber() + " column: " + spe.getColumnNumber() + " " + spe.getMessage());
    } catch (final SAXException se) {
      throw new RuntimeException(se);
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    }
  }

  /**
   * Do validation of the document that cannot be done by the XML parser.
   * 
   * @param document the document to validate
   * @throws ParseException
   * @throws RuntimeException if an error occurs
   */
  private static void validateDocument(final Document document) throws ParseException {
    final Element rootElement = document.getDocumentElement();
    if (!"fll".equals(rootElement.getTagName())) {
      throw new RuntimeException("Not a fll challenge description file");
    }

    for (final Element childNode : XMLUtils.filterToElements(rootElement.getChildNodes())) {
      if ("Performance".equals(childNode.getNodeName())
          || "SubjectiveCategory".equals(childNode.getNodeName())) {
        final Element childElement = childNode;

        // get all nodes named goal at any level under category element
        final Map<String, Element> goals = new HashMap<String, Element>();
        for (final Element element : XMLUtils.filterToElements(childElement.getElementsByTagName("goal"))) {
          final String name = element.getAttribute("name");
          goals.put(name, element);

          // check initial values
          final double initialValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("initialValue")).doubleValue();
          if (XMLUtils.isEnumeratedGoal(element)) {
            boolean foundMatch = false;
            for (final Element valueEle : XMLUtils.filterToElements(element.getChildNodes())) {
              final double score = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueEle.getAttribute("score")).doubleValue();
              if (FP.equals(score, initialValue, INITIAL_VALUE_TOLERANCE)) {
                foundMatch = true;
              }
            }
            if (!foundMatch) {
              throw new RuntimeException(new Formatter().format("Initial value for %s(%d) does not match the score of any value element within the goal", name,
                                                                initialValue).toString());
            }

          } else {
            final double min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).doubleValue();
            final double max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).doubleValue();
            if (FP.lessThan(initialValue, min, INITIAL_VALUE_TOLERANCE)) {
              throw new RuntimeException(new Formatter().format("Initial value for %s(%d) is less than min(%d)", name, initialValue, min).toString());
            }
            if (FP.greaterThan(initialValue, max, INITIAL_VALUE_TOLERANCE)) {
              throw new RuntimeException(new Formatter().format("Initial value for %s(%d) is greater than max(%d)", name, initialValue, max).toString());
            }
          }
        }

        // for all computedGoals
        for (final Element computedGoalElement : XMLUtils.filterToElements(childElement.getElementsByTagName("computedGoal"))) {

          // for all termElements
          for (final Element termElement : XMLUtils.filterToElements(computedGoalElement.getElementsByTagName("term"))) {

            // check that the computed goal only references goals
            final String referencedGoalName = termElement.getAttribute("goal");
            if (!goals.containsKey(referencedGoalName)) {
              throw new RuntimeException("Computed goal '"
                  + computedGoalElement.getAttribute("name") + "' references goal '" + referencedGoalName + "' which is not a standard goal");
            }
          }

          // for all goalRef elements
          for (final Element goalRefElement : XMLUtils.filterToElements(computedGoalElement.getElementsByTagName("goalRef"))) {

            // can't reference a non-enum goal with goalRef in enumCond
            final String referencedGoalName = goalRefElement.getAttribute("goal");
            final Element referencedGoalElement = goals.get(referencedGoalName);
            if (!XMLUtils.isEnumeratedGoal(referencedGoalElement)) {
              throw new RuntimeException("Computed goal '"
                  + computedGoalElement.getAttribute("name") + "' has a goalRef element that references goal '" + referencedGoalName + " "
                  + referencedGoalElement + "' which is not an enumerated goal");
            }
          }

        } // end foreach computed goal

        // for all terms
        for (final Element termElement : XMLUtils.filterToElements(childElement.getElementsByTagName("term"))) {
          final String goalValueType = termElement.getAttribute("scoreType");
          final String referencedGoalName = termElement.getAttribute("goal");
          final Element referencedGoalElement = goals.get(referencedGoalName);
          // can't use the raw score of an enum inside a polynomial term
          if ("raw".equals(goalValueType)
              && (XMLUtils.isEnumeratedGoal(referencedGoalElement) || XMLUtils.isComputedGoal(referencedGoalElement))) {
            throw new RuntimeException("Cannot use the raw score from an enumerated or computed goal in a polynomial term.  Referenced goal '"
                + referencedGoalName + "'");
          }
        }

      } // end if child node (performance or subjective)
    } // end foreach child node
  } // end validateDocument
}
