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
    
    out.println("<form action='tournaments.jsp' method='GET' name='tournaments'>");

    if(null != request.getParameter("commit")) {
      commitData(request, response, connection, application);
    } else {
      out.println("<p><b>Tournament name's must be unique, otherwise the last one in the table will overwrite any others above it with the same name.</b></p>");
      
      out.println("<table border='1'><tr><th>Name</th><th>Location</th></tr>");

      int row = 0; //keep track of which row we're generating
      
      if(null == request.getParameter("name0")) {
        //this is the first time the page has been visited so we need to read
        //the names out of the DB
        ResultSet rs = null;
        Statement stmt = null;
        try {
          stmt = connection.createStatement();
          rs = stmt.executeQuery("SELECT Name, Location FROM Tournaments");
          for(row=0; rs.next(); row++) {
            final String name = rs.getString(1);
            final String location = rs.getString(2);
            if(!"DUMMY".equals(name)) {
              generateRow(out, row, name, location);
            } else {
              row--;
            }
          }
        } finally {
          Utilities.closeResultSet(rs);
          Utilities.closeStatement(stmt);
        }
      } else {
        //need to walk the parameters to see what we've been passed
        String name = request.getParameter("name" + row);
        String location = request.getParameter("location" + row);
        while(null != name) {
          generateRow(out, row, name, location);
          
          row++;
          name = request.getParameter("name" + row);
          location = request.getParameter("location" + row);
        }
      }

      //if there aren't enough names in the database, generate some more
      final int tableRows = Math.max(numRows, row);
      
      for(; row < tableRows; row++) {
        generateRow(out, row, null, null);
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
   * @param location location of region, can be null
   */
  private static void generateRow(final JspWriter out,
                                  final int row,
                                  final String name,
                                  final String location) throws IOException {
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
    out.println(" maxlength='16' size='16'></td>");

    out.print("  <td><input type='text' name='location" + row + "'");
    if(null != location) {
      out.print(" value='" + location + "'");
    }
    out.println(" size='32'></td>");

    out.println("</tr>");
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
      insertPrep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES(?, ?)");
      updatePrep = connection.prepareStatement("UPDATE Tournaments SET Name = ?, Location = ? WHERE Name = ?");
      deletePrep = connection.prepareStatement("DELETE FROM Tournaments WHERE Name = ?");
        
      int row = 0;
      String key = request.getParameter("key" + row);
      String name = request.getParameter("name" + row);
      String location = request.getParameter("location" + row);
      while(null != name) {
        if("new".equals(key) && !"".equals(name)) {
          //new tournament
          insertPrep.setString(1, name);
          insertPrep.setString(2, name);
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
            updatePrep.setString(3, name);
            updatePrep.executeUpdate();
          }
        }
        row++;
        key = request.getParameter("key" + row);
        name = request.getParameter("name" + row);
        location = request.getParameter("location" + row);
      }
      
    } finally {
      Utilities.closePreparedStatement(insertPrep);
      Utilities.closePreparedStatement(updatePrep);
      Utilities.closePreparedStatement(deletePrep);
    }

    //reinitialize the TournamentTeams table
    Queries.initializeTournamentTeams(connection);
    Queries.populateTournamentTeams(application);
    
    //finally redirect to index.jsp 
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }
}
