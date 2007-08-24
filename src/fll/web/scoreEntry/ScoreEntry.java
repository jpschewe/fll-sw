/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Formatter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Utilities;
import fll.db.Queries;
import fll.xml.XMLUtils;

/**
 * Java code used in scoreEntry.jsp.
 * 
 * @version $Revision$
 */
public final class ScoreEntry {

  private static final Logger LOG = Logger.getLogger(ScoreEntry.class);

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
  public static void generateIsConsistent(final JspWriter writer, final Document document) throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);

    writer.println("function isConsistent() {");

    // check all goal min and max values
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i = 0; i < goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");

      writer.println("  <!-- " + name + " -->");
      if(element.getElementsByTagName("value").getLength() > 0) {
        // enumerated
        writer.println("  <!-- nothing to check -->");
      } else {
        final String rawVarName = getVarNameForRawScore(name);
        writer.println("  if(" + rawVarName + " < " + min + " || " + rawVarName + " > " + max + ") {");
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
  public static void generateIncrementMethods(final JspWriter writer, final Document document) throws IOException, ParseException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    final Formatter formatter = new Formatter(writer);

    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i = 0; i < goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");
      final String rawVarName = getVarNameForRawScore(name);

      formatter.format("<!-- %s -->%n", name);
      formatter.format("var %s;%n", rawVarName);
      formatter.format("var %s;%n", getVarNameForComputedScore(name));

      if(element.getElementsByTagName("value").getLength() > 0 || ("0".equals(min) && "1".equals(max))) {
        formatter.format("function %s(newValue) {%n", getSetMethodName(name));
        formatter.format("  var temp = %s;%n", rawVarName);
        formatter.format("  %s = newValue;%n", rawVarName);
        formatter.format("  if(!isConsistent()) {%n");
        formatter.format("    %s = temp;%n", rawVarName);
        formatter.format("  }%n");
        formatter.format("  refresh();%n");
        formatter.format("}%n");
      } else {
        formatter.format("function %s(increment) {%n", getIncrementMethodName(name));
        formatter.format("  %s += increment;%n", rawVarName);
        formatter.format("  if(!isConsistent()) {%n");
        formatter.format("    %s -= increment;%n", rawVarName);
        formatter.format("  }%n");
        formatter.format("  refresh();%n");
        formatter.format("}%n");
      }

      writer.newLine();
      writer.newLine();
    } // end for each goal

    // generate the methods to update the computed goal variables
    final NodeList computedGoals = performanceElement.getElementsByTagName("computedGoal");
    for(int i = 0; i < computedGoals.getLength(); i++) {
      final Element ele = (Element)computedGoals.item(i);
      final String goalName = ele.getAttribute("name");
      formatter.format("<!-- %s -->%n", goalName);
      formatter.format("var %s;%n", getVarNameForComputedScore(goalName));
      generateComputedGoalFunction(formatter, ele);
    }
  }

  /**
   * Generate the body of the refresh function
   */
  public static void generateRefreshBody(final JspWriter writer, final Document document) throws ParseException, IOException {
    final Formatter formatter = new Formatter(writer);

    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);

    // output the assignments of each element
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i = 0; i < goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String multiplier = element.getAttribute("multiplier");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");
      final String rawVarName = getVarNameForRawScore(name);
      final String computedVarName = getVarNameForComputedScore(name);

      formatter.format("<!-- %s -->%n", name);

      final NodeList posValues = element.getElementsByTagName("value");
      if(posValues.getLength() > 0) {
        // enumerated
        for(int valueIdx = 0; valueIdx < posValues.getLength(); valueIdx++) {
          final Element valueEle = (Element)posValues.item(valueIdx);

          final String value = valueEle.getAttribute("value");
          final double valueScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueEle.getAttribute("score")).doubleValue();
          if(valueIdx > 0) {
            formatter.format("} else ");
          }

          formatter.format("if(%s == \"%s\") {%n", rawVarName, value);
          formatter.format("  document.scoreEntry.%s[%d].checked = true;%n", name, valueIdx);
          formatter.format("  %s = %f * %s;%n", computedVarName, valueScore, multiplier);
        }
        formatter.format("}%n");
      } else if("0".equals(min) && "1".equals(max)) {
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

      writer.newLine();
    } // end foreach goal

    // output calls to the computed goal methods
    final NodeList computedGoals = performanceElement.getElementsByTagName("computedGoal");
    for(int i = 0; i < computedGoals.getLength(); i++) {
      final Element ele = (Element)computedGoals.item(i);
      final String goalName = ele.getAttribute("name");
      final String computedVarName = getVarNameForComputedScore(goalName);
      formatter.format("%s();%n", getComputedMethodName(goalName));

      // add to the total score
      formatter.format("score += %s;%n", computedVarName);

      writer.newLine();
    }

    // FIX output calls to the restriction checks

  }

  /**
   * Generate the reset method, initializes all variables to their default
   * values.
   */
  public static void generateReset(final JspWriter writer, final Document document) throws IOException, ParseException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    writer.println("function reset() {");

    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i = 0; i < goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final int initialValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("initialValue")).intValue();
      if(XMLUtils.isEnumeratedGoal(element)) {
        // find score that matches initialValue or is min
        final NodeList values = element.getElementsByTagName("value");
        boolean found = false;
        for(int valueIdx = 0; valueIdx < values.getLength() && !found; ++valueIdx) {
          final Element valueEle = (Element)values.item(valueIdx);
          final String value = valueEle.getAttribute("value");
          final int valueScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(valueEle.getAttribute("score")).intValue();
          if(valueScore == initialValue) {
            writer.println("  " + getVarNameForRawScore(name) + " = \"" + value + "\";");
            found = true;
          }
        }
        if(!found) {
          // fall back to just using the first enum value
          LOG.warn(new Formatter().format("Initial value for enum goal '%s' does not match the score of any enum value", name));
          writer.println("  " + getVarNameForRawScore(name) + " = \"" + ((Element)values.item(0)).getAttribute("value") + "\";");
        }

      } else {
        writer.println("  " + getVarNameForRawScore(name) + " = " + initialValue + ";");
      }
    }

    writer.println("}");
  }

  /**
   * Generate the score entry form.
   */
  public static void generateScoreEntry(final JspWriter writer, final Document document, final HttpServletRequest request) throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    final NodeList goals = performanceElement.getChildNodes();
    for(int i = 0; i < goals.getLength(); i++) {
      final Element goalEle = (Element)goals.item(i);
      final String goalEleName = goalEle.getNodeName();
      if("computedGoal".equals(goalEleName) || "goal".equals(goalEleName)) {
        final String name = goalEle.getAttribute("name");
        final String title = goalEle.getAttribute("title");
        try {
          writer.println("<!-- " + name + " -->");
          writer.println("<tr>");
          if(null != request.getParameter(name + "_error")) {
            writer.println("  <td name='error' bgcolor='red'>");
          } else {
            writer.println("  <td>");
          }
          writer.println("    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='3'><b>" + title + ":<b></font>");
          writer.println("  </td>");

          if("computedGoal".equals(goalEleName)) {
            writer.println("  <td colspan='2'>&nbsp;</td>");
          } else if("goal".equals(goalEleName)) {
            if(XMLUtils.isEnumeratedGoal(goalEle)) {
              // enumerated
              writer.println("  <td colspan='2'>");
              generateEnumeratedGoalButtons(goalEle, name, writer);
              writer.println("  </td>");
            } else {
              writer.println("  <td>");
              // start inc/dec buttons
              writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='150'>");
              writer.println("      <tr align='center'>");

              final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalEle.getAttribute("min")).intValue();
              final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalEle.getAttribute("max")).intValue();
              if(0 == min && 1 == max) {
                generateYesNoButtons(name, writer);
              } else {
                final int range = max - min;

                if(range >= 10) {
                  generateIncDecButton(name, 5, writer);
                } else if(range >= 5) {
                  generateIncDecButton(name, 3, writer);
                }

                // +1
                generateIncDecButton(name, 1, writer);

                // -1
                generateIncDecButton(name, -1, writer);

                if(range >= 10) {
                  generateIncDecButton(name, -5, writer);
                } else if(range >= 5) {
                  generateIncDecButton(name, -3, writer);
                }
              }
              writer.println("       </tr>");
              writer.println("    </table>");
              writer.println("  </td>");
              // end inc/dec buttons

              // count
              writer.println("  <td align='right'>");
              if(0 == min && 1 == max) {
                writer.println("    <input type='text' name='" + name + "_radioValue' size='3' align='right' readonly>");
              } else {
                writer.println("    <input type='text' name='" + name + "' size='3' align='right' readonly>");
              }
              writer.println("  </td>");
            } // end simplegoal
          }

          // computed score
          writer.println("  <td align='right'>");
          writer.println("    <input type='text' name='score_" + name + "' size='3' align='right' readonly>");
          writer.println("  </td>");

          // error message
          if(null != request.getParameter("error")) {
            if(null != request.getParameter(name + "_error")) {
              writer.println("  <td bgcolor='red'>");
              writer.println(request.getParameter(name + "_error"));
              writer.println("</td>");
            } else {
              writer.println("  <td>&nbsp;</td>");
            }
          }

          writer.println("</tr>");
          writer.println("<!-- end " + name + " -->");
          writer.newLine();
        } catch(final ParseException pe) {
          throw new RuntimeException("FATAL: min/max not parsable for goal: " + name);
        }
      }// end check for goal or computedGoal
    } // end foreach child of performance
  }

  /**
   * Generate an increment or decrement button for goal name with
   * increment/decrement increment.
   */
  private static void generateIncDecButton(final String name, final int increment, final JspWriter writer) throws IOException {
    // generate buttons with calls to increment<name>
    final String buttonName = (increment < 0 ? "" : "+") + String.valueOf(increment);
    final String incdec = (increment < 0 ? "dec" : "inc");
    writer.println("        <td>");
    writer.println("          <input id='" + incdec + "_" + name + "_" + String.valueOf(increment) + "' type='button' value='" + buttonName
        + "' onclick='" + getIncrementMethodName(name) + "(" + increment + ")'>");
    writer.println("        </td>");
  }

  /**
   * Generate yes and no buttons for goal name.
   */
  private static void generateYesNoButtons(final String name, final JspWriter writer) throws IOException {
    // generate radio buttons with calls to set<name>
    writer.println("        <td>");
    writer.println("          <input type='radio' name='" + name + "' value='1' onclick='" + getSetMethodName(name) + "(1)'>");
    writer.println("          Yes");
    writer.println("          &nbsp;&nbsp;");
    writer.println("          <input type='radio' name='" + name + "' value='0' onclick='" + getSetMethodName(name) + "(0)'>");
    writer.println("          No");
    writer.println("        </td>");
  }

  /**
   * Generate the initial assignment of the global variables for editing a
   * team's score.
   */
  public static void generateInitForScoreEdit(final JspWriter writer,
                                              final ServletContext application,
                                              final Document document,
                                              final int teamNumber,
                                              final int runNumber) throws SQLException, IOException {
    final Connection connection = (Connection)application.getAttribute("connection");
    final String tournament = Queries.getCurrentTournament(connection);
    final Statement stmt = connection.createStatement();
    final ResultSet rs = stmt.executeQuery("SELECT * from Performance" + " WHERE TeamNumber = " + teamNumber + " AND RunNumber = " + runNumber
        + " AND Tournament = '" + tournament + "'");
    try {
      if(rs.next()) {
        writer.println("  gbl_NoShow = " + rs.getString("NoShow") + ";");

        final Element rootElement = document.getDocumentElement();
        final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
        final NodeList goals = performanceElement.getElementsByTagName("goal");
        for(int i = 0; i < goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          final String name = element.getAttribute("name");
          final NodeList values = element.getElementsByTagName("value");
          final String rawVarName = getVarNameForRawScore(name);

          if(values.getLength() > 0) {
            // enumerated
            final String storedValue = rs.getString(name);
            boolean found = false;
            for(int valueIdx = 0; valueIdx < values.getLength(); valueIdx++) {
              final Element valueElement = (Element)values.item(valueIdx);
              final String value = valueElement.getAttribute("value");
              if(value.equals(storedValue)) {
                writer.println("  " + rawVarName + " = \"" + value + "\";");
                found = true;
              }
            }
            if(!found) {
              throw new RuntimeException("Found enumerated value in the database that's not in the XML document, goal: " + name + " value: "
                  + storedValue);
            }
          } else {
            // just use the value that is stored in the database
            writer.println("  " + rawVarName + " = " + rs.getString(name) + ";");
          }
        }
      } else {
        throw new RuntimeException("Cannot find TeamNumber and RunNumber in Performance table" + " TeamNumber: " + teamNumber + " RunNumber: "
            + runNumber);
      }
    } finally {
      rs.close();
      stmt.close();
    }
  }

  private static void generateEnumeratedGoalButtons(final Element goal, final String goalName, final JspWriter writer)
      throws IOException, ParseException {
    writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='100%'>");

    final NodeList posValues = goal.getElementsByTagName("value");
    for(int v = 0; v < posValues.getLength(); v++) {
      final Element valueEle = (Element)posValues.item(v);
      final String valueTitle = valueEle.getAttribute("title");
      final String value = valueEle.getAttribute("value");
      writer.println("      <tr>");
      writer.println("        <td>");
      writer.println("          <input type='radio' name='" + goalName + "' value='" + value + "' onclick='" + getSetMethodName(goalName) + "(\""
          + value + "\")'>");
      writer.println("        </td>");
      writer.println("        <td>");
      writer.println("          " + valueTitle);
      writer.println("        </td>");
      writer.println("      </tr>");
    }

    writer.println("        </table>");
  }

  /**
   * The name of the javascript variable that represents the raw score for a
   * goal
   * 
   * @param goalName
   * @return
   */
  private static String getVarNameForComputedScore(final String goalName) {
    return goalName + "_computed";
  }

  /**
   * The name of the javascript variable that represents the computed score for
   * a goal
   */
  private static String getVarNameForRawScore(final String goalName) {
    return goalName + "_raw";
  }

  private static String getIncrementMethodName(final String goalName) {
    return "increment" + goalName;
  }

  private static String getSetMethodName(final String goalName) {
    return "set" + goalName;
  }

  private static String getComputedMethodName(final String goalName) {
    return "compute" + goalName;
  }

  private static void generateComputedGoalFunction(final Formatter formatter, final Element ele) throws ParseException {
    final String goalName = ele.getAttribute("name");

    formatter.format("function %s() {%n", getComputedMethodName(goalName));
    final Element switchEle = (Element)ele.getFirstChild();
    generateSwitch(formatter, switchEle, goalName, INDENT_LEVEL);

    formatter.format("%sdocument.scoreEntry.score_%s.value = %s;%n", generateIndentSpace(INDENT_LEVEL), goalName, getVarNameForComputedScore(goalName));
    formatter.format("}%n");
  }

  private static void generateSwitch(final Formatter formatter, final Element ele, final String goalName, final int indent) throws ParseException {
    final NodeList children = ele.getChildNodes();
    for(int childIdx = 0; childIdx < children.getLength(); ++childIdx) {
      final Element childEle = (Element)children.item(childIdx);
      final String childName = childEle.getNodeName();
      if("case".equals(childName)) {
        final Element conditionEle = (Element)childEle.getFirstChild();
        generateCondition(formatter, conditionEle, indent);
        formatter.format(" {%n");
        final Element resultEle = (Element)conditionEle.getNextSibling();
        final String resultName = resultEle.getNodeName();
        if("result".equals(resultName)) {
          formatter.format("%s%s = %s;%n", generateIndentSpace(indent + INDENT_LEVEL), getVarNameForComputedScore(goalName), polyToString(resultEle));
        } else if("switch".equals(resultName)) {
          generateSwitch(formatter, resultEle, goalName, indent + INDENT_LEVEL);
        } else {
          throw new RuntimeException("Expected 'result' or 'switch', but found: " + resultName);
        }
        formatter.format("%s}", generateIndentSpace(indent));
      } else if("default".equals(childName)) {
        formatter.format(" else {%n");
        formatter.format("%s%s = %s;%n", generateIndentSpace(indent + INDENT_LEVEL), getVarNameForComputedScore(goalName), polyToString(childEle));
        formatter.format("%s}%n", generateIndentSpace(indent));
      } else {
        throw new RuntimeException("Expecting 'case' or 'default', but found: " + childName);
      }
    }
  }

  private static String stringOrGoalToString(final Element ele) {
    if("stringConstant".equals(ele.getNodeName())) {
      return '"' + ele.getAttribute("value") + '"';
    } else if("goalRef".equals(ele.getNodeName())) {
      final String goal = ele.getAttribute("goal");
      return getVarNameForRawScore(goal);
    } else {
      throw new RuntimeException("Expecting 'stringConstant' or 'goalRef' found: " + ele.getNodeName());
    }
  }

  private static String polyToString(final Element ele) throws ParseException {
    final Formatter formatter = new Formatter();
    final NodeList children = ele.getChildNodes();
    for(int childIdx = 0; childIdx < children.getLength(); ++childIdx) {
      final Element childEle = (Element)children.item(childIdx);

      if(childIdx > 0) {
        formatter.format(" + ");
      }

      if("term".equals(childEle.getNodeName())) {
        final String goal = childEle.getAttribute("goal");
        final double coefficient = Utilities.NUMBER_FORMAT_INSTANCE.parse(childEle.getAttribute("coefficient")).doubleValue();
        final String scoreType = childEle.getAttribute("scoreType");
        formatter.format("%f * %s_%s", coefficient, goal, scoreType);
      } else if("constant".equals(childEle.getNodeName())) {
        final double value = Utilities.NUMBER_FORMAT_INSTANCE.parse(childEle.getAttribute("value")).doubleValue();
        formatter.format("%f", value);
      } else {
        throw new RuntimeException("Expected 'term' or 'constant', but found: " + childEle.getNodeName());
      }
    }
    return formatter.toString();
  }

  private static String ineqToString(final Element ele) {
    final String nodeName = ele.getNodeName();
    if("less-than".equals(nodeName)) {
      return "<";
    } else if("less-than-or-equal".equals(nodeName)) {
      return "<=";
    } else if("greater-than".equals(nodeName)) {
      return ">";
    } else if("greater-than-or-equal".equals(nodeName)) {
      return ">=";
    } else if("equal-to".equals(nodeName)) {
      return "==";
    } else if("not-equal-to".equals(nodeName)) {
      return "!=";
    } else {
      throw new RuntimeException("Expected an inequality name, but found: " + nodeName);
    }
  }

  private static void generateCondition(final Formatter formatter, final Element ele, final int indent) throws ParseException {
    formatter.format("%sif(", generateIndentSpace(indent));

    final Element leftEle = (Element)ele.getFirstChild();
    final Element leftVal = (Element)leftEle.getFirstChild();
    final Element ineqEle = (Element)leftEle.getNextSibling();
    final Element rightEle = (Element)ineqEle.getNextSibling();
    final Element rightVal = (Element)rightEle.getFirstChild();
    final String nodeName = ele.getNodeName();
    if("condition".equals(nodeName)) {
      formatter.format("%s %s %s", polyToString(leftVal), ineqToString(ineqEle), polyToString(rightVal));
    } else if("enumCondition".equals(nodeName)) {
      formatter.format("%s %s %s", stringOrGoalToString(leftVal), ineqToString(ineqEle), stringOrGoalToString(rightVal));
    } else {
      throw new RuntimeException("Expecting 'condition' or 'enumCond', but found: " + nodeName);
    }
    formatter.format(")");
  }

  private static String generateIndentSpace(final int indent) {
    final StringBuilder retval = new StringBuilder();
    for(int i = 0; i < indent; ++i) {
      retval.append(' ');
    }
    return retval.toString();
  }

}
