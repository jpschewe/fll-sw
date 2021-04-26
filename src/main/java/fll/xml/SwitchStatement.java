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
import java.util.Objects;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * A switch statement in the challenge description.
 */
public class SwitchStatement implements Evaluatable, CaseStatementResult, Serializable {

  /**
   * XML tag for the switch statement.
   */
  public static final String TAG_NAME = "switch";

  /**
   * XML tag for the default case of the switch statement.
   */
  public static final String DEFAULT_TAG_NAME = "default";

  /**
   * Default constructor creates an object with no {@link #getCases()} and a null
   * {@link #getDefaultCase()}.
   */
  public SwitchStatement() {
    mCases = new LinkedList<>();
    mDefaultCase = new ComplexPolynomial();
  }

  /**
   * Construct a switch statement from an XML document.
   *
   * @param ele the element to parse
   * @param goalScope where to lookup goals
   * @param variableScope where to lookup variables
   */
  public SwitchStatement(final Element ele,
                         final @UnknownInitialization GoalScope goalScope,
                         final @UnknownInitialization VariableScope variableScope) {
    ComplexPolynomial defaultCase = null;
    mCases = new LinkedList<>();
    for (final Element caseEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if (CaseStatement.TAG_NAME.equals(caseEle.getNodeName())) {
        final CaseStatement cs = new CaseStatement(caseEle, goalScope, variableScope);
        mCases.add(cs);
      } else if (DEFAULT_TAG_NAME.equals(caseEle.getNodeName())) {
        defaultCase = new ComplexPolynomial(caseEle, goalScope, variableScope);
      } else {
        throw new FLLInternalException("Expecting '"
            + CaseStatement.TAG_NAME
            + "' or '"
            + DEFAULT_TAG_NAME
            + "', but found '"
            + caseEle.getNodeName()
            + "'");
      }
    }

    if (null == defaultCase) {
      throw new FLLInternalException("All switch statements must have a default case");
    }
    mDefaultCase = defaultCase;
  }

  private final List<CaseStatement> mCases;

  /**
   * @return the cases in the switch statement
   */
  public List<CaseStatement> getCases() {
    return Collections.unmodifiableList(mCases);
  }

  /**
   * Add a case statement to the end of the case statements.
   *
   * @param v the case statement to add
   */
  public void addCase(final CaseStatement v) {
    mCases.add(v);
  }

  /**
   * Add a case statement at the specified position.
   *
   * @param index the index to add the case statement at
   * @param v the case statement to add
   * @throws IndexOutOfBoundsException see {@link List#add(int, Object)}
   */
  public void addCase(final int index,
                      final CaseStatement v)
      throws IndexOutOfBoundsException {
    mCases.add(index, v);
  }

  /**
   * Remove a case statement.
   *
   * @param v the case statement to remove
   * @return true if the case statement was removed
   */
  public boolean removeCase(final CaseStatement v) {
    return mCases.remove(v);
  }

  /**
   * Remove a case statement at a specified position.
   *
   * @param index the position to remove from
   * @return the case statement that was removed
   * @throws IndexOutOfBoundsException see {@link List#remove(int)}
   */
  public CaseStatement removeCase(final int index) throws IndexOutOfBoundsException {
    return mCases.remove(index);
  }

  private @NotOnlyInitialized ComplexPolynomial mDefaultCase;

  /**
   * The default case for this switch statement.
   * This may be null until {@link #toXml(Document)} or
   * {@link #evaluate(TeamScore)} are called.
   *
   * @return the polynomial, may be null
   */
  public ComplexPolynomial getDefaultCase() {
    return mDefaultCase;
  }

  /**
   * @param v the new default case
   * @see #getDefaultCase()
   */
  public void setDefaultCase(final ComplexPolynomial v) {
    mDefaultCase = v;
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    for (final CaseStatement cs : getCases()) {
      if (cs.getCondition().isTrue(teamScore)) {
        return cs.evaluate(teamScore);
      }
    }

    Objects.requireNonNull(mDefaultCase, "Switch statement must have a default case to be evaluated");
    return getDefaultCase().evaluate(teamScore);
  }

  /**
   * @param doc the document the XML element is to be added to, used to create
   *          elements
   * @return the XML element for the current state
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    for (final CaseStatement cs : mCases) {
      final Element caseEle = cs.toXml(doc);
      ele.appendChild(caseEle);
    }

    Objects.requireNonNull(mDefaultCase, "Switch statement must have a default case to be saved");
    final Element defaultEle = doc.createElement(DEFAULT_TAG_NAME);
    mDefaultCase.populateXml(doc, defaultEle);
    ele.appendChild(defaultEle);

    return ele;
  }

  /**
   * Computed based on the cases.
   */
  @Override
  public ScoreType getScoreType() {
    final boolean hasFloatingPointCases = getCases().stream().anyMatch(g -> g.getScoreType() == ScoreType.FLOAT);
    if (!hasFloatingPointCases) {
      if (FloatingPointType.DECIMAL.equals(getDefaultCase().getFloatingPoint())) {
        return ScoreType.FLOAT;
      } else {
        return ScoreType.INTEGER;
      }
    } else {
      return ScoreType.FLOAT;
    }
  }

}
