/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import fll.Utilities;
import fll.Queries;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import net.mtu.eggplant.util.CollectionUtils;

/**
 * Java code used in tournaments.jsp
 *
 * @version $Revision$
 */
final public class Tournaments {
   
  private Tournaments() {
     
  }

  /**
   * Generate the tournaments page
   */
  public static void generatePage(final JspWriter out,
                                  final ServletContext application,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response)
    throws SQLException, IOException, ParseException {
    final Connection connection = (Connection)application.getAttribute("connection");
    
    final String numRowsStr = request.getParameter("numRows");
    int numRows;      
    if(null == numRowsStr) {
      numRows = 0;
    } else {
      try {
        numRows = NumberFormat.getInstance().parse(numRowsStr).intValue();
      } catch(final ParseException nfe) {
        numRows = 0;
      }
    }
    if(null != request.getParameter("addRow")) {
      numRows++;
    }
    
    out.println("<form action='tournaments.jsp' method='POST' name='tournaments'>");

    if(null != request.getParameter("commit") && verifyData(out, request)) {
      commitData(request, response, connection, application);
    } else {
      out.println("<p><b>Tournament name's must be unique and next tournament must refer to the name of another tournament listed.  Tournaments can be removed by erasing the name and location.</b></p>");
      
      out.println("<table border='1'><tr><th>Name</th><th>Location</th><th>Next Tournament</th></tr>");

      int row = 0; //keep track of which row we're generating
      
      if(null == request.getParameter("name0")) {
        //this is the first time the page has been visited so we need to read
        //the names out of the DB
        ResultSet rs = null;
        Statement stmt = null;
        try {
          stmt = connection.createStatement();
          rs = stmt.executeQuery("SELECT Name, Location, NextTournament FROM Tournaments ORDER BY Name");
          for(row=0; rs.next(); row++) {
            final String name = rs.getString(1);
            final String location = rs.getString(2);
            final String next = rs.getString(3);
            generateRow(out, row, name, location, next);
          }
        } finally {
          Utilities.closeResultSet(rs);
          Utilities.closeStatement(stmt);
        }
      } else {
        //need to walk the parameters to see what we've been passed
        String name = request.getParameter("name" + row);
        String location = request.getParameter("location" + row);
        String next = request.getParameter("next" + row); 
        while(null != name) {
          generateRow(out, row, name, location, next);
          
          row++;
          name = request.getParameter("name" + row);
          location = request.getParameter("location" + row);
          next = request.getParameter("next" + row);
        }
      }

      //if there aren't enough names in the database, generate some more
      final int tableRows = Math.max(numRows, row);
      
      for(; row < tableRows; row++) {
        generateRow(out, row, null, null, null);
      }

      out.println("</table>");
      out.println("<input type='hidden' name='numRows' value='" + tableRows + "'>");
      out.println("<input type='submit' name='addRow' value='Add Row'>");
      out.println("<input type='submit' name='commit' value='Finished'>");
    }
    
    out.println("</form>");
  }


  /**
   * Generate a row in the Tournament table defaulting the form elements to
   * the given information.
   *
   * @param out where to print
   * @param row the row being generated, used for naming form elements
   * @param name name of tournament, can be null
   * @param location location of tournament, can be null
   * @param next next tournament after this one, can be null
   */
  private static void generateRow(final JspWriter out,
                                  final int row,
                                  final String name,
                                  final String location,
                                  final String next) throws IOException {
    out.println("<tr>");

    out.print("  <input type='hidden' name='key" + row + "'");
    if(null != name) {
      out.print(" value='" + name + "'");
    } else {
      out.print(" value='new'");
    }
    out.println(">");
    
    out.print("  <td><input type='text' name='name" + row + "'");
    if(null != name) {
      out.print(" value='" + name + "'");
    }
    if("DUMMY".equals(name) || "DROP".equals(name)) {
      out.print(" readonly");
    }
    out.println(" maxlength='16' size='16'></td>");

    out.print("  <td><input type='text' name='location" + row + "'");
    if(null != location) {
      out.print(" value='" + location + "'");
    }
    if("DUMMY".equals(name) || "DROP".equals(name)) {
      out.print(" readonly");
    }
    out.println(" size='64'></td>");

    out.print("  <td><input type='text' name='next" + row + "'");
    if(null != next) {
      out.print(" value='" + next + "'");
    }
    if("DUMMY".equals(name) || "DROP".equals(name)) {
      out.print(" readonly");
    }
    out.println(" size='16'></td>");
    
    out.println("</tr>");
  }

  /**
   * Verify that the data in the request is valid.  Checks for things like
   * multiple tournaments with the same name and the next tournament parameter
   * pointing to a non-existant tournament.
   *
   * @return true if everything is ok, false otherwise and write message to
   * out
   */
  private static boolean verifyData(final JspWriter out,
                                    final HttpServletRequest request)
    throws IOException {
    final Map tournamentNames = CollectionUtils.createHashMap(20);
    boolean retval = true;
    int row = 0;
    String name = request.getParameter("name" + row);
    while(null != name) {
      if(tournamentNames.containsKey(name)) {
        out.println("<p><font color='red'>Row " + (row+1) + " contains duplicate name " + name + "</font></p>");
        retval = false;
      } else {
        tournamentNames.put(name, name);
      }

      row++;
      name = request.getParameter("name" + row);
    }

    for(int i=0; i<row; i++) {
      final String next = request.getParameter("next" + i);
      if(null != next && !"".equals(next) && !tournamentNames.containsKey(next)) {
        out.println("<p><font color='red'>Unknown tournament referenced as the next tournament for " + request.getParameter("name" + i) + ", row " + (i+1) + "</font></p>");
        retval = false;
      }
    }
    
    return retval;
  }
  
  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   */
  private static void commitData(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final Connection connection,
                                 final ServletContext application)
    throws SQLException, IOException {
    PreparedStatement updatePrep = null;
    PreparedStatement insertPrep = null;
    PreparedStatement deletePrep = null;
    try {
      //walk request parameters and insert data into database
      insertPrep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location, NextTournament) VALUES(?, ?)");
      updatePrep = connection.prepareStatement("UPDATE Tournaments SET Name = ?, Location = ?, NextTournament = ? WHERE Name = ?");
      deletePrep = connection.prepareStatement("DELETE FROM Tournaments WHERE Name = ?");
        
      int row = 0;
      String key = request.getParameter("key" + row);
      String name = request.getParameter("name" + row);
      String location = request.getParameter("location" + row);
      String next = request.getParameter("next" + row);
      while(null != name) {
        if("new".equals(key) && !"".equals(name)) {
          //new tournament
          insertPrep.setString(1, name);
          insertPrep.setString(2, name);
          insertPrep.setString(3, next);
          insertPrep.executeUpdate();
        } else {
          if("".equals(name)) {
            //delete if no name
            deletePrep.setString(1, name);
            deletePrep.executeUpdate();
          } else {
            //update with new values
            updatePrep.setString(1, name);
            updatePrep.setString(2, location);
            updatePrep.setString(3, next);
            updatePrep.setString(4, name);
            updatePrep.executeUpdate();
          }
        }
        row++;
        key = request.getParameter("key" + row);
        name = request.getParameter("name" + row);
        location = request.getParameter("location" + row);
        next = request.getParameter("next" + row);
      }
      
    } finally {
      Utilities.closePreparedStatement(insertPrep);
      Utilities.closePreparedStatement(updatePrep);
      Utilities.closePreparedStatement(deletePrep);
    }

    //reinitialize the TournamentTeams table
    Queries.populateTournamentTeams(application);
    
    //finally redirect to index.jsp
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }
}
