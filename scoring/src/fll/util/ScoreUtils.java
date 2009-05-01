/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.util.Functions;

import org.w3c.dom.Element;

import fll.Utilities;
import fll.web.playoff.TeamScore;
import fll.xml.XMLUtils;

/**
 * Scoring utilities.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class ScoreUtils {

  private ScoreUtils() {
    // no instances
  }

  /**
   * Compute total score for values in current row of rs based on goals from
   * challenge document.
   * 
   * @param teamScore ResultSet holding the raw score values for each goal
   * @return the score, 0 on a bye, Double.NaN if all scores in a row are null
   *         or an exception occurred talking to the database (indicating that
   *         this row should be ignored)
   */
  public static double computeTotalScore(final TeamScore teamScore) throws ParseException {

    final Element categoryElement = teamScore.getCategoryDescription();
    if (!teamScore.scoreExists()) {
      return Double.NaN;
    }

    double computedTotal = 0;
    for (final Element goal : XMLUtils.filterToElements(categoryElement.getElementsByTagName("goal"))) {
      final String goalName = goal.getAttribute("name");
      final Double val = teamScore.getComputedScore(goalName);
      if (null == val) {
        return Double.NaN;
      }
      computedTotal += val;
    }

    // do computed goals
    for (final Element computedGoalEle : XMLUtils.filterToElements(categoryElement.getElementsByTagName("computedGoal"))) {
      final Double val = evalComputedGoal(computedGoalEle, teamScore);
      if (null == val) {
        return Double.NaN;
      }
      computedTotal += val;
    }

    return computedTotal;
  }

  /**
   * Evaluate a computed goal.
   * 
   * @param computedGoalEle
   * @param teamScore
   * @return the score, null if a score that it depends upon is missing
   * @throws ParseException
   */
  public static Double evalComputedGoal(final Element computedGoalEle, final TeamScore teamScore) throws ParseException {
    final String computedGoalName = computedGoalEle.getAttribute("name");
    final Map<String, Double> variableValues = new HashMap<String, Double>();

    for (final Element childElement : XMLUtils.filterToElements(computedGoalEle.getChildNodes())) {
      if ("variable".equals(childElement.getNodeName())) {
        final String variableName = childElement.getAttribute("name");
        final Double variableValue = evalPoly(childElement, teamScore, variableValues);
        variableValues.put(variableName, variableValue);
      } else if ("switch".equals(childElement.getNodeName())) {
        return evalSwitch(childElement, teamScore, variableValues);
      } else {
        throw new RuntimeException("Unexpected element in computed goal.  Expected 'switch' or 'variable', but found '"
            + childElement.getNodeName() + "'");
      }
    }
    throw new RuntimeException("Error: no 'switch' element found in computed goal: "
        + computedGoalName);
  }

  /**
   * Compute the value of a switch statement in a computed goal.
   * 
   * @param switchElement the XML element representing the switch statement
   * @param teamScore the scores for the simple goals
   * @param variableValues the values of known variables
   * @return the value of the switch
   * @throws ParseException
   */
  private static Double evalSwitch(final Element switchElement, final TeamScore teamScore, final Map<String, Double> variableValues) throws ParseException {
    for (final Element caseElement : XMLUtils.filterToElements(switchElement.getChildNodes())) {
      if ("case".equals(caseElement.getNodeName())) {
        final List<Element> children = XMLUtils.filterToElements(caseElement.getChildNodes());
        final Element conditionEle = children.get(0);
        if (evalCondition(conditionEle, teamScore, variableValues)) {
          final Element resultElement = children.get(1);
          if ("result".equals(resultElement.getNodeName())) {
            return evalPoly(resultElement, teamScore, variableValues);
          } else if ("switch".equals(resultElement.getNodeName())) {
            return evalSwitch(resultElement, teamScore, variableValues);
          } else {
            throw new RuntimeException("Unexpected result type for condition: "
                + resultElement.getNodeName());
          }
        }
      } else if ("default".equals(caseElement.getNodeName())) {
        return evalPoly(caseElement, teamScore, variableValues);
      } else {
        throw new RuntimeException("Unexpected node name found in switch statement: "
            + caseElement.getNodeName());
      }
    }
    throw new RuntimeException("Malformed switch statement found, no default case");
  }

  /**
   * Evaluate a polynomial condition.
   * 
   * @throws ParseException
   */
  private static boolean evalPolyCond(final Element leftEle,
                                      final Element ineqEle,
                                      final Element rightEle,
                                      final TeamScore teamScore,
                                      final Map<String, Double> variableValues) throws ParseException {
    final Double leftVal = evalPoly(leftEle, teamScore, variableValues);
    final Double rightVal = evalPoly(rightEle, teamScore, variableValues);
    if (null == leftVal
        || null == rightVal) {
      // missing score, return false
      return false;
    }

    if ("less-than".equals(ineqEle.getNodeName())) {
      return leftVal < rightVal;
    } else if ("less-than-or-equal".equals(ineqEle.getNodeName())) {
      return leftVal <= rightVal;
    } else if ("greater-than".equals(ineqEle.getNodeName())) {
      return leftVal > rightVal;
    } else if ("greater-than-or-equal".equals(ineqEle.getNodeName())) {
      return leftVal >= rightVal;
    } else if ("equal-to".equals(ineqEle.getNodeName())) {
      return leftVal == rightVal;
    } else if ("no-equal-to".equals(ineqEle.getNodeName())) {
      return leftVal != rightVal;
    } else {
      throw new RuntimeException("Unexpected inequality found in condition: "
          + ineqEle.getNodeName());
    }
  }

  /**
   * Evaluate an eumeration condition.
   * 
   * @param leftEle
   * @param ineqEle
   * @param rightEle
   * @param teamScore
   * @return
   */
  private static boolean evalEnumCond(final Element leftEle, final Element ineqEle, final Element rightEle, final TeamScore teamScore) {
    final String leftStr = evalStrOrGoalRef(XMLUtils.filterToElements(leftEle.getChildNodes()).get(0), teamScore);
    final String rightStr = evalStrOrGoalRef(XMLUtils.filterToElements(rightEle.getChildNodes()).get(0), teamScore);
    if ("equal-to".equals(ineqEle.getNodeName())) {
      return Functions.safeEquals(leftStr, rightStr);
    } else if ("no-equal-to".equals(ineqEle.getNodeName())) {
      return !Functions.safeEquals(leftStr, rightStr);
    } else {
      throw new RuntimeException("Unexpected inequality found in enum condition: "
          + ineqEle.getNodeName());
    }
  }

  private static String evalStrOrGoalRef(final Element ele, final TeamScore teamScore) {
    if ("stringConstant".equals(ele.getNodeName())) {
      return ele.getAttribute("value");
    } else if ("goalRef".equals(ele.getNodeName())) {
      final String goalName = ele.getAttribute("goal");
      return teamScore.getEnumRawScore(goalName);
    } else {
      throw new RuntimeException("Expected 'stringConstant' or 'goalRef', but found: "
          + ele.getNodeName());
    }
  }

  /**
   * Evaluate a generatic condition iside a case of a computed goal
   * 
   * @param conditionEle the element describing the condition
   * @param teamScore the team's score
   * @param variableValues the values of the known variables
   * @return if the condition is true
   * @throws ParseException
   * @see #evalPolyCond(Element, Element, Element, TeamScore)
   * @see #evalEnumCond(Element, Element, Element, TeamScore)
   */
  private static boolean evalCondition(final Element conditionEle, final TeamScore teamScore, final Map<String, Double> variableValues) throws ParseException {
    final List<Element> children = XMLUtils.filterToElements(conditionEle.getChildNodes());
    final Element leftEle = children.get(0);
    final Element ineqEle = children.get(1);
    final Element rightEle = children.get(2);
    if ("enumCondition".equals(conditionEle.getNodeName())) {
      return evalEnumCond(leftEle, ineqEle, rightEle, teamScore);
    } else if ("condition".equals(conditionEle.getNodeName())) {
      return evalPolyCond(leftEle, ineqEle, rightEle, teamScore, variableValues);
    } else {
      throw new RuntimeException("Unexpected condition type: "
          + conditionEle.getNodeName());
    }
  }

  /**
   * Evaluate a polynomial.
   * 
   * @param ele the element that contains the terms
   * @param teamScore the team score
   * @param variableValues the values of known variables
   * @return the value of the polynomial, null if a score is missing
   * @throws ParseException if there is an error parsing the XML values
   */
  public static Double evalPoly(final Element ele, final TeamScore teamScore, final Map<String, Double> variableValues) throws ParseException {
    double value = 0;
    for (final Element child : XMLUtils.filterToElements(ele.getChildNodes())) {
      if ("constant".equals(child.getNodeName())) {
        final String valueStr = child.getAttribute("value");
        final double val = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueStr).doubleValue();
        value += val;
      } else if ("term".equals(child.getNodeName())) {
        final Double termVal = evalTerm(child, teamScore);
        if (null == termVal) {
          return null;
        } else {
          value += termVal;
        }
      } else if ("variableRef".equals(child.getNodeName())) {
        final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(child.getAttribute("coefficient")).doubleValue();
        final String variable = child.getAttribute("variable");
        if (!variableValues.containsKey(variable)) {
          throw new RuntimeException("Unknown variable '"
              + variable + "'" + " known variables: " + variableValues);
        }
        final String floatingPoint = child.getAttribute("floatingPoint");
        final Double val = variableValues.get(variable);
        if (null == val) {
          return null;
        } else {
          value += applyFloatingPoint(coefficient
              * val, floatingPoint);
        }
      } else {
        throw new RuntimeException("Expected 'constant', 'term' or 'variableRef', but found '"
            + child.getNodeName() + "'");
      }
    }
    return value;
  }

  /**
   * Evaluate a term in a polynomial.
   * 
   * @param ele the term element
   * @param teamScore the team's score
   * @return the value of the term, null if a score is missing
   * @throws ParseException
   */
  private static Double evalTerm(final Element ele, final TeamScore teamScore) throws ParseException {
    final String coefStr = ele.getAttribute("coefficient");
    final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(coefStr).doubleValue();
    final String goal = ele.getAttribute("goal");
    final String scoreType = ele.getAttribute("scoreType");
    final double value;
    if ("raw".equals(scoreType)) {
      final Double rawScore = teamScore.getRawScore(goal);
      if (null == rawScore) {
        return null;
      } else {
        value = coefficient
            * rawScore;
      }
    } else if ("computed".equals(scoreType)) {
      final Double rawScore = teamScore.getComputedScore(goal);
      if (null == rawScore) {
        return null;
      } else {
        value = coefficient
            * rawScore;
      }
    } else {
      throw new RuntimeException("Unexpected score type: "
          + scoreType);
    }
    final String floatingPoint = ele.getAttribute("floatingPoint");
    return applyFloatingPoint(value, floatingPoint);
  }

  /**
   * Convert a value based on the specified floating point handling.
   * 
   * @param value
   * @param floatingPoint
   * @return
   */
  private static double applyFloatingPoint(final double value, final String floatingPoint) {
    if ("decimal".equals(floatingPoint)) {
      return value;
    } else if ("round".equals(floatingPoint)) {
      return Math.round(value);
    } else if ("truncate".equals(floatingPoint)) {
      return (int) value;
    } else {
      throw new RuntimeException("Unexpected floating point type: "
          + floatingPoint);
    }
  }

  /**
   * Schedule the specified finalists into time slots such that no 1 team is
   * scheduled to be judged in two categories at the same time. Also try and
   * minimize the amount of time to perform the finalist judging.
   * 
   * @param finalists Map of category names to a collection of team numbers that
   *          are finalists for that category
   * @return Each element in the list represents a time slot. A time slot is a
   *         map from categories to teams that are scheduled to be judged during
   *         the time slot in the specified category
   */
  public static List<Map<String, Integer>> scheduleFinalists(final Map<String, Collection<Integer>> finalists) {
    // reorient the map to be based on teams so that we can figure out which
    // team has the most categories and schedule that team first
    final Map<Integer, List<String>> finalistsCount = new HashMap<Integer, List<String>>();
    for (final String category : finalists.keySet()) {
      final Collection<Integer> teams = finalists.get(category);
      for (final Integer team : teams) {
        if (!finalistsCount.containsKey(team)) {
          finalistsCount.put(team, new LinkedList<String>());
        }
        finalistsCount.get(team).add(category);
      }
    }
    // sort the list so that the team in the most categories is first, this
    // should ensure the minimum amount of time to do the finalist judging
    final List<Integer> sortedTeams = new LinkedList<Integer>(finalistsCount.keySet());
    Collections.sort(sortedTeams, new Comparator<Integer>() {
      public int compare(final Integer teamOne, final Integer teamTwo) {
        final int numCatsOne = finalistsCount.get(teamOne).size();
        final int numCatsTwo = finalistsCount.get(teamTwo).size();
        if (numCatsOne == numCatsTwo) {
          return 0;
        } else if (numCatsOne < numCatsTwo) {
          return 1;
        } else {
          return -1;
        }
      }
    });

    final List<Map<String, Integer>> schedule = new LinkedList<Map<String, Integer>>();
    for (final int team : sortedTeams) {
      for (final String category : finalistsCount.get(team)) {
        boolean scheduled = false;
        final Iterator<Map<String, Integer>> iter = schedule.iterator();
        while (iter.hasNext()
            && !scheduled) {
          final Map<String, Integer> timeSlot = iter.next();

          if (!timeSlot.containsKey(category)) {
            // check if this team is somewhere else in the slot
            boolean conflict = false;
            for (final Map.Entry<String, Integer> entry : timeSlot.entrySet()) {
              if (entry.getValue().equals(team)) {
                conflict = true;
              }
            }
            if (!conflict) {
              timeSlot.put(category, team);
              scheduled = true;
            }
          }
        } // end loop over slots
        if (!scheduled) {
          // add a new slot
          final Map<String, Integer> newTimeSlot = new HashMap<String, Integer>();
          newTimeSlot.put(category, team);
          schedule.add(newTimeSlot);
        }
      } // end foreach category
    } // end foreach team

    return schedule;

  } // end method

}
