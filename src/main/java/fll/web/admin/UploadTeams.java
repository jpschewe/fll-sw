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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentLevel;
import fll.Utilities;
import fll.db.Queries;
import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.StoreColumnNames;
import fll.web.UploadSpreadsheet;
import fll.web.UserRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;

/**
 * Java code for uploading team data to the database. Redirects to
 * teamColumnSelection.jsp.
 */
@WebServlet("/admin/UploadTeams")
public final class UploadTeams extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    final String fileName = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SPREADSHEET_FILE_KEY,
                                                                  String.class);

    File file = null;
    try (Connection connection = datasource.getConnection()) {
      file = new File(fileName);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Wrote teams data to: "
            + file.getAbsolutePath());
      }

      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      parseFile(file, sheetName, connection, session);
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving team data into the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving team data into the database: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving team data into the database", e);
    } finally {
      if (null != file) {
        if (!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
    SessionAttributes.appendToMessage(session, message.toString());
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
   * @param sheetName the name of the sheet in the spreadsheet file
   * @throws SQLException if an error occurs talking to the database
   * @throws IOException if an error occurs reading from file
   * @throws InvalidFormatException if there is a problem reading the file
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate the list of columns to create AllTeams table")
  public static void parseFile(final File file,
                               final @Nullable String sheetName,
                               final Connection connection,
                               final HttpSession session)
      throws SQLException, IOException, InvalidFormatException {
    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);

    final int headerRowIndex = SessionAttributes.getNonNullAttribute(session, StoreColumnNames.HEADER_ROW_INDEX_KEY,
                                                                     Integer.class)
                                                .intValue();
    // skip forward to the header row
    reader.skipRows(headerRowIndex);

    // stores <option value='columnName'>columnName</option> for each column
    final StringBuffer selectOptions = new StringBuffer();

    // parse out the header line as the names of the columns
    // final List<String> columnNames = splitLine(reader.readLine());
    final @Nullable String @Nullable [] columnNames = reader.readNext();
    if (null == columnNames) {
      LOGGER.warn("No Data in uploaded file");
      return;
    }

    // build the SQL for inserting a row into the temporary table
    final StringBuffer insertPrepSQL = new StringBuffer();
    insertPrepSQL.append("INSERT INTO AllTeams VALUES(");

    // build the SQL for creating the temporary tables
    final StringBuffer createTable = new StringBuffer();
    createTable.append("CREATE TABLE AllTeams (");

    // iterate over each column name and append to appropriate buffers
    boolean first = true;
    final List<String> columnNamesSeen = new LinkedList<>();
    for (final String header : columnNames) {
      final String columnName = sanitizeColumnName(header);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("header: "
            + header
            + " columnName: "
            + columnName);
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
          + columnName
          + "'>"
          + columnName
          + "</option>");
    }
    createTable.append(")");
    insertPrepSQL.append(")");

    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate("DROP TABLE IF EXISTS AllTeams"); // make sure the
      // table doesn't yet
      // exist
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("creating table: "
            + createTable.toString());
      }
      stmt.executeUpdate(createTable.toString()); // create AllTeams

      try (PreparedStatement insertPrep = connection.prepareStatement(insertPrepSQL.toString())) {
        insertLinesIntoAllTeams(reader, columnNamesSeen, insertPrep);
      }
    }

    // save this for other pages to use
    session.setAttribute("columnSelectOptions", selectOptions.toString());
  }

  private static void insertLinesIntoAllTeams(final CellFileReader reader,
                                              final List<String> columnNamesSeen,
                                              final PreparedStatement insertPrep)
      throws IOException, SQLException {
    // loop over the rest of the rows and insert them into AllTeams
    @Nullable
    String @Nullable [] values;
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
                } catch (final NumberFormatException e) {
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
    if (StringUtils.isBlank(teamNumberColumn)) {
      // Error, redirect back to teamColumnSelection.jsp with error message
      SessionAttributes.appendToMessage(session, "<p class='error'>You must specify a column for TeamNumber</p>");
      response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
      return false;
    }

    final String tournamentColumn = request.getParameter("tournament");
    final String eventDivisionColumn = request.getParameter("event_division");
    final String judgingStationColumn = request.getParameter("judging_station");
    final String waveColumn = request.getParameter("wave");

    if (!StringUtils.isBlank(tournamentColumn)
        && StringUtils.isBlank(eventDivisionColumn)) {
      SessionAttributes.appendToMessage(session,
                                        "<p class='error'>You must specify a column for Award Group when specifying a Tournament column</p>");
      response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
      return false;
    }

    if (!StringUtils.isBlank(tournamentColumn)
        && StringUtils.isBlank(judgingStationColumn)) {
      SessionAttributes.appendToMessage(session,
                                        "<p class='error'>You must specify a column for Judging Station when specifying a Tournament column</p>");
      response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
      return false;
    }

    final StringBuilder message = new StringBuilder();

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
          && !"".equals(parameter)
          && !"TeamNumber".equals(parameter)
          && !"tournament".equals(parameter)
          && !"event_division".equals(parameter)
          && !"judging_station".equals(parameter)) {
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
      SessionAttributes.appendToMessage(session, message.toString());
      response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
      return false;
    }

    // now copy the data over converting the team number to an integer
    final String selectSQL = "SELECT "
        + dataColumns.toString()
        + " FROM AllTeams";
    final String insertSQL = "INSERT INTO Teams ("
        + dbColumns.toString()
        + ") VALUES("
        + values.toString()
        + ")";
    try (PreparedStatement prep = connection.prepareStatement(insertSQL);
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(selectSQL)) {
      while (rs.next()) {
        // convert TeamNumber to an integer
        final String teamNumStr = rs.getString(1);

        if (null == teamNumStr) {
          out.println("<font color='red'>Error team number 'null' is not numeric.<br/>");
          out.println("Go back and check your input file for errors.<br/></font>");
          return false;
        }

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Inserting "
              + teamNumStr
              + " into Teams");
        }
        try {
          final Number num = Utilities.getFloatingPointNumberFormat().parse(teamNumStr);

          if (((int) Math.floor(num.doubleValue()) != (int) Math.ceil(num.doubleValue()))
              || num.intValue() < 0) {
            SessionAttributes.appendToMessage(session, "<p class='error'>All team numbers must be positive integers: "
                + num
                + "</p>");
            response.sendRedirect(response.encodeRedirectURL("index.jsp"));
            return false;
          }

          final int teamNum = num.intValue();
          prep.setInt(1, teamNum);
        } catch (final ParseException e) {
          out.println("<font color='red'>Error, "
              + teamNumStr
              + " is not numeric.<br/>");
          out.println("Go back and check your input file for errors.<br/></font>");
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(e, e);
          }
          return false;
        } catch (final NumberFormatException nfe) {
          out.println("<font color='red'>Error, "
              + teamNumStr
              + " is not numeric.<br/>");
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
              + teamNumStr
              + " into Teams table, probably have two teams with the same team number", sqle);
        }
      } // for each row imported
    }

    // if a tournament column is specified, put teams in tournaments
    if (!StringUtils.isBlank(tournamentColumn)
        && !StringUtils.isBlank(eventDivisionColumn)
        && !StringUtils.isBlank(judgingStationColumn)) {
      updateTournamentTeams(connection, teamNumberColumn, tournamentColumn, eventDivisionColumn, judgingStationColumn,
                            waveColumn);
    }

    return true;
  }

  /**
   * Check if there are any team numbers in AllTeams that are in Teams.
   *
   * @param connection database connection
   * @param message where to put the message for the user
   * @return true if no problems, false otherwise (message will be updated)
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate name of teamNumberColumn")
  private static boolean verifyNoDuplicateTeamNumbers(final Connection connection,
                                                      final StringBuilder message,
                                                      final String teamNumberColumn)
      throws SQLException {
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT Teams.TeamNumber" //
            + " FROM Teams, AllTeams" //
            + " WHERE AllTeams."
            + teamNumberColumn
            + " = Teams.TeamNumber")) {

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
            + teams.toString()
            + "</p>");
        return false;
      } else {
        return true;
      }
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
   *          division
   * @param judgingStationColumn which column in AllTeams contains the
   *          judging station
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
                                "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate the list of columns for AllTeams table, Can't use PreparedStatement for constant value to select when inserting dummy tournament id")
  private static void updateTournamentTeams(final Connection connection,
                                            final String teamNumberColumn,
                                            final String tournamentColumn,
                                            final String eventDivisionColumn,
                                            final String judgingStationColumn,
                                            final @Nullable String waveColumn)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      final StringBuilder sql = new StringBuilder();
      sql.append("SELECT ");
      sql.append(teamNumberColumn);
      sql.append(", "
          + tournamentColumn);

      sql.append(", "
          + eventDivisionColumn);
      sql.append(", "
          + judgingStationColumn);

      if (!StringUtils.isBlank(waveColumn)) {
        sql.append(", "
            + waveColumn);
      }

      sql.append(" FROM AllTeams");

      try (ResultSet rs = stmt.executeQuery(sql.toString())) {

        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final String tournamentName = rs.getString(2);
          if (null == tournamentName) {
            LOGGER.debug("Team {} is missing tournament, skipping", teamNumber);
            continue;
          }

          final String eventDivision = rs.getString(3);
          if (null == eventDivision) {
            LOGGER.debug("Team {} is missing award group, skipping", teamNumber);
            continue;
          }

          final String judgingStation = rs.getString(4);
          if (null == judgingStation) {
            LOGGER.debug("Team {} is missing judging group, skipping", teamNumber);
            continue;
          }

          final String wave;
          if (!StringUtils.isBlank(waveColumn)) {
            wave = castNonNull(rs.getString(5));
          } else {
            wave = "";
          }

          final Tournament tournament;
          if (!Tournament.doesTournamentExist(connection, tournamentName)) {
            Tournament.createTournament(connection, tournamentName, tournamentName, null,
                                        TournamentLevel.getByName(connection,
                                                                  TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME));
            tournament = Tournament.findTournamentByName(connection, tournamentName);
          } else {
            tournament = Tournament.findTournamentByName(connection, tournamentName);
          }

          Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), eventDivision,
                                      judgingStation, wave);
        } // foreach result
      }
    } // stmt

  }

  /**
   * Create a string that's a valid database column name.
   * 
   * @param str the string to sanitize
   * @return a string that can be used as a database column name
   */
  public static String sanitizeColumnName(final @Nullable String str) {
    if (null == str
        || "".equals(str)) {
      return "EMPTYHEADER_"
          + emptyHeaderCount++;
    } else if ("constraint".equalsIgnoreCase(str)) {
      return "CONSTRAINT_";
    } else {
      String ret = str.trim();
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static int emptyHeaderCount = 0;

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9_]");

}
