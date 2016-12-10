/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

/**
 * Term in a polynomial.
 */
public class Term extends AbstractTerm {

  public static final String TAG_NAME = "term";

  /**
   * Create a new term.
   * 
   * @param ele the XML element that represents the term
   * @param goalScope the scope to lookup goals in
   * @param variableScope the scope to lookup variables in, may be null
   * @throws VariablRefNotAllowedException
   */
  public Term(final Element ele,
              final GoalScope goalScope,
              final VariableScope variableScope)
      throws VariableRefNotAllowedException {
    super(ele);

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

  private final Collection<GoalRef> mGoals = new LinkedList<GoalRef>();

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
   * @param v the goal to remove
   * @return if the goal reference was found and removed
   */
  public boolean removeGoal(final GoalRef v) {
    return mGoals.remove(v);
  }

  private final List<VariableRef> mVariables = new LinkedList<VariableRef>();

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
   * Remove a variable reference
   * 
   * @param var the variable reference to remove
   * @return if the variable reference was found and removed
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
