/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

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

    final List<RubricRange> rubric = new LinkedList<RubricRange>();
    final NodelistElementCollectionAdapter rubricEles = new NodelistElementCollectionAdapter(
                                                                                             ele.getElementsByTagName("rubric"));
    if (rubricEles.hasNext()) {
      final Element rubricEle = rubricEles.next();
      for (final Element rangeEle : new NodelistElementCollectionAdapter(rubricEle.getElementsByTagName("range"))) {
        final RubricRange range = new RubricRange(rangeEle);
        rubric.add(range);
      }
    }
    
    // sort so that the lowest range is first
    Collections.sort(rubric, LEAST_RUBRIC_RANGE);

    mRubric = Collections.unmodifiableList(rubric);

    final List<EnumeratedValue> values = new LinkedList<EnumeratedValue>();
    for (final Element valueEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("value"))) {
      final EnumeratedValue value = new EnumeratedValue(valueEle);
      values.add(value);
    }
    mValues = Collections.unmodifiableList(values);

  }


  private static final Comparator<RubricRange> LEAST_RUBRIC_RANGE = new Comparator<RubricRange>() {
    public int compare(final RubricRange one,
                       final RubricRange two) {
      return Integer.compare(one.getMin(), two.getMin());
    }
  };
  
  private final List<RubricRange> mRubric;

  /**
   * 
   * @return unmodifiable list, sorted with lowest range first
   */
  public List<RubricRange> getRubric() {
    return mRubric;
  }

  private final List<EnumeratedValue> mValues;

  /**
   * 
   * @return unmodifiable list
   */
  public List<EnumeratedValue> getValues() {
    return mValues;
  }

  private final double mMin;

  public double getMin() {
    return mMin;
  }

  private final double mMax;

  public double getMax() {
    return mMax;
  }

  private final double mMultiplier;

  public double getMultiplier() {
    return mMultiplier;
  }

  private final double mInitialValue;

  public double getInitialValue() {
    return mInitialValue;
  }

  private final ScoreType mScoreType;

  public ScoreType getScoreType() {
    return mScoreType;
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
