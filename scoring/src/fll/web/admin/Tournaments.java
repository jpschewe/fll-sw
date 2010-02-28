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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.hsqldb.Types;

import fll.Utilities;
import fll.db.Queries;
import fll.web.SessionAttributes;

/**
 * Java code used in tournaments.jsp
 */
public final class Tournaments {

  private static final Logger LOGGER = Logger.getLogger(Tournaments.class);

  private static final int NEW_TOURNAMENT_KEY = -1;

  private Tournaments() {
    // no instances
  }

  /**
   * Generate the tournaments page
   */
  public static void generatePage(final JspWriter out, final HttpSession session, final HttpServletRequest request, final HttpServletResponse response)
      throws SQLException, IOException, ParseException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Connection connection = datasource.getConnection();

    final String numRowsStr = request.getParameter("numRows");
    int numRows;
    if (null == numRowsStr) {
      numRows = 0;
    } else {
      try {
        numRows = Utilities.NUMBER_FORMAT_INSTANCE.parse(numRowsStr).intValue();
      } catch (final ParseException nfe) {
        numRows = 0;
      }
    }
    if (null != request.getParameter("addRow")) {
      numRows++;
    }

    out.println("<form action='tournaments.jsp' method='POST' name='tournaments'>");

    final boolean verified;
    if (null != request.getParameter("commit")
        && verifyData(out, request)) {
      verified = true;
    } else {
      verified = false;
    }

    if (verified) {
      commitData(request, response, connection);
    } else {
      out
         .println("<p><b>Tournament name's must be unique and next tournament must refer to the name of another tournament listed.  Tournaments can be removed by erasing the name.</b></p>");

      out.println("<table border='1'><tr><th>Name</th><th>Location</th><th>Next Tournament</th></tr>");

      int row = 0; // keep track of which row we're generating

      if (null == request.getParameter("name0")) {
        // this is the first time the page has been visited so we need to read
        // the names out of the DB
        ResultSet rs = null;
        Statement stmt = null;
        try {
          stmt = connection.createStatement();
          rs = stmt.executeQuery("SELECT Name, Location, NextTournament, tournament_id FROM Tournaments ORDER BY Name");
          for (row = 0; rs.next(); row++) {
            final String name = rs.getString(1);
            final String location = rs.getString(2);
            final int next = rs.getInt(3);
            final String nextName;
            if (!rs.wasNull()) {
              nextName = Queries.getTournamentName(connection, next);
            } else {
              nextName = null;
            }
            final int tournamentID = rs.getInt(4);
            generateRow(out, row, tournamentID, name, location, nextName);
          }
        } finally {
          SQLFunctions.closeResultSet(rs);
          SQLFunctions.closeStatement(stmt);
        }
      } else {
        // need to walk the parameters to see what we've been passed
        String keyStr = request.getParameter("key"
            + row);
        String name = request.getParameter("name"
            + row);
        String location = request.getParameter("location"
            + row);
        String nextName = request.getParameter("next"
            + row);
        while (null != keyStr) {
          final int key = Integer.valueOf(keyStr);
          generateRow(out, row, key, name, location, nextName);

          row++;
          keyStr = request.getParameter("key"
              + row);
          name = request.getParameter("name"
              + row);
          location = request.getParameter("location"
              + row);
          nextName = request.getParameter("next"
              + row);
        }
      }

      // if there aren't enough names in the database, generate some more
      final int tableRows = Math.max(numRows, row);

      for (; row < tableRows; row++) {
        generateRow(out, row, null, null, null, null);
      }

      out.println("</table>");
      out.println("<input type='hidden' name='numRows' value='"
          + tableRows + "'>");
      out.println("<input type='submit' name='addRow' value='Add Row'>");
      out.println("<input type='submit' name='commit' value='Finished'>");
    }

