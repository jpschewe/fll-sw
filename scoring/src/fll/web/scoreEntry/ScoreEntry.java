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
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FP;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.xml.ChallengeParser;
import fll.xml.ScoreType;
import fll.xml.XMLUtils;

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
                                          final ServletContext application) throws IOException {
    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

    writer.println("function isConsistent() {");

    // check all goal min and max values
    for (final Element element : new NodelistElementCollectionAdapter(performanceElement.getElementsByTagName("goal"))) {
      final String name = element.getAttribute("name");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");

      writer.println("  <!-- "
          + name + " -->");
      if (element.getElementsByTagName("value").getLength() > 0) {
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
    }

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
                                              final ServletContext application) throws IOException, ParseException {
    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
    final Formatter formatter = new Formatter(writer);

    for (final Element element : new NodelistElementCollectionAdapter(performanceElement.getElementsByTagName("goal"))) {
      final String name = element.getAttribute("name");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");
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
      if (ScoreType.FLOAT == XMLUtils.getScoreType(element)) {
        formatter.format("  var num = parseFloat(str);%n");
      } else {
        formatter.format("  var num = parseInt(str);%n");
      }
      formatter.format("  if(!isNaN(num)) {%n");
      formatter.format("    %s(num);%n", getSetMethodName(name));
      formatter.format("  }%n");
      formatter.format("  refresh();%n");
      formatter.format("}%n");

      if (element.getElementsByTagName("value").getLength() == 0
          && !("0".equals(min) && "1".equals(max))) {
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

    // generate the methods to update the computed goal variables
    for (final Element ele : new NodelistElementCollectionAdapter(
                                                                  performanceElement.getElementsByTagName("computedGoal"))) {
      final String goalName = ele.getAttribute("name");
      formatter.format("<!-- %s -->%n", goalName);
      formatter.format("var %s;%n", getVarNameForComputedScore(goalName));
      generateComputedGoalFunction(formatter, ele);
    }
  }

  /**
   * Generate the body of the refresh function
   */
  public static void generateRefreshBody(final Writer writer,
                                         final ServletContext application) throws ParseException, IOException {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Entering generateRefreshBody");
    }

    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Formatter formatter = new Formatter(writer);

    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

    // output the assignments of each element
    for (final Element element : new NodelistElementCollectionAdapter(performanceElement.getElementsByTagName("goal"))) {
      final String name = element.getAttribute("name");
      final String multiplier = element.getAttribute("multiplier");
      final double min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).doubleValue();
      final double max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).doubleValue();
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

      final List<Element> posValues = new NodelistElementCollectionAdapter(element.getElementsByTagName("value")).asList();
      if (posValues.size() > 0) {
        // enumerated
        for (int valueIdx = 0; valueIdx < posValues.size(); valueIdx++) {
          final Element valueEle = (Element) posValues.get(valueIdx);

          final String value = valueEle.getAttribute("value");
          final double valueScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueEle.getAttribute("score"))
                                                                    .doubleValue();
          if (valueIdx > 0) {
            formatter.format("} else ");
          }

          formatter.format("if(%s == \"%s\") {%n", rawVarName, value);
          formatter.format("  document.scoreEntry.%s[%d].checked = true;%n", name, valueIdx);
          formatter.format("  %s = %f * %s;%n", computedVarName, valueScore, multiplier);
          
          formatter.format("  document.scoreEntry.%s.value = '%s'%n", getElementNameForYesNoDisplay(name), value.toUpperCase());
        }
        formatter.format("}%n");
      } else if (0 == min
          && 1 == max) {
        // set the radio button to match the gbl variable
        formatter.format("if(%s == 0) {%n", rawVarName);
        formatter.format("  document.scoreEntry.%s[1].checked = true%n", name);
        formatter.format("  document.scoreEntry.%s_radioValue.value = 'NO'%n", name);
        formatter.format("} else {%n");
        formatter.format("  document.scoreEntry.%s[0].checked = true%n", name);
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
    } // end foreach goal

    // set the radio buttons for score verification
    formatter.format("if(Verified == 0) {%n");
    formatter.format("  document.scoreEntry.Verified[1].checked = true%n"); // NO
    formatter.format("} else {%n");
    formatter.format("  document.scoreEntry.Verified[0].checked = true%n"); // YES
    formatter.format("}%n");

    // output calls to the computed goal methods
    for (final Element ele : new NodelistElementCollectionAdapter(
                                                                  performanceElement.getElementsByTagName("computedGoal"))) {
      final String goalName = ele.getAttribute("name");
      final String computedVarName = getVarNameForComputedScore(goalName);
      formatter.format("%s();%n", getComputedMethodName(goalName));

      // add to the total score
      formatter.format("score += %s;%n", computedVarName);

      formatter.format("%n");
    }

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
                                                   final ServletContext application) throws IOException, ParseException {
    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Formatter formatter = new Formatter(writer);

    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

    final Collection<String> goalsWithRestrictions = new LinkedList<String>();
    final List<Element> restrictions = new NodelistElementCollectionAdapter(
                                                                            performanceElement.getElementsByTagName("restriction")).asList();

    // find out which goals are involved in restrictions
    for (final Element restrictEle : restrictions) {
      goalsWithRestrictions.addAll(getGoalsInRestriction(restrictEle));
    }

    // output variable declaration for each goal
    for (final String goalName : goalsWithRestrictions) {
      formatter.format("  var %s = \"\";%n", getElementIDForError(goalName));
    }

    // output actual check of restriction
    for (int restrictIdx = 0; restrictIdx < restrictions.size(); ++restrictIdx) {
      final Element restrictEle = (Element) restrictions.get(restrictIdx);
      final double lowerBound = Utilities.NUMBER_FORMAT_INSTANCE.parse(restrictEle.getAttribute("lowerBound"))
                                                                .doubleValue();
      final double upperBound = Utilities.NUMBER_FORMAT_INSTANCE.parse(restrictEle.getAttribute("upperBound"))
                                                                .doubleValue();
      final String message = restrictEle.getAttribute("message");

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

  private static Set<String> getGoalsInRestriction(final Element restrictEle) {
    final Set<String> goals = new HashSet<String>();
    for (final Element termEle : new NodelistElementCollectionAdapter(restrictEle.getElementsByTagName("term"))) {
      final String goalName = termEle.getAttribute("goal");
      goals.add(goalName);
    }
    return goals;
  }

  /**
   * Generate init for new scores, initializes all variables to their default
   * values.
   */
  public static void generateInitForNewScore(final JspWriter writer,
                                             final ServletContext application) throws IOException, ParseException {
    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

    for (final Element element : new NodelistElementCollectionAdapter(performanceElement.getElementsByTagName("goal"))) {
      final String name = element.getAttribute("name");
      final double initialValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("initialValue"))
                                                                  .doubleValue();
      if (XMLUtils.isEnumeratedGoal(element)) {
        // find score that matches initialValue or is min
        final List<Element> values = new NodelistElementCollectionAdapter(element.getElementsByTagName("value")).asList();
        boolean found = false;
        for (final Element valueEle : values) {
          final String value = valueEle.getAttribute("value");
          final double valueScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueEle.getAttribute("score"))
                                                                    .doubleValue();
          if (FP.equals(valueScore, initialValue, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
            writer.println("  "
                + getVarNameForRawScore(name) + " = \"" + value + "\";");
            found = true;
          }
        }
        if (!found) {
          // fall back to just using the first enum value
          LOG.warn(new Formatter().format("Initial value for enum goal '%s' does not match the score of any enum value",
                                          name));
          writer.println("  "
              + getVarNameForRawScore(name) + " = \"" + values.get(0).getAttribute("value") + "\";");
        }

      } else {
        writer.println("  "
            + getVarNameForRawScore(name) + " = " + initialValue + ";");
      }
    }

    writer.println("  Verified = 0;");

    writer.println("  gbl_NoShow = 0;");

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
                                        final ServletContext application) throws IOException {
    final Document document = ApplicationAttributes.getChallengeDocument(application);
    final Formatter formatter = new Formatter(writer);

    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
    for (final Element goalEle : new NodelistElementCollectionAdapter(performanceElement.getChildNodes())) {
      final String goalEleName = goalEle.getNodeName();
      if ("computedGoal".equals(goalEleName)
          || "goal".equals(goalEleName)) {
        final String name = goalEle.getAttribute("name");
        final String title = goalEle.getAttribute("title");
        try {
          writer.println("<!-- "
              + name + " -->");
          writer.println("<tr>");
          writer.println("  <td>");
          writer.println("    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='3'><b>"
              + title + ":</b></font>");
          writer.println("  </td>");

          if ("computedGoal".equals(goalEleName)) {
            writer.println("  <td colspan='2' align='center'><b>Computed Goal</b></td>");
          } else if ("goal".equals(goalEleName)) {
            if (XMLUtils.isEnumeratedGoal(goalEle)) {
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
          formatter.format("  <td class='error' id='error_%s'>&nbsp;</td>%n", name);

          writer.println("</tr>");
          writer.println("<!-- end "
              + name + " -->");
          writer.newLine();
        } catch (final ParseException pe) {
          throw new RuntimeException("FATAL: min/max not parsable for goal: "
              + name);
        }
      }// end check for goal or computedGoal
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
  private static void generateSimpleGoalButtons(final Element goalEle,
                                                final String name,
                                                final JspWriter writer) throws IOException, ParseException {
    // start inc/dec buttons
    writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='150'>");
    writer.println("      <tr align='center'>");

    final double min = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalEle.getAttribute("min")).doubleValue();
    final double max = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalEle.getAttribute("max")).doubleValue();
    if (0 == min
        && 1 == max) {
      generateYesNoButtons(name, writer);
    } else {
      final double range = max
          - min;

      if (range >= 10) {
        generateIncDecButton(name, 5, writer);
      } else if (range >= 5) {
        generateIncDecButton(name, 3, writer);
      }

      // +1
      generateIncDecButton(name, 1, writer);

      // -1
      generateIncDecButton(name, -1, writer);

      if (range >= 10) {
        generateIncDecButton(name, -5, writer);
      } else if (range >= 5) {
        generateIncDecButton(name, -3, writer);
      }
    }
    writer.println("       </tr>");
    writer.println("    </table>");
    writer.println("  </td>");
    // end inc/dec buttons

    // count
    writer.println("  <td align='right'>");
    if (0 == min
        && 1 == max) {
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
                                           final JspWriter writer) throws IOException {
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
                                           final JspWriter writer) throws IOException {
    // generate radio buttons with calls to set<name>
    writer.println("        <td>");
    writer.println("          <input type='radio' id='"
        + name + "_yes' name='" + name + "' value='1' onclick='" + getSetMethodName(name) + "(1)'>");
    writer.println("          <label for='"
        + name + "_yes'>Yes</label>");
    writer.println("          &nbsp;&nbsp;");
    writer.println("          <input type='radio' id='"
        + name + "_no' name='" + name + "' value='0' onclick='" + getSetMethodName(name) + "(0)'>");
    writer.println("          <label for='"
        + name + "_no'>No</label>");
    writer.println("        </td>");
  }

  /**
   * Generate the initial assignment of the global variables for editing a
   * team's score.
   */
  public static void generateInitForScoreEdit(final JspWriter writer,
                                              final ServletContext application,
                                              final HttpSession session) throws SQLException, IOException {
    final Document document = ApplicationAttributes.getChallengeDocument(application);
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
        final Element rootElement = document.getDocumentElement();
        final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
        for (final Element element : new NodelistElementCollectionAdapter(
                                                                          performanceElement.getElementsByTagName("goal"))) {
          final String name = element.getAttribute("name");
          final List<Element> values = new NodelistElementCollectionAdapter(element.getElementsByTagName("value")).asList();
          final String rawVarName = getVarNameForRawScore(name);

          if (values.size() > 0) {
            // enumerated
            final String storedValue = rs.getString(name);
            boolean found = false;
            for (final Element valueElement : values) {
              final String value = valueElement.getAttribute("value");
              if (value.equals(storedValue)) {
                writer.println("  "
                    + rawVarName + " = \"" + value + "\";");
                found = true;
              }
            }
            if (!found) {
              throw new RuntimeException(
                                         "Found enumerated value in the database that's not in the XML document, goal: "
                                             + name + " value: " + storedValue);
            }
          } else {
            // just use the value that is stored in the database
            writer.println("  "
                + rawVarName + " = " + rs.getString(name) + ";");
          }
        }
        // Always init the special double-check column
        writer.println("  Verified = "
            + rs.getBoolean("Verified") + ";");

        writer.println("  gbl_NoShow = "
            + rs.getBoolean("NoShow") + ";");
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

  private static void generateEnumeratedGoalButtons(final Element goal,
                                                    final String goalName,
                                                    final JspWriter writer) throws IOException, ParseException {
    writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='100%'>");

    for (final Element valueEle : new NodelistElementCollectionAdapter(goal.getElementsByTagName("value"))) {
      final String valueTitle = valueEle.getAttribute("title");
      final String value = valueEle.getAttribute("value");
      writer.println("      <tr>");
      writer.println("        <td>");
      writer.println("          <input type='radio' name='"
          + goalName + "' value='" + value + "' id='" + getIDForEnumRadio(goalName, value) + "' ' onclick='"
          + getSetMethodName(goalName) + "(\"" + value + "\")'>");
      writer.println("        </td>");
      writer.println("        <td>");
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
   * Name of the element that stores the textual value of the specified yes/no value.
   */
  public static String getElementNameForYesNoDisplay(final String goalName) {
    return goalName + "_radioValue";
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
                                                   final Element ele) throws ParseException {
    final String goalName = ele.getAttribute("name");

    formatter.format("function %s() {%n", getComputedMethodName(goalName));
    for (final Element childEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if ("variable".equals(childEle.getNodeName())) {
        final String varName = getComputedGoalLocalVarName(childEle.getAttribute("name"));
        final String varValue = polyToString(childEle);
        formatter.format("var %s = %s;%n", varName, varValue);
      } else if ("switch".equals(childEle.getNodeName())) {
        generateSwitch(formatter, childEle, goalName, INDENT_LEVEL);
      } else {
        throw new RuntimeException("Unexpected element in computed goal.  Expected 'switch' or 'variable', but found '"
            + childEle.getNodeName() + "'");
      }
    }

    formatter.format("%sdocument.scoreEntry.score_%s.value = %s;%n", generateIndentSpace(INDENT_LEVEL), goalName,
                     getVarNameForComputedScore(goalName));
    formatter.format("}%n");
  }

  /**
   * Get the name of a local variable inside a computed goal function that
   * stores the specified variable value
   */
  private static String getComputedGoalLocalVarName(final String varName) {
    return varName;
  }

  private static void generateSwitch(final Formatter formatter,
                                     final Element ele,
                                     final String goalName,
                                     final int indent) throws ParseException {
    // keep track if there are any case statements
    boolean first = true;
    boolean hasCase = false;
    for (final Element childEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      final String childName = childEle.getNodeName();
      if ("case".equals(childName)) {
        hasCase = true;
        final List<Element> childChildren = new NodelistElementCollectionAdapter(childEle.getChildNodes()).asList();
        final Element conditionEle = childChildren.get(0);
        final String ifPrefix;
        if (!first) {
          ifPrefix = " else ";
        } else {
          first = false;
          ifPrefix = generateIndentSpace(indent);
        }
        generateCondition(formatter, ifPrefix, conditionEle);
        formatter.format(" {%n");
        final Element resultEle = childChildren.get(1);
        final String resultName = resultEle.getNodeName();
        if ("result".equals(resultName)) {
          formatter.format("%s%s = %s;%n", generateIndentSpace(indent
              + INDENT_LEVEL), getVarNameForComputedScore(goalName), polyToString(resultEle));
        } else if ("switch".equals(resultName)) {
          generateSwitch(formatter, resultEle, goalName, indent
              + INDENT_LEVEL);
        } else {
          throw new RuntimeException("Expected 'result' or 'switch', but found: "
              + resultName);
        }
        formatter.format("%s}", generateIndentSpace(indent));
      } else if ("default".equals(childName)) {
        if (hasCase) {
          formatter.format(" else {%n");
        }
        formatter.format("%s%s = %s;%n", generateIndentSpace(indent
            + INDENT_LEVEL), getVarNameForComputedScore(goalName), polyToString(childEle));
        if (hasCase) {
          formatter.format("%s}%n", generateIndentSpace(indent));
        }
      } else {
        throw new RuntimeException("Expecting 'case' or 'default', but found: "
            + childName);
      }
    }
  }

  /**
   * @param ele either a "stringConstant" element or a "goalRef" element
   * @return the string value
   */
  private static String stringOrGoalToString(final Element ele) {
    if ("stringConstant".equals(ele.getNodeName())) {
      return '"' + ele.getAttribute("value") + '"';
    } else if ("goalRef".equals(ele.getNodeName())) {
      final String goal = ele.getAttribute("goal");
      return getVarNameForRawScore(goal);
    } else {
      throw new RuntimeException("Expecting 'stringConstant' or 'goalRef' found: "
          + ele.getNodeName());
    }
  }

  /**
   * @param ele the element that is a polynomial, all of it's children are
   *          terms, variableRefs and constants
   * @return the string that represents the polynomial
   * @throws ParseException
   */
  private static String polyToString(final Element ele) throws ParseException {
    final Formatter formatter = new Formatter();
    boolean first = true;
    for (final Element childEle : new NodelistElementCollectionAdapter(ele.getChildNodes())) {
      if (!first) {
        formatter.format(" + ");
      } else {
        first = false;
      }

      if ("term".equals(childEle.getNodeName())) {
        final String goal = childEle.getAttribute("goal");
        final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(childEle.getAttribute("coefficient"))
                                                                   .doubleValue();
        final String scoreType = childEle.getAttribute("scoreType");
        final String varName;
        if ("raw".equals(scoreType)) {
          varName = getVarNameForRawScore(goal);
        } else if ("computed".equals(scoreType)) {
          varName = getVarNameForComputedScore(goal);
        } else {
          throw new RuntimeException("Expected 'raw' or 'computed', but found: "
              + scoreType);
        }
        final String value = new Formatter().format("%f * %s", coefficient, varName).toString();
        final String floatingPoint = childEle.getAttribute("floatingPoint");
        formatter.format("%s", applyFloatingPoint(value, floatingPoint));
      } else if ("variableRef".equals(childEle.getNodeName())) {
        final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(childEle.getAttribute("coefficient"))
                                                                   .doubleValue();
        final String variable = getComputedGoalLocalVarName(childEle.getAttribute("variable"));
        final String floatingPoint = childEle.getAttribute("floatingPoint");
        final String value = new Formatter().format("%f * %s", coefficient, variable).toString();
        formatter.format("%s", applyFloatingPoint(value, floatingPoint));
      } else if ("constant".equals(childEle.getNodeName())) {
        final double value = Utilities.NUMBER_FORMAT_INSTANCE.parse(childEle.getAttribute("value")).doubleValue();
        formatter.format("%f", value);
      } else {
        throw new RuntimeException("Expected 'term' or 'constant', but found: "
            + childEle.getNodeName());
      }
    }
    return formatter.toString();
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
                                           final String floatingPoint) {
    if ("decimal".equals(floatingPoint)) {
      return value;
    } else if ("round".equals(floatingPoint)) {
      return "Math.round("
          + value + ")";
    } else if ("truncate".equals(floatingPoint)) {
      return "parseInt("
          + value + ")";
    } else {
      throw new RuntimeException("Unexpected floating point type: "
          + floatingPoint);
    }
  }

  private static String ineqToString(final Element ele) {
    final String nodeName = ele.getNodeName();
    if ("less-than".equals(nodeName)) {
      return "<";
    } else if ("less-than-or-equal".equals(nodeName)) {
      return "<=";
    } else if ("greater-than".equals(nodeName)) {
      return ">";
    } else if ("greater-than-or-equal".equals(nodeName)) {
      return ">=";
    } else if ("equal-to".equals(nodeName)) {
      return "==";
    } else if ("not-equal-to".equals(nodeName)) {
      return "!=";
    } else {
      throw new RuntimeException("Expected an inequality name, but found: "
          + nodeName);
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
                                        final Element ele) throws ParseException {
    formatter.format("%sif(", ifPrefix);

    final List<Element> elementChildren = new NodelistElementCollectionAdapter(ele.getChildNodes()).asList();
    final Element leftEle = elementChildren.get(0);
    final Element leftVal = new NodelistElementCollectionAdapter(leftEle.getChildNodes()).asList().get(0);
    final Element ineqEle = elementChildren.get(1);
    final Element rightEle = elementChildren.get(2);
    final Element rightVal = new NodelistElementCollectionAdapter(rightEle.getChildNodes()).asList().get(0);
    final String nodeName = ele.getNodeName();
    if ("condition".equals(nodeName)) {
      formatter.format("%s %s %s", polyToString(leftEle), ineqToString(ineqEle), polyToString(rightEle));
    } else if ("enumCondition".equals(nodeName)) {
      formatter.format("%s %s %s", stringOrGoalToString(leftVal), ineqToString(ineqEle), stringOrGoalToString(rightVal));
    } else {
      throw new RuntimeException("Expecting 'condition' or 'enumCond', but found: "
          + nodeName);
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
