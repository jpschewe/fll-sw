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

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Polynomial that references goals.
 */
public class BasicPolynomial implements Evaluatable, CaseStatementResult, Serializable {

  /**
   * XML attribute for storing the {@link #getFloatingPoint()} data.
   */
  public static final String FLOATING_POINT_ATTRIBUTE = "floatingPoint";

  /**
   * @param ele XM element holding the polynomial
   * @param goalScope used to lookup goals referenced by the polynomial
   */
  public BasicPolynomial(final Element ele,
                         final @UnknownInitialization GoalScope goalScope) {
    // casting of null to VariableScope should not be needed
    // https://github.com/typetools/checker-framework/issues/4567
    // should be fixed in checker 3.13+
    this(ele, goalScope, (VariableScope) null);
  }

  /**
   * Only to be called from {@link ComplexPolynomial}.
   *
   * @param ele the element to parse
   * @param goalScope used to lookup goals
   * @param variableScope used to lookup variables
   */
  protected BasicPolynomial(final Element ele,
                            final @UnknownInitialization GoalScope goalScope,
                            final @UnknownInitialization @Nullable VariableScope variableScope) {
    mTerms = new LinkedList<>();
    for (final Element termEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(Term.TAG_NAME))) {
      final Term term = new Term(termEle, goalScope, variableScope);
      mTerms.add(term);
    }
    mFloatingPoint = FloatingPointType.fromString(ele.getAttribute(FLOATING_POINT_ATTRIBUTE));
  }

  /**
   * Default construction uses {@link FloatingPointType#TRUNCATE} as the floating
   * point type. The terms list is empty.
   */
  protected BasicPolynomial() {
    mTerms = new LinkedList<>();
    mFloatingPoint = FloatingPointType.TRUNCATE;
  }

  private FloatingPointType mFloatingPoint;

  /**
   * @return the floating point type for the polynomial
   */

  public FloatingPointType getFloatingPoint() {
    return mFloatingPoint;
  }

  /**
   * @param v see {@link #getFloatingPoint()}
   */
  public void setFloatingPoint(final FloatingPointType v) {
    mFloatingPoint = v;
  }

  /**
   * @param value the value to apply the floating point type to
   * @return the value after the floating point type is applied
   */
  protected final double applyFloatingPointType(final double value) {
    switch (getFloatingPoint()) {
    case DECIMAL:
      return value;
    case ROUND:
      return Math.round(value);
    case TRUNCATE:
      return ((long) value);
    default:
      throw new FLLInternalException("Unknown floating point type: "
          + getFloatingPoint());
    }
  }

  private final List<Term> mTerms;

  /**
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
   * Add a term at the specified index in the polynomial.
   *
   * @param index the index to add the term at
   * @param v the term to add
   * @throws IndexOutOfBoundsException if the index isn't valid
   * @see List#add(int, Object)
   */
  public void addTerm(final int index,
                      final Term v)
      throws IndexOutOfBoundsException {
    mTerms.add(index, v);
  }

  /**
   * Remove a term.
   *
   * @param term the term to remove
   * @return if the term was removed
   * @see List#remove(Object)
   */
  public boolean removeTerm(final Term term) {
    return mTerms.remove(term);
  }

  /**
   * Remove the term at the specified index.
   *
   * @param index the index to remove the term at
   * @return the term that was removed
   * @throws IndexOutOfBoundsException if the index isn't valid
   * @see List#remove(int)
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

  /**
   * Store the polynomial into an XML element.
   *
   * @param doc the document used to create elements
   * @param ele the element to store the data in
   */
  public void populateXml(final Document doc,
                          final Element ele) {
    for (final Term term : mTerms) {
      final Element termEle = term.toXml(doc);
      ele.appendChild(termEle);
    }

    ele.setAttribute(FLOATING_POINT_ATTRIBUTE, mFloatingPoint.toXmlString());
  }

  /**
   * @return score type of the polynomial
   * @see #getFloatingPoint()
   */
  public ScoreType getScoreType() {
    if (FloatingPointType.DECIMAL.equals(getFloatingPoint())) {
      return ScoreType.FLOAT;
    } else {
      return ScoreType.INTEGER;
    }
  }

}
