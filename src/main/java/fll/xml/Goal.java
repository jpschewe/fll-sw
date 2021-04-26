/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Represents a goal in the challenge description.
 */
public class Goal extends AbstractGoal {

  /**
   * XML element tag used by this class.
   */
  public static final String TAG_NAME = "goal";

  /**
   * XML attribute used for the minimum value.
   * 
   * @see #getMin()
   */
  public static final String MIN_ATTRIBUTE = "min";

  /**
   * XML attribute used for the maximum value.
   * 
   * @see #getMax()
   */
  public static final String MAX_ATTRIBUTE = "max";

  /**
   * XML attribute used for the multiplier.
   * 
   * @see #getMultiplier()
   */
  public static final String MULTIPLIER_ATTRIBUTE = "multiplier";

  /**
   * XML attribute used for the initial value.
   * 
   * @see #getInitialValue()
   */
  public static final String INITIAL_VALUE_ATTRIBUTE = "initialValue";

  /**
   * XML attribute used for the required value.
   * 
   * @see #isRequired()
   */
  public static final String REQUIRED_ATTRIBUTE = "required";

  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Create the goal from an XML element.
   *
   * @param ele the element to parse
   */
  public Goal(final Element ele) {
    super(ele);

    mMin = Double.valueOf(ele.getAttribute(MIN_ATTRIBUTE));
    mMax = Double.valueOf(ele.getAttribute(MAX_ATTRIBUTE));
    mMultiplier = Double.valueOf(ele.getAttribute(MULTIPLIER_ATTRIBUTE));
    mInitialValue = Double.valueOf(ele.getAttribute(INITIAL_VALUE_ATTRIBUTE));

    mScoreType = ChallengeParser.getScoreType(ele);

    if (ele.hasAttribute(REQUIRED_ATTRIBUTE)) {
      mRequired = Boolean.valueOf(ele.getAttribute(REQUIRED_ATTRIBUTE));
    } else {
      mRequired = false;
    }

    int minSeen = Integer.MAX_VALUE;
    mRubric = new LinkedList<>();
    final NodelistElementCollectionAdapter rubricEles = new NodelistElementCollectionAdapter(ele.getElementsByTagName(RubricRange.RUBRIC_TAG_NAME));
    if (rubricEles.hasNext()) {
      final Element rubricEle = rubricEles.next();
      for (final Element rangeEle : new NodelistElementCollectionAdapter(rubricEle.getElementsByTagName(RubricRange.TAG_NAME))) {
        final RubricRange range = new RubricRange(rangeEle);
        mRubric.add(range);

        minSeen = Math.min(minSeen, range.getMin());
      }
    }

    // handle old challenge descriptions that don't have "ND" for 0
    if (!mRubric.isEmpty()
        && ScoreType.INTEGER.equals(mScoreType)
        && minSeen > mMin) {
      LOGGER.debug("Found goal without 'ND' rubric range for minimum value: {}", getName());
      final RubricRange range = new RubricRange("");
      range.setShortDescription("ND");
      range.setMin(0);
      range.setMax(0);
      mRubric.add(range);
    }

    mValues = new LinkedList<>();
    for (final Element valueEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(EnumeratedValue.TAG_NAME))) {
      final EnumeratedValue value = new EnumeratedValue(valueEle);
      mValues.add(value);
    }

  }

  /**
   * Create a goal with default values for min (0), max (1), multiplier (1),
   * required (false), empty rubric, empty enumerated values and
   * initial value (0) and score type {@link ScoreType#INTEGER}.
   *
   * @param name see {@link #getName()}
   */
  public Goal(final String name) {
    super(name);
    mMin = 0;
    mMax = 1;
    mMultiplier = 1;
    mInitialValue = 0;
    mRequired = false;
    mRubric = new LinkedList<>();
    mValues = new LinkedList<>();
    mScoreType = ScoreType.INTEGER;
  }

  private static final Comparator<RubricRange> LEAST_RUBRIC_RANGE = (one,
                                                                     two) -> Integer.compare(one.getMin(),
                                                                                             two.getMin());

  private final List<RubricRange> mRubric;

  /**
   * @return unmodifiable list, sorted with lowest range first
   */
  public List<RubricRange> getRubric() {
    // sort so that the lowest range is first
    Collections.sort(mRubric, LEAST_RUBRIC_RANGE);

    return Collections.unmodifiableList(mRubric);
  }

  /**
   * Replace the rubric.
   *
   * @param v see {@link #getRubric()}
   */
  public void setRubric(final List<RubricRange> v) {
    mRubric.clear();
    mRubric.addAll(v);
  }

  /**
   * Remove a rubric range.
   *
   * @param v the rubric range to remove
   * @return if the rubric range was removed
   */
  public boolean removeRubricRange(final RubricRange v) {
    return mRubric.remove(v);
  }

  /**
   * Add a rubric range.
   *
   * @param v the rubric range to add
   */
  public void addRubricRange(final RubricRange v) {
    mRubric.add(v);

    // sort so that the lowest range is first
    Collections.sort(mRubric, LEAST_RUBRIC_RANGE);
  }

  private final Collection<EnumeratedValue> mValues;

  /**
   * @return unmodifiable collection
   */
  @Override
  public Collection<EnumeratedValue> getValues() {
    return Collections.unmodifiableCollection(mValues);
  }

  /**
   * Add an enumerated value.
   *
   * @param v the value to add
   */
  public void addValue(final EnumeratedValue v) {
    mValues.add(v);
  }

  /**
   * Remove an enumerated value, if all enumerated values are removed the goal
   * is no longer an enumerated goal.
   *
   * @param v the value to remove
   * @return if the value was removed
   */
  public boolean removeValue(final EnumeratedValue v) {
    return mValues.remove(v);
  }

  /**
   * Clear all of the values, makes this goal no longer enumerated.
   */
  public void removeAllValues() {
    mValues.clear();
  }

  private double mMin;

  @Override
  public double getMin() {
    return mMin;
  }

  /**
   * @param v see {@link #getMin()}
   */
  public void setMin(final double v) {
    mMin = v;
  }

  private double mMax;

  @Override
  public double getMax() {
    return mMax;
  }

  /**
   * @param v see {@link #getMax()}
   */
  public void setMax(final double v) {
    mMax = v;
  }

  private double mMultiplier;

  /**
   * @return the multiplier of the raw score for the goal
   */
  public double getMultiplier() {
    return mMultiplier;
  }

  /**
   * @param v see {@link #getMultiplier()}
   */
  public void setMultiplier(final double v) {
    mMultiplier = v;
  }

  private double mInitialValue;

  /**
   * @return the initial raw value for the goal
   */
  public double getInitialValue() {
    return mInitialValue;
  }

  /**
   * @param v see {@link #getInitialValue()}
   */
  public void setInitialValue(final double v) {
    mInitialValue = v;
  }

  private ScoreType mScoreType;

  @Override
  public ScoreType getScoreType() {
    return mScoreType;
  }

  /**
   * @param v see {@link #getScoreType()}
   */
  public void setScoreType(final ScoreType v) {
    mScoreType = v;
  }

  @Override
  public boolean isEnumerated() {
    return !getValues().isEmpty();
  }

  @Override
  public double getRawScore(final TeamScore teamScore) {
    final double score = internalGetRawScore(teamScore);
    return applyScoreType(score);
  }

  private double internalGetRawScore(final TeamScore teamScore) {
    if (isEnumerated()) {
      final String val = teamScore.getEnumRawScore(getName());
      for (final EnumeratedValue ev : getValues()) {
        if (ev.getValue().equals(val)) {
          return ev.getScore();
        }
      }
      return Double.NaN;
    } else {
      final double score = teamScore.getRawScore(getName());
      return score;
    }
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    final double rawScore = getRawScore(teamScore);
    return rawScore
        * getMultiplier();
  }

  @Override
  public boolean isComputed() {
    return false;
  }

  private boolean mRequired;

  /**
   * @return True if the goal is required for award consideration.
   */
  public boolean isRequired() {
    return mRequired;
  }

  /**
   * @param v see {@link #isRequired()}
   */
  public void setRequired(final boolean v) {
    mRequired = v;
  }

  @Override
  public Element toXml(final Document doc) {
    final Element ele = doc.createElementNS(null, TAG_NAME);

    populateXml(doc, ele);

    if (!isEnumerated()) {
      ele.setAttribute(MIN_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mMin));
      ele.setAttribute(MAX_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mMax));
      ele.setAttribute(MULTIPLIER_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mMultiplier));
    }
    ele.setAttribute(INITIAL_VALUE_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mInitialValue));

    ele.setAttribute(ChallengeParser.SCORE_TYPE_ATTRIBUTE, mScoreType.toXmlString());
    ele.setAttribute(REQUIRED_ATTRIBUTE, Boolean.toString(mRequired));

    final List<RubricRange> rubric = getRubric();
    if (!rubric.isEmpty()) {
      final Element rubricEle = doc.createElement(RubricRange.RUBRIC_TAG_NAME);
      for (final RubricRange range : rubric) {
        final Element rangeEle = range.toXml(doc);
        rubricEle.appendChild(rangeEle);
      }
      ele.appendChild(rubricEle);
    }

    final Collection<EnumeratedValue> values = getValues();
    if (!values.isEmpty()) {
      for (final EnumeratedValue value : values) {
        final Element valueEle = value.toXml(doc);
        ele.appendChild(valueEle);
      }
    }

    return ele;
  }

}
