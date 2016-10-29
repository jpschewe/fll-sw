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

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

/**
 * Term in a polynomial.
 */
public class Term extends AbstractTerm {

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
              final VariableScope variableScope) throws VariableRefNotAllowedException {
    super(ele);

    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("goalRef"))) {
      final GoalRef goal = new GoalRef(goalEle, goalScope);
      mGoals.add(goal);
    }

    for (final Element variableEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("variableRef"))) {
      if(null == variableScope) {
        throw new VariableRefNotAllowedException("Variable scope is null, therefore this term cannot reference variables");
      }
      
      final VariableRef variable = new VariableRef(variableEle, variableScope);
      mVariables.add(variable);
    }

  }

  private final Collection<GoalRef> mGoals = new LinkedList<GoalRef>();
  public Collection<GoalRef> getGoals() {
    return Collections.unmodifiableCollection(mGoals);
  }

  private final List<VariableRef> mVariables = new LinkedList<VariableRef>();
  public Collection<VariableRef> getVariables() {
    return Collections.unmodifiableCollection(mVariables);
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

}
