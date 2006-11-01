/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import fll.Queries;
import fll.Utilities;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.ParseException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Java code used in judges.jsp
 *
 * @version $Revision$
 */
public final class Judges {
   
  private Judges() {
     
  }

  /**
   * Generate the judges page
   */
  public static void generatePage(final JspWriter out,
                                  final ServletContext application,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response)
    throws SQLException, IOException, ParseException {
    final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
    final Connection connection = (Connection)application.getAttribute("connection");
    final String tournament = Queries.getCurrentTournament(connection);
    final String submitButton = request.getParameter("submit");

    final NodeList subjectiveCategories = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");

    // count the number of rows present
    int rowIndex= 0;
    while(null != request.getParameter("cat" + (rowIndex+1))) {
      ++rowIndex;
      out.println("<!-- found a row " + rowIndex+ "-->");
    }
    if("Add Row".equals(submitButton)) {
      out.println("<!-- adding another row to " + rowIndex+ "-->");
      ++rowIndex;
    }
    out.println("<!-- final count of rows is " + rowIndex + "-->");
    final int numRows = rowIndex + 1;
    out.println("<form action='judges.jsp' method='POST' name='judges'>");

    String errorString = null;
    if("Finished".equals(submitButton)) {
      errorString = generateVerifyTable(out, subjectiveCategories, request);
    } else if("Commit".equals(submitButton)) {
      commitData(subjectiveCategories, request, response, connection, Queries.getCurrentTournament(connection));
    }

    if(null == submitButton || "Cancel".equals(submitButton) || "Add Row".equals(submitButton) || null != errorString) {
      if(null != errorString) {
        out.println("<p id='error'><font color='red'>" + errorString + "</font></p>");
      }

      //get list of divisions and add "All" as a possible value 
      final List<String> divisions = Queries.getDivisions(connection);
      divisions.add(0, "All");
      
      out.println("<p>Judges ID's must be unique.  They can be just the name of the judge.  Keep in mind that this ID needs to be entered on the judging forms.  There must be at least 1 judge for each category.</p>");
      
      out.println("<table border='1'><tr><th>ID</th><th>Category</th><th>Division</th></tr>");

      int row = 0; //keep track of which row we're generating
      
      if(null == request.getParameter("cat0")) {
        //this is the first time the page has been visited so we need to read
        //the judges out of the DB
        ResultSet rs = null;
        Statement stmt = null;
        try {
          stmt = connection.createStatement();
          rs = stmt.executeQuery("SELECT id, category, event_division FROM Judges WHERE Tournament = '" + tournament + "'");
          for(row=0; rs.next(); row++) {
            final String id = rs.getString(1);
            final String category = rs.getString(2);
            final String division = rs.getString(3);
            generateRow(out, subjectiveCategories, divisions, row, id, category, division);
          }
        } finally {
          Utilities.closeResultSet(rs);
          Utilities.closeStatement(stmt);
        }
      } else {
        //need to walk the parameters to see what we've been passed
        String id = request.getParameter("id" + row);
        String category = request.getParameter("cat" + row);
        String division = request.getParameter("div" + row);
        while(null != category) {
          generateRow(out, subjectiveCategories, divisions, row, id, category, division);
          
          row++;
          id = request.getParameter("id" + row);
          category= request.getParameter("cat" + row);
          division = request.getParameter("div" + row);
        }
      }

      //if there aren't enough judges in the database for each category,
      //generate some more
      final int tableRows = Math.max(Math.max(numRows, subjectiveCategories.getLength()),
                                     row);
      
      for(; row < tableRows; row++) {
        generateRow(out, subjectiveCategories, divisions, row, null, null, null);
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
   * @param subjectiveCategories the possible categroies
   * @param divisions List of divisions as Strings, "All" is always the first
   * element in the list
   * @param row the row being generated, used for naming form elements
   * @param id id of judge, can be null
   * @param cat category of judge, can be null
   * @param division division judge is scoring, can be null
   */
  private static void generateRow(final JspWriter out,
                                  final NodeList subjectiveCategories,
                                  final List divisions,
                                  final int row,
                                  final String id,
                                  final String cat,
                                  final String division)
    throws IOException {
    out.println("<tr>");
    out.print("  <td><input type='text' name='id" + row + "'");
    if(null != id) {
      out.print(" value='" + id + "'");
    }
    out.println("></td>");
    
    out.println("  <td><select name='cat" + row + "'>");
    for(int choice=0; choice < subjectiveCategories.getLength(); choice++) {
      final Element category = (Element)subjectiveCategories.item(choice);
      final String categoryName = category.getAttribute("name");
      out.print("  <option value='" + categoryName + "'");
      if(categoryName.equals(cat)) {
        out.print(" selected");
      }
      out.println(">" + categoryName + "</option>");
    }
    out.println("  </select></td>");

    out.println("  <td><select name='div" + row + "'>");
    final Iterator divisionIter = divisions.iterator();
    while(divisionIter.hasNext()) {
      final String div = (String)divisionIter.next();
      out.print("  <option value='" + div + "'");
      if(div.equals(division)) {
        out.print(" selected");
      }
      out.println(">" + div + "</option>");
    }
    out.println("  </select></td>");
    
    out.println("</tr>");
  }

  /**
   * Validate the list of judges in request and if ok generate a final table
   * for the user to check along with buttons for submit and cancel.  If
   * something is wrong, return a useful message.
   *
   * @return null if everything is ok, otherwise the error message
   */
  private static String generateVerifyTable(final JspWriter out,
                                            final NodeList subjectiveCategories,
                                            final HttpServletRequest request) throws IOException, ParseException {
    //keep track of any errors
    final StringBuffer error = new StringBuffer();

    // keep track of which categories have judges
    final Map<String, Set<String>> hash = new HashMap<String, Set<String>>();
    
    //populate a hash where key is category name and value is an empty
    //Set.  Use set so there are no duplicates
    for(int i=0; i<subjectiveCategories.getLength(); i++) {
      final String categoryName = ((Element)subjectiveCategories.item(i)).getAttribute("name");
      hash.put(categoryName, new HashSet<String>());
    }
    
    //walk request and push judge id into the Set, if not null or empty,
    //in the value for each category in the hash.
    int row=0;
    String id = request.getParameter("id" + row);
    String category= request.getParameter("cat" + row);
    while(null != category) {
      if(null != id) {
        id = id.trim();
        id = id.toUpperCase();
        if(id.length() > 0) {
          final Set<String> set = hash.get(category);
          set.add(id);
        }
      }
      
      row++;
      id = request.getParameter("id" + row);
      category= request.getParameter("cat" + row);
    }

    
    //now walk the keys of the hash and make sure that all values have a list
    //of size > 0, otherwise append an error to error.
    final Iterator keyIter = hash.keySet().iterator();
    while(keyIter.hasNext()) {
      final String categoryName = (String)keyIter.next();
      final Set set = (Set)hash.get(categoryName);
      if(set.isEmpty()) {
        error.append("You must specify at least one judge for " + categoryName + "<br>");
      }
    }
    
    if(error.length() > 0) {
      return error.toString();
    } else {
      out.println("<p>If everything looks ok, click Commit, otherwise click Cancel and you'll go back to the edit page.</p>");
      //generate final table with submit button
      out.println("<table border='1'><tr><th>ID</th><th>Category</th><th>Division</th></tr>");


      //walk request and put data in a table
      row=0;
      id = request.getParameter("id" + row);
      category= request.getParameter("cat" + row);
      String division = request.getParameter("div" + row);
      while(null != category) {
        if(null != id && division != null) {
          id = id.trim();
          id = id.toUpperCase();
          if(id.length() > 0) {
            out.println("<tr>");
            out.println("  <td>" + id + " <input type='hidden' name='id" + row + "' value='" + id + "'></td>");
            out.println("  <td>" + category + " <input type='hidden' name='cat" + row + "' value='" + category + "'></td>");
            out.println("  <td>" + division + " <input type='hidden' name='div" + row + "' value='" + division + "'></td>");
            out.println("</tr>");
          
          }
        }
      
        row++;
        id = request.getParameter("id" + row);
        category= request.getParameter("cat" + row);
        division= request.getParameter("div" + row);
      }

      
      out.println("</table>");
      out.println("<input type='submit' name='submit' value='Commit'>");
      out.println("<input type='submit' name='submit' value='Cancel'>");
      
      return null;
    }
  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   *
   * @param tournament the current tournament
   */
  private static void commitData(final NodeList subjectiveCategories,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final Connection connection,
                                 final String tournament)
    throws SQLException, IOException {
    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      //delete old data in judges
      stmt.executeUpdate("DELETE FROM Judges where Tournament = '" + tournament + "'");
      
      //walk request parameters and insert data into database
      prep = connection.prepareStatement("INSERT INTO Judges (id, category, event_division, Tournament) VALUES(?, ?, ?, ?)");
      prep.setString(4, tournament);
      int row = 0;
      String id = request.getParameter("id" + row);
      String category= request.getParameter("cat" + row);
      String division= request.getParameter("div" + row);
      while(null != category) {
        prep.setString(1, id);
        prep.setString(2, category);
        prep.setString(3, division);
        prep.executeUpdate();
          
        row++;
        id = request.getParameter("id" + row);
        category = request.getParameter("cat" + row);
        division = request.getParameter("div" + row);
      }
      
    } finally {
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(prep);
    }
    
    //finally redirect to index.jsp 
    response.sendRedirect(response.encodeRedirectURL("index.jsp?message=Successfully+assigned+judges."));
  }

}
