/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

public class ComputedGoal extends AbstractGoal implements VariableScope {

  public ComputedGoal(final Element ele,
                      final GoalScope goalScope) {
    super(ele);

    final Map<String, Variable> variables = new HashMap<String, Variable>();
    for (final Element varEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("variable"))) {
      final Variable var = new Variable(varEle, goalScope);
      variables.put(var.getName(), var);
    }
    mVariables = Collections.unmodifiableMap(variables);

    final Element switchEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("switch")).next();
    mSwitch = new SwitchStatement(switchEle, goalScope, this);
  }

  private final Map<String, Variable> mVariables;

  public Collection<Variable> getVariables() {
    return mVariables.values();
  }

  private final SwitchStatement mSwitch;

  public SwitchStatement getSwitch() {
    return mSwitch;
  }

  public Variable getVariable(final String name) throws ScopeException {
    if (mVariables.containsKey(name)) {
      return mVariables.get(name);
    } else {
      throw new ScopeException("Cannot find variable '"
          + name + "'");
    }
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

  public double getMax() {
    return Double.MAX_VALUE;
  }

}
