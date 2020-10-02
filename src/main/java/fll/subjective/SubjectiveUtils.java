/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fll.Utilities;
import fll.web.admin.DownloadSubjectiveData;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.SubjectiveScoreCategory;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Utils for the subjective scoring application.
 */
public final class SubjectiveUtils {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private SubjectiveUtils() {
    // no instances
  }

  /**
   * Given two subjective data files (as zip files), compare them.
   * 
   * @param masterFile the first file to compare
   * @param compareFile the second file to compare
   * @return null if the challenge descriptors are different, otherwise the
   *         differences
   * @throws IOException if there is an error reading the raw file
   * @throws ZipException if there is an error in the subjective data file
   * @throws SAXException if there is an error parsing the subjective scores
   *           documents
   * @see #compareScoreDocuments(ChallengeDescription, Document, Document)
   */
  public static @Nullable Collection<SubjectiveScoreDifference> compareSubjectiveFiles(final File masterFile,
                                                                                       final File compareFile)
      throws ZipException, IOException, SAXException {
    ZipFile masterZipfile = null;
    ZipFile compareZipfile = null;

    try {
      masterZipfile = new ZipFile(masterFile);

      final ZipEntry masterScoreZipEntry = masterZipfile.getEntry("score.xml");
      if (null == masterScoreZipEntry) {
        throw new RuntimeException("Master Zipfile does not contain score.xml as expected");
      }
      final InputStream masterScoreStream = masterZipfile.getInputStream(masterScoreZipEntry);
      final Document masterScoreDocument = XMLUtils.parseXMLDocument(masterScoreStream);
      masterScoreStream.close();

      final InputStream masterChallengeStream = masterZipfile.getInputStream(masterZipfile.getEntry("challenge.xml"));
      final ChallengeDescription masterDescription = ChallengeParser.parse(new InputStreamReader(masterChallengeStream,
                                                                                                 Utilities.DEFAULT_CHARSET));
      masterChallengeStream.close();

      compareZipfile = new ZipFile(compareFile);

      final ZipEntry compareScoreZipEntry = compareZipfile.getEntry("score.xml");
      if (null == compareScoreZipEntry) {
        throw new RuntimeException("Compare Zipfile does not contain score.xml as expected");
      }
      final InputStream compareScoreStream = compareZipfile.getInputStream(compareScoreZipEntry);
      final Document compareScoreDocument = XMLUtils.parseXMLDocument(compareScoreStream);
      compareScoreStream.close();

      final InputStream compareChallengeStream = compareZipfile.getInputStream(compareZipfile.getEntry("challenge.xml"));
      final ChallengeDescription compareDescription = ChallengeParser.parse(new InputStreamReader(compareChallengeStream,
                                                                                                  Utilities.DEFAULT_CHARSET));
      compareChallengeStream.close();

      compareZipfile.close();

      if (!XMLUtils.compareDocuments(masterDescription.toXml(), compareDescription.toXml())) {
        return null;
      } else {
        return compareScoreDocuments(masterDescription, masterScoreDocument, compareScoreDocument);
      }
    } finally {
      if (null != masterZipfile) {
        try {
          masterZipfile.close();
        } catch (final IOException e) {
          LOGGER.debug("Error closing master zipfile", e);
        }
      }
      if (null != compareZipfile) {
        try {
          compareZipfile.close();
        } catch (final IOException e) {
          LOGGER.debug("Error closing compare zipfile", e);
        }
      }
    }

  }

