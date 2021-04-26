/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fll.Utilities;
import fll.db.GenerateDB;
import fll.util.FLLInternalException;
import fll.util.FP;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Parse challenge description and generate script/text for scoreEntry page.
 */
public final class ChallengeParser {

  /**
   * The expected namespace for FLL documents.
   */
  public static final String FLL_NAMESPACE = "http://www.hightechkids.org";

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Used to compare the initial value against enum values and min/maxes.
   */
  public static final double INITIAL_VALUE_TOLERANCE = 1E-4;

  /**
   * Current version of the schema.
   */
  public static final int CURRENT_SCHEMA_VERSION = 3;

  /**
   * The XML attribute used to store the {@link ScoreType}.
   */
  public static final String SCORE_TYPE_ATTRIBUTE = "scoreType";

  /**
   * The XML attribute used to store the {@link WinnerType}.
   */
  public static final String WINNER_ATTRIBUTE = "winner";

  private ChallengeParser() {
    // no instances
  }

  /**
   * Parse the challenge document from the given stream. The document will be
   * validated and must be in the fll namespace. Does not close the stream after
   * reading.
   *
   * @param stream a stream containing document
   * @return the parsed challenge description
   * @throws ChallengeXMLException on error with the XML
   * @throws ChallengeValidationException on an error doing additional validation
   *           of the challenge
   */
  public static ChallengeDescription parse(final Reader stream)
      throws ChallengeXMLException, ChallengeValidationException {
    try {
      final StringWriter writer = new StringWriter();
      stream.transferTo(writer);
      String content = writer.toString();

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
      if (schemaVersion == 2) {
        content = transform2To3(content);
      }

      schemaVersion = determineSchemaVersion(content);
      if (schemaVersion != CURRENT_SCHEMA_VERSION) {
        throw new ChallengeXMLException("Error upgrading document, should have version "
            + CURRENT_SCHEMA_VERSION
            + ", but is "
            + schemaVersion);
      }

      final ClassLoader classLoader = Utilities.getClassLoader();

      final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      try (InputStream schemaStream = classLoader.getResourceAsStream("fll/resources/fll.xsd")) {
        if (null == schemaStream) {
          throw new FLLInternalException("Unable to find fll.xsd");
        }

        final Source schemaFile = new StreamSource(schemaStream);
        final Schema schema = factory.newSchema(schemaFile);

        final Document document = XMLUtils.parse(new StringReader(content), schema);

        // challenge descriptor specific checks
        validateDocument(document);

        final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

        validateDescription(description);

        return description;
      } // schema stream
    } catch (final SAXParseException spe) {
      throw new ChallengeXMLException(String.format("Error parsing file line: %d column: %d%n Message: %s%n This may be caused by using the wrong version of the software or an improperly formatted challenge descriptor or attempting to parse a file that is not a challenge descriptor.",
                                                    spe.getLineNumber(), spe.getColumnNumber(), spe.getMessage()),
                                      spe);
    } catch (final SAXException se) {
      throw new ChallengeXMLException("The challenge descriptor was found to be invalid, check that you are parsing a challenge descriptor file and not something else",
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

  private static String transform0To1(final String content) throws IOException {
    return transformSchema(content, "fll/resources/schema0-to-schema1.xsl");
  }

  private static String transform1To2(final String content) throws IOException {
    return transformSchema(content, "fll/resources/schema1-to-schema2.xsl");
  }

  private static String transform2To3(final String content) throws IOException {
    return transformSchema(content, "fll/resources/schema2-to-schema3.xsl");
  }

  private static String transformSchema(final String content,
                                        final String xsl)
      throws IOException {
    final ClassLoader classLoader = Utilities.getClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream(xsl)) {
      if (null == stream) {
        throw new FLLInternalException("Cannot find "
            + xsl);
      }
      return applyTransform(content, stream);
    }
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
   * Do validation of the description that can't be done by the XML parser.
   * 
   * @param description the challenge description to validate
   * @throws RuntimeException if an error occurs
   */
  private static void validateDescription(final ChallengeDescription description) throws ChallengeValidationException {
    description.getSubjectiveCategories().forEach(ChallengeParser::validateSubjectiveCategory);

  }

  /**
   * Run validity checks on the specified subjective category.
   * 
   * @param category the category to check
   * @throws ChallengeValidationException if there is an error
   */
  public static void validateSubjectiveCategory(final SubjectiveScoreCategory category)
      throws ChallengeValidationException {
    validateCategoryRubric(category);
  }

  private static void validateCategoryRubric(final SubjectiveScoreCategory category)
      throws ChallengeValidationException {
    final List<Goal> goals = category.getAllGoals().stream() //
                                     .filter(Goal.class::isInstance) //
                                     .map(Goal.class::cast) //
                                     .collect(Collectors.toList());
    if (goals.isEmpty()) {
      // nothing to check
      return;
    }

    final Goal firstGoal = goals.get(0);
    final List<RubricRange> firstRubricRange = firstGoal.getRubric();
    for (final Goal compareGoal : goals) {
      final List<RubricRange> compareRubricRange = compareGoal.getRubric();
      if (firstRubricRange.size() != compareRubricRange.size()) {
        throw new ChallengeValidationException(String.format("Rubric range size not the same between goal %s and %s",
                                                             firstGoal.getTitle(), compareGoal.getTitle()));
      }

      final Iterator<RubricRange> firstIter = firstRubricRange.iterator();
      final Iterator<RubricRange> compareIter = compareRubricRange.iterator();
      while (firstIter.hasNext()
          && compareIter.hasNext()) {
        final RubricRange firstRange = firstIter.next();
        final RubricRange compareRange = compareIter.next();
        if (!firstRange.getTitle().equals(compareRange.getTitle())) {
          throw new ChallengeValidationException(String.format("Rubric range titles not the same between goal %s (%s) and goal %s (%s)",
                                                               firstGoal.getTitle(), firstRange.getTitle(),
                                                               compareGoal.getTitle(), compareRange.getTitle()));
        }

        if (firstRange.getMin() != compareRange.getMin()) {
          throw new ChallengeValidationException(String.format("Rubric range min not the same between goal %s (%d) and goal %s (%d)",
                                                               firstGoal.getTitle(), firstRange.getMin(),
                                                               compareGoal.getTitle(), compareRange.getMin()));
        }

        if (firstRange.getMax() != compareRange.getMax()) {
          throw new ChallengeValidationException(String.format("Rubric range max not the same between goal %s (%d) and goal %s (%d)",
                                                               firstGoal.getTitle(), firstRange.getMax(),
                                                               compareGoal.getTitle(), compareRange.getMax()));
        }

      }
    }
  }

  /**
   * Do validation of the document that cannot be done by the XML parser.
   *
   * @param document the document to validate
   * @throws ParseException if there is a problem parsing the document
   * @throws RuntimeException if an error occurs
   */
  private static void validateDocument(final Document document) throws ParseException, RuntimeException {
    final Element rootElement = document.getDocumentElement();
    if (!"fll".equals(rootElement.getTagName())) {
      throw new ChallengeXMLException("Not a fll challenge description file");
    }

    for (final Element childNode : new NodelistElementCollectionAdapter(rootElement.getChildNodes())) {
      if ("Performance".equals(childNode.getNodeName())
          || "SubjectiveCategory".equals(childNode.getNodeName())) {
        validateCategory(childNode);
      } // end if child node (performance or subjective)
    } // end foreach child node
  } // end validateDocument

  private static void validateCategory(final Element categoryElement) throws ParseException {
    // get all nodes named goal at any level under category element
    final Map<String, Element> simpleGoals = new HashMap<>();
    for (final Element goalElement : new NodelistElementCollectionAdapter(categoryElement.getElementsByTagName("goal"))) {
      final String name = goalElement.getAttribute("name");
      simpleGoals.put(name, goalElement);

      validateGoalInitialValue(goalElement, name);
    } // foreach goal

    final Map<String, Element> computedGoals = validateComputedGoals(categoryElement, simpleGoals);

    // computed and non-computed goals
    final Map<String, Element> allGoals = new HashMap<>();
    allGoals.putAll(simpleGoals);
    allGoals.putAll(computedGoals);

    checkForCircularDependencies(computedGoals);

    validateGoalRefs(categoryElement, allGoals);
  }

  private static void validateGoalRefs(final Element categoryElement,
                                       final Map<String, Element> allGoals) {
    for (final Element termElement : new NodelistElementCollectionAdapter(categoryElement.getElementsByTagName("goalRef"))) {
      final String goalValueType = termElement.getAttribute("scoreType");
      final String referencedGoalName = termElement.getAttribute("goal");
      final Element referencedGoalElement = allGoals.get(referencedGoalName);
      if (null != referencedGoalElement
          && "raw".equals(goalValueType)
          && (ChallengeParser.isEnumeratedGoal(referencedGoalElement)
              || ChallengeParser.isComputedGoal(referencedGoalElement))) {
        // can't use the raw score of an enum inside a polynomial term
        throw new IllegalScoreTypeUseException("Cannot use the raw score from an enumerated or computed goal in a polynomial term.  Referenced goal '"
            + referencedGoalName
            + "'");
      }
    } // foreach goalRef
  }

  private static Map<String, Element> validateComputedGoals(final Element categoryElement,
                                                            final Map<String, Element> simpleGoals) {
    final Map<String, Element> computedGoals = new HashMap<>();
    for (final Element computedGoalElement : new NodelistElementCollectionAdapter(categoryElement.getElementsByTagName("computedGoal"))) {
      final String name = computedGoalElement.getAttribute("name");
      computedGoals.put(name, computedGoalElement);

      for (final Element goalRefElement : new NodelistElementCollectionAdapter(computedGoalElement.getElementsByTagName("enumGoalRef"))) {

        // can't reference a non-enum goal with goalRef in enumCond
        final String referencedGoalName = goalRefElement.getAttribute("goal");
        final Element referencedGoalElement = simpleGoals.get(referencedGoalName);
        if (null == referencedGoalElement) {
          throw new ChallengeValidationException("Computed goal '"
              + name
              + "' references enumerated goal '"
              + referencedGoalName
              + "' which cannot be found");
        } else if (!ChallengeParser.isEnumeratedGoal(referencedGoalElement)) {
          throw new InvalidEnumCondition("Computed goal '"
              + name
              + "' has an enumGoalRef element that references goal '"
              + referencedGoalName
              + " "
              + referencedGoalElement
              + "' which is not an enumerated goal");
        }
      } // foreach goalRef element

    } // end foreach computed goal
    return computedGoals;
  }

  private static void validateGoalInitialValue(final Element goalElement,
                                               final String name)
      throws ParseException {
    final String rawInitialValue = goalElement.getAttribute(Goal.INITIAL_VALUE_ATTRIBUTE);
    final Number parsedInitialValue = Utilities.getXmlFloatingPointNumberFormat().parse(rawInitialValue);
    final double initialValue = parsedInitialValue.doubleValue();
    LOGGER.trace("{} Raw initialValue: '{}' parsed: {} double: {}", name, rawInitialValue, parsedInitialValue,
                 initialValue);

    if (ChallengeParser.isEnumeratedGoal(goalElement)) {
      boolean foundMatch = false;
      for (final Element valueEle : new NodelistElementCollectionAdapter(goalElement.getChildNodes())) {
        final double score = Utilities.getXmlFloatingPointNumberFormat().parse(valueEle.getAttribute("score"))
                                      .doubleValue();
        if (FP.equals(score, initialValue, INITIAL_VALUE_TOLERANCE)) {
          foundMatch = true;
        }
      }
      if (!foundMatch) {
        throw new InvalidInitialValue(String.format("Initial value for %s(%f) does not match the score of any value element within the goal",
                                                    name, initialValue));
      }

    } else {
      final String rawMin = goalElement.getAttribute(Goal.MIN_ATTRIBUTE);
      final Number numMin = Utilities.getXmlFloatingPointNumberFormat().parse(rawMin);
      final double min = numMin.doubleValue();
      final String rawMax = goalElement.getAttribute(Goal.MAX_ATTRIBUTE);
      final Number numMax = Utilities.getXmlFloatingPointNumberFormat().parse(rawMax);
      final double max = numMax.doubleValue();

      LOGGER.trace("{} Raw min: '{}' parsed: {} double: {}", name, rawMin, numMin, min);
      LOGGER.trace("{} Raw max: '{}' parsed: {} double: {}", name, rawMax, numMax, max);

      if (FP.lessThan(initialValue, min, INITIAL_VALUE_TOLERANCE)) {
        throw new InvalidInitialValue(String.format("Initial value for %s(%f) is less than min(%f)", name, initialValue,
                                                    min));
      }
      if (FP.greaterThan(initialValue, max, INITIAL_VALUE_TOLERANCE)) {
        throw new InvalidInitialValue(String.format("Initial value for %s(%f) is greater than max(%f)", name,
                                                    initialValue, max));
      }
    }
  }

  /**
   * @param computedGoals
   */
  private static void checkForCircularDependencies(final Map<String, Element> computedGoals) {
    for (final Map.Entry<String, Element> entry : computedGoals.entrySet()) {
      final String thisGoal = entry.getKey();

      final Set<String> visited = new HashSet<>();
      final List<String> toVisit = new LinkedList<>();

      toVisit.addAll(getImmediateComputedGoalDependencies(entry.getValue()));
      while (!toVisit.isEmpty()) {
        final String dep = toVisit.remove(0);
        if (dep.equals(thisGoal)) {
          throw new CircularComputedGoalException("'"
              + thisGoal
              + "' has a circular dependency");
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
  private static Set<String> getImmediateComputedGoalDependencies(final @Nullable Element computedGoalElement) {
    if (null == computedGoalElement) {
      return Collections.emptySet();
    } else {
      final Set<String> dependencies = new HashSet<>();
      for (final Element termElement : new NodelistElementCollectionAdapter(computedGoalElement.getElementsByTagName("goalRef"))) {

        // check that the computed goal only references goals
        final String referencedGoalName = termElement.getAttribute("goal");
        dependencies.add(referencedGoalName);
      }
      return dependencies;
    }
  }

  /**
   * If the new document differs from the current document in a way that the
   * database structure will be modified.
   *
   * @param curDoc the current document
   * @param newDoc the document to check against
   * @return null if everything checks out OK, otherwise the error message
   */
  public static @Nullable String compareStructure(final ChallengeDescription curDoc,
                                                  final ChallengeDescription newDoc) {
    final PerformanceScoreCategory curPerfElement = curDoc.getPerformance();
    final PerformanceScoreCategory newPerfElement = newDoc.getPerformance();

    final Map<String, String> curPerGoals = gatherColumnDefinitions(curPerfElement);
    final Map<String, String> newPerGoals = gatherColumnDefinitions(newPerfElement);
    final String goalCompareMessage = compareGoalDefinitions("Performance", curPerGoals, newPerGoals);
    if (null != goalCompareMessage) {
      return goalCompareMessage;
    }

    final List<SubjectiveScoreCategory> curSubCats = curDoc.getSubjectiveCategories();
    final List<SubjectiveScoreCategory> newSubCats = newDoc.getSubjectiveCategories();
    if (curSubCats.size() != newSubCats.size()) {
      return "New document has "
          + newSubCats.size()
          + " subjective categories, current document has "
          + curSubCats.size()
          + " subjective categories";
    }
    final Map<String, Map<String, String>> curCats = new HashMap<>();
    for (final SubjectiveScoreCategory ele : curSubCats) {
      final String name = ele.getName();
      final Map<String, String> goalDefs = gatherColumnDefinitions(ele);
      curCats.put(name, goalDefs);
    }

    for (final SubjectiveScoreCategory ele : newSubCats) {
      final String name = ele.getName();
      if (!curCats.containsKey(name)) {
        return "New document has subjective category '"
            + name
            + "' which is not in the current document";
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

  /**
   * @return message on an error, null otherwise
   */
  private static @Nullable String compareGoalDefinitions(final String category,
                                                         final Map<String, String> curGoals,
                                                         final Map<String, String> newGoals) {
    if (curGoals.size() != newGoals.size()) {
      return "New document has "
          + newGoals.size()
          + " goals in category '"
          + category
          + "', current document has "
          + curGoals.size()
          + " goals";
    }

    for (final Map.Entry<String, String> curEntry : curGoals.entrySet()) {
      if (!newGoals.containsKey(curEntry.getKey())) {
        return "New document is missing goal '"
            + curEntry.getKey()
            + "'";
      }
      final String curDef = curEntry.getValue();
      final String newDef = newGoals.get(curEntry.getKey());
      if (!curDef.equals(newDef)) {
        return "Database definition for goal '"
            + curEntry.getKey()
            + "' in category '"
            + category
            + "' is different in the new document from the current document"
            + " '"
            + curDef
            + "' vs '"
            + newDef
            + "'";
      }
    }
    return null;
  }

  /**
   * Get the column definitions for all goals in the specified element
   */
  private static Map<String, String> gatherColumnDefinitions(final ScoreCategory element) {
    final Map<String, String> goalDefs = new HashMap<>();

    for (final AbstractGoal goal : element.getAllGoals()) {
      if (!goal.isComputed()) {
        final String columnDefinition = GenerateDB.generateGoalColumnDefinition(goal);
        final String goalName = goal.getName();
        goalDefs.put(goalName, columnDefinition);
      }
    }

    return goalDefs;
  }

  /**
   * Find a child element by tag name. This is very similar to
   * {@link Element#getElementsByTagName(String)}, except that it only works with
   * the direct children.
   *
   * @param tagname the tag name to select
   * @param parent the element to check the children of
   * @return the list of elements, may be empty
   */

  public static List<Element> getChildElementsByTagName(final Element parent,
                                                        final String tagname) {
    final List<Element> retval = new LinkedList<>();
    for (final Element child : new NodelistElementCollectionAdapter(parent.getChildNodes())) {
      if (tagname.equals(child.getTagName())) {
        retval.add(child);
      }
    }
    return retval;
  }

  /**
   * Get all challenge descriptors build into the software.
   *
   * @return a collection of all URLs to the challenge descriptions in the
   *         software
   */
  public static Collection<URL> getAllKnownChallengeDescriptorURLs() {
    LOGGER.debug("Top of getAllKnownChallengeDescriptorURLs");

    final String baseDir = "fll/resources/challenge-descriptors/";

    final ClassLoader classLoader = Utilities.getClassLoader();
    final URL directory = classLoader.getResource(baseDir);
    if (null == directory) {
      LOGGER.warn("base dir for challenge descriptors not found");
      return Collections.emptyList();
    }

    LOGGER.debug("Found challenge descriptors directory as {}", directory);

    final Collection<URL> urls = new LinkedList<>();
    if ("file".equals(directory.getProtocol())) {
      LOGGER.debug("Using file protocol");

      try {
        final URI uri = directory.toURI();
        final File fileDir = new File(uri);
        final File[] files = fileDir.listFiles();
        if (null != files) {
          for (final File file : files) {
            if (file.getName().endsWith(".xml")) {
              try {
                final URL fileUrl = file.toURI().toURL();
                urls.add(fileUrl);
              } catch (final MalformedURLException e) {
                LOGGER.error("Unable to convert file to URL: "
                    + file.getAbsolutePath(), e);
              }
            }
          }
        }
      } catch (final URISyntaxException e) {
        LOGGER.error("Unable to convert URL to URI: "
            + e.getMessage(), e);
      }
    } else if (directory.getProtocol().equals("jar")) {
      LOGGER.debug("Using jar protocol");

      final CodeSource src = XMLUtils.class.getProtectionDomain().getCodeSource();
      if (null != src) {
        final URL jar = src.getLocation();
        LOGGER.debug("src location {}", jar);

        try (JarInputStream zip = new JarInputStream(jar.openStream())) {
          JarEntry ze = null;
          while ((ze = zip.getNextJarEntry()) != null) {
            final String entryName = ze.getName();
            if (entryName.startsWith(baseDir)
                && entryName.endsWith(".xml")) {
              // add 1 to baseDir to skip past the path separator
              final String challengeName = entryName.substring(baseDir.length());

              // check that the file really exists and turn it into a URL
              final URL challengeUrl = classLoader.getResource(baseDir
                  + challengeName);
              if (null != challengeUrl) {
                urls.add(challengeUrl);
              } else {
                // TODO could write the resource out to a temporary file if
                // needed
                // then mark the file as delete on exit
                LOGGER.warn("URL doesn't exist for "
                    + baseDir
                    + challengeName
                    + " entry: "
                    + entryName);
              }
            }
          }

        } catch (final IOException e) {
          LOGGER.error("Error reading jar file at: "
              + jar.toString(), e);
        }

      } else {
        LOGGER.warn("Null code source in protection domain, cannot get challenge descriptors");
      }
    } else {
      throw new UnsupportedOperationException("Cannot list files for URL "
          + directory);

    }

    return urls;

  }

  /**
   * Get the score type for a particular element.
   *
   * @param element where to get the attribute from
   * @return the score type, defaults to {@link ScoreType#INTEGER}
   */
  public static ScoreType getScoreType(final Element element) {
    if (element.hasAttribute(ChallengeParser.SCORE_TYPE_ATTRIBUTE)) {
      final String str = element.getAttribute(ChallengeParser.SCORE_TYPE_ATTRIBUTE);
      final String sortStr;
      if (!str.isEmpty()) {
        sortStr = str.toUpperCase();
      } else {
        sortStr = ScoreType.INTEGER.toString();
      }
      return Enum.valueOf(ScoreType.class, sortStr);
    } else {
      return ScoreType.INTEGER;
    }
  }

  /**
   * Get the winner criteria for a particular element.
   *
   * @param element where to get the attribute from
   * @return the winner type, defaults to {@link WinnerType#HIGH}
   */
  public static WinnerType getWinnerCriteria(final Element element) {
    if (element.hasAttribute(ChallengeParser.WINNER_ATTRIBUTE)) {
      final String str = element.getAttribute(ChallengeParser.WINNER_ATTRIBUTE);
      final String sortStr;
      if (!str.isEmpty()) {
        sortStr = str.toUpperCase();
      } else {
        sortStr = WinnerType.HIGH.toString();
      }
      return Enum.valueOf(WinnerType.class, sortStr);
    } else {
      return WinnerType.HIGH;
    }
  }

  /**
   * Check if an element describes a computed goal or not.
   *
   * @param element the goal element
   * @return if the element represents a computed goal
   */
  public static boolean isComputedGoal(final Element element) {
    return "computedGoal".equals(element.getNodeName());
  }

  /**
   * Check if an element describes an enumerated goal or not.
   *
   * @param element the goal element
   * @return if the element represents an enumerated goal
   */
  public static boolean isEnumeratedGoal(final Element element) {
    if (!"goal".equals(element.getNodeName())) {
      // not a goal element
      return false;
    }

    final Iterator<Element> values = new NodelistElementCollectionAdapter(element.getElementsByTagName("value")).iterator();
    return values.hasNext();
  }

  /**
   * Find a subjective category by name.
   *
   * @param challengeDocument the document to look in
   * @param name the name to look for
   * @return the element or null if one is not found
   */
  public static @Nullable Element getSubjectiveCategoryByName(final Document challengeDocument,
                                                              final String name) {
    for (final Element categoryElement : new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement()
                                                                                               .getElementsByTagName("subjectiveCategory"))) {
      final String categoryName = categoryElement.getAttribute("name");
      if (categoryName.equals(name)) {
        return categoryElement;
      }
    }
    return null;
  }

}
