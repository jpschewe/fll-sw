/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

public class ComputedGoal extends AbstractGoal implements VariableScope {

  public ComputedGoal(final Element ele,
                      final GoalScope goalScope) {
    super(ele);

    final List<Variable> variables = new LinkedList<Variable>();
    for (final Element varEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("variable"))) {
      final Variable var = new Variable(varEle, goalScope);
      variables.add(var);
    }
    mVariables = Collections.unmodifiableList(variables);

    final Element switchEle = new NodelistElementCollectionAdapter(ele.getElementsByTagName("switch")).next();
    mSwitch = new SwitchStatement(switchEle, goalScope, this);
  }

  private final List<Variable> mVariables;

  public List<Variable> getVariables() {
    return mVariables;
  }

  private final SwitchStatement mSwitch;

  public SwitchStatement getSwitch() {
    return mSwitch;
  }

  public Variable getVariable(final String name) throws ScopeException {
    for (final Variable var : mVariables) {
      if (name.equals(var.getName())) {
        return var;
      }
    }
    throw new ScopeException("Cannot find variable '"
        + name + "'");
  }

}
