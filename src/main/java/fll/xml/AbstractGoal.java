/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.util.FP;
import fll.web.playoff.TeamScore;

/**
 * Base type for goals in the challenge description.
 */
public abstract class AbstractGoal extends GoalElement {

  /**
   * XML attribute for goal name.
   */
  public static final String NAME_ATTRIBUTE = "name";

  /**
   * Default constructor for creating a new goal.
   */
  public AbstractGoal() {
    super();
  }

  /**
   * Constructor for reading from an XML document.
   *
   * @param ele the XML element to parse
   */
  public AbstractGoal(final Element ele) {
    super(ele);

    mName = ele.getAttribute(NAME_ATTRIBUTE);
  }

  /**
   * @param name see {@link #getName()}
   */
  public AbstractGoal(final String name) {
    this();

    mName = name;
  }

  private String mName = "no_name";

  /**
   * @return the name of the goal
   */
  public String getName(@UnknownInitialization(AbstractGoal.class) AbstractGoal this) {
    return mName;
  }

  /**
   * @param v see {@link #getName()}
   *          Fires property change event.
   */
  public void setName(final String v) {
    final String old = mName;
    mName = v;
    firePropertyChange("name", old, v);
  }

  /**
   * Get the raw score.
   *
   * @param teamScore the score to evaluate
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getRawScore(TeamScore teamScore);

  /**
   * Get the computed score. Raw values are available through
   * {@link #getRawScore(TeamScore)}.
   *
   * @param teamScore the score to evaluate
   * @return the score or NaN if there is currently no score for this goal
   */
  @Override
  public abstract double evaluate(TeamScore teamScore);

  /**
   * @return true if this is a computed goal and can be safely cast to
   *         {@link ComputedGoal}
   */
  public abstract boolean isComputed();

  /***
   * @return true if this is an enumerated goal
   */
  public abstract boolean isEnumerated();

  /**
   * @return Read-only collection of the values.
   */
  public abstract Collection<EnumeratedValue> getValues();

  /**
   * Get the enumerated values from the goal and sort them for display.
   * This ensures that all usages have the elements in the same order.
   *
   * @return the enumerated values for this goal
   */
  public List<EnumeratedValue> getSortedValues() {
    final List<EnumeratedValue> values = new LinkedList<>(getValues());
    Collections.sort(values, EnumeratedValueLowestFirst.INSTANCE);
    return values;
  }

  /**
   * @return the score type
   */
  public abstract ScoreType getScoreType();

  /**
   * @return minimum raw value for the goal
   */
  public abstract double getMin();

  /**
   * @return maximum raw value for the goal
   */
  public abstract double getMax();

  /**
   * @return if this goal is a yes/no.
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

  /**
   * @param score the raw score
   * @return the value after the score type has been applied
   */
  protected final double applyScoreType(final double score) {
    switch (getScoreType()) {
    case FLOAT:
      return score;
    case INTEGER:
      return ((long) score);
    default:
      throw new FLLInternalException("Unknown score type: "
          + getScoreType());
    }
  }

  /**
   * @param document the document used to create elements
   * @param ele the goal element to be populated with this object's state
   */
  protected void populateXml(final Document document,
                             final Element ele) {
    super.populateXml(document, ele);
    ele.setAttribute(NAME_ATTRIBUTE, getName());
  }

  private static final class EnumeratedValueLowestFirst implements Comparator<EnumeratedValue>, Serializable {
    public static final EnumeratedValueLowestFirst INSTANCE = new EnumeratedValueLowestFirst();

    private EnumeratedValueLowestFirst() {
    }

    @Override
    public int compare(final EnumeratedValue one,
                       final EnumeratedValue two) {
      final int scoreCompare = Double.compare(one.getScore(), two.getScore());
      if (0 == scoreCompare) {
        return one.getValue().compareTo(two.getValue());
      } else {
        return scoreCompare;
      }
    }
  }

  /**
   * Subclasses need to override this method to create the appropriate element and
   * then call {@link #populateXml(Document, Element)} to get data from this
   * class.
   *
   * @param doc the document to create elements with
   * @return XML element that represents the current state
   */
  public abstract Element toXml(Document doc);

  @Override
  public boolean isGoal() {
    return true;
  }

  @Override
  public boolean isGoalGroup() {
    return false;
  }

}
