/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.util.FP;
import fll.web.playoff.TeamScore;

public abstract class AbstractGoal implements Serializable {

  public AbstractGoal(final Element ele) {
    mName = ele.getAttribute("name");
    mTitle = ele.getAttribute("title");
    if (ele.hasAttribute("required")) {
      mRequired = Boolean.valueOf(ele.getAttribute("required"));
    } else {
      mRequired = false;
    }

    final List<Element> descEles = XMLUtils.getChildElementsByTagName(ele, "description");
    if (descEles.size() > 0) {
      final Element descEle = descEles.get(0);
      mDescription = descEle.getTextContent();
    } else {
      mDescription = null;
    }

    mCategory = ele.getAttribute("category");
  }

  private final String mCategory;

  public String getCategory() {
    return mCategory;
  }

  private final boolean mRequired;

  /**
   * True if the goal is required for award consideration.
   */
  public boolean isRequired() {
    return mRequired;
  }

  private final String mName;

  public String getName() {
    return mName;
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private final String mDescription;

  /**
   * @return the description, may be null
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * Get the raw score.
   * 
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getRawScore(final TeamScore teamScore);

  /**
   * Get the computed score.
   * 
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getComputedScore(final TeamScore teamScore);

  public abstract boolean isComputed();

  public abstract boolean isEnumerated();

  /**
   * Read-only list of the values.
   */
  public abstract List<EnumeratedValue> getValues();

  /**
   * Get the enumerated values from the goal and sort them for display.
   * This ensures that all usages have the elements in the same order.
   */
  public List<EnumeratedValue> getSortedValues() {
    final List<EnumeratedValue> values = new LinkedList<>(getValues());
    Collections.sort(values, EnumeratedValueLowestFirst.INSTANCE);
    return values;
  }

  public abstract ScoreType getScoreType();

  public abstract double getMin();

  public abstract double getMax();

  /**
   * Check if this goal is a yes/no.
   */
  public boolean isYesNo() {
    if (isComputed()
        || isEnumerated()) {
      return false;
    } else {
      return FP.equals(0, getMin(), ChallengeParser.INITIAL_VALUE_TOLERANCE)
          && FP.equals(1, getMax(), ChallengeParser.INITIAL_VALUE_TOLERANCE);
    }
  }

  protected final double applyScoreType(final double score) {
    switch (getScoreType()) {
    case FLOAT:
      return score;
    case INTEGER:
      return (double) ((long) score);
    default:
      throw new FLLInternalException("Unknown score type: "
          + getScoreType());
    }
  }

  private static final class EnumeratedValueHighestFirst implements Comparator<EnumeratedValue>, Serializable {
    public static final EnumeratedValueHighestFirst INSTANCE = new EnumeratedValueHighestFirst();

    private EnumeratedValueHighestFirst() {
    }

    public int compare(final EnumeratedValue one,
                       final EnumeratedValue two) {
      return -1
          * Double.compare(one.getScore(), two.getScore());
    }
  }

  private static final class EnumeratedValueLowestFirst implements Comparator<EnumeratedValue>, Serializable {
    public static final EnumeratedValueLowestFirst INSTANCE = new EnumeratedValueLowestFirst();

    private EnumeratedValueLowestFirst() {
    }

    public int compare(final EnumeratedValue one,
                       final EnumeratedValue two) {
      return Double.compare(one.getScore(), two.getScore());
    }
  }

}
