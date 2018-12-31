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

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

public class Goal extends AbstractGoal {

  public static final String TAG_NAME = "goal";

  public static final String MIN_ATTRIBUTE = "min";

  public static final String MAX_ATTRIBUTE = "max";

  public static final String MULTIPLIER_ATTRIBUTE = "multiplier";

  public static final String INITIAL_VALUE_ATTRIBUTE = "initialValue";

  public static final String REQUIRED_ATTRIBUTE = "required";

  public Goal(final Element ele) {
    super(ele);

    mMin = Double.valueOf(ele.getAttribute(MIN_ATTRIBUTE));
    mMax = Double.valueOf(ele.getAttribute(MAX_ATTRIBUTE));
    mMultiplier = Double.valueOf(ele.getAttribute(MULTIPLIER_ATTRIBUTE));
    mInitialValue = Double.valueOf(ele.getAttribute(INITIAL_VALUE_ATTRIBUTE));

    mScoreType = XMLUtils.getScoreType(ele);

    if (ele.hasAttribute(REQUIRED_ATTRIBUTE)) {
      mRequired = Boolean.valueOf(ele.getAttribute(REQUIRED_ATTRIBUTE));
    } else {
      mRequired = false;
    }

    mRubric = new LinkedList<RubricRange>();
    final NodelistElementCollectionAdapter rubricEles = new NodelistElementCollectionAdapter(ele.getElementsByTagName(RubricRange.RUBRIC_TAG_NAME));
    if (rubricEles.hasNext()) {
      final Element rubricEle = rubricEles.next();
      for (final Element rangeEle : new NodelistElementCollectionAdapter(rubricEle.getElementsByTagName(RubricRange.TAG_NAME))) {
        final RubricRange range = new RubricRange(rangeEle);
        mRubric.add(range);
      }
    }

    mValues = new LinkedList<EnumeratedValue>();
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
  public Goal(@Nonnull final String name) {
    super(name);
    mMin = 0;
    mMax = 1;
    mMultiplier = 1;
    mInitialValue = 0;
    mRequired = false;
    mRubric = new LinkedList<RubricRange>();
    mValues = new LinkedList<EnumeratedValue>();
    mScoreType = ScoreType.INTEGER;
  }

  private static final Comparator<RubricRange> LEAST_RUBRIC_RANGE = new Comparator<RubricRange>() {
    public int compare(final RubricRange one,
                       final RubricRange two) {
      return Integer.compare(one.getMin(), two.getMin());
    }
  };

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
   * @param v the new value
   */
  public void setRubric(final List<RubricRange> v) {
    mRubric.clear();
    mRubric.addAll(v);
  }

  /**
   * Remove a rubric range
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

  public double getMin() {
    return mMin;
  }

  public void setMin(final double v) {
    mMin = v;
  }

  private double mMax;

  public double getMax() {
    return mMax;
  }

  public void setMax(final double v) {
    mMax = v;
  }

  private double mMultiplier;

  public double getMultiplier() {
    return mMultiplier;
  }

  public void setMultiplier(final double v) {
    mMultiplier = v;
  }

  private double mInitialValue;

  public double getInitialValue() {
    return mInitialValue;
  }

  public void setInitialValue(final double v) {
    mInitialValue = v;
  }

  private ScoreType mScoreType;

  @Nonnull
  public ScoreType getScoreType() {
    return mScoreType;
  }

  public void setScoreType(@Nonnull final ScoreType v) {
    mScoreType = v;
  }

  public boolean isEnumerated() {
    return !getValues().isEmpty();
  }

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

  public double getComputedScore(final TeamScore teamScore) {
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
   * True if the goal is required for award consideration.
   */
  public boolean isRequired() {
    return mRequired;
  }

  public void setRequired(final boolean v) {
    mRequired = v;
  }

  @Override
  public Element toXml(final Document doc) {
    final Element ele = doc.createElementNS(null, TAG_NAME);

    populateXml(doc, ele);

    if (!isEnumerated()) {
      ele.setAttribute(MIN_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mMin));
      ele.setAttribute(MAX_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mMax));
      ele.setAttribute(MULTIPLIER_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mMultiplier));
    }
    ele.setAttribute(INITIAL_VALUE_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mInitialValue));

    ele.setAttribute(XMLUtils.SCORE_TYPE_ATTRIBUTE, mScoreType.toXmlString());
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
