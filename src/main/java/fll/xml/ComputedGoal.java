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

import javax.annotation.Nonnull;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Represents a computed goal in the challenge.
 */
public class ComputedGoal extends AbstractGoal implements VariableScope {

  /**
   * XML element tag used by this class.
   */
  public static final String TAG_NAME = "computedGoal";

  /**
   * @param ele the element to parse
   * @param goalScope {@link #getGoalScope()}
   */
  public ComputedGoal(final Element ele,
                      final @UnknownInitialization GoalScope goalScope) {
    super(ele);

    this.goalScope = goalScope;

    mVariables = new LinkedList<>();
    for (final Element varEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(Variable.TAG_NAME))) {
      final Variable var = new Variable(varEle, goalScope);
      mVariables.add(var);
    }

    final Element switchEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName(SwitchStatement.TAG_NAME)).next();
    mSwitch = new SwitchStatement(switchEle, goalScope, this);
  }

  /**
   * Default constructor, creates an object with no variables and a default switch
   * statement.
   * 
   * @param name see {@link #getName()}
   * @param goalScope see {@link #getGoalScope()}
   */
  public ComputedGoal(@Nonnull final String name,
                      final GoalScope goalScope) {
    super(name);
    this.goalScope = goalScope;
    mVariables = new LinkedList<>();
    mSwitch = new SwitchStatement();
  }

  private final Collection<Variable> mVariables;

  /**
   * The variables in the computed goal.
   * 
   * @return unmodifiable collection
   */
  public Collection<Variable> getVariables() {
    return Collections.unmodifiableCollection(mVariables);
  }

  @Override
  public Collection<Variable> getAllVariables() {
    return getVariables();
  }

  /**
   * Add a variable.
   * 
   * @param v the variable to add
   */
  public void addVariable(final Variable v) {
    mVariables.add(v);
  }

  /**
   * Remove a variable.
   * 
   * @param v the variable to remove
   * @return true if the variable was removed
   */
  public boolean removeVariable(final Variable v) {
    return mVariables.remove(v);
  }

  private final @NotOnlyInitialized SwitchStatement mSwitch;

  /**
   * @return the switch statement for this goal
   */
  public SwitchStatement getSwitch() {
    return mSwitch;
  }

  @Override
  public Variable getVariable(final String name) throws ScopeException {
    for (final Variable var : mVariables) {
      if (var.getName().equals(name)) {
        return var;
      }
    }
    throw new ScopeException("Cannot find variable '"
        + name
        + "'");
  }

  @Override
  public double getRawScore(final TeamScore teamScore) {
    final double score = getSwitch().evaluate(teamScore);
    return applyScoreType(score);

  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    return getRawScore(teamScore);
  }

  @Override
  public boolean isComputed() {
    return true;
  }

  @Override
  public boolean isEnumerated() {
    return false;
  }

  @Override
  public List<EnumeratedValue> getValues() {
    return Collections.emptyList();
  }

  @Override
  public ScoreType getScoreType() {
    return getSwitch().getScoreType();
  }

  @Override
  public double getMin() {
    return -1
        * Double.MAX_VALUE;
  }

  @Override
  public double getMax() {
    return Double.MAX_VALUE;
  }

  @Override
  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);

    populateXml(doc, ele);

    for (final Variable var : mVariables) {
      final Element varEle = var.toXml(doc);
      ele.appendChild(varEle);
    }

    final Element switchEle = mSwitch.toXml(doc);
    ele.appendChild(switchEle);

    return ele;
  }

  private final @NotOnlyInitialized GoalScope goalScope;

  /**
   * @return where to lookup goals for the computation
   */
  public GoalScope getGoalScope() {
    return goalScope;
  }

}
