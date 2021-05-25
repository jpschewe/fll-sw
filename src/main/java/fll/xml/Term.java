/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Term in a polynomial.
 */
public class Term extends Object implements Evaluatable, Serializable {

  /**
   * XML tag for the term element.
   */
  public static final String TAG_NAME = "term";

  /**
   * XML tag for the constant element in a term.
   */
  public static final String CONSTANT_TAG_NAME = "constant";

  /**
   * XML attribute for the constant.
   */
  public static final String CONSTANT_VALUE_ATTRIBUTE = "value";

  /**
   * Create a new term.
   *
   * @param ele the XML element that represents the term
   * @param goalScope the scope to lookup goals in
   * @param variableScope the scope to lookup variables in, may be null
   * @throws VariableRefNotAllowedException if there is no variable scope and a
   *           variable reference is found
   */
  public Term(final Element ele,
              final @UnknownInitialization GoalScope goalScope,
              final @UnknownInitialization @Nullable VariableScope variableScope)
      throws VariableRefNotAllowedException {
    super();

    double coefficient = 1;
    for (final Element constantEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(CONSTANT_TAG_NAME))) {
      final double value = Double.parseDouble(constantEle.getAttribute(CONSTANT_VALUE_ATTRIBUTE));
      coefficient = coefficient
          * value;
    }

    mCoefficient = coefficient;

    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(GoalRef.TAG_NAME))) {
      final GoalRef goal = new GoalRef(goalEle, goalScope);
      mGoals.add(goal);
    }

    for (final Element variableEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(VariableRef.TAG_NAME))) {
      if (null == variableScope) {
        throw new VariableRefNotAllowedException("Variable scope is null, therefore this term cannot reference variables");
      }

      final VariableRef variable = new VariableRef(variableEle, variableScope);
      mVariables.add(variable);
    }

  }

  /**
   * Creates a {@link Term} without any variables or goals.
   */
  public Term() {
    super();

    mCoefficient = 1;
  }

  private final Collection<GoalRef> mGoals = new LinkedList<>();

  /**
   * @return unmodifiable collection
   */
  public Collection<GoalRef> getGoals() {
    return Collections.unmodifiableCollection(mGoals);
  }

  /**
   * Add a goal reference.
   *
   * @param v the goal reference to add
   */
  public void addGoal(final GoalRef v) {
    mGoals.add(v);
  }

  /**
   * Remove a goal reference.
   *
   * @param v the goal to remove (only the first goal is removed if there are
   *          multiple instances of the goal)
   * @return if the goal reference was found and removed
   * @see List#remove(Object)
   */
  public boolean removeGoal(final GoalRef v) {
    return mGoals.remove(v);
  }

  private final List<VariableRef> mVariables = new LinkedList<>();

  /**
   * @return unmodifiable collection
   */
  public Collection<VariableRef> getVariables() {
    return Collections.unmodifiableCollection(mVariables);
  }

  /**
   * Add a variable reference.
   *
   * @param var the variable reference to add
   */
  public void addVariable(final VariableRef var) {
    mVariables.add(var);
  }

  /**
   * Remove a variable reference.
   *
   * @param var the variable reference to remove (only the first one is removed if
   *          there are multiple entries)
   * @return if the variable reference was found and removed
   * @see List#remove(Object)
   */
  public boolean removeVariable(final VariableRef var) {
    return mVariables.remove(var);
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    double value = getCoefficient();

    for (final GoalRef goal : mGoals) {
      value = value
          * goal.evaluate(teamScore);
    }

    for (final VariableRef variable : mVariables) {
      value = value
          * variable.evaluate(teamScore);
    }

    return value;
  }

  private double mCoefficient;

  /**
   * @return the coefficient of the term
   */
  public double getCoefficient() {
    return mCoefficient;
  }

  /**
   * @param v see {@link #getCoefficient()}
   */
  public void setCoefficient(final double v) {
    mCoefficient = v;
  }

  private Element createConstantElement(final Document doc) {
    final Element ele = doc.createElement(CONSTANT_TAG_NAME);
    ele.setAttribute(CONSTANT_VALUE_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mCoefficient));
    return ele;
  }

  /**
   * @param doc the XML document used to create elements
   * @return the XML element representing the current state
   */
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    final Element constantEle = createConstantElement(doc);
    ele.appendChild(constantEle);

    for (final GoalRef goal : mGoals) {
      final Element goalEle = goal.toXml(doc);
      ele.appendChild(goalEle);
    }

    for (final VariableRef variable : mVariables) {
      final Element variableEle = variable.toXml(doc);
      ele.appendChild(variableEle);
    }

    return ele;
  }

}
