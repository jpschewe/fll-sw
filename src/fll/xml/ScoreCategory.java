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

import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

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

    mGoals = new LinkedList<AbstractGoal>();
    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if ("goal".equals(goalEle.getNodeName())) {
        final Goal goal = new Goal(goalEle);
        mGoals.add(goal);
      } else if ("computedGoal".equals(goalEle.getNodeName())) {
        final ComputedGoal compGoal = new ComputedGoal(goalEle, this);
        mGoals.add(compGoal);
      }
    }
  }

  private final List<AbstractGoal> mGoals;

  /**
   * The goals for the category in the order they should be displayed.
   * 
   * @return unmodifiable list
   */
  public List<AbstractGoal> getGoals() {
    return Collections.unmodifiableList(mGoals);
  }

  /**
   * Add the specified goal to the end of the list.
   * 
   * @param v
   */
  public void addGoal(final AbstractGoal v) {
    mGoals.add(v);
  }

  /**
   * Add a goal at the specified index.
   * 
   * @param index the index to add the goal at
   * @param v the goal to add
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void addGoal(final int index,
                      final AbstractGoal v)
      throws IndexOutOfBoundsException {
    mGoals.add(index, v);
  }

  /**
   * Remove the specified goal from the list.
   * 
   * @param v the goal to remove
   * @return if the goal was removed
   */
  public boolean removeGoal(final AbstractGoal v) {
    return mGoals.remove(v);
  }

  /**
   * Remove the goal at the specified index.
   * 
   * @param index the index of the goal to remove
   * @return the removed goal
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public AbstractGoal removeGoal(final int index) throws IndexOutOfBoundsException {
    return mGoals.remove(index);
  }

  private final String mName;

  public String getName() {
    return mName;
  }

  private String mTitle;

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(final String v) {
    mTitle = v;
  }

  private double mWeight;

  public double getWeight() {
    return mWeight;
  }

  public void setWeight(final double v) {
    mWeight = v;
  }

  @Override
  public AbstractGoal getGoal(final String name) {
    for (final AbstractGoal goal : mGoals) {
      if (goal.getName().equals(name)) {
        return goal;
      }
    }
    throw new ScopeException("Cannot find goal named '"
        + name + "'");
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    if (!teamScore.scoreExists()) {
      return Double.NaN;
    } else if (teamScore.isNoShow()) {
      return 0;
    }

    double total = 0;
    for (final AbstractGoal g : getGoals()) {
      total += g.getComputedScore(teamScore);
    }
    return total;
  }

  /**
   * Determines the {@link ScoreType} for this category.
   * This is done by walking all of the goals and checking their score type.
   * If any goal is floating point, then this category can have a floating
   * point score.
   * 
   * @return not null
   */
  public ScoreType getScoreType() {
    final boolean hasFloatingPointGoals = getGoals().stream().anyMatch(g -> g.getScoreType() == ScoreType.FLOAT);
    return hasFloatingPointGoals ? ScoreType.FLOAT : ScoreType.INTEGER;
  }

}
