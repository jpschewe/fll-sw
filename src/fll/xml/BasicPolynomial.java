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

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

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

    mTerms = new LinkedList<Term>();
    for (final Element termEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("term"))) {
      final Term term = new Term(termEle, goalScope, variableScope);
      mTerms.add(term);
    }
    mFloatingPoint = FloatingPointType.fromString(ele.getAttribute("floatingPoint"));
  }

  private FloatingPointType mFloatingPoint;

  public FloatingPointType getFloatingPoint() {
    return mFloatingPoint;
  }
  
  public void setFloatingPoint(final FloatingPointType v) {
    mFloatingPoint = v;
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

  /**
   * 
   * @return unmodifiable list
   */
  public List<Term> getTerms() {
    return Collections.unmodifiableList(mTerms);
  }
  
  /**
   * Add a term to the end of the polynomial.
   * 
   * @param term the term to add
   */
  public void addTerm(final Term term) {
    mTerms.add(term);
  }
  
  /**
   * Add a term at the specified index in the polynomial
   * 
   * @param index the index to add the term at
   * @param v the term to add
   * @throws IndexOutOfBoundsException
   */
  public void addTerm(final int index, final Term v) throws IndexOutOfBoundsException {
    mTerms.add(index, v);
  }
  
  /**
   * Remove a term.
   * 
   * @param term the term to remove
   * @return if the term was removed
   */
  public boolean removeTerm(final Term term) {
    return mTerms.remove(term);
  }
  
  /**
   * Remove the term at the specified index.
   * 
   * @param index the index to remove the term at
   * @return the term that was removed
   * @throws IndexOutOfBoundsException
   */
  public Term removeTerm(final int index) throws IndexOutOfBoundsException {
    return mTerms.remove(index);
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
