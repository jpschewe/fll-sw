/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import javax.servlet.jsp.JspWriter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Queries;
import fll.Utilities;

import java.text.NumberFormat;

import java.io.IOException;

import java.util.Iterator;


/**
 * Code for soreGroupScores.jsp.
 *
 * @version $Revision$
 */
final public class ScoreGroupScores {
   
  private ScoreGroupScores() {
     
  }

  /**
   * Generate the actual report for category scores
   */
  public static void generateReport(final String tournament,
                                    final Document document,
                                    final Connection connection,
                                    final JspWriter out)
    throws SQLException, IOException {

    final NodeList subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
      
    out.println("<h1>FLL Categorized Score Summary by score group</h1>");

    out.println("<hr>");

    out.println("<h2>Tournament: " + tournament + "</h2>");

      
    //get the list of divisions
    final Iterator divisionIter = Queries.getDivisions(connection).iterator();
    while(divisionIter.hasNext()) {
      final String division = (String)divisionIter.next();

      //foreach category
      for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String catName = catElement.getAttribute("name");
        final String catTitle = catElement.getAttribute("title");

        generateCategoryTable(tournament, connection, out, catName, catTitle, division);
      }

      //FIX need page break here
    }
  }

  /**
   * Generate the table for a category
   */
  private static void generateCategoryTable(final String tournament,
                                            final Connection connection,
                                            final JspWriter out,
                                            final String categoryName,
                                            final String categoryTitle,
                                            final String division)
    throws SQLException, IOException {
    
    Statement teamsStmt = null;
    ResultSet teamsRS = null;
    Statement stmt = null;
    ResultSet scoreRS = null;
    Statement groupStmt = null;
    ResultSet groupRS = null;
    try {
      teamsStmt = connection.createStatement();
      stmt = connection.createStatement();
      groupStmt = connection.createStatement();

      groupRS = groupStmt.executeQuery("SELECT ScoreGroup FROM SummarizedScores,Teams"
                                       + " WHERE SummarizedScores.Tournament = '" + tournament + "'"
                                       + " AND Teams.TeamNumber = SummarizedScores.TeamNumber"
                                       + " AND SummarizedScores.Category = '" + categoryName + "'"
                                       + " AND Teams.Division = " + division
                                       + " GROUP BY ScoreGroup");
      while(groupRS.next()) {
        final String scoreGroup = groupRS.getString(1);
        
        out.println("<h3>" + categoryTitle + " Division: " + division + " Score Group: " + scoreGroup + "</h3>");
          
        out.println("<table border='0'>");
        out.println("  <tr>");
        out.println("    <th colspan='3'>Team # / Organization / Team Name</th>");
        out.println("    <th>Score</th>");
        out.println("  </tr>");

        out.println("  <tr><td colspan='4'><hr></td></tr>");
          
        //display all team scores
        teamsRS =
          teamsStmt.executeQuery("SELECT Teams.Organization,Teams.TeamName,Teams.TeamNumber,SummarizedScores.StandardizedScore"
                                 + " FROM SummarizedScores,Teams"
                                 + " WHERE Teams.TeamNumber = SummarizedScores.TeamNumber"
                                 + " AND SummarizedScores.Tournament = '" + tournament + "'"
                                 + " AND Teams.Division = " + division
                                 + " AND SummarizedScores.Category = '" + categoryName + "'"
                                 + " AND SummarizedScores.ScoreGroup = '" + scoreGroup + "'"
                                 + " ORDER BY SummarizedScores.StandardizedScore DESC, Teams.TeamNumber");
        while(teamsRS.next()) {
          final int teamNumber = teamsRS.getInt(3);
          final String organization = teamsRS.getString(1);
          final String teamName = teamsRS.getString(2);

          final double score;
          double v = teamsRS.getDouble(4);
          if(teamsRS.wasNull()) {
            score = Double.NaN;
          } else {
            score = v;
          }

          out.println("  <tr>");

          out.println("    <td>" + teamNumber + "</td>");
          out.println("    <td>" + organization + "</td>");
          out.println("    <td>" + teamName + "</td>");
            
          out.println("    <td>" + SCORE_FORMAT.format(score) + "</td>");

          out.println("  </tr>");
            
          out.println("  <tr><td colspan='4'><hr</td></tr>");
        } //end loop over teams
        Utilities.closeResultSet(teamsRS);

        out.println("</table>");
      }//end loop over score groups
      Utilities.closeResultSet(groupRS);
      
    } finally {
      Utilities.closeResultSet(teamsRS);
      Utilities.closeResultSet(scoreRS);
      Utilities.closeResultSet(groupRS);
      Utilities.closeStatement(teamsStmt);
      Utilities.closeStatement(stmt);
      Utilities.closeStatement(groupStmt);
    }
  }
  
  private static final NumberFormat SCORE_FORMAT = NumberFormat.getInstance();
  static {
    SCORE_FORMAT.setMaximumFractionDigits(2);
    SCORE_FORMAT.setMinimumFractionDigits(2);
  }    
    
}
