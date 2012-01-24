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
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;
import net.mtu.eggplant.xml.XMLUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.util.LogUtils;
import fll.xml.ChallengeParser;

/**
 * Utils for the subjective scoring application.
 */
public final class SubjectiveUtils {

  private static final Logger LOGGER = LogUtils.getLogger();

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
   * @throws IOException
   * @throws ZipException
   * @see #compareScoreDocuments(Document, Document, Document)
   */
  public static Collection<SubjectiveScoreDifference> compareSubjectiveFiles(final File masterFile,
                                                                             final File compareFile)
      throws ZipException, IOException {
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
      final Document masterChallengeDoc = ChallengeParser.parse(new InputStreamReader(masterChallengeStream));
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
      final Document compareChallengeDoc = ChallengeParser.parse(new InputStreamReader(compareChallengeStream));
      compareChallengeStream.close();

      compareZipfile.close();

      if (!fll.xml.XMLUtils.compareDocuments(masterChallengeDoc, compareChallengeDoc)) {
        return null;
      } else {
        return compareScoreDocuments(masterChallengeDoc, masterScoreDocument, compareScoreDocument);
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
   * Compare the scores between two documents.
   * 
   * @param challengeDocument the challenge descriptor, needed to get the list
   *          of subjective categories
   * @param master the original document
   * @param compare the document to compare to master
   * @return the differences found in compare wrt. master
   */
  public static Collection<SubjectiveScoreDifference> compareScoreDocuments(final Document challengeDocument,
                                                                            final Document master,
                                                                            final Document compare) {
    final Element masterScoresElement = master.getDocumentElement();
    final Element compareScoresElement = compare.getDocumentElement();

    final Collection<SubjectiveScoreDifference> diffs = new LinkedList<SubjectiveScoreDifference>();
    for (final Element masterScoreCategory : new NodelistElementCollectionAdapter(masterScoresElement.getChildNodes())) {
      compareScoreCategory(challengeDocument, masterScoreCategory, compareScoresElement, diffs);
    }
    return diffs;
  }

  private static void compareScoreCategory(final Document challengeDocument,
                                           final Element masterScoreCategory,
                                           final Element compareScoresElement,
                                           final Collection<SubjectiveScoreDifference> diffs) {
    final String categoryName = masterScoreCategory.getNodeName();
    final Element compareScoreCategory = getCategoryNode(compareScoresElement, categoryName);
    if (null == compareScoreCategory) {
      throw new RuntimeException("Compare score document doesn't have scores for category: "
          + categoryName);
    }

    final Element categoryDescription = fll.xml.XMLUtils.getSubjectiveCategoryByName(challengeDocument, categoryName);
    if (null == categoryDescription) {
      throw new RuntimeException(
                                 "Cannot find subjective category description for category in score document category: "
                                     + categoryName);
    }

    final List<Element> goalDescriptions = new NodelistElementCollectionAdapter(
                                                                                categoryDescription.getElementsByTagName("goal")).asList();

    // for each score element
    final List<Element> masterScores = new NodelistElementCollectionAdapter(
                                                                            masterScoreCategory.getElementsByTagName("score")).asList();
    final List<Element> compareScores = new NodelistElementCollectionAdapter(
                                                                             compareScoreCategory.getElementsByTagName("score")).asList();
    if (masterScores.size() != compareScores.size()) {
      throw new RuntimeException("Score documents have different number of score elements");
    }
    for (final Element masterScore : masterScores) {
      final Element compareScore = findCorrespondingScoreElement(masterScore, compareScores);
      diffScores(goalDescriptions, diffs, categoryDescription, masterScore, compareScore);
    }
  }

  private static Element findCorrespondingScoreElement(final Element masterScore,
                                                       final List<Element> compareScores) {
    try {
      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(masterScore.getAttribute("teamNumber")).intValue();
      final String judge = masterScore.getAttribute("judge");
      for (final Element compareScore : compareScores) {
        final int compareTeamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(compareScore.getAttribute("teamNumber"))
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

  private static void diffScores(final List<Element> goalDescriptions,
                                 final Collection<SubjectiveScoreDifference> diffs,
                                 final Element categoryDescription,
                                 final Element masterScore,
                                 final Element compareScore) {
    final String categoryTitle = categoryDescription.getAttribute("title");

    try {
      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(masterScore.getAttribute("teamNumber")).intValue();
      final String judge = masterScore.getAttribute("judge");

      final Boolean masterNoShow = XMLUtils.getBooleanAttributeValue(masterScore, "NoShow");
      final Boolean compareNoShow = XMLUtils.getBooleanAttributeValue(masterScore, "NoShow");
      if (!ComparisonUtils.safeEquals(masterNoShow, compareNoShow)) {
        diffs.add(new BooleanSubjectiveScoreDifference(categoryTitle, "NoShow", teamNumber, judge, masterNoShow,
                                                       compareNoShow));
      }

      for (final Element goalDescription : goalDescriptions) {
        final String goalTitle = goalDescription.getAttribute("title");
        final String goalName = goalDescription.getAttribute("name");
        if (fll.xml.XMLUtils.isEnumeratedGoal(goalDescription)) {
          final String masterValueStr = XMLUtils.getStringAttributeValue(masterScore, goalName);
          final String compareValueStr = XMLUtils.getStringAttributeValue(compareScore, goalName);
          if (!ComparisonUtils.safeEquals(masterValueStr, compareValueStr)) {
            diffs.add(new StringSubjectiveScoreDifference(categoryTitle, goalTitle, teamNumber, judge, masterValueStr,
                                                          compareValueStr));
          }
        } else {
          final Double masterValue = XMLUtils.getDoubleAttributeValue(masterScore, goalName);
          final Double compareValue = XMLUtils.getDoubleAttributeValue(compareScore, goalName);
          if (!ComparisonUtils.safeEquals(masterValue, compareValue)) {
            diffs.add(new DoubleSubjectiveScoreDifference(categoryTitle, goalTitle, teamNumber, judge, masterValue,
                                                          compareValue));
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
   * @return null if not found
   */
  private static Element getCategoryNode(final Element scoresElement,
                                         final String categoryName) {
    for (final Element scoreCategory : new NodelistElementCollectionAdapter(scoresElement.getChildNodes())) {
      if (categoryName.equals(scoreCategory.getNodeName())) {
        return scoreCategory;
      }
    }
    return null;
  }
}
