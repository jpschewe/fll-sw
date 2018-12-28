/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * Polynomial that references goals.
 */
public class BasicPolynomial implements Evaluatable, Serializable {

  public BasicPolynomial(final Element ele,
                         final GoalScope goalScope) {
    this(ele, goalScope, null);
  }

  /**
   * Only to be called from {@link ComplexPolynomial}.
   */
  protected BasicPolynomial(final Element ele,
                            final GoalScope goalScope,
                            final VariableScope variableScope) {

    final List<Term> terms = new LinkedList<Term>();
    for (final Element termEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("term"))) {
      final Term term = new Term(termEle, goalScope, variableScope);
      terms.add(term);
    }
    mTerms = Collections.unmodifiableList(terms);
    mFloatingPoint = FloatingPointType.fromString(ele.getAttribute("floatingPoint"));
  }

  private final FloatingPointType mFloatingPoint;

  public FloatingPointType getFloatingPoint() {
    return mFloatingPoint;
  }

  protected final double applyFloatingPointType(final double value) {
    switch (getFloatingPoint()) {
    case DECIMAL:
      return value;
    case ROUND:
      return Math.round(value);
    case TRUNCATE:
      return (double) ((long) value);
    default:
      throw new FLLInternalException("Unknown floating point type: "
          + getFloatingPoint());
    }
  }

  private final List<Term> mTerms;

  public List<Term> getTerms() {
    return mTerms;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    if (!teamScore.scoreExists()) {
      return Double.NaN;
    }

    double score = 0;
    for (final Term t : getTerms()) {
      final double val = t.evaluate(teamScore);
      score += val;
    }
    return applyFloatingPointType(score);
  }

}
