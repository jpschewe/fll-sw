/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fll.Utilities;
import fll.db.GenerateDB;
import fll.util.FP;
import fll.util.LogUtils;
import net.mtu.eggplant.io.IOUtils;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Parse challenge description and generate script/text for scoreEntry page.
 */
public final class ChallengeParser {

  /**
   * The expected namespace for FLL documents
   */
  public static final String FLL_NAMESPACE = "http://www.hightechkids.org";

  private static final Logger LOG = LogUtils.getLogger();

  /**
   * Parse the specified XML document and report errors.
   * <ul>
   * <li>arg[0] - the location of the document to parse
   * </ul>
   */
  public static void main(final String[] args) {
    LogUtils.initializeLogging();

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
      final Reader input = new InputStreamReader(new FileInputStream(challengeFile), Utilities.DEFAULT_CHARSET);
      final Document challengeDocument = ChallengeParser.parse(input);

      final ChallengeDescription description = new ChallengeDescription(challengeDocument.getDocumentElement());

      LOG.info("Title: "
          + description.getTitle());
    } catch (final Throwable e) {
      LOG.fatal(e, e);
      System.exit(1);
    }
  }

  /**
   * Used to compare the initial value against enum values and min/maxes.
   */
  public static final double INITIAL_VALUE_TOLERANCE = 1E-4;

  public static final int CURRENT_SCHEMA_VERSION = 2;

  private ChallengeParser() {
    // no instances
  }

  /**
   * Parse the challenge document from the given stream. The document will be
   * validated and must be in the fll namespace. Does not close the stream after
   * reading.
   * 
   * @param stream a stream containing document
   * @return not null
   * @throws ChallengeXMLException on error
   */
  public static Document parse(final Reader stream) throws ChallengeXMLException {
    try {
      String content = IOUtils.readIntoString(stream);

      int schemaVersion = determineSchemaVersion(content);
      if (schemaVersion == 0) {
        content = transform0To1(content);
      } else if (schemaVersion < 0) {
        throw new ChallengeXMLException("Schema version not known: "
            + schemaVersion);
      }

      schemaVersion = determineSchemaVersion(content);
      if (schemaVersion == 1) {
        content = transform1To2(content);
      }

      schemaVersion = determineSchemaVersion(content);
      if (schemaVersion != CURRENT_SCHEMA_VERSION) {
        throw new ChallengeXMLException("Error upgrading document, should have version "
            + CURRENT_SCHEMA_VERSION + ", but is " + schemaVersion);
      }

      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final Source schemaFile = new StreamSource(classLoader.getResourceAsStream("fll/resources/fll.xsd"));
      final Schema schema = factory.newSchema(schemaFile);

      final Document document = XMLUtils.parse(new StringReader(content), schema);

      // challenge descriptor specific checks
      validateDocument(document);

      return document;
    } catch (final SAXParseException spe) {
      throw new ChallengeXMLException(
                                      String.format("Error parsing file line: %d column: %d%n Message: %s%n This may be caused by using the wrong version of the software or an improperly formatted challenge descriptor or attempting to parse a file that is not a challenge descriptor.",
                                                    spe.getLineNumber(), spe.getColumnNumber(), spe.getMessage()), spe);
    } catch (final SAXException se) {
      throw new ChallengeXMLException(
                                      "The challenge descriptor was found to be invalid, check that you are parsing a challenge descriptor file and not something else",
                                      se);
    } catch (final ParseException pe) {
      throw new ChallengeXMLException(pe);
    } catch (final IOException e) {
      throw new ChallengeXMLException(e);
    }
  }

  private static String applyTransform(final String content,
                                       final InputStream transform) {
    try {
      final Source stylesheet = new StreamSource(transform);

      final Document oldDocument = net.mtu.eggplant.xml.XMLUtils.parseXMLDocument(new StringReader(content));

      // Use a Transformer for output
      final TransformerFactory tFactory = TransformerFactory.newInstance();
      final Transformer transformer = tFactory.newTransformer(stylesheet);

      final DOMSource source = new DOMSource(oldDocument);
      final StringWriter outputWriter = new StringWriter();
      final StreamResult result = new StreamResult(outputWriter);
      transformer.transform(source, result);

      return outputWriter.toString();
    } catch (final IOException e) {
      throw new ChallengeXMLException("Error transforming description", e);
    } catch (final SAXException e) {
      throw new ChallengeXMLException("Error transforming description", e);
    } catch (final TransformerConfigurationException e) {
      throw new ChallengeXMLException("Error creating transformer", e);
    } catch (final TransformerException e) {
      throw new ChallengeXMLException("Error transforming description", e);
    }

  }

  /**
   * Convert from version 0 to version 1 of the schema.
   * 
   * @param content
   * @return
   */
  private static String transform0To1(final String content) {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    return applyTransform(content, classLoader.getResourceAsStream("fll/resources/schema0-to-schema1.xsl"));
  }

  private static String transform1To2(final String content) {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    return applyTransform(content, classLoader.getResourceAsStream("fll/resources/schema1-to-schema2.xsl"));
  }

  /**
   * Determine the version of the schema used in the XML document.
   * 
   * @param content
   * @return
   * @throws IOException
   * @throws SAXException
   */
  private static int determineSchemaVersion(final String content) throws SAXException, IOException {
    final Document document = net.mtu.eggplant.xml.XMLUtils.parseXMLDocument(new StringReader(content));
    final Element rootElement = document.getDocumentElement();
    if (!"fll".equals(rootElement.getTagName())) {
      throw new ChallengeXMLException("Not a fll challenge description file");
    }

    if (rootElement.hasAttribute("schemaVersion")) {
      return Integer.parseInt(rootElement.getAttribute("schemaVersion"));
    } else {
      return 0;
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
      throw new ChallengeXMLException("Not a fll challenge description file");
    }

    for (final Element childNode : new NodelistElementCollectionAdapter(rootElement.getChildNodes())) {
      if ("Performance".equals(childNode.getNodeName())
          || "SubjectiveCategory".equals(childNode.getNodeName())) {
        final Element childElement = childNode;

        // get all nodes named goal at any level under category element
        final Map<String, Element> simpleGoals = new HashMap<String, Element>();
        for (final Element element : new NodelistElementCollectionAdapter(childElement.getElementsByTagName("goal"))) {
          final String name = element.getAttribute("name");
          simpleGoals.put(name, element);

          // check initial values
          final double initialValue = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("initialValue"))
                                                                      .doubleValue();
          if (XMLUtils.isEnumeratedGoal(element)) {
            boolean foundMatch = false;
            for (final Element valueEle : new NodelistElementCollectionAdapter(element.getChildNodes())) {
              final double score = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.parse(valueEle.getAttribute("score")).doubleValue();
              if (FP.equals(score, initialValue, INITIAL_VALUE_TOLERANCE)) {
                foundMatch = true;
              }
            }
            if (!foundMatch) {
              throw new InvalidInitialValue(
                                            String.format("Initial value for %s(%f) does not match the score of any value element within the goal",
                                                          name, initialValue));
            }

          } else {
            final double min = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).doubleValue();
            final double max = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).doubleValue();
            if (FP.lessThan(initialValue, min, INITIAL_VALUE_TOLERANCE)) {
              throw new InvalidInitialValue(String.format("Initial value for %s(%f) is less than min(%f)", name,
                                                          initialValue, min));
            }
            if (FP.greaterThan(initialValue, max, INITIAL_VALUE_TOLERANCE)) {
              throw new InvalidInitialValue(String.format("Initial value for %s(%f) is greater than max(%f)", name,
                                                          initialValue, max));
            }
          }
        } // foreach goal

        // for all computedGoals
        final Map<String, Element> computedGoals = new HashMap<String, Element>();
        for (final Element computedGoalElement : new NodelistElementCollectionAdapter(
                                                                                      childElement.getElementsByTagName("computedGoal"))) {
          final String name = computedGoalElement.getAttribute("name");
          computedGoals.put(name, computedGoalElement);

          for (final Element goalRefElement : new NodelistElementCollectionAdapter(
                                                                                   computedGoalElement.getElementsByTagName("enumGoalRef"))) {

            // can't reference a non-enum goal with goalRef in enumCond
            final String referencedGoalName = goalRefElement.getAttribute("goal");
            final Element referencedGoalElement = simpleGoals.get(referencedGoalName);
            if (!XMLUtils.isEnumeratedGoal(referencedGoalElement)) {
              throw new InvalidEnumCondition("Computed goal '"
                  + computedGoalElement.getAttribute("name") + "' has an enumGoalRef element that references goal '"
                  + referencedGoalName + " " + referencedGoalElement + "' which is not an enumerated goal");
            }
          } // foreach goalRef element

        } // end foreach computed goal

        // computed and non-computed goals
        final Map<String, Element> allGoals = new HashMap<String, Element>();
        allGoals.putAll(simpleGoals);
        allGoals.putAll(computedGoals);

        checkForCircularDependencies(computedGoals);

        for (final Element termElement : new NodelistElementCollectionAdapter(
                                                                              childElement.getElementsByTagName("goalRef"))) {
          final String goalValueType = termElement.getAttribute("scoreType");
          final String referencedGoalName = termElement.getAttribute("goal");
          final Element referencedGoalElement = allGoals.get(referencedGoalName);
          if (null != referencedGoalElement
              && "raw".equals(goalValueType)
              && (XMLUtils.isEnumeratedGoal(referencedGoalElement) || XMLUtils.isComputedGoal(referencedGoalElement))) {
            // can't use the raw score of an enum inside a polynomial term
            throw new IllegalScoreTypeUseException(
                                                   "Cannot use the raw score from an enumerated or computed goal in a polynomial term.  Referenced goal '"
                                                       + referencedGoalName + "'");
          }
        } // foreach term

      } // end if child node (performance or subjective)
    } // end foreach child node
  } // end validateDocument

  /**
   * @param computedGoals
   */
  private static void checkForCircularDependencies(final Map<String, Element> computedGoals) {
    for (Map.Entry<String, Element> entry : computedGoals.entrySet()) {
      final String thisGoal = entry.getKey();

      final Set<String> visited = new HashSet<String>();
      final List<String> toVisit = new LinkedList<String>();

      toVisit.addAll(getImmediateComputedGoalDependencies(entry.getValue()));
      while (!toVisit.isEmpty()) {
        final String dep = toVisit.remove(0);
        if (dep.equals(thisGoal)) {
          throw new CircularComputedGoalException("'"
              + thisGoal + "' has a circular dependency");
        }

        if (visited.add(dep)) {

          final Set<String> immediateDependencies = getImmediateComputedGoalDependencies(computedGoals.get(dep));
          for (final String idep : immediateDependencies) {
            if (!visited.contains(idep)) {
              toVisit.add(idep);
            }
          }
        }

      } // while !toVisit.isEmpty

    } // foreach computed goal
  }

  /**
   * Find all immediate dependencies of the specified computed goal element.
   * 
   * @param computedGoalElement the element for the computed goal, if null and
   *          empty set is returned
   * @return the set of dependencies, will return the names of all goals
   *         referenced
   */
  private static Set<String> getImmediateComputedGoalDependencies(final Element computedGoalElement) {
    if (null == computedGoalElement) {
      return Collections.emptySet();
    } else {
      final Set<String> dependencies = new HashSet<String>();
      for (final Element termElement : new NodelistElementCollectionAdapter(
                                                                            computedGoalElement.getElementsByTagName("goalRef"))) {

        // check that the computed goal only references goals
        final String referencedGoalName = termElement.getAttribute("goal");
        dependencies.add(referencedGoalName);
      }
      return dependencies;
    }
  }

  public static String compareStructure(final Document curDoc,
                                        final Document newDoc) {
    return compareStructure(new ChallengeDescription(curDoc.getDocumentElement()),
                            new ChallengeDescription(newDoc.getDocumentElement()));
  }

  /**
   * If the new document differs from the current document in a way that the
   * database structure will be modified.
   * 
   * @param curDoc the current document
   * @param newDoc the document to check against
   * @return null if everything checks out OK, otherwise the error message
   */
  public static String compareStructure(final ChallengeDescription curDoc,
                                        final ChallengeDescription newDoc) {
    final PerformanceScoreCategory curPerfElement = curDoc.getPerformance();
    final PerformanceScoreCategory newPerfElement = newDoc.getPerformance();

    final Map<String, String> curPerGoals = gatherColumnDefinitions(curPerfElement);
    final Map<String, String> newPerGoals = gatherColumnDefinitions(newPerfElement);
    final String goalCompareMessage = compareGoalDefinitions("Performance", curPerGoals, newPerGoals);
    if (null != goalCompareMessage) {
      return goalCompareMessage;
    }

    final List<ScoreCategory> curSubCats = curDoc.getSubjectiveCategories();
    final List<ScoreCategory> newSubCats = newDoc.getSubjectiveCategories();
    if (curSubCats.size() != newSubCats.size()) {
      return "New document has "
          + newSubCats.size() + " subjective categories, current document has " + curSubCats.size()
          + " subjective categories";
    }
    final Map<String, Map<String, String>> curCats = new HashMap<String, Map<String, String>>();
    for (final ScoreCategory ele : curSubCats) {
      final String name = ele.getName();
      final Map<String, String> goalDefs = gatherColumnDefinitions(ele);
      curCats.put(name, goalDefs);
    }

    for (final ScoreCategory ele : newSubCats) {
      final String name = ele.getName();
      if (!curCats.containsKey(name)) {
        return "New document has subjective category '"
            + name + "' which is not in the current document";
      }

      final Map<String, String> curGoalDefs = curCats.get(name);
      final Map<String, String> newGoalDefs = gatherColumnDefinitions(ele);
      final String goalMsg = compareGoalDefinitions(name, curGoalDefs, newGoalDefs);
      if (null != goalMsg) {
        return goalMsg;
      }
    }

    return null;
  }

  private static String compareGoalDefinitions(final String category,
                                               final Map<String, String> curGoals,
                                               final Map<String, String> newGoals) {
    if (curGoals.size() != newGoals.size()) {
      return "New document has "
          + newGoals.size() + " goals in category '" + category + "', current document has " + curGoals.size()
          + " goals";
    }

    for (final Map.Entry<String, String> curEntry : curGoals.entrySet()) {
      if (!newGoals.containsKey(curEntry.getKey())) {
        return "New document is missing goal '"
            + curEntry.getKey() + "'";
      }
      final String curDef = curEntry.getValue();
      final String newDef = newGoals.get(curEntry.getKey());
      if (!curDef.equals(newDef)) {
        return "Database definition for goal '"
            + curEntry.getKey() + "' in category '" + category
            + "' is different in the new document from the current document" + " '" + curDef + "' vs '" + newDef + "'";
      }
    }
    return null;
  }

  /**
   * Get the column definitions for all goals in the specified element
   */
  private static Map<String, String> gatherColumnDefinitions(final ScoreCategory element) {
    final Map<String, String> goalDefs = new HashMap<String, String>();

    for (final AbstractGoal goal : element.getGoals()) {
      if (!goal.isComputed()) {
        final String columnDefinition = GenerateDB.generateGoalColumnDefinition(goal);
        final String goalName = goal.getName();
        goalDefs.put(goalName, columnDefinition);
      }
    }

    return goalDefs;
  }

}
