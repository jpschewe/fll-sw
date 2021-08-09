/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.TournamentLevel;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.util.CellFileReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadSpreadsheet;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Assign teams to tournaments, creating tournaments if needed.
 */
@WebServlet("/admin/ProcessTeamTournamentAssignmentsUpload")
public final class ProcessTeamTournamentAssignmentsUpload extends BaseFLLServlet {

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
    final String advanceFile = SessionAttributes.getNonNullAttribute(session,
                                                                     UploadTeamTournamentAssignments.ADVANCE_FILE_KEY,
                                                                     String.class);
    final File file = new File(advanceFile);
    Connection connection = null;
    try {
      if (!file.exists()
          || !file.canRead()) {
        throw new RuntimeException("Cannot read file: "
            + advanceFile);
      }

      final String teamNumberColumnName = WebUtils.getParameterOrNull(request, "teamNumber");
      if (null == teamNumberColumnName) {
        throw new FLLRuntimeException("Cannot find 'teamNumber' request parameter");
      }

      final String tournamentColumnName = WebUtils.getParameterOrNull(request, "tournament");
      if (null == tournamentColumnName) {
        throw new FLLRuntimeException("Cannot find 'tournament' request parameter");
      }

      final String eventDivisionColumnName = WebUtils.getParameterOrNull(request, "event_division");
      final String judgingStationColumnName = WebUtils.getParameterOrNull(request, "judging_station");

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      processFile(connection, message, file, sheetName, teamNumberColumnName, tournamentColumnName,
                  eventDivisionColumnName, judgingStationColumnName);

      if (!file.delete()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Error deleting file, will need to wait until exit. Filename: "
              + file.getAbsolutePath());
        }
      }

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving team assignments into the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team assignments into the database", sqle);
    } catch (final ParseException e) {
      message.append("<p class='error'>Error saving team assignments into the database: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving team assignments into the database", e);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving team assignments into the database: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving team assignments into the database", e);
    } finally {
      SessionAttributes.appendToMessage(session, message.toString());

      if (!file.delete()) {
        file.deleteOnExit();
      }
      SQLFunctions.close(connection);

      response.sendRedirect(response.encodeRedirectURL("index.jsp"));
    }
  }

  /**
   * Make the changes
   *
   * @throws InvalidFormatException
   */
  private static void processFile(final Connection connection,
                                  final StringBuilder message,
                                  final File file,
                                  final @Nullable String sheetName,
                                  final String teamNumberColumnName,
                                  final String tournamentColumnName,
                                  final @Nullable String eventDivisionColumnName,
                                  final @Nullable String judgingStationColumnName)
      throws SQLException, IOException, ParseException, InvalidFormatException {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("File name: "
          + file.getName());
    }

    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);

    // parse out the first non-blank line as the names of the columns
    @Nullable
    String @Nullable [] columnNames = reader.readNext();
    while (null != columnNames
        && columnNames.length < 1) {
      columnNames = reader.readNext();
    }
    if (null == columnNames) {
      LOGGER.warn("No data in file");
      return;
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("sheetName: "
          + sheetName //
          + " teamNumberColumnName: "
          + teamNumberColumnName //
          + " tournamentColumnName: "
          + tournamentColumnName //
          + " eventDivisionColumnName: "
          + eventDivisionColumnName //
          + " judgingStationColumnName: "
          + judgingStationColumnName //
      );
      LOGGER.trace("Column names size: "
          + columnNames.length //
          + " names: "
          + Arrays.asList(columnNames).toString() //
          + " teamNumber column: "
          + teamNumberColumnName);
    }

    int teamNumColumnIdx = -1;
    int tournamentColumnIdx = -1;
    int eventDivisionColumnIdx = -1;
    int judgingStationColumnIdx = -1;
    int index = 0;
    while (index < columnNames.length
        && (-1 == teamNumColumnIdx
            || -1 == tournamentColumnIdx
            || -1 == eventDivisionColumnIdx
            || -1 == judgingStationColumnIdx)) {
      if (-1 == teamNumColumnIdx
          && teamNumberColumnName.equals(columnNames[index])) {
        teamNumColumnIdx = index;
      }

      if (-1 == tournamentColumnIdx
          && tournamentColumnName.equals(columnNames[index])) {
        tournamentColumnIdx = index;
      }
      if (null != eventDivisionColumnName
          && -1 == eventDivisionColumnIdx
          && eventDivisionColumnName.equals(columnNames[index])) {
        eventDivisionColumnIdx = index;
      }
      if (null != judgingStationColumnName
          && -1 == judgingStationColumnIdx
          && judgingStationColumnName.equals(columnNames[index])) {
        judgingStationColumnIdx = index;
      }

      ++index;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("teamNumIdx: "
          + teamNumColumnIdx//
          + " tournamentColumnIdex: "
          + tournamentColumnIdx //
          + " eventDivisionColumnIdex: "
          + eventDivisionColumnIdx //
          + " judgingStationColumnIdx: "
          + judgingStationColumnIdx //
      );

    }

    if (-1 == teamNumColumnIdx
        || -1 == tournamentColumnIdx) {
      throw new FLLInternalException("Cannot find index for team number column '"
          + teamNumberColumnName
          + "' or tournament '"
          + tournamentColumnName
          + "'");
    }

    int rowsProcessed = 0;
    @Nullable
    String @Nullable [] data = reader.readNext();
    while (null != data) {
      if (teamNumColumnIdx < data.length
          && tournamentColumnIdx < data.length) {
        final String teamNumStr = data[teamNumColumnIdx];
        if (null != teamNumStr
            && !"".equals(teamNumStr.trim())) {
          final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumStr).intValue();

          final String tournamentName = data[tournamentColumnIdx];
          if (null == tournamentName) {
            throw new FLLRuntimeException("Missing tournament name for team "
                + teamNumber);
          }

          final Tournament tournament;
          if (!Tournament.doesTournamentExist(connection, tournamentName)) {
            // create the tournament
            Tournament.createTournament(connection, tournamentName, tournamentName, null,
                                        TournamentLevel.getByName(connection,
                                                                  TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME));
            tournament = Tournament.findTournamentByName(connection, tournamentName);
            message.append("<p>Created tournament '"
                + tournamentName
                + "'</p>");
          } else {
            tournament = Tournament.findTournamentByName(connection, tournamentName);
          }

          final String eventDivision;
          if (eventDivisionColumnIdx < 0
              || null == data[eventDivisionColumnIdx]) {
            eventDivision = GenerateDB.DEFAULT_TEAM_DIVISION;
          } else {
            eventDivision = data[eventDivisionColumnIdx];
          }
          final String judgingStation;
          if (judgingStationColumnIdx < 0
              || null == data[judgingStationColumnIdx]) {
            judgingStation = GenerateDB.DEFAULT_TEAM_DIVISION;
          } else {
            judgingStation = data[judgingStationColumnIdx];
          }

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Adding team "
                + teamNumber
                + " to tournament "
                + tournament.getTournamentID());
          }

          if (!Queries.isTeamInTournament(connection, teamNumber, tournament.getTournamentID())) {
            Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), eventDivision,
                                        judgingStation);
          }
          ++rowsProcessed;
        }
      }

      data = reader.readNext();
    }

    message.append("<p>Successfully processed "
        + rowsProcessed
        + " rows of data</p>");

  }

}
