/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import fll.Queries;
import fll.Utilities;

import fll.xml.ChallengeParser;

import java.io.IOException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Iterator;

import javax.servlet.jsp.JspWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Code for finalComputedScores.jsp
 *
 * @version $Revision$
 */
final public class FinalComputedScores {

  /**
   * For debugging
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));
      if(null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }
      final Connection connection = Utilities.createDBConnection("disk");

      final fll.web.debug.DebugJspWriter out = new fll.web.debug.DebugJspWriter(new java.io.PrintWriter(System.out));
      generateReport("STATE", challengeDocument, connection, out);
      out.flush();
    }
    catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private FinalComputedScores() {
     
  }

  /**
   * Generate the actual report.
   */
  public static void generateReport(final String tournament,
                                    final Document document,
                                    final Connection connection,
                                    final JspWriter out) throws SQLException, IOException {

    Statement stmt = null;
    Statement teamsStmt = null;
    ResultSet rawScoreRS = null;
    ResultSet teamsRS = null;
    ResultSet scaledScoreRS = null;
    try {
      final NodeList subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
      stmt = connection.createStatement();
      teamsStmt = connection.createStatement();

      final Iterator divisionIter = Queries.getDivisions(connection).iterator();
      while(divisionIter.hasNext()) {
        final String division = (String)divisionIter.next();
      
        out.println("<h2>Division: " + division + "</h2>");
      
        out.println("<table border='0'>");
        out.println("  <tr>");
        out.println("    <th>Team # / Organization / Team Name</th>");
        out.println("    <th>&nbsp;</th>");
        for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
          final Element catElement = (Element)subjectiveCategories.item(cat);
          final double catWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(catElement.getAttribute("weight")).doubleValue();
          if(catWeight > 0.0) {
            final String catTitle = catElement.getAttribute("title");

            out.println("    <th>" + catTitle + "</th>");
          }
        }

        out.println("    <th>Performance</th>");

        out.println("    <th>Overall Score</th>");
        out.println("  </tr>");
        out.println("  <tr><td colspan='" + (subjectiveCategories.getLength() + 4) + "'><hr></td></tr>");
        teamsRS = teamsStmt.executeQuery("SELECT Teams.Organization,Teams.TeamName,Teams.TeamNumber,FinalScores.OverallScore"
                                         + " FROM SummarizedScores,Teams,FinalScores"
                                         + " WHERE FinalScores.TeamNumber = Teams.TeamNumber"
                                         + " AND FinalScores.Tournament = '" + tournament + "'"
                                         + " AND Teams.TeamNumber = SummarizedScores.TeamNumber"
                                         + " AND SummarizedScores.Tournament = '" + tournament + "'"
                                         + " AND Teams.Division = '" + division + "'"
                                         + " GROUP BY TeamNumber ORDER BY FinalScores.OverallScore DESC, Teams.TeamNumber");
        while(teamsRS.next()) {
          final int teamNumber = teamsRS.getInt(3);
          final String organization = teamsRS.getString(1);
          final String teamName = teamsRS.getString(2);

          final double totalScore;
          final double ts = teamsRS.getDouble(4);
          if(teamsRS.wasNull()) {
            totalScore = Double.NaN;
          } else {
            totalScore = ts;
          }
          
          //raw scores
          out.println("  <tr>");
          out.println("    <td>" + teamNumber + " " + organization + "</td>");
          out.println("    <td align='right'>Raw: </td>");

          //subjective categories
          for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
            final Element catElement = (Element)subjectiveCategories.item(cat);
            final double catWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(catElement.getAttribute("weight")).doubleValue();
            if(catWeight > 0.0) {
              final String catName = catElement.getAttribute("name");
              rawScoreRS = stmt.executeQuery("SELECT RawScore FROM SummarizedScores WHERE TeamNumber = " + teamNumber + " AND Category = '" + catName + "' AND Tournament = '" + tournament + "'");
              final double rawScore;
              if(rawScoreRS.next()) {
                final double v = rawScoreRS.getDouble(1);
                if(rawScoreRS.wasNull()) {
                  rawScore = Double.NaN;
                } else {
                  rawScore = v;
                }
              } else {
                rawScore = Double.NaN;
              }
              out.println("    <td" + (Double.isNaN(rawScore) ? " class=warn>No Score" : ">" + SCORE_FORMAT.format(rawScore)) + "</td>");
              rawScoreRS.close();
            }
          }

          //performance
          rawScoreRS = stmt.executeQuery("SELECT RawScore FROM SummarizedScores WHERE TeamNumber = " + teamNumber + " AND Category = 'Performance' AND Tournament = '" + tournament + "'");
          final double rawScore;
          if(rawScoreRS.next()) {
            final double v = rawScoreRS.getDouble(1);
            if(rawScoreRS.wasNull()) {
              rawScore = Double.NaN;
            } else {
              rawScore = v;
            }
          } else {
            rawScore = Double.NaN;
          }
          out.println("    <td" + (Double.isNaN(rawScore) ? " class=warn>No Score" : ">" + SCORE_FORMAT.format(rawScore)) + "</td>");
          rawScoreRS.close();
          out.println("    <td>&nbsp;</td>");
          out.println("  </tr>");

          //scaled scores
          out.println("  <tr>");
          out.println("    <td>" + teamName + "</td>");
          out.println("    <td align='right'>Scaled: </td>");

          //subjective categories
          for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
            final Element catElement = (Element)subjectiveCategories.item(cat);
            final double catWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(catElement.getAttribute("weight")).doubleValue();
            if(catWeight > 0.0) {
              final String catName = catElement.getAttribute("name");
              scaledScoreRS = stmt.executeQuery("SELECT StandardizedScore FROM SummarizedScores WHERE TeamNumber = " + teamNumber + " AND Category = '" + catName + "' AND Tournament = '" + tournament + "'");
              final double scaledScore;
              if(scaledScoreRS.next()) {
                final double v = scaledScoreRS.getDouble(1);
                if(scaledScoreRS.wasNull()) {
                  scaledScore = Double.NaN;
                } else {
                  scaledScore = v;
                }
              } else {
                scaledScore = Double.NaN;
              }

              out.println("    <td" + (Double.isNaN(scaledScore) ? " class=warn>No Score" : ">" + SCORE_FORMAT.format(scaledScore)) + "</td>");

              scaledScoreRS.close();
            }
          }

          //performance
          {
            scaledScoreRS = stmt.executeQuery("SELECT StandardizedScore FROM SummarizedScores WHERE TeamNumber = " + teamNumber + " AND Category = 'Performance' AND Tournament = '" + tournament + "'");
            final double scaledScore;
            if(scaledScoreRS.next()) {
              final double v = scaledScoreRS.getDouble(1);
              if(scaledScoreRS.wasNull()) {
                scaledScore = Double.NaN;
              } else {
                scaledScore = v;
              }
            } else {
              scaledScore = Double.NaN;
            }

            out.println("    <td" + (Double.isNaN(scaledScore) ? " class=warn>No Score" :  ">" + SCORE_FORMAT.format(scaledScore)) + "</td>");
          }
          
          scaledScoreRS.close();

          //total score
          out.println("    <td" + (Double.isNaN(totalScore) ? " class=warn>No Score" : ">" + SCORE_FORMAT.format(totalScore)) + "</td>");

          out.println("  <tr><td colspan='" + (subjectiveCategories.getLength() + 4) + "'><hr></td></tr>");
        }
        
        out.println("</table>");
        teamsRS.close();

        //FIX need page break here

      } //end while(divisionIter.next())

    } catch(final ParseException pe) {
      throw new RuntimeException("Error parsing category weight!", pe);
    } finally {
      Utilities.closeResultSet(rawScoreRS);
      Utilities.closeResultSet(teamsRS);
      Utilities.closeResultSet(scaledScoreRS);
      
      Utilities.closeStatement(stmt);
      Utilities.closeStatement(teamsStmt);
    }
  }

  private static final NumberFormat SCORE_FORMAT = NumberFormat.getInstance();
  static {
    SCORE_FORMAT.setMaximumFractionDigits(2);
    SCORE_FORMAT.setMinimumFractionDigits(2);
  }    

}
