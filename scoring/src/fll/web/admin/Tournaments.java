/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Java code used in tournaments.jsp
 */
public final class Tournaments {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final int NEW_TOURNAMENT_KEY = -1;

  private Tournaments() {
    // no instances
  }

  /**
   * Generate the tournaments page
   */
  public static void generatePage(final JspWriter out,
                                  final ServletContext application,
                                  final HttpSession session,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response)
      throws SQLException, IOException, ParseException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final String numRowsStr = request.getParameter("numRows");
      int numRows;
      if (null == numRowsStr) {
        numRows = 0;
      } else {
        try {
          numRows = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(numRowsStr).intValue();
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
        commitData(session, request, response, connection, description);
      } else {
        out.println("<p><b>Tournament name's must be unique.  Tournaments can be removed by erasing the name.</b></p>");

        out.println("<table border='1'><tr><th>Name</th><th>Description</th></tr>");

        int row = 0; // keep track of which row we're generating

        if (null == request.getParameter("name0")) {
          // this is the first time the page has been visited so we need to read
          // the names out of the DB
          final Iterator<Tournament> tournaments = Tournament.getTournaments(connection).iterator();
          for (row = 0; tournaments.hasNext(); row++) {
            final Tournament tournament = tournaments.next();
            generateRow(out, row, tournament.getTournamentID(), tournament.getName(), tournament.getDescription());
          }
        } else {
          // need to walk the parameters to see what we've been passed
          String keyStr = request.getParameter("key"
              + row);
          String name = request.getParameter("name"
              + row);
          String location = request.getParameter("description"
              + row);
          while (null != keyStr) {
            final int key = Integer.parseInt(keyStr);
            generateRow(out, row, key, name, location);

            row++;
            keyStr = request.getParameter("key"
                + row);
            name = request.getParameter("name"
                + row);
            location = request.getParameter("description"
                + row);
          }
        }

        // if there aren't enough names in the database, generate some more
        final int tableRows = Math.max(numRows, row);

        for (; row < tableRows; row++) {
          generateRow(out, row, null, null, null);
        }

        out.println("</table>");
        out.println("<input type='hidden' name='numRows' value='"
            + tableRows
            + "'>");
        out.println("<input type='submit' name='addRow' value='Add Row'>");
        out.println("<input type='submit' name='commit' value='Finished'>");
      }

      out.println("</form>");
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Generate a row in the Tournament table defaulting the form elements to the
   * given information.
   * 
   * @param out where to print
   * @param row the row being generated, used for naming form elements
   * @param name name of tournament, can be null
   * @param description description of tournament, can be null
   * @throws SQLException
   * @throws IllegalArgumentException
   */
  private static void generateRow(final JspWriter out,
                                  final int row,
                                  final Integer key,
                                  final String name,
                                  final String description)
      throws IOException, IllegalArgumentException, SQLException {
    out.println("<tr>");

    out.print("  <input type='hidden' name='key"
        + row
        + "'");
    if (null != key) {
      out.print(" value='"
          + key
          + "'");
    } else {
      out.print(" value='"
          + NEW_TOURNAMENT_KEY
          + "'");
    }
    out.println(">");

    out.print("  <td><input type='text' name='name"
        + row
        + "'");
    if (null != name) {
      out.print(" value='"
          + WebUtils.escapeForHtmlFormValue(name)
          + "'");
    }
    if (GenerateDB.DUMMY_TOURNAMENT_NAME.equals(name)
        || GenerateDB.DROP_TOURNAMENT_NAME.equals(name)) {
      out.print(" readonly");
    }
    out.println(" maxlength='128' size='16'></td>");

    out.print("  <td><input type='text' name='description"
        + row
        + "'");
    if (null != description) {
      out.print(" value='"
          + WebUtils.escapeForHtmlFormValue(description)
          + "'");
    }
    if (GenerateDB.DUMMY_TOURNAMENT_NAME.equals(name)
        || GenerateDB.DROP_TOURNAMENT_NAME.equals(name)) {
      out.print(" readonly");
    }
    out.println(" size='64'></td>");

    out.println("</tr>");
  }

  /**
   * Verify that the data in the request is valid. Checks for things like
   * multiple tournaments with the same name.
   * 
   * @return true if everything is ok, false otherwise and write message to out
   */
  @SuppressFBWarnings(value = "XSS_REQUEST_PARAMETER_TO_JSP_WRITER", justification = "Need to write out the name specified as part of the error message")
  private static boolean verifyData(final JspWriter out,
                                    final HttpServletRequest request)
      throws IOException {
    final Set<String> tournamentNames = new HashSet<>();
    boolean retval = true;

    { // build up info of what tournaments exist
      int row = 0;
      String name = request.getParameter("name"
          + row);
      while (null != name) {
        if (!"".equals(name)) {
          if (tournamentNames.contains(name)) {
            out.println("<p><font color='red'>Row "
                + (row
                    + 1)
                + " contains duplicate name "
                + name
                + "</font></p>");
            retval = false;
          }
        }
        row++;
        name = request.getParameter("name"
            + row);
      }
    }

    return retval;
  }

  private static void createAndInsertTournaments(final HttpServletRequest request,
                                                 final Connection connection,
                                                 final ChallengeDescription description,
                                                 final StringBuilder message)
      throws SQLException {
    final int currentTournament = Queries.getCurrentTournament(connection);

    int row = 0;
    String keyStr = request.getParameter("key"
        + row);
    String name = request.getParameter("name"
        + row);
    String location = request.getParameter("description"
        + row);
    while (null != keyStr) {
      final int key = Integer.parseInt(keyStr);
      if (NEW_TOURNAMENT_KEY == key) {
        if (null != name
            && !"".equals(name)) {
          // new tournament
          Tournament.createTournament(connection, name, location);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adding a new tournament "
                + name);
          }
        }
      } else if (null == name
          || "".equals(name)) {
        final Tournament tournament = Tournament.findTournamentByID(connection, key);
        if (null != tournament) {
          if (key == currentTournament) {
            message.append("<p class='warning'>Unable to delete tournament '"
                + tournament.getName()
                + "' because it is the current tournament</p>");
          } else {
            // delete if no name
            if (tournament.containsScores(connection, description)) {
              message.append("<p class='warning'>Unable to delete tournament '"
                  + tournament.getName()
                  + "' that contains scores</p>");
            } else {
              Tournament.deleteTournament(connection, key);
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleting a tournament "
                    + key);
              }
            }
          } // not current tournament
        } // tournament exists
      } else {
        // update with new values
        Queries.updateTournament(connection, key, name, location);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Updating a tournament "
              + key
              + " to "
              + name);
        }
      }

      row++;
      keyStr = request.getParameter("key"
          + row);
      name = request.getParameter("name"
          + row);
      location = request.getParameter("description"
          + row);
    }
  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   * 
   * @param description
   */
  private static void commitData(final HttpSession session,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final Connection connection,
                                 final ChallengeDescription description)
      throws SQLException, IOException {
    final StringBuilder message = new StringBuilder();

    createAndInsertTournaments(request, connection, description, message);

    message.append("<p id='success'>Committed tournament changes.</p>");

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());

    // finally redirect to index.jsp
    // out.println("DEBUG: normally you'd be redirected to <a
    // href='index.jsp'>here</a>");
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

}
