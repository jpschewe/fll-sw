/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.db.Queries;
import fll.web.ApplicationAttributes;

/**
 * Java code used in judges.jsp
 *
 * @version $Revision$
 */
public final class Tables {
   
  private Tables() {
     
  }

  /**
   * Generate the judges page
   */
  public static void generatePage(final JspWriter out,
                                  final ServletContext application,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response)
    throws SQLException, IOException, ParseException {
    final Connection connection = (Connection)application.getAttribute(ApplicationAttributes.CONNECTION);
    final String tournament = Queries.getCurrentTournament(connection);
    final String submitButton = request.getParameter("submit");

    int rowIndex= 0;
    while(null != request.getParameter("SideA" + (rowIndex+1))) {
      ++rowIndex;
      out.println("<!-- found a row " + rowIndex+ "-->");
    }
    if("Add Row".equals(submitButton)) {
      out.println("<!-- adding another row to " + rowIndex+ "-->");
      ++rowIndex;
    }
    out.println("<!-- final count of rows is " + rowIndex + "-->");
    final int numRows = rowIndex + 1;
    

    out.println("<form action='tables.jsp' method='POST' name='tables'>");

    String errorString = null;
    if("Finished".equals(submitButton)) {
      errorString = commitData(request, response, connection, Queries.getCurrentTournament(connection));
    }

    if(null == submitButton || "Add Row".equals(submitButton) || null != errorString) {
      if(null != errorString) {
        out.println("<p id='error'><font color='red'>" + errorString + "</font></p>");
      }
      
      out.println("<p>Table labels should be unique. These labels must occur in pairs, where a label refers to a single side of a table. E.g. If the skirt of a table was red on one side and green on the other, the labels could be Red and Green, but if the table was red all around they could be Red1 and Red2.</p>");
      
      out.println("<table border='1'><tr><th>Side A</th><th>Side B</th><th>Delete?</th></tr>");

      int row = 0; //keep track of which row we're generating
      
      if(null == request.getParameter("SideA0")) {
        //this is the first time the page has been visited so we need to read
        //the table labels out of the DB
        ResultSet rs = null;
        Statement stmt = null;
        try {
          stmt = connection.createStatement();
          rs = stmt.executeQuery("SELECT SideA,SideB FROM tablenames WHERE Tournament = '" + tournament + "' ORDER BY PairID");
          for(row=0; rs.next(); row++) {
            final String sideA = rs.getString(1);
            final String sideB = rs.getString(2);
            generateRow(out, row, sideA, sideB, "");
          }
        } finally {
          SQLFunctions.closeResultSet(rs);
          SQLFunctions.closeStatement(stmt);
        }
      } else {
        //need to walk the parameters to see what we've been passed
        String sideA = request.getParameter("SideA" + row);
        String sideB = request.getParameter("SideB" + row);
        String delete = request.getParameter("delete" + row);
        while(null != sideA) {
          generateRow(out, row, sideA, sideB, delete);
          
          row++;
          sideA = request.getParameter("SideA" + row);
          sideB= request.getParameter("SideB" + row);
          delete = request.getParameter("delete" + row);
        }
      }

      final int tableRows = Math.max(numRows, row);
      for(; row < tableRows; row++) {
        generateRow(out, row, null, null, null);
      }
      
      out.println("</table>");
      out.println("<input type='submit' name='submit' value='Add Row'>");
      out.println("<input type='submit' name='submit' value='Finished'>");
    }
    
    out.println("</form>");
  }


  /**
   * Generate a row in the judges table defaulting the form elemenets to the
   * given information.
   *
   * @param out where to print
   * element in the list
   * @param row the row being generated, used for naming form elements
   * @param sideA Label for side A of the table, can be null
   * @param sideB Label for side B of the table, can be null
   * @param delete Either "checked" or null, depending on whether the check box should initially be checked or not.
   */
  private static void generateRow(final JspWriter out,
                                  final int row,
                                  final String sideA,
                                  final String sideB,
                                  final String delete)
    throws IOException {
    out.println("<tr>");
    out.print("  <td><input type='text' name='SideA" + row + "'");
    if(null != sideA) {
      out.print(" value='" + sideA + "'");
    }
    out.println("></td>");
    out.print("  <td><input type='text' name='SideB" + row + "'");
    if(null != sideA) {
      out.print(" value='" + sideB + "'");
    }
    out.println("></td>");

    out.println("  <td><input type='checkbox' value='checked' name='delete" + row + "' " + (null == delete ? "" : delete) + ">");

    out.println("  </td>");
    
    out.println("</tr>");
  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   *
   * @param tournament the current tournament
   */
  private static String commitData(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final Connection connection,
                                 final String tournament)
    throws SQLException, IOException {
    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      //delete old data in tablenames
      stmt.executeUpdate("DELETE FROM tablenames where Tournament = '" + tournament + "'");
      
      //walk request parameters and insert data into database
      prep = connection.prepareStatement("INSERT INTO tablenames (Tournament, PairID, SideA, SideB) VALUES('" + tournament + "',?, ?, ?)");
      int row = 0;
      String sideA = request.getParameter("SideA" + row);
      String sideB = request.getParameter("SideB" + row);
      String delete= request.getParameter("delete" + row);
      while(null != sideA) {
        if (null == delete || delete.equals("")) {
          // Don't put blank entries in the database
          if (!(sideA.trim().equals("") && sideB.trim().equals(""))) {
            prep.setInt(1, row);
            prep.setString(2, sideA);
            prep.setString(3, sideB);
            prep.executeUpdate();
          }
        }
          
        row++;
        sideA = request.getParameter("SideA" + row);
        sideB = request.getParameter("SideB" + row);
        delete = request.getParameter("delete" + row);
      }
      
    } finally {
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closePreparedStatement(prep);
    }
    
    //finally redirect to index.jsp 
    response.sendRedirect(response.encodeRedirectURL("index.jsp?message=Successfully+assigned+tables."));
    return null;
  }

}
