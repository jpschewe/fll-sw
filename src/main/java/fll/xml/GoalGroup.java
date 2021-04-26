/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * A group of goals.
 */
public class GoalGroup extends GoalElement {

  /**
   * XML element tag used by this class.
   */
  public static final String TAG_NAME = "goalGroup";

  /**
   * Default constructor for new objects.
   */
  public GoalGroup() {
    super();
  }

  /**
   * Constructor for reading from an XML document.
   *
   * @param ele the XML element to parse
   * @param goalScope used when constructing {@link ComputedGoal} objects
   */
  public GoalGroup(final Element ele,
                   final @UnknownInitialization GoalScope goalScope) {
    super(ele);

    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if (Goal.TAG_NAME.equals(goalEle.getNodeName())) {
        final Goal goal = new Goal(goalEle);
        goals.add(goal);
      } else if (ComputedGoal.TAG_NAME.equals(goalEle.getNodeName())) {
        final ComputedGoal compGoal = new ComputedGoal(goalEle, goalScope);
        goals.add(compGoal);
      }
    }
  }

  private final List<AbstractGoal> goals = new LinkedList<>();

  /**
   * The goals for the group in the order that they are in the challenge
   * description.
   *
   * @return unmodifiable list
   */
  public List<AbstractGoal> getGoals() {
    return Collections.unmodifiableList(goals);
  }

  /**
   * Sum of all goals in the group.
   */
  @Override
  public double evaluate(final TeamScore teamScore) {
    if (!teamScore.scoreExists()) {
      return Double.NaN;
    } else if (teamScore.isNoShow()) {
      return 0D;
    }

    return getGoals().stream().mapToDouble(g -> g.evaluate(teamScore)).sum();
  }

  @Override
  public Element toXml(final Document doc) {
    final Element ele = doc.createElementNS(null, TAG_NAME);

    populateXml(doc, ele);

    for (final AbstractGoal goal : goals) {
      final Element goalEle = goal.toXml(doc);
      ele.appendChild(goalEle);
    }

    return ele;
  }

  /**
   * Add the specified goal to the end of the list.
   *
   * @param v the new goal
   */
  public void addGoal(final AbstractGoal v) {
    goals.add(v);
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
    goals.add(index, v);
  }

  /**
   * Remove the specified goal from the list.
   *
   * @param v the goal to remove
   * @return if the goal was removed
   */
  public boolean removeGoal(final AbstractGoal v) {
    return goals.remove(v);
  }

  /**
   * Remove the goal at the specified index.
   *
   * @param index the index of the goal to remove
   * @return the removed goal
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public AbstractGoal removeGoal(final int index) throws IndexOutOfBoundsException {
    return goals.remove(index);
  }

  @Override
  public boolean isGoal() {
    return false;
  }

  @Override
  public boolean isGoalGroup() {
    return true;
  }

}