    out.println("</form>");
  }

  /**
   * Generate a row in the Tournament table defaulting the form elements to the
   * given information.
   * 
   * @param out where to print
   * @param row the row being generated, used for naming form elements
   * @param name name of tournament, can be null
   * @param location location of tournament, can be null
   * @param nextName next tournament after this one, can be null
   * @throws SQLException
   * @throws IllegalArgumentException
   */
  private static void generateRow(final JspWriter out, final int row, final Integer key, final String name, final String location, final String nextName)
      throws IOException, IllegalArgumentException, SQLException {
    out.println("<tr>");

    out.print("  <input type='hidden' name='key"
        + row + "'");
    if (null != key) {
      out.print(" value='"
          + key + "'");
    } else {
      out.print(" value='"
          + NEW_TOURNAMENT_KEY + "'");
    }
    out.println(">");

    out.print("  <td><input type='text' name='name"
        + row + "'");
    if (null != name) {
      out.print(" value='"
          + name + "'");
    }
    if ("DUMMY".equals(name)
        || "DROP".equals(name)) {
      out.print(" readonly");
    }
    out.println(" maxlength='16' size='16'></td>");

    out.print("  <td><input type='text' name='location"
        + row + "'");
    if (null != location) {
      out.print(" value='"
          + location + "'");
    }
    if ("DUMMY".equals(name)
        || "DROP".equals(name)) {
      out.print(" readonly");
    }
    out.println(" size='64'></td>");

    out.print("  <td><input type='text' name='next"
        + row + "'");
    if (null != nextName) {
      out.print(" value='"
          + nextName + "'");
    }
    if ("DUMMY".equals(name)
        || "DROP".equals(name)) {
      out.print(" readonly");
    }
    out.println(" size='16'></td>");

    out.println("</tr>");
  }

  /**
   * Verify that the data in the request is valid. Checks for things like
   * multiple tournaments with the same name and the next tournament parameter
   * pointing to a non-existant tournament.
   * 
   * @return true if everything is ok, false otherwise and write message to out
   */
  private static boolean verifyData(final JspWriter out, final HttpServletRequest request) throws IOException {
    final Map<String, String> tournamentNames = new HashMap<String, String>();
    boolean retval = true;

    { // build up info of what tournaments exist
      int row = 0;
      String name = request.getParameter("name"
          + row);
      String next = request.getParameter("next"
          + row);
      while (null != name) {
        if (!"".equals(name)) {
          if (tournamentNames.containsKey(name)) {
            out.println("<p><font color='red'>Row "
                + (row + 1) + " contains duplicate name " + name + "</font></p>");
            retval = false;
          } else {
            if ("".equals(next)) {
              next = null;
            }
            tournamentNames.put(name, next);
          }
        }
        row++;
        name = request.getParameter("name"
            + row);
        next = request.getParameter("next"
            + row);
      }
    }

    // loop over tournamentNames to find circularities and unknown tournaments
    final Set<String> visited = new HashSet<String>();
    final Iterator<Map.Entry<String, String>> tournIter = tournamentNames.entrySet().iterator();
    while (tournIter.hasNext()) {
      final Map.Entry<String, String> entry = tournIter.next();
      final String current = entry.getKey();
      String next = entry.getValue();
      if (null != next
          && !tournamentNames.containsKey(next)) {
        out.println("<p><font color='red'>Unknown tournament referenced as the next tournament for "
            + current + ", next: " + next + "</font></p>");
        retval = false;
      }

      final Set<String> nextList = new HashSet<String>();
      while (null != next) {
        if (nextList.contains(next)) {
          out.println("<p><font color='red'>Circularity found starting with tournament: "
              + current + " containing tournaments: " + nextList + "</font></p>");
          retval = false;
          // error, stop now so we don't get too deep
          next = null;
        } else if (visited.contains(next)) {
          // already followed this path, short circuit
          next = null;
        } else {
          nextList.add(next);
          next = tournamentNames.get(next);
        }
      }
      visited.addAll(nextList);
      visited.add(current);
    }

    return retval;
  }

  private static void createAndInsertTournaments(final HttpServletRequest request, final Connection connection) throws SQLException {
    PreparedStatement updatePrep = null;
    PreparedStatement insertPrep = null;
    PreparedStatement deletePrep = null;
    try {
      insertPrep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES(?, ?)");
      updatePrep = connection.prepareStatement("UPDATE Tournaments SET Name = ?, Location = ? WHERE tournament_id = ?");
      deletePrep = connection.prepareStatement("DELETE FROM Tournaments WHERE tournament_id = ?");

      int row = 0;
      String keyStr = request.getParameter("key"
          + row);
      String name = request.getParameter("name"
          + row);
      String location = request.getParameter("location"
          + row);
      while (null != keyStr) {
        final int key = Integer.valueOf(keyStr);
        if (NEW_TOURNAMENT_KEY == key) {
          if (null != name
              && !"".equals(name)) {
            // new tournament
            insertPrep.setString(1, name);
            insertPrep.setString(2, location);
            insertPrep.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Adding a new tournament "
                  + name);
            }
          }
        } else if (null == name
            || "".equals(name)) {
          // delete if no name
          deletePrep.setInt(1, key);
          deletePrep.executeUpdate();
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Deleting a tournament "
                + key);
          }
        } else {
          // update with new values
          updatePrep.setString(1, name);
          updatePrep.setString(2, location);
          updatePrep.setInt(3, key);
          updatePrep.executeUpdate();
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Updating a tournament "
                + key + " to " + name);
          }
        }

        row++;
        keyStr = request.getParameter("key"
            + row);
        name = request.getParameter("name"
            + row);
        location = request.getParameter("location"
            + row);
      }
    } finally {
      SQLFunctions.closePreparedStatement(insertPrep);
      SQLFunctions.closePreparedStatement(updatePrep);
      SQLFunctions.closePreparedStatement(deletePrep);
    }
  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   */
  private static void commitData(final HttpServletRequest request, final HttpServletResponse response, final Connection connection) throws SQLException,
      IOException {
    createAndInsertTournaments(request, connection);
    setNextTournaments(request, connection);

    // finally redirect to index.jsp
    // out.println("DEBUG: normally you'd be redirected to <a href='index.jsp'>here</a>");
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

  private static void setNextTournaments(final HttpServletRequest request, final Connection connection) throws SQLException {
    PreparedStatement setNextPrep = null;
    try {
      setNextPrep = connection.prepareStatement("UPDATE Tournaments SET NextTournament = ? WHERE tournament_id = ?");

      int row = 0;
      String keyStr = request.getParameter("key"
          + row);
      String name = request.getParameter("name"
          + row);
      String next = request.getParameter("next"
          + row);
      while (null != keyStr) {
        if (null != name
            && !"".equals(name)) {
          
        
          final int tournamentID = Queries.getTournamentID(connection, name);
          setNextPrep.setInt(2, tournamentID);
          if(null != next
              && !"".equals(next)) {
            final int nextTournamentID = Queries.getTournamentID(connection, next);            
            setNextPrep.setInt(1, nextTournamentID);
          } else {
            setNextPrep.setNull(1, Types.INTEGER);
          }
          setNextPrep.executeUpdate();
        }

        row++;
        keyStr = request.getParameter("key"
            + row);
        name = request.getParameter("name"
            + row);
        next = request.getParameter("next"
            + row);
      }
    } finally {
      SQLFunctions.closePreparedStatement(setNextPrep);
    }
  }
}
