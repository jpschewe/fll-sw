/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadSpreadsheet;

/**
 * Java code for uploading team data to the database. Called from
 * teamColumnSelection.jsp.
 */
@WebServlet("/admin/UploadTeams")
public final class UploadTeams extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    final String fileName = SessionAttributes.getNonNullAttribute(session, "spreadsheetFile", String.class);

    File file = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      file = new File(fileName);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Wrote teams data to: "
            + file.getAbsolutePath());
      }

      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      parseFile(file, sheetName, connection, session);
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving team data into the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving team data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving team data into the database", e);
    } finally {
      SQLFunctions.close(connection);
      if (null != file) {
        if (!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));

  }

  /**
   * Take file and parse it as tab delimited.
   * 
   * @param file the file that the teams are in
   * @param connection the database connection to connect to
   * @param session used to store the list of columns in a format suitable for
   *          use in selection form elements. The attribute name is
   *          columnSelectOptions.
   * @throws SQLException if an error occurs talking to the database
   * @throws IOException if an error occurs reading from file
   * @throws InvalidFormatException
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate the list of columns to create AllTeams table")
  public static void parseFile(final File file,
                               final String sheetName,
                               final Connection connection,
                               final HttpSession session)
      throws SQLException, IOException, InvalidFormatException {
    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);

    // stores <option value='columnName'>columnName</option> for each column
    final StringBuffer selectOptions = new StringBuffer();

    // parse out the first line as the names of the columns
    // final List<String> columnNames = splitLine(reader.readLine());
    final String[] columnNames = reader.readNext();

    // build the SQL for inserting a row into the temporary table
    final StringBuffer insertPrepSQL = new StringBuffer();
    insertPrepSQL.append("INSERT INTO AllTeams VALUES(");

    // build the SQL for creating the temporary tables
    final StringBuffer createTable = new StringBuffer();
    createTable.append("CREATE TABLE AllTeams (");

    // iterate over each column name and append to appropriate buffers
    boolean first = true;
    final List<String> columnNamesSeen = new LinkedList<String>();
    for (final String header : columnNames) {
      final String columnName = sanitizeColumnName(header);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("header: "
            + header + " columnName: " + columnName);
      }
      if (columnNamesSeen.contains(columnName)) {
        throw new RuntimeException("Duplicate column name found: "
            + columnName);
      } else {
        columnNamesSeen.add(columnName);
      }

      if (first) {
        first = false;
      } else {
        createTable.append(", ");
        insertPrepSQL.append(", ");
      }
      createTable.append(columnName
          + " longvarchar");
      insertPrepSQL.append("?");
      selectOptions.append("<option value='"
          + columnName + "'>" + columnName + "</option>");
    }
    createTable.append(")");
    insertPrepSQL.append(")");

    PreparedStatement insertPrep = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS AllTeams"); // make sure the
      // table doesn't yet
      // exist
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("creating table: "
            + createTable.toString());
      }
      stmt.executeUpdate(createTable.toString()); // create AllTeams

      insertPrep = connection.prepareStatement(insertPrepSQL.toString());

      insertLinesIntoAllTeams(reader, columnNamesSeen, insertPrep);
    } finally {
      SQLFunctions.close(insertPrep);
      SQLFunctions.close(stmt);
    }

    // save this for other pages to use
    session.setAttribute("columnSelectOptions", selectOptions.toString());
  }

  private static void insertLinesIntoAllTeams(final CellFileReader reader,
                                              final List<String> columnNamesSeen,
                                              final PreparedStatement insertPrep)
      throws IOException, SQLException {
    try {
      // loop over the rest of the rows and insert them into AllTeams
      String[] values;
      while (null != (values = reader.readNext())) {
        if (values.length > 0) { // skip empty lines

          boolean allEmpty = true;
          try {
            int column = 1;
            for (final String value : values) {
              if (column <= columnNamesSeen.size()) {
                if (null != value) {
                  final String trimmed = value.trim();
                  if (trimmed.length() > 0) {
                    allEmpty = false;
                  }
                } else {
                  insertPrep.setString(column, null);
                }
                String finalValue = null;
                if (null != value) {
                  int v = 0;
                  try {
                    v = Integer.parseInt(value.trim());
                    finalValue = String.valueOf(v);
                  } catch (NumberFormatException e) {
                    finalValue = value.trim();
                  }
                }
                insertPrep.setString(column, finalValue);
                ++column;
              }
            }
            for (; column <= columnNamesSeen.size(); column++) {
              insertPrep.setString(column, null);
            }

            if (!allEmpty) {
              insertPrep.executeUpdate();
            }
          } catch (final SQLException e) {
            throw new FLLRuntimeException("Error inserting row in to AllTeam: "
                + Arrays.toString(values), e);
          }
        }
      }
    } finally {
      SQLFunctions.close(insertPrep);
    }
  }

  /**
   * Verify the team information. Used with verifyTeams.jsp
   * <ul>
   * <li>Make sure that the column for TeamNumber is specified</li>
   * <li>Make sure that the TeamNumbers are unique</li>
   * <li>Insert the new teams into the Teams table</li>
   * <li>update tournament teams table</li>
   * </ul>
   * 
   * @param connection connection with admin priviliges to DB
   * @param request used to get the information about which columns are mapped
   *          to which
   * @param response used to redirect pages for errors
   * @param session used to store error messages
   * @param out used to output information/messages to user user about the
   *          verification
   * @return if successful
   * @throws SQLException on error talking to DB
   * @throws IOException on error writing to webpage
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
                                "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate the list of columns for AllTeams table, Can't use PreparedStatement for constant value to select when inserting dummy tournament id")
  public static boolean verifyTeams(final Connection connection,
                                    final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final HttpSession session,
                                    final JspWriter out)
      throws SQLException, IOException {
    final String teamNumberColumn = request.getParameter("TeamNumber");
    if (null == teamNumberColumn
        || "".equals(teamNumberColumn)) {
      // Error, redirect back to teamColumnSelection.jsp with error message
      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>You must specify a column for TeamNumber</p>");
      response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
      return false;
    }

    final String tournamentColumn = request.getParameter("tournament");
    final String eventDivisionColumn = request.getParameter("event_division");
    final String judgingStationColumn = request.getParameter("judging_station");

    final StringBuilder message = new StringBuilder();

    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      final StringBuffer dbColumns = new StringBuffer();
      final StringBuffer dataColumns = new StringBuffer();
      final StringBuffer values = new StringBuffer();

      // always have TeamNumber
      dbColumns.append("TeamNumber");
      dataColumns.append(teamNumberColumn);
      values.append("?");
      int numValues = 1;

      final Enumeration<?> paramIter = request.getParameterNames();
      while (paramIter.hasMoreElements()) {
        final String parameter = (String) paramIter.nextElement();
        if (null != parameter
            && !"".equals(parameter) && !"TeamNumber".equals(parameter) && !"tournament".equals(parameter)
            && !"event_division".equals(parameter) && !"judging_station".equals(parameter)) {
          final String value = request.getParameter(parameter);
          if (null != value
              && !"".equals(value)) {
            dbColumns.append(", "
                + parameter);
            dataColumns.append(", "
                + value);
            values.append(",?");
            numValues++;
          }
        }
      }

      if (!verifyNoDuplicateTeamNumbers(connection, message, teamNumberColumn)) {
        session.setAttribute(SessionAttributes.MESSAGE, message);
        response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
        return false;
      }

      // now copy the data over converting the team number to an integer
      final String selectSQL = "SELECT "
          + dataColumns.toString() + " FROM AllTeams";
      final String insertSQL = "INSERT INTO Teams ("
          + dbColumns.toString() + ") VALUES(" + values.toString() + ")";
      prep = connection.prepareStatement(insertSQL);

      stmt = connection.createStatement();
      rs = stmt.executeQuery(selectSQL);
      while (rs.next()) {
        // convert TeamNumber to an integer
        final String teamNumStr = rs.getString(1);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Inserting "
              + teamNumStr + " into Teams");
        }
        try {
          final Number num = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.parse(teamNumStr);

          if (((int) Math.floor(num.doubleValue()) != (int) Math.ceil(num.doubleValue()))
              || num.intValue() < 0) {
            session.setAttribute(SessionAttributes.MESSAGE,
                                 "<p class='error'>All team numbers must be positive integers: "
                                     + num + "</p>");
            response.sendRedirect(response.encodeRedirectURL("index.jsp"));
            return false;
          }

          final int teamNum = num.intValue();
          prep.setInt(1, teamNum);
        } catch (final ParseException e) {
          out.println("<font color='red'>Error, "
              + teamNumStr + " is not numeric.<br/>");
          out.println("Go back and check your input file for errors.<br/></font>");
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(e, e);
          }
          return false;
        } catch (final NumberFormatException nfe) {
          out.println("<font color='red'>Error, "
              + teamNumStr + " is not numeric.<br/>");
          out.println("Go back and check your input file for errors.<br/></font>");
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(nfe, nfe);
          }
          return false;
        }

        for (int i = 1; i < numValues; i++) { // skip TeamNumber
          String value = rs.getString(i
              + 1);
          if (null == value) {
            // handle empty data as empty string instead of null, handled better
            // elsewhere
            value = "";
          }
          prep.setString(i
              + 1, value);
        }
        try {
          prep.executeUpdate();
        } catch (final SQLException sqle) {
          throw new FLLRuntimeException("Got error inserting teamNumber "
              + teamNumStr + " into Teams table, probably have two teams with the same team number", sqle);
        }
      } // for each row imported

    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    updateTournamentTeams(connection, teamNumberColumn, tournamentColumn, eventDivisionColumn, judgingStationColumn);

    return true;
  }

  /**
   * Check if there are any team numbers in AllTeams that are in Teams.
   * 
   * @param connection
   * @param message
   * @return true if no problems, false otherwise (message will be updated)
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate name of teamNumberColumn")
  private static boolean verifyNoDuplicateTeamNumbers(final Connection connection,
                                                      final StringBuilder message,
                                                      final String teamNumberColumn)
      throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Teams.TeamNumber" //
          + " FROM Teams, AllTeams" //
          + " WHERE AllTeams." + teamNumberColumn + " = Teams.TeamNumber");

      final StringBuilder teams = new StringBuilder();
      boolean first = true;
      while (rs.next()) {
        if (!first) {
          teams.append(", ");
        } else {
          first = false;
        }

        final int team = rs.getInt(1);
        teams.append(team);
      }

      if (!first) {
        // found duplicates
        message.append("<p class='error'>The following teams are already in the database, please remove them from your spreadsheet and try again: "
            + teams.toString() + "</p>");
        return false;
      } else {
        return true;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Update TournamentTeams table with newly inserted teams.
   * 
   * @param connection database connection
   * @param teamNumberColumn which column in AllTeams contains the team
   *          number
   * @param tournamentColumn which column in AllTeams contains the
   *          tournament
   * @param eventDivisionColumn which column in AllTeams contains the event
   *          division - may be null
   * @param judgingStationColumn which column in AllTeams contains the
   *          judging station - may be null
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
                                "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate the list of columns for AllTeams table, Can't use PreparedStatement for constant value to select when inserting dummy tournament id")
  static private void updateTournamentTeams(final Connection connection,
                                            final String teamNumberColumn,
                                            final String tournamentColumn,
                                            final String eventDivisionColumn,
                                            final String judgingStationColumn)
      throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      final String eventDivisionSql;
      if (null != eventDivisionColumn
          && !eventDivisionColumn.isEmpty()) {
        eventDivisionSql = eventDivisionColumn;
      } else {
        eventDivisionSql = "'"
            + GenerateDB.DEFAULT_TEAM_DIVISION + "'";
      }

      final String judgingStationSql;
      if (null != judgingStationColumn
          && !judgingStationColumn.isEmpty()) {
        judgingStationSql = judgingStationColumn;
      } else {
        judgingStationSql = "'"
            + GenerateDB.DEFAULT_TEAM_DIVISION + "'";
      }

      // if a tournament is specified for the new data, set it
      if (null != tournamentColumn
          && !tournamentColumn.isEmpty()) {

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(teamNumberColumn);
        sql.append(", "
            + tournamentColumn);

        sql.append(", "
            + eventDivisionSql);
        sql.append(", "
            + judgingStationSql);

        sql.append(" FROM AllTeams");

        rs = stmt.executeQuery(sql.toString());

        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final String tournamentName = rs.getString(2);
          Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);
          if (null == tournament) {
            Tournament.createTournament(connection, tournamentName, tournamentName);
            tournament = Tournament.findTournamentByName(connection, tournamentName);
          }

          final String eventDivision = rs.getString(3);
          final String judgingStation = rs.getString(4);

          Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), eventDivision,
                                      judgingStation);
        }
      } else {
        // put all new teams in the DUMMY tournament by default and make the
        // event division and judging station be the default division
        final Tournament dummyTournament = Tournament.findTournamentByName(connection,
                                                                           GenerateDB.DUMMY_TOURNAMENT_NAME);
        final int dummyTournamentID = dummyTournament.getTournamentID();
        stmt.executeUpdate("INSERT INTO TournamentTeams " //
            + " (Tournament, TeamNumber, event_division, judging_station)" // "
            + " SELECT " + dummyTournamentID + ", Teams.TeamNumber, " + eventDivisionSql + ", " + judgingStationSql //
            + " FROM Teams, AllTeams" //
            + "   WHERE Teams.TeamNumber = AllTeams." + teamNumberColumn);
      }

    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(rs);
    }
  }

  /**
   * Create a string that's a valid column name.
   * </ul>
   */
  private static String sanitizeColumnName(final String str) {
    if (null == str
        || "".equals(str)) {
      return "EMPTYHEADER_"
          + _emptyHeaderCount++;
    } else if ("constraint".equalsIgnoreCase(str)) {
      return "CONSTRAINT_";
    } else {
      String ret = str.trim();
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static int _emptyHeaderCount = 0;

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9_]");

}
