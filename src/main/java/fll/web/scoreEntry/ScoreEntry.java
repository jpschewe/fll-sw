/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import fll.Team;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FP;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.web.playoff.Playoff;
import fll.xml.AbstractConditionStatement;
import fll.xml.AbstractGoal;
import fll.xml.BasicPolynomial;
import fll.xml.CaseStatement;
import fll.xml.CaseStatementResult;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ComplexPolynomial;
import fll.xml.ComputedGoal;
import fll.xml.ConditionStatement;
import fll.xml.EnumConditionStatement;
import fll.xml.EnumeratedValue;
import fll.xml.FloatingPointType;
import fll.xml.Goal;
import fll.xml.GoalElement;
import fll.xml.GoalGroup;
import fll.xml.GoalRef;
import fll.xml.GoalScoreType;
import fll.xml.InequalityComparison;
import fll.xml.PerformanceScoreCategory;
import fll.xml.Restriction;
import fll.xml.ScoreType;
import fll.xml.StringValue;
import fll.xml.SwitchStatement;
import fll.xml.Term;
import fll.xml.Variable;
import fll.xml.VariableRef;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;

/**
 * Java code used in scoreEntry.jsp.
 */
public final class ScoreEntry {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Number of spaces to indent code at each level.
   */
  private static final int INDENT_LEVEL = 2;

  /**
   * Maximum range to use a slider for.
   */
  private static final int SLIDER_RANGE_MAX = 20;

  private ScoreEntry() {
  }

  private static boolean isTabletEntry(final HttpServletRequest request,
                                       final HttpSession session) {
    return !StringUtils.isBlank((String) session.getAttribute("scoreEntrySelectedTable"))
        || !StringUtils.isBlank(request.getParameter("tablet"));
  }

  /**
   * Set variables in the page context.
   * Checks request for "displayScores" to force display of scores, even
   * when using a tablet.
   *
   * @param application for application variables
   * @param request the web request
   * @param response used to redirect on an error
   * @param pageContext where to store the values
   * @param session get session variables
   * @return true if the page should continue loading, false if it should stop
   * @see #isTabletEntry(HttpServletRequest, HttpSession)
   */
  public static boolean populateContext(final ServletContext application,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final HttpSession session,
                                        final PageContext pageContext) {
    final boolean edit = Boolean.parseBoolean(request.getParameter("EditFlag"));

    final boolean tabletEntry = isTabletEntry(request, session);
    final boolean showScores = !tabletEntry
        || Boolean.parseBoolean(request.getParameter("showScores"));

    final boolean practice = Boolean.parseBoolean(request.getParameter("practice"));

    pageContext.setAttribute("practice", practice);
    pageContext.setAttribute("EditFlag", edit);
    pageContext.setAttribute("showScores", showScores);

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final double minimumAllowedScore = challengeDescription.getPerformance().getMinimumScore();
    pageContext.setAttribute("minimumAllowedScore", minimumAllowedScore);

    final boolean previousVerified;
    final boolean regularMatchPlay;
    if (practice) {
      regularMatchPlay = true;
      previousVerified = true;
    } else {

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {

        // support the unverified runs select box
        final String lTeamNum = request.getParameter("TeamNumber");
        if (null == lTeamNum) {
          SessionAttributes.appendToMessage(session,
                                            "<p name='error' class='error'>Attempted to load score entry page without providing a team number.</p>");
          response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
          return false;
        }
        final int dashIndex = lTeamNum.indexOf('-');
        final int teamNumber;
        final String runNumberStr;
        if (dashIndex > 0) {
          // teamNumber - runNumber
          final String teamStr = lTeamNum.substring(0, dashIndex);
          teamNumber = Integer.parseInt(teamStr);
          runNumberStr = lTeamNum.substring(dashIndex
              + 1);
        } else {
          runNumberStr = request.getParameter("RunNumber");
          teamNumber = Utilities.getIntegerNumberFormat().parse(lTeamNum).intValue();
        }
        final int tournament = Queries.getCurrentTournament(connection);
        final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament);
        final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);
        if (!tournamentTeams.containsKey(Integer.valueOf(teamNumber))) {
          throw new RuntimeException("Selected team number is not valid: "
              + teamNumber);
        }
        final Team team = tournamentTeams.get(Integer.valueOf(teamNumber));
        pageContext.setAttribute("team", team);

        // the next run the team will be competing in
        final int nextRunNumber = Queries.getNextRunNumber(connection, team.getTeamNumber());

        final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection, tournament);

        // what run number we're going to edit/enter
        int lRunNumber;
        if (edit) {
          if (null == runNumberStr) {
            SessionAttributes.appendToMessage(session,
                                              "<p name='error' class='error'>Please choose a run number when editing scores</p>");
            response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
            return false;
          }
          final int runNumber = Utilities.getIntegerNumberFormat().parse(runNumberStr).intValue();
          if (runNumber == 0) {
            lRunNumber = nextRunNumber
                - 1;
            if (lRunNumber < 1) {
              SessionAttributes.appendToMessage(session,
                                                "<p name='error' class='error'>Selected team has no performance score for this tournament.</p>");
              response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
              return false;
            }
          } else {
            if (!Queries.performanceScoreExists(connection, tournament, teamNumber, runNumber)) {
              SessionAttributes.appendToMessage(session,
                                                "<p name='error' class='error'>Team has not yet competed in run "
                                                    + runNumber
                                                    + ".  Please choose a valid run number.</p>");
              response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
              return false;
            }
            lRunNumber = runNumber;
          }
        } else {
          if (runningHeadToHead
              && nextRunNumber > numSeedingRounds) {
            if (null == Playoff.involvedInUnfinishedPlayoff(connection, tournament,
                                                            Collections.singletonList(teamNumber))) {
              SessionAttributes.appendToMessage(session, "<p name='error' class='error'>Selected team ("
                  + teamNumber
                  + ") is not involved in an unfinished head to head bracket. Please double check that the head to head brackets were properly initialized"
                  + " If you were intending to double check a score, you probably just forgot to check"
                  + " the box for doing so.</p>");
              response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
              return false;
            } else if (!Queries.didTeamReachPlayoffRound(connection, nextRunNumber, teamNumber)) {
              SessionAttributes.appendToMessage(session,
                                                "<p name='error' class='error' id='error-not-advanced'>Selected team has not advanced to the next head to head round.</p>");
              response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
              return false;
            }
          }
          lRunNumber = nextRunNumber;
        }
        pageContext.setAttribute("lRunNumber", lRunNumber);

