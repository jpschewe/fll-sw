/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Base for {@link SubjectiveScoreCategory} and
 * {@link PerformanceScoreCategory}.
 */
public abstract class ScoreCategory implements Evaluatable, Serializable, GoalScope {

  /**
   * XML attribute name used for the weight of a score category.
   */
  public static final String WEIGHT_ATTRIBUTE = "weight";

  /**
   * @param ele the element to parse
   */
  protected ScoreCategory(final Element ele) {
    mWeight = Double.parseDouble(ele.getAttribute(WEIGHT_ATTRIBUTE));

    for (final Element goalEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if (Goal.TAG_NAME.equals(goalEle.getNodeName())) {
        final GoalElement goal = new Goal(goalEle);
        goalElements.add(goal);
      } else if (ComputedGoal.TAG_NAME.equals(goalEle.getNodeName())) {
        final GoalElement compGoal = new ComputedGoal(goalEle, this);
        goalElements.add(compGoal);
      } else if (GoalGroup.TAG_NAME.equals(goalEle.getNodeName())) {
        final GoalElement group = new GoalGroup(goalEle, this);
        goalElements.add(group);
      }
    }
  }

  /**
   * Default constructor creates an object with no {@link #getGoalElements()} and
   * a
   * {@link #getWeight()} of 1.
   */
  protected ScoreCategory() {
    mWeight = 1;
  }

  private final List<GoalElement> goalElements = new LinkedList<>();

  /**
   * The goal elements for the category in the order they should be displayed.
   *
   * @return unmodifiable list
   */
  public List<GoalElement> getGoalElements() {
    return Collections.unmodifiableList(goalElements);
  }

  /**
   * All goals in the category and it's groups.
   */
  @Override
  public List<AbstractGoal> getAllGoals() {
    final List<AbstractGoal> retval = new LinkedList<>();
    goalElements.stream().forEach(ge -> {
      if (ge.isGoal()) {
        retval.add((AbstractGoal) ge);
      } else if (ge.isGoalGroup()) {
        retval.addAll(((GoalGroup) ge).getGoals());
      } else {
        throw new FLLInternalException("Unknown GoalElement class: "
            + ge.getClass());
      }
    });

    return retval;
  }

  /**
   * Add the specified element to the end of the list.
   *
   * @param v the new element
   */
  public void addGoalElement(final GoalElement v) {
    goalElements.add(v);
  }

  /**
   * Add a element at the specified index.
   *
   * @param index the index to add the goal at
   * @param v the element to add
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void addGoalElement(final int index,
                             final GoalElement v)
      throws IndexOutOfBoundsException {
    goalElements.add(index, v);
  }

  /**
   * Remove the specified element from the list.
   *
   * @param v the element to remove
   * @return if the element was removed
   */
  public boolean removeGoalElement(final GoalElement v) {
    return goalElements.remove(v);
  }

  /**
   * Remove the element at the specified index.
   *
   * @param index the index of the goal to remove
   * @return the removed goal
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public GoalElement removeGoalElement(final int index) throws IndexOutOfBoundsException {
    return goalElements.remove(index);
  }

  private double mWeight;

  /**
   * @return the weight for this category in the overall score
   */
  public double getWeight() {
    return mWeight;
  }

  /**
   * @param v {@link #getWeight()}
   */
  public void setWeight(final double v) {
    mWeight = v;
  }

  @Override
  public AbstractGoal getGoal(final String name) {
    for (final GoalElement ge : goalElements) {
      if (ge.isGoal()) {
        final AbstractGoal goal = (AbstractGoal) ge;
        if (goal.getName().equals(name)) {
          return goal;
        }
      } else if (ge.isGoalGroup()) {
        final GoalGroup group = (GoalGroup) ge;
        for (final AbstractGoal goal : group.getGoals()) {
          if (goal.getName().equals(name)) {
            return goal;
          }
        }
      } else {
        throw new FLLInternalException("Unexpected goal element type: "
            + ge.getClass());
      }
    }
    throw new ScopeException("Cannot find goal named '"
        + name
        + "'");
  }

  @Override
  public double evaluate(final TeamScore teamScore) {
    if (!teamScore.scoreExists()) {
      return Double.NaN;
    } else if (teamScore.isNoShow()) {
      return 0D;
    }

    return goalElements.stream().mapToDouble(g -> g.evaluate(teamScore)).sum();
  }

  /**
   * Compute scores per goal group.
   *
   * @param teamScore the score to evaluate
   * @return goal group to score, empty map if no score or a no show or no groups
   *         defined
   */
  public Map<String, Double> getGoalGroupScores(final TeamScore teamScore) {
    final Map<String, Double> goalGroupScores = new HashMap<>();

    if (!teamScore.scoreExists()) {
      return goalGroupScores;
    } else if (teamScore.isNoShow()) {
      return goalGroupScores;
    }

    for (final GoalElement ge : goalElements) {
      if (ge.isGoalGroup()) {
        final GoalGroup group = (GoalGroup) ge;
        final double groupScore = group.evaluate(teamScore);
        goalGroupScores.merge(group.getTitle(), groupScore, Double::sum);
      }
    }
    return goalGroupScores;
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
    for (final GoalElement ge : goalElements) {
      if (ge.isGoal()) {
        if (((AbstractGoal) ge).getScoreType() == ScoreType.FLOAT) {
          return ScoreType.FLOAT;
        }
      } else if (ge.isGoalGroup()) {
        for (final AbstractGoal goal : ((GoalGroup) ge).getGoals()) {
          if (goal.getScoreType() == ScoreType.FLOAT) {
            return ScoreType.FLOAT;
          }
        }
      } else {
        throw new FLLInternalException("Unexpected goal element class: "
            + ge.getClass());
      }
    }
    return ScoreType.INTEGER;
  }

  /**
   * @param doc used to create elements
   * @param ele the element to add information from this class to
   */
  protected void populateXml(final Document doc,
                             final Element ele) {
    ele.setAttribute(WEIGHT_ATTRIBUTE, Utilities.getXmlFloatingPointNumberFormat().format(mWeight));

    for (final GoalElement ge : goalElements) {
      final Element gele = ge.toXml(doc);
      ele.appendChild(gele);
    }
  }

  /**
   * The name of the category must be a valid database string.
   *
   * @return the name of the category
   */
  public abstract String getName();

}
