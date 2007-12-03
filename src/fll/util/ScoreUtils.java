/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Utilities;
import fll.web.playoff.TeamScore;

/**
 * Scoring utilities.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class ScoreUtils {

  private ScoreUtils() {
    // no instances
  }

  /**
   * Compute total score for values in current row of rs based on goals from
   * challenge document.
   * 
   * @param teamScore
   *          ResultSet holding the raw score values for each goal
   * @return the score, 0 on a bye, Double.NaN if all scores in a row are null
   *         or an exception occurred talking to the database (indicating that
   *         this row should be ignored)
   */
  public static double computeTotalScore(final TeamScore teamScore) throws ParseException {

    final Element categoryElement = teamScore.getCategoryDescription();
    final NodeList goals = categoryElement.getElementsByTagName("goal");
    if(!teamScore.scoreExists()) {
      return Double.NaN;
    }

    double computedTotal = 0;
    for(int i = 0; i < goals.getLength(); i++) {
      final Element goal = (Element)goals.item(i);
      final String goalName = goal.getAttribute("name");
      computedTotal += teamScore.getComputedScore(goalName);
    }

    // do computed goals
    final NodeList computedGoals = categoryElement.getElementsByTagName("computedGoal");
    for(int i = 0; i < computedGoals.getLength(); ++i) {
      final Element computedGoalEle = (Element)computedGoals.item(i);
      computedTotal += evalComputedGoal(computedGoalEle, teamScore);
    }

    return computedTotal;
  }

  public static double evalComputedGoal(final Element computedGoalEle, final TeamScore teamScore) throws ParseException {
    final String computedGoalName = computedGoalEle.getAttribute("name");
    final Map<String, Double> variableValues = new HashMap<String, Double>();

    final NodeList children = computedGoalEle.getChildNodes();
    for(int childIdx = 0; childIdx < children.getLength(); ++childIdx) {
      final Element childElement = (Element)children.item(childIdx);
      if("variable".equals(childElement.getNodeName())) {
        final String variableName = childElement.getAttribute("name");
        final double variableValue = evalPoly(childElement, teamScore, variableValues);
        variableValues.put(variableName, variableValue);
      } else if("switch".equals(childElement.getNodeName())) {
        return evalSwitch(childElement, teamScore, variableValues);
      } else {
        throw new RuntimeException("Unexpected element in computed goal.  Expected 'switch' or 'variable', but found '" + childElement.getNodeName()
            + "'");
      }
    }
    throw new RuntimeException("Error: no 'switch' element found in computed goal: " + computedGoalName);
  }

  /**
   * Compute the value of a switch statement in a computed goal.
   * 
   * @param switchElement
   *          the XML element representing the switch statement
   * @param teamScore
   *          the scores for the simple goals
   * @param variableValues
   *          the values of known variables
   * @return the value of the switch
   * @throws ParseException
   */
  private static double evalSwitch(final Element switchElement, final TeamScore teamScore, final Map<String, Double> variableValues)
      throws ParseException {
    final NodeList caseElements = switchElement.getChildNodes();
    for(int i = 0; i < caseElements.getLength(); ++i) {
      final Element caseElement = (Element)caseElements.item(i);
      if("case".equals(caseElement.getNodeName())) {
        final Element conditionEle = (Element)caseElement.getFirstChild();
        if(evalCondition(conditionEle, teamScore, variableValues)) {
          final Element resultElement = (Element)conditionEle.getNextSibling();
          if("result".equals(resultElement.getNodeName())) {
            return evalPoly(resultElement, teamScore, variableValues);
          } else if("switch".equals(resultElement.getNodeName())) {
            return evalSwitch(resultElement, teamScore, variableValues);
          } else {
            throw new RuntimeException("Unexpected result type for condition: " + resultElement.getNodeName());
          }
        }
      } else if("default".equals(caseElement.getNodeName())) {
        return evalPoly(caseElement, teamScore, variableValues);
      } else {
        throw new RuntimeException("Unexpected node name found in switch statement: " + caseElement.getNodeName());
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
    final double leftVal = evalPoly(leftEle, teamScore, variableValues);
    final double rightVal = evalPoly(rightEle, teamScore, variableValues);
    if("less-than".equals(ineqEle.getNodeName())) {
      return leftVal < rightVal;
    } else if("less-than-or-equal".equals(ineqEle.getNodeName())) {
      return leftVal <= rightVal;
    } else if("greater-than".equals(ineqEle.getNodeName())) {
      return leftVal > rightVal;
    } else if("greater-than-or-equal".equals(ineqEle.getNodeName())) {
      return leftVal >= rightVal;
    } else if("equal-to".equals(ineqEle.getNodeName())) {
      return leftVal == rightVal;
    } else if("no-equal-to".equals(ineqEle.getNodeName())) {
      return leftVal != rightVal;
    } else {
      throw new RuntimeException("Unexpected inequality found in condition: " + ineqEle.getNodeName());
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
    final String leftStr = evalStrOrGoalRef((Element)leftEle.getFirstChild(), teamScore);
    final String rightStr = evalStrOrGoalRef((Element)rightEle.getFirstChild(), teamScore);
    if("equal-to".equals(ineqEle.getNodeName())) {
      return Utilities.safeEquals(leftStr, rightStr);
    } else if("no-equal-to".equals(ineqEle.getNodeName())) {
      return !Utilities.safeEquals(leftStr, rightStr);
    } else {
      throw new RuntimeException("Unexpected inequality found in enum condition: " + ineqEle.getNodeName());
    }
  }

  private static String evalStrOrGoalRef(final Element ele, final TeamScore teamScore) {
    if("stringConstant".equals(ele.getNodeName())) {
      return ele.getAttribute("value");
    } else if("goalRef".equals(ele.getNodeName())) {
      final String goalName = ele.getAttribute("goal");
      return teamScore.getEnumRawScore(goalName);
    } else {
      throw new RuntimeException("Expected 'stringConstant' or 'goalRef', but found: " + ele.getNodeName());
    }
  }

  /**
   * Evaluate a generatic condition iside a case of a computed goal
   * 
   * @param conditionEle
   *          the element describing the condition
   * @param teamScore
   *          the team's score
   * @param variableValues
   *          the values of the known variables
   * @return if the condition is true
   * @throws ParseException
   * @see #evalPolyCond(Element, Element, Element, TeamScore)
   * @see #evalEnumCond(Element, Element, Element, TeamScore)
   */
  private static boolean evalCondition(final Element conditionEle, final TeamScore teamScore, final Map<String, Double> variableValues)
      throws ParseException {
    final Element leftEle = (Element)conditionEle.getFirstChild();
    final Element ineqEle = (Element)leftEle.getNextSibling();
    final Element rightEle = (Element)ineqEle.getNextSibling();
    if("enumCondition".equals(conditionEle.getNodeName())) {
      return evalEnumCond(leftEle, ineqEle, rightEle, teamScore);
    } else if("condition".equals(conditionEle.getNodeName())) {
      return evalPolyCond(leftEle, ineqEle, rightEle, teamScore, variableValues);
    } else {
      throw new RuntimeException("Unexpected condition type: " + conditionEle.getNodeName());
    }
  }

  /**
   * Evaluate a polynomial.
   * 
   * @param ele
   *          the element that contains the terms
   * @param teamScore
   *          the team score
   * @param variableValues
   *          the values of known variables
   * @return the value of the polynomial
   * @throws ParseException
   *           if there is an error parsing the XML values
   */
  public static double evalPoly(final Element ele, final TeamScore teamScore, final Map<String, Double> variableValues) throws ParseException {
    double value = 0;
    final NodeList children = ele.getChildNodes();
    for(int i = 0; i < children.getLength(); ++i) {
      final Element child = (Element)children.item(i);
      if("constant".equals(child.getNodeName())) {
        final String valueStr = child.getAttribute("value");
        final double val = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueStr).doubleValue();
        value += val;
      } else if("term".equals(child.getNodeName())) {
        value += evalTerm(child, teamScore);
      } else if("variableRef".equals(child.getNodeName())) {
        final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(child.getAttribute("coefficient")).doubleValue();
        final String variable = child.getAttribute("variable");
        if(!variableValues.containsKey(variable)) {
          throw new RuntimeException("Unknown variable '" + variable + "'" + " known variables: " + variableValues);
        }
        final String floatingPoint = child.getAttribute("floatingPoint");
        value += applyFloatingPoint(coefficient * variableValues.get(variable), floatingPoint);
      } else {
        throw new RuntimeException("Expected 'constant', 'term' or 'variableRef', but found '" + child.getNodeName() + "'");
      }
    }
    return value;
  }

  /**
   * Evaluate a term in a polynomial.
   * 
   * @param ele
   *          the term element
   * @param teamScore
   *          the team's score
   * @return the value of the term
   * @throws ParseException
   */
  private static double evalTerm(final Element ele, final TeamScore teamScore) throws ParseException {
    final String coefStr = ele.getAttribute("coefficient");
    final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(coefStr).doubleValue();
    final String goal = ele.getAttribute("goal");
    final String scoreType = ele.getAttribute("scoreType");
    final double value;
    if("raw".equals(scoreType)) {
      value = coefficient * teamScore.getRawScore(goal);
    } else if("computed".equals(scoreType)) {
      value = coefficient * teamScore.getComputedScore(goal);
    } else {
      throw new RuntimeException("Unexpected score type: " + scoreType);
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
    if("decimal".equals(floatingPoint)) {
      return value;
    } else if("round".equals(floatingPoint)) {
      return Math.round(value);
    } else if("truncate".equals(floatingPoint)) {
      return (int)value;
    } else {
      throw new RuntimeException("Unexpected floating point type: " + floatingPoint);
    }
  }

}
