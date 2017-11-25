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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

public class ComputedGoal extends AbstractGoal implements VariableScope {

  public static final String TAG_NAME = "computedGoal";

  public ComputedGoal(final Element ele,
                      final GoalScope goalScope) {
    super(ele);

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
   */
  public ComputedGoal(@Nonnull final String name) {
    super(name);
    mVariables = new LinkedList<>();
    mSwitch = new SwitchStatement();
  }

  private final Collection<Variable> mVariables;

  public Collection<Variable> getVariables() {
    return Collections.unmodifiableCollection(mVariables);
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

  private final SwitchStatement mSwitch;

  public SwitchStatement getSwitch() {
    return mSwitch;
  }

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

  public double getRawScore(final TeamScore teamScore) {
    final double score = getSwitch().evaluate(teamScore);
    return applyScoreType(score);

  }

  public double getComputedScore(final TeamScore teamScore) {
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
    return ScoreType.FLOAT;
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

}