        regularMatchPlay = lRunNumber <= numSeedingRounds;

        final String roundText;
        if (runningHeadToHead
            && !regularMatchPlay) {
          final String division = Playoff.getPlayoffDivision(connection, tournament, teamNumber, lRunNumber);
          final int playoffRun = Playoff.getPlayoffRound(connection, tournament, division, lRunNumber);
          roundText = "Playoff&nbsp;Round&nbsp;"
              + playoffRun;
        } else {
          roundText = "Run&nbsp;Number&nbsp;"
              + lRunNumber;
        }
        pageContext.setAttribute("roundText", roundText);

        // check if this is the last run a team has completed
        final int maxRunCompleted = Queries.getMaxRunNumber(connection, teamNumber);
        pageContext.setAttribute("isLastRun", Boolean.valueOf(lRunNumber == maxRunCompleted));

        // check if the score being edited is a bye
        pageContext.setAttribute("isBye",
                                 Boolean.valueOf(Queries.isBye(connection, tournament, teamNumber, lRunNumber)));
        pageContext.setAttribute("isNoShow",
                                 Boolean.valueOf(Queries.isNoShow(connection, tournament, teamNumber, lRunNumber)));

        // check if previous run is verified
        if (lRunNumber > 1) {
          previousVerified = Queries.isVerified(connection, tournament, teamNumber, lRunNumber
              - 1);
        } else {
          previousVerified = true;
        }
      } catch (final ParseException pe) {
        throw new FLLInternalException(pe);
      } catch (final SQLException e) {
        throw new FLLInternalException(e);
      } catch (final IOException e) {
        throw new FLLInternalException(e);
      }

    } // not practice

    pageContext.setAttribute("previousVerified", previousVerified);

    if (practice
        || regularMatchPlay) {
      if (edit) {
        pageContext.setAttribute("top_info_color", "yellow");
      } else {
        pageContext.setAttribute("top_info_color", "#e0e0e0");
      }
    } else {
      pageContext.setAttribute("top_info_color", "#00ff00");
    }

    if (edit) {
      pageContext.setAttribute("body_background", "yellow");
    } else {
      pageContext.setAttribute("body_background", "transparent");
    }

    return true;
  }

  /**
   * Calls either {@link #generateInitForNewScore(JspWriter, ServletContext)} or
   * {@link #generateInitForScoreEdit(JspWriter, ServletContext, PageContext)}
   * based on the value of the EditFlag.
   *
   * @param writer where to write HTML
   * @param application application context
   * @throws IOException if there is an error writing to the output stream
   * @throws SQLException if there is a problem talking to the database
   * @param pageContext used to get the edit flag
   */
  public static void generateInit(final JspWriter writer,
                                  final ServletContext application,
                                  final PageContext pageContext)
      throws IOException, SQLException {

    setupRangeSliders(writer, application);

    final boolean editFlag = (Boolean) pageContext.getAttribute("EditFlag");
    if (editFlag) {
      generateInitForScoreEdit(writer, application, pageContext);
    } else {
      generateInitForNewScore(writer, application);
    }
  }

  private static void setupRangeSliders(final JspWriter writer,
                                        final ServletContext application)
      throws IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = description.getPerformance();

    for (final AbstractGoal element : performanceElement.getAllGoals()) {
      if (!element.isComputed()) {
        final Goal goal = (Goal) element;

        final double min = goal.getMin();
        final double max = goal.getMax();
        final double range = max
            - min
            + 1;
        final String name = goal.getName();

        if (!goal.isYesNo()
            && !goal.isEnumerated()
            && range <= SLIDER_RANGE_MAX) {
          writer.println(String.format("  document.scoreEntry.%s.oninput = function() {", getSliderName(name)));
          writer.println(String.format("    document.scoreEntry.%s.innerHTML = this.value;", name));
          writer.println(String.format("    %s(this.value);", getSetMethodName(name)));
          writer.println("  };");
        } // use slider
      } // not computed
    } // foreach goal
  }

  /**
   * Generate the isConsistent method for the goals in the performance element
   * of document.
   * 
   * @param writer where to write the HTML
   * @param application used to get the challenge description
   * @throws IOException if there is an error writing to {@code writer}
   */
  public static void generateIsConsistent(final JspWriter writer,
                                          final ServletContext application)
      throws IOException {
    final ChallengeDescription descriptor = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = descriptor.getPerformance();

    writer.println("function isConsistent() {");

    // check all goal min and max values
    for (final AbstractGoal element : performanceElement.getAllGoals()) {
      if (!element.isComputed()) {
        final Goal goal = (Goal) element;
        final String name = goal.getName();
        final double min = goal.getMin();
        final double max = goal.getMax();

        writer.println("  //"
            + name);
        if (goal.isEnumerated()) {
          // enumerated
          writer.println("  // nothing to check");
        } else {
          final String rawVarName = getVarNameForRawScore(name);
          writer.println("  if("
              + rawVarName
              + " < "
              + min
              + " || "
              + rawVarName
              + " > "
              + max
              + ") {");
          writer.println("    return false;");
          writer.println("  }");
        }
        writer.println();
      } // !computed
    } // foreach goal

    writer.println("  return true;");
    writer.println("}");
  }

  /**
   * Genrate the increment methods and variable declarations for the goals in
   * the performance element of document. Generate the methods to compute goals
   * as well.
   *
   * @param writer where to write the text
   * @param application application context
   * @param request used to get information about tablet entry
   * @param pageContext used to get the edit flag state
   * @param session used to determine when running on a tablet
   * @throws IOException if there is an error writing to {@code writer}
   */
  public static void generateIncrementMethods(final Writer writer,
                                              final ServletContext application,
                                              final HttpServletRequest request,
                                              final HttpSession session,
                                              final PageContext pageContext)
      throws IOException {
    final boolean tabletEntry = isTabletEntry(request, session);

    final boolean editFlag = (Boolean) pageContext.getAttribute("EditFlag");

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = description.getPerformance();
    final Formatter formatter = new Formatter(writer);

    for (final AbstractGoal goal : performanceElement.getAllGoals()) {
      if (goal.isComputed()) {
        // generate the method to update the computed goal variables
        final String goalName = goal.getName();
        formatter.format("// %s%n", goalName);
        formatter.format("var %s;%n", getVarNameForComputedScore(goalName));
        generateComputedGoalFunction(formatter, (ComputedGoal) goal);
      } else {
        final String name = goal.getName();
        final double min = goal.getMin();
        final double max = goal.getMax();
        final String rawVarName = getVarNameForRawScore(name);

        formatter.format("// %s%n", name);
        formatter.format("var %s;%n", rawVarName);
        formatter.format("var %s;%n", getVarNameForComputedScore(name));

        // set method
        formatter.format("function %s(newValue) {%n", getSetMethodName(name));
        formatter.format("  var temp = %s;%n", rawVarName);
        formatter.format("  %s = newValue;%n", rawVarName);
        formatter.format("  if(!isConsistent()) {%n");
        formatter.format("    %s = temp;%n", rawVarName);
        formatter.format("  }%n");
        formatter.format("  refresh();%n");
        formatter.format("}%n");

        // check input method
        formatter.format("function %s() {%n", getCheckMethodName(name));
        formatter.format("  var str = document.scoreEntry.%s.value;%n", name);
        if (ScoreType.FLOAT == goal.getScoreType()) {
          formatter.format("  var num = parseFloat(str);%n");
        } else {
          formatter.format("  var num = parseInt(str);%n");
        }
        formatter.format("  if(!isNaN(num)) {%n");
        formatter.format("    %s(num);%n", getSetMethodName(name));
        formatter.format("  }%n");
        formatter.format("  refresh();%n");
        formatter.format("}%n");

        if (!goal.isEnumerated()
            && !goal.isYesNo()) {
          formatter.format("function %s(increment) {%n", getIncrementMethodName(name));
          formatter.format("  var temp = %s%n", rawVarName);
          formatter.format("  %s += increment;%n", rawVarName);
          formatter.format("  if(%s > %s) {%n", rawVarName, max);
          formatter.format("    %s = %s;%n", rawVarName, max);
          formatter.format("   }%n");
          formatter.format("  if(%s < %s) {%n", rawVarName, min);
          formatter.format("    %s = %s;%n", rawVarName, min);
          formatter.format("   }%n");
          formatter.format("  if(!isConsistent()) {%n");
          formatter.format("    %s = temp;%n", rawVarName);
          formatter.format("  }%n");
          formatter.format("  refresh();%n");
          formatter.format("}%n");
        }

        formatter.format("%n%n");
      }
    } // end for each goal

    // method for double-check field
    formatter.format("// Verified %n");
    formatter.format("var Verified;%n");
    formatter.format("function %s(newValue) {%n", getSetMethodName("Verified"));
    formatter.format("  Verified = newValue;%n");

    if (!editFlag
        && !tabletEntry) {
      formatter.format("  if (newValue == 1) {");
      formatter.format("    $('#verification-warning').show();");
      formatter.format("  } else if (newValue == 0) {");
      formatter.format("    $('#verification-warning').hide();");
      formatter.format("  }");
    }

    formatter.format("  refresh();%n");
    formatter.format("}%n%n%n");
  }

  /**
   * Generate the body of the refresh function.
   * 
   * @param writer where to write the HTML
   * @param application used to get the challenge descrption
   * @throws IOException if there is an error writing to {@code writer}
   */
  public static void generateRefreshBody(final Writer writer,
                                         final ServletContext application)
      throws IOException {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering generateRefreshBody");
    }

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter formatter = new Formatter(writer);

    final PerformanceScoreCategory performanceElement = description.getPerformance();

    // output the assignments of each element
    for (final AbstractGoal agoal : performanceElement.getAllGoals()) {
      if (agoal.isComputed()) {
        // output calls to the computed goal methods

        final String goalName = agoal.getName();
        final String computedVarName = getVarNameForComputedScore(goalName);
        formatter.format("%s();%n", getComputedMethodName(goalName));

        // add to the total score
        formatter.format("score += %s;%n", computedVarName);

        formatter.format("%n");

      } else {
        final Goal goal = (Goal) agoal;
        final String name = goal.getName();
        final double multiplier = goal.getMultiplier();
        final double min = goal.getMin();
        final double max = goal.getMax();
        final double range = max
            - min
            + 1;
        final String rawVarName = getVarNameForRawScore(name);
        final String computedVarName = getVarNameForComputedScore(name);

        if (LOG.isTraceEnabled()) {
          LOG.trace("name: "
              + name);
          LOG.trace("multiplier: "
              + multiplier);
          LOG.trace("min: "
              + min);
          LOG.trace("max: "
              + max);
        }

        formatter.format("// %s%n", name);

        if (goal.isEnumerated()) {
          // enumerated
          final List<EnumeratedValue> posValues = agoal.getSortedValues();
          for (int valueIdx = 0; valueIdx < posValues.size(); valueIdx++) {
            final EnumeratedValue valueEle = posValues.get(valueIdx);

            final String value = valueEle.getValue();
            final double valueScore = valueEle.getScore();
            if (valueIdx > 0) {
              formatter.format("} else ");
            }

            formatter.format("if(%s == \"%s\") {%n", rawVarName, value);
            formatter.format("  document.scoreEntry.%s[%d].checked = true;%n", name, valueIdx);
            formatter.format("  %s = %f * %s;%n", computedVarName, valueScore, multiplier);

            formatter.format("  document.scoreEntry.%s.value = '%s'%n", getElementNameForYesNoDisplay(name),
                             value.toUpperCase());
          } // foreach value
          formatter.format("}%n");
        } else if (goal.isYesNo()) {
          // set the radio button to match the gbl variable
          formatter.format("if(%s == 0) {%n", rawVarName);
          // 0/1 needs to match the order of the buttons generated in
          // generateYesNoButtons
          formatter.format("  document.scoreEntry.%s[0].checked = true%n", name);
          formatter.format("  document.scoreEntry.%s_radioValue.value = 'NO'%n", name);
          formatter.format("} else {%n");
          formatter.format("  document.scoreEntry.%s[1].checked = true%n", name);
          formatter.format("  document.scoreEntry.%s_radioValue.value = 'YES'%n", name);
          formatter.format("}%n");
          formatter.format("%s = %s * %s;%n", computedVarName, rawVarName, multiplier);
        } else {
          // set the count form element
          formatter.format("document.scoreEntry.%s.value = %s;%n", name, rawVarName);
          formatter.format("%s = %s * %s;%n", computedVarName, rawVarName, multiplier);

          // link slider to count column
          if (range <= SLIDER_RANGE_MAX) {
            formatter.format("document.scoreEntry.%s.value = %s;%n", getSliderName(name), rawVarName);
          }
        }

        // add to the total score
        formatter.format("score += %s;%n", computedVarName);

        // set the score form element
        formatter.format("document.scoreEntry.score_%s.value = %s;%n", name, computedVarName);
        formatter.format("%n");
      }
    } // end foreach goal

    // set the radio buttons for score verification
    formatter.format("if(Verified == 0) {%n");
    // order of elements needs to match generateYesNoButtons
    formatter.format("  document.scoreEntry.Verified[0].checked = true%n"); // NO
    formatter.format("} else {%n");
    formatter.format("  document.scoreEntry.Verified[1].checked = true%n"); // YES
    formatter.format("}%n");

    if (LOG.isTraceEnabled()) {
      LOG.trace("Exiting generateRefreshBody");
    }

  }

  /**
   * Output the body for the check_restrictions method.
   *
   * @param writer where to write
   * @param application used to get the challenge description
   * @throws IOException on an error writing to {@code writer}
   */
  public static void generateCheckRestrictionsBody(final Writer writer,
                                                   final ServletContext application)
      throws IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter formatter = new Formatter(writer);

    final PerformanceScoreCategory performanceElement = description.getPerformance();

    final Collection<Restriction> restrictions = performanceElement.getRestrictions();

    // variables to track which goals need to be highlighted
    for (final AbstractGoal goal : performanceElement.getAllGoals()) {
      if (!goal.isComputed()) {
        formatter.format("  var %s_error = false;%n", goal.getName());
      }
    }

    // output actual check of restriction
    int restrictIdx = 0;
    for (final Restriction restrictEle : restrictions) {
      final double lowerBound = restrictEle.getLowerBound();
      final double upperBound = restrictEle.getUpperBound();
      final String message = restrictEle.getMessage();

      final String polyString = polyToString(restrictEle);
      final String restrictValStr = String.format("restriction_%d_value", restrictIdx);
      formatter.format("  var %s = %s;%n", restrictValStr, polyString);
      formatter.format("  if(%s > %f || %s < %f) {%n", restrictValStr, upperBound, restrictValStr, lowerBound);

      // add error text to score-errors div
      formatter.format("    $('#score-errors').append('<div>%s</div>');%n", message);
      formatter.format("    error_found = true;%n");

      for (final GoalRef ref : restrictEle.getReferencedGoals()) {
        formatter.format("    %s_error = true;%n", ref.getGoalName());
      }
      formatter.format("  }%n");
      ++restrictIdx;
    }

    // highlight or clear errors
    for (final AbstractGoal goal : performanceElement.getAllGoals()) {
      if (!goal.isComputed()) {
        formatter.format("if(%s_error) {%n", goal.getName());
        formatter.format("  const ele = document.getElementById(\"%s_row\");%n", goal.getName());
        formatter.format("  if(!ele.classList.contains(\"restriction-error\")) {%n");
        formatter.format("    ele.classList.add(\"restriction-error\");%n");
        formatter.format("  }%n");
        formatter.format("} else {%n");
        formatter.format("  const ele = document.getElementById(\"%s_row\");%n", goal.getName());
        formatter.format("  if(ele.classList.contains(\"restriction-error\")) {%n");
        formatter.format("    ele.classList.remove(\"restriction-error\");%n");
        formatter.format("  }%n");
        formatter.format("}%n");
      }
    }

  }

  /**
   * Generate init for new scores, initializes all variables to their default
   * values.
   * 
   * @param writer where to write the HTML
   * @param application used to get the challenge description
   * @throws IOException if there is an error writing to {@code writer}
   */
  public static void generateInitForNewScore(final JspWriter writer,
                                             final ServletContext application)
      throws IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = description.getPerformance();

    for (final AbstractGoal element : performanceElement.getAllGoals()) {
      if (!element.isComputed()) {
        final Goal goal = (Goal) element;
        final String name = goal.getName();
        final double initialValue = goal.getInitialValue();
        if (goal.isEnumerated()) {
          // find score that matches initialValue or is min
          final List<EnumeratedValue> values = goal.getSortedValues();
          boolean found = false;
          for (final EnumeratedValue valueEle : values) {
            final String value = valueEle.getValue();
            final double valueScore = valueEle.getScore();
            if (FP.equals(valueScore, initialValue, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
              writer.println("  "
                  + getVarNameForRawScore(name)
                  + " = \""
                  + value
                  + "\";");
              found = true;
            }
          }
          if (!found) {
            // fall back to just using the first enum value
            LOG.warn(String.format("Initial value for enum goal '%s' does not match the score of any enum value",
                                   name));
            writer.println("  "
                + getVarNameForRawScore(name)
                + " = \""
                + values.get(0).getValue()
                + "\";");
          }

        } else {
          writer.println("  "
              + getVarNameForRawScore(name)
              + " = "
              + initialValue
              + ";");
        }
      } // !computed
    } // foreach goal

    writer.println("  Verified = 0;");
  }

  /**
   * Generates the portion of the score entry form where the user checks whether
   * the score has been double-checked or not.
   * 
   * @param writer where to write the HTML
   * @param request used to get information about tablet entry
   * @param session used to get information about scoring on a tablet
   * @throws IOException if there is an error writing to {@code writer}
   */
  public static void generateVerificationInput(final JspWriter writer,
                                               final HttpServletRequest request,
                                               final HttpSession session)
      throws IOException {
    final boolean tabletEntry = isTabletEntry(request, session);

    writer.println("<!-- Score Verification -->");
    writer.println("    <tr>");
    if (!tabletEntry) {
      writer.println("      <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='4' color='red'>Score entry verified:</font></td>");
    } else {
      writer.println("<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='4' color='red'>Team Agrees with the score entry:</font></td>");
    }
    writer.println("      <td><table border='0' cellpadding='0' cellspacing='0' width='150'><tr align='center'>");
    generateYesNoButtons("Verified", writer);
    writer.println("      </tr></table></td>");
    writer.println("      <td colspan='2'>&nbsp;</td>");
    writer.println("    </tr>");
  }

  /**
   * Generate the score entry form.
   * 
   * @param writer where to write the HTML
   * @param application used to get the challenge description
   * @throws IOException if there is an error writing to {@code writer}
   */
  public static void generateScoreEntry(final JspWriter writer,
                                        final ServletContext application)
      throws IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    final PerformanceScoreCategory performanceElement = description.getPerformance();
    for (final GoalElement goalEle : performanceElement.getGoalElements()) {
      if (goalEle.isGoalGroup()) {
        generateGoalGroup(writer, (GoalGroup) goalEle);
      } else if (goalEle.isGoal()) {
        generateGoalEntry(writer, (AbstractGoal) goalEle);
      } else {
        throw new FLLInternalException("Unexpected goal element type: "
            + goalEle.getClass());
      }
    }
  }

  private static void generateGoalGroup(final JspWriter writer,
                                        final GoalGroup group)
      throws IOException {
    final String category = group.getTitleAndDescription();

    writer.println("<tr><td colspan='4' class='goal-group-spacer'>&nbsp;</td></tr>");
    if (!StringUtils.isBlank(category)) {
      writer.println("<tr>");
      writer.println("<td colspan='4' class='center truncate'><b>"
          + category
          + "</b></td>");
      writer.println("</tr>");
    }

    for (final AbstractGoal goal : group.getGoals()) {
      generateGoalEntry(writer, goal);
    }

  }

  private static void generateGoalEntry(final JspWriter writer,
                                        final AbstractGoal goal)
      throws IOException {
    final String name = goal.getName();
    final String title = goal.getTitle();

    writer.println("<!-- "
        + name
        + " -->");
    writer.println("<tr id='"
        + name
        + "_row'>");
    writer.println("  <td class='goal-title'>");
    writer.println("    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
        + title
        + ":");
    writer.println("  </td>");

    if (goal.isComputed()) {
      writer.println("  <td colspan='2' align='center'><b>Computed Goal</b></td>");
    } else {
      if (goal.isEnumerated()) {
        // enumerated
        generateEnumeratedGoalButtons(goal, name, writer);
      } else {
        generateSimpleGoalButtons(goal, name, writer);
      } // end simple goal
    } // goal

    // computed score
    writer.println("  <td align='right' class='score-cell'>");
    writer.println("    <input type='text' name='score_"
        + name
        + "' size='3' align='right' readonly tabindex='-1'>");
    writer.println("  </td>");

    writer.println("</tr>");
    writer.println("<!-- end "
        + name
        + " -->");
    writer.newLine();
  }

  private static final int TICK_WIDTH = 2;

  private static final int TICK_HEIGHT = 15;

  private static void outputTickMark(final JspWriter writer,
                                     final double xPosition)
      throws IOException {
    writer.println(String.format("<rect x=\"%.2f%%\" y=\"0\" width=\"%d\" height=\"100%%\"></rect>", xPosition,
                                 TICK_WIDTH));
  }

  /**
   * Generate a the buttons for a simple goal.
   */
  private static void generateSimpleGoalButtons(final AbstractGoal goalEle,
                                                final String name,
                                                final JspWriter writer)
      throws IOException {

    // edit cell
    writer.println("  <td>");
    final double min = goalEle.getMin();
    final double max = goalEle.getMax();
    final double range = max
        - min
        + 1;
    if (goalEle.isYesNo()) {
      generateYesNoButtons(name, writer);
    } else if (range <= SLIDER_RANGE_MAX) {
      // use slider
      writer.println(String.format("<input class='range' type='range' min='%d' max='%d' class='slider' id='%s' />",
                                   (int) min, (int) max, getSliderName(name)));
      // tick marks
      final int numInternalTicks = (int) (max
          - min
          - 1);
      final double increment = 100.0
          / (max
              - min);

      writer.println(String.format("<svg role=\"presentation\" width=\"100%%\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">",
                                   TICK_HEIGHT));

      outputTickMark(writer, 0);

      for (int i = 0; i < numInternalTicks; ++i) {
        final double xPosition = increment
            * (i
                + 1);
        outputTickMark(writer, xPosition);
      }

      // last tick is at 99% otherwise it doesn't show
      outputTickMark(writer, 99);
      writer.println("</svg>");

    } else {
      // use buttons
      writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='150'>");
      writer.println("      <tr align='center'>");

      final int maxRangeIncrement = (int) Math.floor(range);

      generateIncDecButton(name, -1
          * maxRangeIncrement, writer);

      if (FP.greaterThanOrEqual(range, 10, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
        generateIncDecButton(name, -5, writer);
      } else if (FP.greaterThanOrEqual(range, 5, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
        generateIncDecButton(name, -3, writer);
      }

      // -1
      generateIncDecButton(name, -1, writer);

      // +1
      generateIncDecButton(name, 1, writer);

      if (FP.greaterThanOrEqual(range, 10, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
        generateIncDecButton(name, 5, writer);
      } else if (FP.greaterThanOrEqual(range, 5, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
        generateIncDecButton(name, 3, writer);
      }

      generateIncDecButton(name, maxRangeIncrement, writer);

      writer.println("       </tr>");
      writer.println("    </table>");
    }
    writer.println("  </td>");

    // count cell
    writer.println("  <td align='right'>");
    if (goalEle.isYesNo()) {
      writer.println(String.format("    <input type='text' name='%s_radioValue' size='3' align='right' readonly tabindex='-1'>",
                                   name));
    } else {
      // allow these to be editable
      writer.println(String.format("    <input type='text' name='%s' size='3' align='right' onChange='%s()'>", name,
                                   getCheckMethodName(name)));
    }
    writer.println("  </td>");
  }

  /**
   * Generate an increment or decrement button for goal name with
   * increment/decrement increment.
   */
  private static void generateIncDecButton(final String name,
                                           final int increment,
                                           final JspWriter writer)
      throws IOException {
    // generate buttons with calls to increment<name>
    final String buttonName = (increment < 0 ? "" : "+")
        + String.valueOf(increment);
    final String buttonID = getIncDecButtonID(name, increment);
    writer.println("        <td>");
    writer.println("          <input id='"
        + buttonID
        + "' type='button' value='"
        + buttonName
        + "' onclick='"
        + getIncrementMethodName(name)
        + "("
        + increment
        + ")'>");
    writer.println("        </td>");
  }

  /**
   * Get the id of an increment or decrement button in the generated page.
   * 
   * @param name goal name
   * @param increment the amount of the increment
   * @return the id for the button
   */
  public static String getIncDecButtonID(final String name,
                                         final int increment) {
    final String incdec = (increment < 0 ? "dec" : "inc");
    return incdec
        + "_"
        + name
        + "_"
        + String.valueOf(Math.abs(increment));
  }

  /**
   * Generate yes and no buttons for goal name.
   */
  private static void generateYesNoButtons(final String name,
                                           final JspWriter writer)
      throws IOException {
    // generate radio buttons with calls to set<name>

    // order of yes/no buttons needs to match order in generateRefreshBody
    writer.println(String.format("        <label class='y-n-button'>"));
    writer.println(String.format("          <input type='radio' id='%s_no' name='%s' value='0' onclick='%s(0)'>", name,
                                 name, getSetMethodName(name)));
    writer.println(String.format("        <span id='%s_no_span'>No</span>", name));
    writer.println(String.format("        </label>"));
    writer.println(String.format("        <label class='y-n-button'>"));
    writer.println(String.format("          <input type='radio' id='%s_yes' name='%s' value='1' onclick='%s(1)'>", name,
                                 name, getSetMethodName(name)));
    writer.println(String.format("        <span id='%s_yes_span'>Yes</span>", name));
    writer.println(String.format("        </label>"));
  }

  /**
   * Generate the initial assignment of the global variables for editing a
   * team's score.
   * 
   * @param writer where to write the HTML
   * @param application used to get the challenge description
   * @param page used to get page variables
   * @throws SQLException on a database error
   * @throws IOException if there is a problem writing to {@code writer}
   */
  public static void generateInitForScoreEdit(final JspWriter writer,
                                              final ServletContext application,
                                              final PageContext page)
      throws SQLException, IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Team team = (Team) page.getAttribute("team");
    final int teamNumber = team.getTeamNumber();
    final int runNumber = ((Number) page.getAttribute("lRunNumber")).intValue();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection();
        PreparedStatement prep = connection.prepareStatement("SELECT * from Performance"
            + " WHERE TeamNumber = ?" //
            + " AND RunNumber = ?"//
            + " AND Tournament = ?")) {
      final int tournament = Queries.getCurrentTournament(connection);

      prep.setInt(1, teamNumber);
      prep.setInt(2, runNumber);
      prep.setInt(3, tournament);

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final PerformanceScoreCategory performanceElement = description.getPerformance();
          for (final AbstractGoal element : performanceElement.getAllGoals()) {
            if (!element.isComputed()) {
              final Goal goal = (Goal) element;
              final String name = goal.getName();
              final String rawVarName = getVarNameForRawScore(name);

              if (goal.isEnumerated()) {
                // enumerated
                final String storedValue = rs.getString(name);
                boolean found = false;
                for (final EnumeratedValue valueElement : goal.getSortedValues()) {
                  final String value = valueElement.getValue();
                  if (value.equals(storedValue)) {
                    writer.println("  "
                        + rawVarName
                        + " = \""
                        + value
                        + "\";");
                    found = true;
                  }
                }
                if (!found) {
                  throw new RuntimeException("Found enumerated value in the database that's not in the XML document, goal: "
                      + name
                      + " value: "
                      + storedValue);
                }
              } else {
                // just use the value that is stored in the database
                writer.println("  "
                    + rawVarName
                    + " = "
                    + rs.getString(name)
                    + ";");
              }
            } // !computed
          } // foreach goal
          // Always init the special double-check column
          writer.println("  Verified = "
              + rs.getBoolean("Verified")
              + ";");
        } else {
          throw new RuntimeException("Cannot find TeamNumber and RunNumber in Performance table"
              + " TeamNumber: "
              + teamNumber
              + " RunNumber: "
              + runNumber);
        }
      }
    }
  }

  private static void generateEnumeratedGoalButtons(final AbstractGoal goal,
                                                    final String goalName,
                                                    final JspWriter writer)
      throws IOException {

    writer.println("  <td>");
    for (final EnumeratedValue valueEle : goal.getSortedValues()) {
      final String valueTitle = valueEle.getTitle();
      final String value = valueEle.getValue();
      final String id = getIDForEnumRadio(goalName, value);

      writer.println("      <label class='enum-button'>");
      writer.println(String.format("          <input type='radio' name='%s' value='%s' id='%s' onclick='%s(\"%s\")'/>",
                                   goalName, value, id, getSetMethodName(goalName), value));
      writer.println(String.format("        <span id='%s_span'>", id));
      writer.println("          "
          + valueTitle);
      writer.println("        </span>");
      writer.println("      </label>");
    }
    writer.println("  </td>");

    writer.println("  <td align='right'>");
    writer.println("    <input type='text' name='"
        + goalName
        + "_radioValue' size='10' align='right' readonly tabindex='-1'/>");
    writer.println("  </td>");

  }

  /**
   * Name of the element that stores the textual value of the specified yes/no
   * value.
   * 
   * @param goalName the goal name
   * @return id to use for the yes/no radio buttons
   */
  public static String getElementNameForYesNoDisplay(final String goalName) {
    return goalName
        + "_radioValue";
  }

  /**
   * The ID assigned to the radio button for a particular value of an enumerated
   * goal.
   * 
   * @param goalName the goal name
   * @param value the enumerated value
   * @return id to use for the enum radio button
   */
  public static String getIDForEnumRadio(final String goalName,
                                         final String value) {
    return goalName
        + "_"
        + value;
  }

  /**
   * The name of the javascript variable that represents the raw score for a
   * goal
   *
   * @param goalName
   * @return
   */
  private static String getVarNameForComputedScore(final String goalName) {
    return goalName
        + "_computed";
  }

  /**
   * The name of the javascript variable that represents the computed score for
   * a goal
   */
  private static String getVarNameForRawScore(final String goalName) {
    return goalName
        + "_raw";
  }

  /**
   * The name of the slider for the specified goal.
   */
  private static String getSliderName(final String goalName) {
    return goalName
        + "_slider";
  }

  /**
   * The name of method that increments scores for the specified goal.
   */
  private static String getIncrementMethodName(final String goalName) {
    return "increment_"
        + goalName;
  }

  /**
   * The name of the method that sets scores for the specified goal.
   */
  private static String getSetMethodName(final String goalName) {
    return "set_"
        + goalName;
  }

  /**
   * The name of the method that checks input for the specified goal.
   */
  private static String getCheckMethodName(final String goalName) {
    return "check_"
        + goalName;
  }

  /**
   * The name of the method that computes scores for the specified goal.
   */
  private static String getComputedMethodName(final String goalName) {
    return "compute_"
        + goalName;
  }

  private static void generateComputedGoalFunction(final Formatter formatter,
                                                   final ComputedGoal compGoal) {
    final String goalName = compGoal.getName();

    formatter.format("function %s() {%n", getComputedMethodName(goalName));

    for (final Variable var : compGoal.getVariables()) {
      final String varName = getComputedGoalLocalVarName(var.getName());
      final String varValue = polyToString(var);
      formatter.format("var %s = %s;%n", varName, varValue);
    }

    generateSwitch(formatter, compGoal.getSwitch(), goalName, INDENT_LEVEL);

    formatter.format("%sdocument.scoreEntry.score_%s.value = %s;%n", generateIndentSpace(INDENT_LEVEL), goalName,
                     getVarNameForComputedScore(goalName));
    formatter.format("}%n");
  }

  /**
   * Get the name of a local variable inside a computed goal function that
   * stores the specified variable value.
   */
  private static String getComputedGoalLocalVarName(final String varname) {
    return varname;
  }

  private static void generateSwitch(final Formatter formatter,
                                     final SwitchStatement ele,
                                     final String goalName,
                                     final int indent) {
    // keep track if there are any case statements
    boolean first = true;
    final boolean hasCase = !ele.getCases().isEmpty();

    for (final CaseStatement childEle : ele.getCases()) {
      final AbstractConditionStatement condition = childEle.getCondition();
      final String ifPrefix;
      if (!first) {
        ifPrefix = " else ";
      } else {
        first = false;
        ifPrefix = generateIndentSpace(indent);
      }
      generateCondition(formatter, ifPrefix, condition);

      formatter.format(" {%n");
      final CaseStatementResult caseResult = childEle.getResult();
      if (caseResult instanceof ComplexPolynomial) {
        final ComplexPolynomial resultPoly = (ComplexPolynomial) caseResult;
        formatter.format("%s%s = %s;%n", generateIndentSpace(indent
            + INDENT_LEVEL), getVarNameForComputedScore(goalName),
                         null == resultPoly ? "NULL" : polyToString(resultPoly));
      } else if (caseResult instanceof SwitchStatement) {
        final SwitchStatement resultSwitch = (SwitchStatement) caseResult;
        generateSwitch(formatter, resultSwitch, goalName, indent
            + INDENT_LEVEL);
      } else {
        throw new FLLInternalException("Expected case statement to have result poly or result switch, but found neight");
      }
      formatter.format("%s}", generateIndentSpace(indent));
    }

    if (hasCase) {
      formatter.format(" else {%n");
    }
    formatter.format("%s%s = %s;%n", generateIndentSpace(indent
        + INDENT_LEVEL), getVarNameForComputedScore(goalName), polyToString(ele.getDefaultCase()));
    if (hasCase) {
      formatter.format("%s}%n", generateIndentSpace(indent));
    }
  }

  /**
   * Convert a polynomial to a string. Handles both {@link BasicPolynomial} and
   * {@link ComplexPolynomial}.
   *
   * @param poly the polynomial
   * @return the string that represents the polynomial
   */
  private static String polyToString(final BasicPolynomial poly) {
    final Formatter formatter = new Formatter();

    boolean first = true;
    for (final Term term : poly.getTerms()) {
      if (!first) {
        formatter.format(" + ");
      } else {
        first = false;
      }

      final double coefficient = term.getCoefficient();

      final Formatter termFormatter = new Formatter();
      termFormatter.format("%f", coefficient);

      for (final GoalRef goalRef : term.getGoals()) {
        final String goal = goalRef.getGoalName();
        final GoalScoreType scoreType = goalRef.getScoreType();
        final String varName;
        switch (scoreType) {
        case RAW:
          varName = getVarNameForRawScore(goal);
          break;
        case COMPUTED:
          varName = getVarNameForComputedScore(goal);
          break;
        default:
          throw new RuntimeException("Expected 'raw' or 'computed', but found: "
              + scoreType);
        }
        termFormatter.format("* %s", varName);
      }

      for (final VariableRef varRef : term.getVariables()) {
        final String var = varRef.getVariableName();
        final String varName = getComputedGoalLocalVarName(var);
        termFormatter.format("* %s", varName);
      }

      formatter.format("%s", termFormatter.toString());
    }

    final FloatingPointType floatingPoint = poly.getFloatingPoint();
    return applyFloatingPoint(formatter.toString(), floatingPoint);
  }

  /**
   * Make the appropriate modifications to <code>value</code> to reflect the
   * specified floating point handling
   *
   * @param value the expression
   * @param floatingPoint the floating point handling
   * @return value with the floating point handling applied
   */
  private static String applyFloatingPoint(final String value,
                                           final FloatingPointType floatingPoint) {
    switch (floatingPoint) {
    case DECIMAL:
      return value;
    case ROUND:
      return "Math.round("
          + value
          + ")";
    case TRUNCATE:
      return "parseInt("
          + value
          + ")";
    default:
      throw new RuntimeException("Unexpected floating point type: "
          + floatingPoint);
    }
  }

  private static String ineqToString(final InequalityComparison eq) {
    switch (eq) {
    case EQUAL_TO:
      return "==";
    case GREATER_THAN:
      return ">";
    case GREATER_THAN_OR_EQUAL:
      return ">=";
    case LESS_THAN:
      return "<";
    case LESS_THAN_OR_EQUAL:
      return "<=";
    case NOT_EQUAL_TO:
      return "!=";
    default:
      throw new FLLInternalException("Unknown inequality found: "
          + eq);
    }
  }

  /**
   * @param formatter where to write
   * @param ifPrefix what goes before "if", either spaces or "else"
   * @param ele condition statement
   */
  private static void generateCondition(final Formatter formatter,
                                        final String ifPrefix,
                                        final AbstractConditionStatement ele) {
    formatter.format("%sif(", ifPrefix);

    if (ele instanceof ConditionStatement) {
      final ConditionStatement cond = (ConditionStatement) ele;
      formatter.format("%s %s %s", polyToString(cond.getLeft()), ineqToString(ele.getComparison()),
                       polyToString(cond.getRight()));
    } else if (ele instanceof EnumConditionStatement) {
      final EnumConditionStatement cond = (EnumConditionStatement) ele;

      final StringValue left = cond.getLeft();
      final String leftStr;
      if (left.isGoalRef()) {
        leftStr = getVarNameForRawScore(left.getRawStringValue());
      } else {
        leftStr = "'"
            + left.getRawStringValue()
            + "'";
      }

      final StringValue right = cond.getRight();
      final String rightStr;
      if (right.isGoalRef()) {
        rightStr = getVarNameForRawScore(right.getRawStringValue());
      } else {
        rightStr = "'"
            + right.getRawStringValue()
            + "'";
      }

      formatter.format("%s %s %s", leftStr, ineqToString(ele.getComparison()), rightStr);
    } else {
      throw new FLLInternalException("Expecting ConditionStatement or EnumConditionStatement, but was"
          + ele.getClass().getName());
    }
    formatter.format(")");
  }

  private static String generateIndentSpace(final int indent) {
    final StringBuilder retval = new StringBuilder();
    for (int i = 0; i < indent; ++i) {
      retval.append(' ');
    }
    return retval.toString();
  }

}
