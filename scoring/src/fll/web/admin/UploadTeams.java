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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.Team;
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
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Java code for uploading team data to the database. Called from
 * filterTeams.jsp and columnSelection.jsp.
 */
@WebServlet("/admin/UploadTeams")
public final class UploadTeams extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

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

      final String sheetName = SessionAttributes.getAttribute(session, "sheetName", String.class);

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
    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL("filterTeams.jsp"));

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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                                             "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate the list of columns to create FilteredTeams table")
  public static void parseFile(final File file,
                               final String sheetName,
                               final Connection connection,
                               final HttpSession session) throws SQLException, IOException, InvalidFormatException {
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
    final StringBuffer createFilteredTable = new StringBuffer();
    createFilteredTable.append("CREATE TABLE FilteredTeams (");

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
        createFilteredTable.append(", ");
        insertPrepSQL.append(", ");
      }
      createTable.append(columnName
          + " longvarchar");
      createFilteredTable.append(columnName
          + " longvarchar");
      insertPrepSQL.append("?");
      selectOptions.append("<option value='"
          + columnName + "'>" + columnName + "</option>");
    }
    createTable.append(")");
    createFilteredTable.append(")");
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

      // make sure the table doesn't yet exist
      stmt.executeUpdate("DROP TABLE IF EXISTS FilteredTeams");

      // create FilteredTeams
      stmt.executeUpdate(createFilteredTable.toString());

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
                                              final PreparedStatement insertPrep) throws IOException, SQLException {
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
   * Apply the set of filters currently stored in session.
   * 
   * @param connection the database connection
   * @param request the request where the filters are stored
   * @return the number of rows that match
   * @throws SQLException if an error occurs talking to the database
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate where clause with the filters")
  public static long applyFilters(final Connection connection,
                                  final HttpServletRequest request) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      final String whereClause = createFilterWhereClauseFromRequest(request);

      stmt = connection.createStatement();

      String sql = "SELECT COUNT(*) FROM AllTeams";
      if (whereClause.length() > 0) {
        sql += " WHERE "
            + whereClause;
      }

      rs = stmt.executeQuery(sql);
      if (rs.next()) {
        return rs.getLong(1);
      } else {
        throw new RuntimeException("Internal error, can't get count");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Copy teams out of AllTeams into FilteredTeams applying the filters in
   * request
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate where clause based on filters")
  public static void copyFilteredTeams(final Connection connection,
                                       final HttpServletRequest request) throws SQLException {
    Statement stmt = null;
    try {
      final String whereClause = createFilterWhereClauseFromRequest(request);

      stmt = connection.createStatement();

      // first clean out FilteredTeams
      stmt.executeUpdate("DELETE FROM FilteredTeams");

      // do the copy
      String sql = "INSERT INTO FilteredTeams SELECT * FROM AllTeams";
      if (whereClause.length() > 0) {
        sql += " WHERE "
            + whereClause;
      }
      stmt.executeUpdate(sql);
    } finally {
      SQLFunctions.close(stmt);
    }
  }

  private static String createFilterWhereClauseFromRequest(final HttpServletRequest request) {
    final StringBuffer whereClause = new StringBuffer();
    int filterCount = 0;
    String filterColumn = request.getParameter("filterColumn"
        + filterCount);
    String filterText = request.getParameter("filterText"
        + filterCount);
    String filterDelete = request.getParameter("filterDelete"
        + filterCount);
    while (null != filterColumn) {
      if (!"".equals(filterText)
          && !"1".equals(filterDelete)) {
        if (whereClause.length() > 0) {
          whereClause.append(" AND ");
        }
        whereClause.append(filterColumn
            + " LIKE '" + filterText.trim() + "'");
      }

      filterCount++;
      filterColumn = request.getParameter("filterColumn"
          + filterCount);
      filterText = request.getParameter("filterText"
          + filterCount);
      filterDelete = request.getParameter("filterDelete"
          + filterCount);
    }
    return whereClause.toString();
  }

  /**
   * Verify the team information. Used with verifyTeams.jsp
   * <ul>
   * <li>Make sure that the column for TeamNumber is specified</li>
   * <li>Make sure that the TeamNumbers are unique - just use count</li>
   * <li>Delete all rows from the Teams table</li>
   * <li>Copy the teams into the Teams table</li>
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = {
                                                             "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
                                                             "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate the list of columns for FilteredTeams table, Can't use PreparedStatement for constant value to select when inserting dummy tournament id")
  public static boolean verifyTeams(final Connection connection,
                                    final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final ServletContext application,
                                    final HttpSession session,
                                    final JspWriter out) throws SQLException, IOException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    
    final String teamNumberColumn = request.getParameter("TeamNumber");
    if (null == teamNumberColumn
        || "".equals(teamNumberColumn)) {
      // Error, redirect back to teamColumnSelection.jsp with error message
      // FIXME use standard message type when upload servlet is modified
      session.setAttribute("errorMessage", "You must specify a column for TeamNumber");
      response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
      return false;
    }

    final String tournamentColumn = request.getParameter("tournament");

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
            && !"".equals(parameter) && !"TeamNumber".equals(parameter) && !"tournament".equals(parameter)) {
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

      // clean out some data first
      prep = connection.prepareStatement("DELETE FROM TournamentTeams");
      prep.executeUpdate();
      prep = connection.prepareStatement("DELETE FROM PlayoffData");
      prep.executeUpdate();
      prep = connection.prepareStatement("DELETE FROM Performance");
      prep.executeUpdate();
      prep = connection.prepareStatement("DELETE FROM FinalScores");
      prep.executeUpdate();
      
      for (final ScoreCategory categoryElement : description.getSubjectiveCategories()) {
        final String tableName = categoryElement.getName();
        prep = connection.prepareStatement("DELETE FROM "
            + tableName);
        prep.executeUpdate();
      }

      prep = connection.prepareStatement("DELETE FROM sched_subjective");
      prep.executeUpdate();
      prep = connection.prepareStatement("DELETE FROM sched_perf_rounds");
      prep.executeUpdate();
      prep = connection.prepareStatement("DELETE FROM schedule");
      prep.executeUpdate();

      final List<String> internalTeams = new LinkedList<String>();
      internalTeams.add(String.valueOf(Team.BYE.getTeamNumber()));
      internalTeams.add(String.valueOf(Team.TIE.getTeamNumber()));
      internalTeams.add(String.valueOf(Team.NULL.getTeamNumber()));
      prep = connection.prepareStatement("DELETE FROM Teams WHERE TeamNumber NOT IN ( "
          + StringUtils.join(internalTeams, ",") + " )");
      prep.executeUpdate();

      // now copy the data over converting the team number to an integer
      final String selectSQL = "SELECT "
          + dataColumns.toString() + " FROM FilteredTeams";
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
          final Number num = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumStr);

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
          String value = rs.getString(i + 1);
          if (null == value) {
            // handle empty data as empty string instead of null, handled better
            // elsewhere
            value = "";
          }
          prep.setString(i + 1, value);
        }
        try {
          prep.executeUpdate();
        } catch (final SQLException sqle) {
          throw new FLLRuntimeException("Got error inserting teamNumber "
              + teamNumStr + " into Teams table, probably have two teams with the same team number", sqle);
        }
      }// for each row imported

    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    setTournamentTeams(connection, teamNumberColumn, tournamentColumn);

    return true;
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = {
                                                             "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
                                                             "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate the list of columns for FilteredTeams table, Can't use PreparedStatement for constant value to select when inserting dummy tournament id")
  static private void setTournamentTeams(final Connection connection,
                                         final String teamNumberColumn,
                                         final String tournamentColumn) throws SQLException {
    Statement stmt = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      // put all teams in the DUMMY tournament by default and make the event
      // division the same as the team division
      stmt.executeUpdate("DELETE FROM TournamentTeams");
      final Tournament dummyTournament = Tournament.findTournamentByName(connection, GenerateDB.DUMMY_TOURNAMENT_NAME);
      final int dummyTournamentID = dummyTournament.getTournamentID();
      stmt.executeUpdate("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division, judging_station) SELECT "
          + dummyTournamentID + ", Teams.TeamNumber, Teams.Division, Teams.Division FROM Teams");

      if (null != tournamentColumn) {
        rs = stmt.executeQuery("SELECT "
            + teamNumberColumn + ", " + tournamentColumn + " FROM FilteredTeams");
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final String tournamentName = rs.getString(2);
          Tournament tournament = Tournament.findTournamentByName(connection, tournamentName);
          if (null == tournament) {
            Tournament.createTournament(connection, tournamentName, tournamentName);
            tournament = Tournament.findTournamentByName(connection, tournamentName);
          }
          Queries.changeTeamCurrentTournament(connection, teamNumber, tournament.getTournamentID());
        }
      }
    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(rs);
      SQLFunctions.close(selectPrep);
    }
  }

  /**
   * Create a string that's a valid column name.
   * <ul>
   * <li>Replace '#' with '_'</li>
   * <li>Replace ' ' with '_'</li>
   * <li>Replace '/' with '_'</li>
   * <li>Replace '-' with '_'</li>
   * <li>Replace ',' with '_'</li>
   * <li>Replace null or empty string with EMPTYHEADER1 where number increments
   * each time found</li>
   * <li>Replace '?' with '_'</li>
   * <li>Replace the string 'constraint' with 'CONSTRAINT_'</li>
   * <li>Replace ':' with '_'</li>
   * <li>Replace '&' with '_'</li>
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
      String ret = str;
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static int _emptyHeaderCount = 0;

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[ #?/\\-,:&.]");

}
