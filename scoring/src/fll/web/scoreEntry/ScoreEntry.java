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
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import fll.Team;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FP;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.xml.AbstractConditionStatement;
import fll.xml.AbstractGoal;
import fll.xml.BasicPolynomial;
import fll.xml.CaseStatement;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ComplexPolynomial;
import fll.xml.ComputedGoal;
import fll.xml.ConditionStatement;
import fll.xml.EnumConditionStatement;
import fll.xml.EnumeratedValue;
import fll.xml.FloatingPointType;
import fll.xml.Goal;
import fll.xml.GoalRef;
import fll.xml.GoalScoreType;
import fll.xml.InequalityComparison;
import fll.xml.PerformanceScoreCategory;
import fll.xml.Restriction;
import fll.xml.ScoreType;
import fll.xml.SwitchStatement;
import fll.xml.Term;
import fll.xml.Variable;
import fll.xml.VariableRef;

/**
 * Java code used in scoreEntry.jsp.
 */
public final class ScoreEntry {

  private static final Logger LOG = LogUtils.getLogger();

  /**
   * Number of spaces to indent code at each level.
   */
  private static final int INDENT_LEVEL = 2;

  private ScoreEntry() {
  }

  /**
   * Generate the isConsistent method for the goals in the performance element
   * of document.
   */
  public static void generateIsConsistent(final JspWriter writer,
                                          final ServletContext application)
      throws IOException {
    final ChallengeDescription descriptor = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = descriptor.getPerformance();

    writer.println("function isConsistent() {");

    // check all goal min and max values
    for (final AbstractGoal element : performanceElement.getGoals()) {
      if (!element.isComputed()) {
        final Goal goal = (Goal) element;
        final String name = goal.getName();
        final double min = goal.getMin();
        final double max = goal.getMax();

        writer.println("  <!-- "
            + name + " -->");
        if (goal.isEnumerated()) {
          // enumerated
          writer.println("  <!-- nothing to check -->");
        } else {
          final String rawVarName = getVarNameForRawScore(name);
          writer.println("  if("
              + rawVarName + " < " + min + " || " + rawVarName + " > " + max + ") {");
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
   * @throws ParseException
   */
  public static void generateIncrementMethods(final Writer writer,
                                              final ServletContext application)
      throws IOException, ParseException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = description.getPerformance();
    final Formatter formatter = new Formatter(writer);

    for (final AbstractGoal goal : performanceElement.getGoals()) {
      if (goal.isComputed()) {
        // generate the method to update the computed goal variables
        final String goalName = goal.getName();
        formatter.format("<!-- %s -->%n", goalName);
        formatter.format("var %s;%n", getVarNameForComputedScore(goalName));
        generateComputedGoalFunction(formatter, (ComputedGoal) goal);
      } else {
        final String name = goal.getName();
        final double min = goal.getMin();
        final double max = goal.getMax();
        final String rawVarName = getVarNameForRawScore(name);

        formatter.format("<!-- %s -->%n", name);
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
    formatter.format("<!-- Verified -->%n");
    formatter.format("var Verified;%n");
    formatter.format("function %s(newValue) {%n", getSetMethodName("Verified"));
    formatter.format("  Verified = newValue;%n");
    formatter.format("  if (newValue == 1 && document.getElementsByName('EditFlag').length == 0) {");
    formatter.format("    replaceText('verification_error', 'Are you sure this score has been Verified?');");
    formatter.format("  } else if (newValue == 0) {");
    formatter.format("    replaceText('verification_error', '');");
    formatter.format("  }");
    formatter.format("  refresh();%n");
    formatter.format("}%n%n%n");
  }

  /**
   * Generate the body of the refresh function
   */
  public static void generateRefreshBody(final Writer writer,
                                         final ServletContext application)
      throws ParseException, IOException {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering generateRefreshBody");
    }

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter formatter = new Formatter(writer);

    final PerformanceScoreCategory performanceElement = description.getPerformance();

    // output the assignments of each element
    for (final AbstractGoal agoal : performanceElement.getGoals()) {
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

        formatter.format("<!-- %s -->%n", name);

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
   * @throws IOException
   * @throws ParseException
   */
  public static void generateCheckRestrictionsBody(final Writer writer,
                                                   final ServletContext application)
      throws IOException, ParseException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter formatter = new Formatter(writer);

    final PerformanceScoreCategory performanceElement = description.getPerformance();

    final Collection<String> goalsWithRestrictions = new LinkedList<String>();
    final List<Restriction> restrictions = performanceElement.getRestrictions();

    // find out which goals are involved in restrictions
    for (final Restriction restrictEle : restrictions) {
      goalsWithRestrictions.addAll(getGoalsInRestriction(restrictEle));
    }

    // output variable declaration for each goal
    for (final String goalName : goalsWithRestrictions) {
      formatter.format("  var %s = \"\";%n", getElementIDForError(goalName));
    }

    // output actual check of restriction
    for (int restrictIdx = 0; restrictIdx < restrictions.size(); ++restrictIdx) {
      final Restriction restrictEle = restrictions.get(restrictIdx);
      final double lowerBound = restrictEle.getLowerBound();
      final double upperBound = restrictEle.getUpperBound();
      final String message = restrictEle.getMessage();

      final String polyString = polyToString(restrictEle);
      final String restrictValStr = String.format("restriction_%d_value", restrictIdx);
      formatter.format("  var %s = %s;%n", restrictValStr, polyString);
      formatter.format("  if(%s > %f || %s < %f) {%n", restrictValStr, upperBound, restrictValStr, lowerBound);
      // append error text to each error cell if the restriction is violated
      for (final String goalName : getGoalsInRestriction(restrictEle)) {
        final String errorId = getElementIDForError(goalName);
        formatter.format("    %s = %s + \" \" + \"%s\";%n", errorId, errorId, message);
      }
      formatter.format("    error_found = true;%n");
      formatter.format("  }%n");
    }

    // output text assignment for each goal involved in a restriction
    for (final String goalName : goalsWithRestrictions) {
      final String errorId = getElementIDForError(goalName);
      formatter.format("  replaceText(\"%s\", %s);%n", errorId, errorId);
      formatter.format("  if(%s.length > 0) {%n", errorId);
      formatter.format("    var el = document.getElementById(\"%s\");%n", errorId);
      formatter.format("  }%n");
      formatter.format("  replaceText(\"%s\", %s);%n", errorId, errorId);
      formatter.format("%n");
    }
  }

  private static Set<String> getGoalsInRestriction(final Restriction restrictEle) {
    final Set<String> goals = new HashSet<String>();
    for (final Term termEle : restrictEle.getTerms()) {
      for (final GoalRef goal : termEle.getGoals()) {
        final String goalName = goal.getGoalName();
        goals.add(goalName);
      }
    }
    return goals;
  }

  /**
   * Generate init for new scores, initializes all variables to their default
   * values.
   */
  public static void generateInitForNewScore(final JspWriter writer,
                                             final ServletContext application)
      throws IOException, ParseException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final PerformanceScoreCategory performanceElement = description.getPerformance();

    for (final AbstractGoal element : performanceElement.getGoals()) {
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
                  + getVarNameForRawScore(name) + " = \"" + value + "\";");
              found = true;
            }
          }
          if (!found) {
            // fall back to just using the first enum value
            LOG.warn(String.format("Initial value for enum goal '%s' does not match the score of any enum value",
                                   name));
            writer.println("  "
                + getVarNameForRawScore(name) + " = \"" + values.get(0).getValue() + "\";");
          }

        } else {
          writer.println("  "
              + getVarNameForRawScore(name) + " = " + initialValue + ";");
        }
      } // !computed
    } // foreach goal

