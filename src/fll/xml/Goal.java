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

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

public class Goal extends AbstractGoal {

  public Goal(final Element ele) {
    super(ele);

    mMin = Double.valueOf(ele.getAttribute("min"));
    mMax = Double.valueOf(ele.getAttribute("max"));
    mMultiplier = Double.valueOf(ele.getAttribute("multiplier"));
    mInitialValue = Double.valueOf(ele.getAttribute("initialValue"));

    mScoreType = XMLUtils.getScoreType(ele);

    mRubric = new LinkedList<RubricRange>();
    final NodelistElementCollectionAdapter rubricEles = new NodelistElementCollectionAdapter(ele.getElementsByTagName("rubric"));
    if (rubricEles.hasNext()) {
      final Element rubricEle = rubricEles.next();
      for (final Element rangeEle : new NodelistElementCollectionAdapter(rubricEle.getElementsByTagName("range"))) {
        final RubricRange range = new RubricRange(rangeEle);
        mRubric.add(range);
      }
    }

    // sort so that the lowest range is first
    Collections.sort(mRubric, LEAST_RUBRIC_RANGE);

    mValues = new LinkedList<EnumeratedValue>();
    for (final Element valueEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("value"))) {
      final EnumeratedValue value = new EnumeratedValue(valueEle);
      mValues.add(value);
    }

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
    return Collections.unmodifiableList(mRubric);
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
   * Add an enumerated value
   * 
   * @param v the value to add
   */
  public void addValue(final EnumeratedValue v) {
    mValues.add(v);
  }

  /**
   * Remove an enumted value, if all enumted values are removed the goal
   * is no longer an enumerated goal.
   * 
   * @param v the value to remove
   * @return if the value was removed
   */
  public boolean removeValue(final EnumeratedValue v) {
    return mValues.remove(v);
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

  public ScoreType getScoreType() {
    return mScoreType;
  }

  public void setScoreType(final ScoreType v) {
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

}
