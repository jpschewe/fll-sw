/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class Goal extends AbstractGoal {

  public Goal(final Element ele) {
    super(ele);

    mMin = Double.valueOf(ele.getAttribute("min"));
    mMax = Double.valueOf(ele.getAttribute("max"));
    mMultiplier = Double.valueOf(ele.getAttribute("multiplier"));
    mInitialValue = Double.valueOf(ele.getAttribute("initialValue"));

    mScoreType = XMLUtils.getScoreType(ele);
    mCategory = ele.getAttribute("category");

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
    mRubric = Collections.unmodifiableList(rubric);

    final List<EnumeratedValue> values = new LinkedList<EnumeratedValue>();
    for (final Element valueEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("value"))) {
      final EnumeratedValue value = new EnumeratedValue(valueEle);
      values.add(value);
    }
    mValues = Collections.unmodifiableList(values);

  }

  private final List<RubricRange> mRubric;

  public List<RubricRange> getRubric() {
    return mRubric;
  }

  private final List<EnumeratedValue> mValues;

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

  private final String mCategory;

  public String getCategory() {
    return mCategory;
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
      final Double score = teamScore.getRawScore(getName());
      if (null == score) {
        return Double.NaN;
      } else {
        return score;
      }
    }
  }

  public double getComputedScore(final TeamScore teamScore) {
    final Double rawScore = getRawScore(teamScore);
    if (null == rawScore) {
      return Double.NaN;
    } else {
      return rawScore
          * getMultiplier();
    }
  }

  @Override
  public boolean isComputed() {
    return false;
  }

}