    writer.println("  Verified = 0;");
  }

  /**
   * Generates the portion of the score entry form where the user checks whether
   * the score has been double-checked or not.
   */
  public static void generateVerificationInput(final JspWriter writer) throws IOException {
    writer.println("<!-- Score Verification -->");
    writer.println("    <tr>");
    writer.println("      <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='4' color='red'>Score entry verified:</font></td>");
    writer.println("      <td><table border='0' cellpadding='0' cellspacing='0' width='150'><tr align='center'>");
    generateYesNoButtons("Verified", writer);
    writer.println("      </tr></table></td>");
    writer.println("      <td colspan='2'>&nbsp;</td>");
    writer.println("      <td class='error' id='verification_error'>&nbsp;</td>");
    writer.println("    </tr>");
  }

  /**
   * Generate the score entry form.
   */
  public static void generateScoreEntry(final JspWriter writer,
                                        final ServletContext application)
      throws IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final Formatter formatter = new Formatter(writer);

    String prevCategory = null;
    final PerformanceScoreCategory performanceElement = description.getPerformance();
    for (final AbstractGoal goalEle : performanceElement.getGoals()) {
      final String name = goalEle.getName();
      final String title = goalEle.getTitle();
      final String category = goalEle.getCategory();

      try {

        if (!StringUtils.equals(prevCategory, category)) {
          writer.println("<tr><td colspan='5'>&nbsp;</td></tr>");
          if (!StringUtils.isEmpty(category)) {
            writer.println("<tr><td colspan='5' class='center'><b>"
                + category + "</b></td></tr>");
          }
        }

        writer.println("<!-- "
            + name + " -->");
        writer.println("<tr>");
        writer.println("  <td>");
        writer.println("    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='3'><b>"
            + title + ":</b></font>");
        writer.println("  </td>");

        if (goalEle.isComputed()) {
          writer.println("  <td colspan='2' align='center'><b>Computed Goal</b></td>");
        } else {
          if (goalEle.isEnumerated()) {
            // enumerated
            writer.println("  <td>");
            generateEnumeratedGoalButtons(goalEle, name, writer);
            writer.println("  </td>");
          } else {
            writer.println("  <td>");
            generateSimpleGoalButtons(goalEle, name, writer);
            writer.println("  </td>");
          } // end simple goal
        } // goal

        // computed score
        writer.println("  <td align='right'>");
        writer.println("    <input type='text' name='score_"
            + name + "' size='3' align='right' readonly tabindex='-1'>");
        writer.println("  </td>");

        // error message
        formatter.format("  <td class='error,score-error' id='error_%s'>&nbsp;</td>%n", name);

        writer.println("</tr>");
        writer.println("<!-- end "
            + name + " -->");
        writer.newLine();
      } catch (final ParseException pe) {
        throw new RuntimeException("FATAL: min/max not parsable for goal: "
            + name);
      }

      prevCategory = category;
    } // end foreach child of performance
  }

  /**
   * Generate a the buttons for a simple goal.
   * 
   * @param goalEle
   * @param name
   * @param writer
   * @throws IOException
   * @throws ParseException
   */
  private static void generateSimpleGoalButtons(final AbstractGoal goalEle,
                                                final String name,
                                                final JspWriter writer)
      throws IOException, ParseException {
    // start inc/dec buttons
    writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='150'>");
    writer.println("      <tr align='center'>");

    final double min = goalEle.getMin();
    final double max = goalEle.getMax();
    if (goalEle.isYesNo()) {
      generateYesNoButtons(name, writer);
    } else {
      final double range = max
          - min;
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

    }
    writer.println("       </tr>");
    writer.println("    </table>");
    writer.println("  </td>");
    // end inc/dec buttons

    // count
    writer.println("  <td align='right'>");
    if (FP.equals(0, min, ChallengeParser.INITIAL_VALUE_TOLERANCE)
        && FP.equals(1, max, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
      writer.println("    <input type='text' name='"
          + name + "_radioValue' size='3' align='right' readonly tabindex='-1'>");
    } else {
      // allow these to be editable
      writer.println("    <input type='text' name='"
          + name + "' size='3' align='right' onChange='" + getCheckMethodName(name) + "()'>");
    }
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
        + buttonID + "' type='button' value='" + buttonName + "' onclick='" + getIncrementMethodName(name) + "("
        + increment + ")'>");
    writer.println("        </td>");
  }

  public static String getIncDecButtonID(final String name,
                                         final int increment) {
    final String incdec = (increment < 0 ? "dec" : "inc");
    return incdec
        + "_" + name + "_" + String.valueOf(Math.abs(increment));
  }

  /**
   * Generate yes and no buttons for goal name.
   */
  private static void generateYesNoButtons(final String name,
                                           final JspWriter writer)
      throws IOException {
    // generate radio buttons with calls to set<name>

    // order of yes/no buttons needs to match order in generateRefreshBody
    writer.println("        <td>");
    writer.println("          <input type='radio' id='"
        + name + "_no' name='" + name + "' value='0' onclick='" + getSetMethodName(name) + "(0)'>");
    writer.println("          <label for='"
        + name + "_no'>No</label>");

    writer.println("          &nbsp;&nbsp;");

    writer.println("          <input type='radio' id='"
        + name + "_yes' name='" + name + "' value='1' onclick='" + getSetMethodName(name) + "(1)'>");
    writer.println("          <label for='"
        + name + "_yes'>Yes</label>");
    writer.println("        </td>");
  }

  /**
   * Generate the initial assignment of the global variables for editing a
   * team's score.
   */
  public static void generateInitForScoreEdit(final JspWriter writer,
                                              final ServletContext application,
                                              final HttpSession session)
      throws SQLException, IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final int teamNumber = SessionAttributes.getNonNullAttribute(session, "team", Team.class).getTeamNumber();
    final int runNumber = SessionAttributes.getNonNullAttribute(session, "lRunNumber", Number.class).intValue();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    PreparedStatement prep = null;
    ResultSet rs = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final int tournament = Queries.getCurrentTournament(connection);

      prep = connection.prepareStatement("SELECT * from Performance"
          + " WHERE TeamNumber = ?" //
          + " AND RunNumber = ?"//
          + " AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, runNumber);
      prep.setInt(3, tournament);

      rs = prep.executeQuery();
      if (rs.next()) {
        final PerformanceScoreCategory performanceElement = description.getPerformance();
        for (final AbstractGoal element : performanceElement.getGoals()) {
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
                      + rawVarName + " = \"" + value + "\";");
                  found = true;
                }
              }
              if (!found) {
                throw new RuntimeException("Found enumerated value in the database that's not in the XML document, goal: "
                    + name + " value: " + storedValue);
              }
            } else {
              // just use the value that is stored in the database
              writer.println("  "
                  + rawVarName + " = " + rs.getString(name) + ";");
            }
          } // !computed
        } // foreach goal
        // Always init the special double-check column
        writer.println("  Verified = "
            + rs.getBoolean("Verified") + ";");
      } else {
        throw new RuntimeException("Cannot find TeamNumber and RunNumber in Performance table"
            + " TeamNumber: " + teamNumber + " RunNumber: " + runNumber);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }
  }

  private static void generateEnumeratedGoalButtons(final AbstractGoal goal,
                                                    final String goalName,
                                                    final JspWriter writer)
      throws IOException, ParseException {
    writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='100%'>");

    for (final EnumeratedValue valueEle : goal.getSortedValues()) {
      final String valueTitle = valueEle.getTitle();
      final String value = valueEle.getValue();
      final String id = getIDForEnumRadio(goalName, value);
      writer.println("      <tr>");
      writer.println("        <td>");
      writer.println("          <input type='radio' name='"
          + goalName + "' value='" + value + "' id='" + id + "' ' onclick='" + getSetMethodName(goalName) + "(\""
          + value + "\")'>");
      writer.println("        </td>");
      writer.println("        <td><label for='"
          + id + "'/>");
      writer.println("          "
          + valueTitle);
      writer.println("        </td>");
      writer.println("      </tr>");
    }

    writer.println("        </table>");

    writer.println("  <td align='right'>");
    writer.println("    <input type='text' name='"
        + goalName + "_radioValue' size='10' align='right' readonly tabindex='-1'>");

  }

  /**
   * Name of the element that stores the textual value of the specified yes/no
   * value.
   */
  public static String getElementNameForYesNoDisplay(final String goalName) {
    return goalName
        + "_radioValue";
  }

  /**
   * The ID assigned to the radio button for a particular value of an enumerated
   * goal.
   */
  public static String getIDForEnumRadio(final String goalName,
                                         final String value) {
    return goalName
        + "_" + value;
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

  /**
   * The ID of the element that holds errors for the specified goal. This name
   * is also used for the name of the variable that holds the error text for the
   * goal in check_restrictions.
   */
  private static String getElementIDForError(final String goalName) {
    return "error_"
        + goalName;
  }

  private static void generateComputedGoalFunction(final Formatter formatter,
                                                   final ComputedGoal compGoal)
      throws ParseException {
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
                                     final int indent)
      throws ParseException {
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
      if (null != childEle.getResultPoly()) {
        final ComplexPolynomial resultPoly = childEle.getResultPoly();
        formatter.format("%s%s = %s;%n", generateIndentSpace(indent
            + INDENT_LEVEL), getVarNameForComputedScore(goalName), polyToString(resultPoly));
      } else if (null != childEle.getResultSwitch()) {
        generateSwitch(formatter, childEle.getResultSwitch(), goalName, indent
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
   * @throws ParseException
   */
  private static String polyToString(final BasicPolynomial poly) throws ParseException {
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
          + value + ")";
    case TRUNCATE:
      return "parseInt("
          + value + ")";
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
   * @param ele
   * @throws ParseException
   */
  private static void generateCondition(final Formatter formatter,
                                        final String ifPrefix,
                                        final AbstractConditionStatement ele)
      throws ParseException {
    formatter.format("%sif(", ifPrefix);

    if (ele instanceof ConditionStatement) {
      ConditionStatement cond = (ConditionStatement) ele;
      formatter.format("%s %s %s", polyToString(cond.getLeft()), ineqToString(ele.getComparison()),
                       polyToString(cond.getRight()));
    } else if (ele instanceof EnumConditionStatement) {
      EnumConditionStatement cond = (EnumConditionStatement) ele;
      final String leftStr;
      if (null != cond.getLeftGoal()) {
        leftStr = getVarNameForRawScore(cond.getLeftGoal().getName());
      } else {
        leftStr = "'"
            + cond.getLeftString() + "'";
      }

      final String rightStr;
      if (null != cond.getRightGoal()) {
        rightStr = getVarNameForRawScore(cond.getRightGoal().getName());
      } else {
        rightStr = "'"
            + cond.getLeftString() + "'";
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
