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

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

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

    final List<Goal> goals = new LinkedList<Goal>();
    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("goal"))) {
      final Goal goal = new Goal(goalEle);
      goals.add(goal);
    }
    mGoals = Collections.unmodifiableList(goals);

    final List<ComputedGoal> computedGoals = new LinkedList<ComputedGoal>();
    for (final Element compEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("computedGoal"))) {
      final ComputedGoal compGoal = new ComputedGoal(compEle, this);
      computedGoals.add(compGoal);
    }
    mComputedGoals = Collections.unmodifiableList(computedGoals);

  }

  private final List<Goal> mGoals;

  public List<Goal> getGoals() {
    return mGoals;
  }

  private final List<ComputedGoal> mComputedGoals;

  public List<ComputedGoal> getComputedGoals() {
    return mComputedGoals;
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
    for (final Goal g : mGoals) {
      if (g.getName().equals(name)) {
        return g;
      }
    }

    for (final ComputedGoal g : mComputedGoals) {
      if (g.getName().equals(name)) {
        return g;
      }
    }
    throw new ScopeException("Cannot find goal named '"
        + name + "'");
  }

}
