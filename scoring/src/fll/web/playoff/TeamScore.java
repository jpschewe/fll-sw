/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.text.ParseException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Utilities;
import fll.util.ScoreUtils;
import fll.xml.XMLUtils;

/**
 * Represents a score for a team. Only the values of simple goals are available
 * through this object. The values of computed goals are only computed when
 * computing the {@link fll.db.Queries#computeTotalScore(TeamScore) total score}.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public abstract class TeamScore {

  /**
   * Run number used for team scores that are not performance scores.
   */
  public static final int NON_PERFORMANCE_RUN_NUMBER = -1;

  public TeamScore(final Element categoryDescription, final int teamNumber) {
    this(categoryDescription, teamNumber, NON_PERFORMANCE_RUN_NUMBER);
  }

  public TeamScore(final Element categoryDescription, final int teamNumber, final int runNumber) {
    _categoryDescription = categoryDescription;
    _teamNumber = teamNumber;
    _runNumber = runNumber;
  }

  /**
   * The team that this score is for.
   * 
   * @return the team
   */
  public final int getTeamNumber() {
    return _teamNumber;
  }

  private final int _teamNumber;

  /**
   * Check if the score exists. If it doesn't exist, the other score methods
   * will throw a RuntimeException
   * 
   * @return true if the score exists
   */
  public abstract boolean scoreExists();

  /**
   * Is this score a no show?
   * 
   * @return true if this score is a no show
   */
  public abstract boolean isNoShow();

  /**
   * What run do these scores apply to?
   * 
   * @return the run for the scores
   */
  public final int getRunNumber() {
    return _runNumber;
  }

  private final int _runNumber;

  /**
   * The raw score for a particular simple goal, as a double.
   * 
   * @param goalName
   *          the goal to get the score for
   * @return the score
   */
  public abstract double getRawScore(String goalName);

  /**
   * The computed score for a particular goal. This handles both "goal" elements
   * and "computedGoal" elements.
   * 
   * @param goalName
   *          the goal to get the score for
   * @return the score
   */
  public final double getComputedScore(final String goalName) {
    assertScoreExists();
    try {
      final Element goalDescription = getGoalDescription(goalName);
      if(XMLUtils.isComputedGoal(goalDescription)) {
        return ScoreUtils.evalComputedGoal(goalDescription, this);
      } else {
        final double multiplier = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalDescription.getAttribute("multiplier")).doubleValue();
        final NodeList values = goalDescription.getElementsByTagName("value");
        if(values.getLength() == 0) {
          final double score = getRawScore(goalName);
          return multiplier * score;
        } else {
          // enumerated
          // find enum value that matches raw score value
          final String enumVal = getEnumRawScore(goalName);
          if(null != enumVal) {
            boolean found = false;
            double score = -1;
            for(int v = 0; v < values.getLength() && !found; v++) {
              final Element value = (Element)values.item(v);
              if(value.getAttribute("value").equals(enumVal)) {
                score = Utilities.NUMBER_FORMAT_INSTANCE.parse(value.getAttribute("score")).doubleValue();
                found = true;
              }
            }
            if(!found) {
              throw new RuntimeException("Error, enum value in database '" + enumVal + "' for goal: " + goalName + " is not a valid value");
            }
            return score * multiplier;
          } else {
            throw new RuntimeException("Error, got null as value for enumerated goal: " + goalName + " team: " + getTeamNumber() + " run: "
                + getRunNumber() + " category: " + getCategoryDescription().getAttribute("name"));
          }
        }
      }
    } catch(final ParseException pe) {
      throw new RuntimeException(pe);
    }
  }

  /**
   * The raw score for a particular enumerated goal, as a String.
   * 
   * @param goalName
   *          the goal to get the score for
   * @return the score
   */
  public abstract String getEnumRawScore(String goalName);

  /**
   * Cleanup any resources used. The object is no longer valid after a call to
   * cleanup.
   */
  public void cleanup() {
    // nothing by default
  }

  /**
   * The description of the category this score represents.
   * 
   * @return the category description
   */
  public final Element getCategoryDescription() {
    return _categoryDescription;
  }

  /**
   * Get the name of the the category, same as the name of the database table
   * that stores the category information.
   * 
   * @return the name of the category
   */
  public final String getCategoryName() {
    if("Performance".equals(getCategoryDescription().getNodeName())) {
      return "Performance";
    } else if("subjectiveCategory".equals(getCategoryDescription().getNodeName())) {
      return getCategoryDescription().getAttribute("name");
    } else {
      throw new RuntimeException("Unexpected category element found: " + getCategoryDescription().getNodeName());
    }
  }

  private final Element _categoryDescription;

  /**
   * Find the goal description for the specified goal name
   * 
   * @param name
   *          the name of the goal to find
   * @return the goal description, null if none found
   */
  public final Element getGoalDescription(final String name) {
    final NodeList children = getCategoryDescription().getChildNodes();
    for(int i = 0; i < children.getLength(); ++i) {
      final Element child = (Element)children.item(i);
      if(("goal".equals(child.getNodeName()) || "computedGoal".equals(child.getNodeName())) && name.equals(child.getAttribute("name"))) {
        return child;
      }
    }
    return null;
  }

  /**
   * If the score doesn't exist, throw a RuntimeException
   * 
   */
  protected final void assertScoreExists() {
    if(!scoreExists()) {
      throw new RuntimeException("Attempt to retrieve team score data when the data does not exist");
    }
  }
}
