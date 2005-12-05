/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;

import fll.Queries;
import fll.Utilities;

import java.io.IOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.ParseException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Java code used in scoreEntry.jsp.
 * 
 * @version $Revision$
 */
public final class ScoreEntry {

  private ScoreEntry() {
  }


  /**
   * Generate the isConsistent method for the goals and restrictions in the
   * performance element of document.
   */
  public static void generateIsConsistent(final JspWriter writer,
                                          final Document document) throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    
    writer.println("function isConsistent() {");
    writer.println("  var restrictionSum;");

    //check all goal min and max values
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");
      
      writer.println("  <!-- " + name + " -->");
      if(element.getElementsByTagName("value").getLength() > 0) {
        //enumerated
        writer.println("  <!-- nothing to check -->");
      } else {
        writer.println("  if(gbl_" + name + " < " + min + " || gbl_" + name + " > " + max + ") {");
        writer.println("    return false;");
        writer.println("  }");
      }
      writer.println();
    }
    
    writer.println("  return true;");
    writer.println("}");
  }

  /**
   * Genrate the increment methods and variable declarations for the goals
   * in the performance element of document.
   */
  public static void generateIncrementMethods(final JspWriter writer,
                                              final Document document) throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);

    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");

      writer.println("<!-- " + name + " -->");
      writer.println("var gbl_" + name + ";");

      if(element.getElementsByTagName("value").getLength() > 0
         || ("0".equals(min) && "1".equals(max))) {
        writer.println("function set" + name + "(newValue) {");
        writer.println("  var temp = gbl_" + name + ";");
        writer.println("  gbl_" + name + " = newValue;");
        writer.println("  if(!isConsistent()) {");
        writer.println("    gbl_" + name + " = temp;");
        writer.println("  }");
        writer.println("  refresh();");
        writer.println("}");
      } else {
        writer.println("function increment" + name + "(increment) {");
        writer.println("  gbl_" + name + " += increment;");
        writer.println("  if(!isConsistent()) {");
        writer.println("    gbl_" + name + " -= increment;");
        writer.println("  }");
        writer.println("  refresh();");
        writer.println("}");
      }

      writer.newLine();
      writer.newLine();
    }
  }

  /**
   * Generate the body of the refresh function
   */
  public static void generateRefreshBody(final JspWriter writer,
                                         final Document document) throws ParseException, IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String multiplier = element.getAttribute("multiplier");
      final String min = element.getAttribute("min");
      final String max = element.getAttribute("max");
      
      writer.println("<!-- " + name + " -->");
      writer.println("var score_" + name + " = gbl_" + name + " * " + multiplier + ";");
      writer.println("score += score_" + name + ";");

      //set the score form element
      writer.println("document.scoreEntry.score_" + name + ".value = score_" + name + ";");

      final NodeList posValues = element.getElementsByTagName("value");
      if(posValues.getLength() > 0) {
        //enumerated
        for(int v=0; v<posValues.getLength(); v++) {
          final Element value = (Element)posValues.item(v);
        
          final int valueScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(value.getAttribute("score")).intValue();
          if(v > 0) {
            writer.print("} else ");
          }
          
          writer.println("if(gbl_" + name + " == " + valueScore + ") {");
          writer.println("  document.scoreEntry." + name + "[" + v + "].checked = true;");
        }
        writer.println("}");
      } else if("0".equals(min) && "1".equals(max)) {
        //set the radio button to match the gbl variable
        writer.println("if(gbl_" + name + " == 0) {");
        writer.println("  document.scoreEntry." + name + "[1].checked = true");
        writer.println("  document.scoreEntry." + name + "_radioValue.value = 'NO'");
        writer.println("} else {");
        writer.println("  document.scoreEntry." + name + "[0].checked = true");
        writer.println("  document.scoreEntry." + name + "_radioValue.value = 'YES'");
        writer.println("}");
      } else {
        //set the count form element
        writer.println("document.scoreEntry." + name + ".value = gbl_" + name + ";");
      }
      writer.newLine();
    }
  }

  /**
   * Generate the reset method, initializes all variables to their default
   * values.
   */
  public static void generateReset(final JspWriter writer,
                                   final Document document) throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    writer.println("function reset() {");
    
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String initialValue = element.getAttribute("initialValue");

      writer.println("  gbl_" + name + " = " + initialValue + ";");
    }

    writer.println("}");
  }

  /**
   * Generate the score entry form for document.
   */
  public static void generateScoreEntry(final JspWriter writer,
                                        final Document document,
                                        final HttpServletRequest request)
    throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      final String title = element.getAttribute("title");
      try {
        final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).intValue();
        final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).intValue();
        final int range = max - min;
        
        writer.println("<!-- " + name + " -->");
        writer.println("<tr>");
        if(null != request.getParameter(name + "_error")) {
          writer.println("  <td nowrap bgcolor='red'>");
        } else {
          writer.println("  <td nowrap>");
        }
        writer.println("    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='3'><b>" + title + ":<b></font>");
        writer.println("  </td>");
        
        if(element.getElementsByTagName("value").getLength() > 0) {
          //enumerated
          writer.println("  <td colspan='2'>");
          generateEnumeratedGoalButtons(element, name, writer);
          writer.println("  </td>");
        } else {
          writer.println("  <td>");
          //start inc/dec buttons
          writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='150'>");
          writer.println("      <tr align='center'>");

          if(0 == min && 1 == max) {
            generateYesNoButtons(name, writer);
          } else {
            if(range >= 10) {
              generateIncDecButton(name, 5, writer);
            } else if(range >= 5) {
              generateIncDecButton(name, 3, writer);
            }
          
            //+1
            generateIncDecButton(name, 1, writer);

            //-1
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
          //end inc/dec buttons

          //count
          writer.println("  <td align='right'>");
          if(0 == min && 1 == max) {
            writer.println("    <input type='text' name='" + name + "_radioValue' size='3' align='right' readonly>");
          } else {
            writer.println("    <input type='text' name='" + name + "' size='3' align='right' readonly>");
          }
          writer.println("  </td>");
        }

        //score
        writer.println("  <td align='right'>");
        writer.println("    <input type='text' name='score_" + name + "' size='3' align='right' readonly>");
        writer.println("  </td>");

        //error message
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
    }
  }


  /**
   * Generate an increment or decrement button for goal name with
   * increment/decrement increment.
   */
  private static void generateIncDecButton(final String name,
                                           final int increment,
                                           final JspWriter writer) throws IOException {
    //generate buttons with calls to increment<name>
    final String buttonName = (increment < 0 ? "" : "+") + String.valueOf(increment);
    writer.println("        <td>");
    writer.println("          <input type='button' value='" + buttonName + "' onclick='increment" + name + "(" + increment + ")'>");
    writer.println("        </td>");
  }

  /**
   * Generate yes and no buttons for goal name.
   */
  private static void generateYesNoButtons(final String name,
                                           final JspWriter writer) throws IOException {
    //generate radio buttons with calls to set<name>
    writer.println("        <td>");
    writer.println("          <input type='radio' name='" + name + "' value='1' onclick='set" + name + "(1)'>");
    writer.println("          Yes");
    writer.println("          &nbsp;&nbsp;");
    writer.println("          <input type='radio' name='" + name + "' value='0' onclick='set" + name + "(0)'>");
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
    final ResultSet rs = stmt.executeQuery("SELECT * from Performance"
                                           + " WHERE TeamNumber = " + teamNumber
                                           + " AND RunNumber = " + runNumber
                                           + " AND Tournament = '" + tournament + "'"
                                           );
    try {
      if(rs.next()) {
        writer.println("  gbl_NoShow = " + rs.getString("NoShow") + ";");

        final Element rootElement = document.getDocumentElement();
        final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
        final NodeList goals = performanceElement.getElementsByTagName("goal");
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          final String name = element.getAttribute("name");
          final NodeList values = element.getElementsByTagName("value");
          if(values.getLength() > 0) {
            //enumerated
            final String storedValue = rs.getString(name);
            boolean found = false;
            for(int v=0; v<values.getLength(); v++) {
              final Element valueElement = (Element)values.item(v);
              if(valueElement.getAttribute("value").equals(storedValue)) {
                writer.println("  gbl_" + name + " = " + valueElement.getAttribute("score") + ";");
                found = true;
              }
            }
            if(!found) {
              throw new RuntimeException("Found enumerated value in the database that's not in the XML document, goal: " + name + " value: " + storedValue);
            }
          } else {
            writer.println("  gbl_" + name + " = " + rs.getString(name) + ";");
          }
        }
      } else {
        throw new RuntimeException("Cannot find TeamNumber and RunNumber in Performance table"
                                   + " TeamNumber: " + teamNumber
                                   + " RunNumber: " + runNumber);
      }
    } finally {
      rs.close();
      stmt.close();
    }
  }

  private static void generateEnumeratedGoalButtons(final Element goal,
                                                    final String goalName,
                                                    final JspWriter writer)
    throws IOException, ParseException {
    writer.println("    <table border='0' cellpadding='0' cellspacing='0' width='100%'>");
    
    final NodeList posValues = goal.getElementsByTagName("value");
    for(int v=0; v<posValues.getLength(); v++) {
      final Element value = (Element)posValues.item(v);
      final String valueTitle = value.getAttribute("title");
      final String valueValue = value.getAttribute("value");
      final int valueScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(value.getAttribute("score")).intValue();
      writer.println("      <tr>");
      writer.println("        <td>");
      writer.println("          <input type='radio' name='" + goalName + "' value='" + valueValue + "' onclick='set" + goalName + "(" + valueScore + ")'>");
      writer.println("        </td>");
      writer.println("        <td>");
      writer.println("          " + valueTitle);
      writer.println("        </td>");
      writer.println("      </tr>");
    }

    writer.println("        </table>");
  }
}
