/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;

/**
 * 
 */
public class ScoreCategory implements Evaluatable, Serializable, GoalScope {

  public ScoreCategory(final Element ele) {
    this(ele, ele.getAttribute("name"), ele.getAttribute("title"));
  }

  protected ScoreCategory(final Element ele,
                          final String name,
                          final String title) {
    mName = name;
    mTitle = title;
    mWeight = Double.valueOf(ele.getAttribute("weight"));

    final Map<String, AbstractGoal> goals = new HashMap<String, AbstractGoal>();
    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if ("goal".equals(goalEle.getNodeName())) {
        final Goal goal = new Goal(goalEle);
        goals.put(goal.getName(), goal);
      } else if ("computedGoal".equals(goalEle.getNodeName())) {
        final ComputedGoal compGoal = new ComputedGoal(goalEle, this);
        goals.put(compGoal.getName(), compGoal);
      }
    }
    mGoals = Collections.unmodifiableMap(goals);

  }

  private final Map<String, AbstractGoal> mGoals;

  public Collection<AbstractGoal> getGoals() {
    return mGoals.values();
  }

  private final String mName;

  public String getName() {
    return mName;
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private final double mWeight;

  public double getWeight() {
    return mWeight;
  }

  public AbstractGoal getGoal(final String name) {
    if (mGoals.containsKey(name)) {
      return mGoals.get(name);
    } else {
      throw new ScopeException("Cannot find goal named '"
          + name + "'");
    }

  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    double total = 0;
    for (final AbstractGoal g : getGoals()) {
      total += g.getComputedScore(teamScore);
    }
    return total;
  }

}