  /**
   * Find the "subjectiveCategory" element in the specified challenge descriptor
   * with the given name.
   * 
   * @param challengeDocument the challenge descriptor
   * @param name the name of the subjective category
   * @return the element or null if it's not found
   */
  public static Element getSubjectiveElement(final Document challengeDocument,
                                             final String name) {
    for (final Element subjectiveElement : new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement()
                                                                                                 .getElementsByTagName("subjectiveCategory"))) {
      if (name.equals(subjectiveElement.getAttribute("name"))) {
        return subjectiveElement;
      }
    }
    return null;
  }

  /**
   * Compare the scores between two documents.
   * 
   * @param challenge the challenge descriptor, needed to get the list
   *          of subjective categories
   * @param master the original document
   * @param compare the document to compare to master
   * @return the differences found in compare wrt. master
   */
  public static Collection<SubjectiveScoreDifference> compareScoreDocuments(final ChallengeDescription challenge,
                                                                            final Document master,
                                                                            final Document compare) {
    final Element masterScoresElement = master.getDocumentElement();
    final Element compareScoresElement = compare.getDocumentElement();

    final Collection<SubjectiveScoreDifference> diffs = new LinkedList<SubjectiveScoreDifference>();
    for (final Element masterScoreCategory : new NodelistElementCollectionAdapter(masterScoresElement.getChildNodes())) {
      System.out.println("Master score category element name: "
          + masterScoreCategory.getLocalName()
          + " category name: "
          + masterScoreCategory.getAttribute("name"));
      compareScoreCategory(challenge, masterScoreCategory, compareScoresElement, diffs);
    }
    return diffs;
  }

  private static void compareScoreCategory(final ChallengeDescription challenge,
                                           final Element masterScoreCategory,
                                           final Element compareScoresElement,
                                           final Collection<SubjectiveScoreDifference> diffs) {
    // masterScoreCategory and compareScoreCategory are "subjectiveCategory"
    final String categoryName = masterScoreCategory.getAttribute("name");
    final Element compareScoreCategory = getCategoryNode(compareScoresElement, categoryName);

    final SubjectiveScoreCategory category = challenge.getSubjectiveCategoryByName(categoryName);
    if (null == category) {
      throw new RuntimeException("Cannot find subjective category description for category in score document category: "
          + categoryName);
    }

    final List<AbstractGoal> goals = category.getAllGoals();

    // for each score element
    final List<Element> masterScores = new NodelistElementCollectionAdapter(masterScoreCategory.getElementsByTagName("score")).asList();
    final List<Element> compareScores = new NodelistElementCollectionAdapter(compareScoreCategory.getElementsByTagName("score")).asList();
    if (masterScores.size() != compareScores.size()) {
      throw new RuntimeException("Score documents have different number of score elements");
    }
    for (final Element masterScore : masterScores) {
      System.out.println("Master score - team: "
          + masterScore.getAttribute("teamNumber"));
      final Element compareScore = findCorrespondingScoreElement(masterScore, compareScores);
      System.out.println("Compare score - team: "
          + compareScore.getAttribute("teamNumber"));
      diffScores(goals, diffs, category, masterScore, compareScore);
    }
  }

  private static Element findCorrespondingScoreElement(final Element masterScore,
                                                       final List<Element> compareScores) {
    try {
      final int teamNumber = Utilities.getIntegerNumberFormat().parse(masterScore.getAttribute("teamNumber"))
                                      .intValue();
      final String judge = masterScore.getAttribute("judge");
      for (final Element compareScore : compareScores) {
        final int compareTeamNumber = Utilities.getIntegerNumberFormat().parse(compareScore.getAttribute("teamNumber"))
                                               .intValue();
        final String compareJudge = compareScore.getAttribute("judge");
        if (teamNumber == compareTeamNumber
            && judge.equals(compareJudge)) {
          return compareScore;
        }
      }
    } catch (final ParseException exception) {
      throw new RuntimeException("Unable to parse team number", exception);
    }
    return null;
  }

  private static void diffScores(final List<AbstractGoal> goals,
                                 final Collection<SubjectiveScoreDifference> diffs,
                                 final SubjectiveScoreCategory category,
                                 final Element masterScore,
                                 final Element compareScore) {
    final String categoryTitle = category.getTitle();

    try {
      final int teamNumber = Utilities.getIntegerNumberFormat().parse(masterScore.getAttribute("teamNumber"))
                                      .intValue();
      final String judge = masterScore.getAttribute("judge");

      final Boolean masterNoShow = XMLUtils.getBooleanAttributeValue(masterScore, "NoShow");
      final Boolean compareNoShow = XMLUtils.getBooleanAttributeValue(masterScore, "NoShow");
      if (!Objects.equals(masterNoShow, compareNoShow)) {
        diffs.add(new BooleanSubjectiveScoreDifference(categoryTitle, "NoShow", teamNumber, judge, masterNoShow,
                                                       compareNoShow));
      }

      for (final AbstractGoal goal : goals) {
        final String goalTitle = goal.getTitle();
        final String goalName = goal.getName();
        final Element masterSubscoreElement = getSubscoreElement(masterScore, goalName);

        final Element compareSubscoreElement = getSubscoreElement(compareScore, goalName);

        if (null == masterSubscoreElement
            && null == compareSubscoreElement) {
          // equal
          LOGGER.trace("Both elements are null");
        } else if (null == masterSubscoreElement) {
          diffs.add(new StringSubjectiveScoreDifference(categoryTitle, goalTitle, teamNumber, judge, null, "not null"));
        } else if (null == compareSubscoreElement) {
          diffs.add(new StringSubjectiveScoreDifference(categoryTitle, goalTitle, teamNumber, judge, "not null", null));
        } else {
          if (goal.isEnumerated()) {
            final String masterValueStr = XMLUtils.getStringAttributeValue(masterSubscoreElement, "value");
            final String compareValueStr = XMLUtils.getStringAttributeValue(compareSubscoreElement, "value");
            if (!Objects.equals(masterValueStr, compareValueStr)) {
              diffs.add(new StringSubjectiveScoreDifference(categoryTitle, goalTitle, teamNumber, judge, masterValueStr,
                                                            compareValueStr));
            }
          } else {
            final Double masterValue = XMLUtils.getDoubleAttributeValue(masterSubscoreElement, "value");
            final Double compareValue = XMLUtils.getDoubleAttributeValue(compareSubscoreElement, "value");
            if (!Objects.equals(masterValue, compareValue)) {
              diffs.add(new DoubleSubjectiveScoreDifference(categoryTitle, goalTitle, teamNumber, judge, masterValue,
                                                            compareValue));
            }
          }
        }

      }
    } catch (final ParseException exception) {
      throw new RuntimeException("Unable to parse team number", exception);
    }
  }

  /**
   * Get the category node that matches the specified name.
   * 
   * @param scoresElement the "scores" element from a subjective scores document
   * @param categoryName the name of the category to find
   * @return null if not found
   */
  public static Element getCategoryNode(final Element scoresElement,
                                        final String categoryName) {
    for (final Element scoreCategory : new NodelistElementCollectionAdapter(scoresElement.getChildNodes())) {
      final String name = scoreCategory.getAttribute("name");
      if (categoryName.equals(name)) {
        return scoreCategory;
      }
    }

    throw new RuntimeException("Score document doesn't have scores for category: "
        + categoryName);
  }

  /**
   * Given the score element, find the subscore element for the specified goal
   * in the subjective score document.
   * 
   * @param scoreElement the "score" element from a subjective scores document
   * @param goalName the name of the goal to find
   * @return the element or null if not found
   */
  public static @Nullable Element getSubscoreElement(final Element scoreElement,
                                                     final String goalName) {
    final NodeList nlist = scoreElement.getElementsByTagName(DownloadSubjectiveData.SUBSCORE_NODE_NAME);
    for (int i = 0; i < nlist.getLength(); ++i) {
      final Node n = nlist.item(i);
      LOGGER.info(n.getLocalName());
    }
    for (final Element subEle : new NodelistElementCollectionAdapter(scoreElement.getElementsByTagName(DownloadSubjectiveData.SUBSCORE_NODE_NAME))) {
      final String name = subEle.getAttribute("name");
      LOGGER.info("Checking goal with name: "
          + name);
      if (goalName.equals(name)) {
        return subEle;
      }
    }
    return null;
  }

}
