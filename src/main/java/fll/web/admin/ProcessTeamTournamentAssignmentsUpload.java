/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import fll.db.Queries;
import fll.util.CellFileReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.StoreColumnNames;
import fll.web.UploadSpreadsheet;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
    final String advanceFile = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY,
                                                                     String.class);
    final Path file = Paths.get(advanceFile);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      if (!Files.exists(file)
          || !Files.isReadable(file)) {
        throw new RuntimeException("Cannot read file: "
            + advanceFile);
      }

      final @Nullable String teamNumberColumnName = WebUtils.getParameterOrNull(request, "teamNumber");
      if (null == teamNumberColumnName) {
        throw new FLLRuntimeException("Cannot find 'teamNumber' request parameter");
      }

      final @Nullable String tournamentColumnName = WebUtils.getParameterOrNull(request, "tournament");
      if (null == tournamentColumnName) {
        throw new FLLRuntimeException("Cannot find 'tournament' request parameter");
      }

      final @Nullable String eventDivisionColumnName = WebUtils.getParameterOrNull(request, "event_division");
      final @Nullable String judgingStationColumnName = WebUtils.getParameterOrNull(request, "judging_station");
      final @Nullable String waveColumnName = WebUtils.getParameterOrNull(request, "wave");

      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      final int headerRowIndex = SessionAttributes.getNonNullAttribute(session, StoreColumnNames.HEADER_ROW_INDEX_KEY,
                                                                       Integer.class)
                                                  .intValue();

      processFile(connection, message, file, sheetName, headerRowIndex, teamNumberColumnName, tournamentColumnName,
                  eventDivisionColumnName, judgingStationColumnName, waveColumnName);

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
      try {
        Files.delete(file);
      } catch (final IOException e) {
        LOGGER.debug("Error deleting spreadsheet temp file, will get deleted on JVM exit", e);
      }

      SessionAttributes.appendToMessage(session, message.toString());

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
                                  final Path file,
                                  final @Nullable String sheetName,
                                  final int headerRowIndex,
                                  final String teamNumberColumnName,
                                  final String tournamentColumnName,
                                  final @Nullable String eventDivisionColumnName,
                                  final @Nullable String judgingStationColumnName,
                                  final @Nullable String waveColumnName)
      throws SQLException, IOException, ParseException, InvalidFormatException {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("File name: {}", file);
    }

    try (CellFileReader reader = CellFileReader.createCellReader(file, sheetName)) {
      reader.skipRows(headerRowIndex);

      @Nullable
      String @Nullable [] columnNames = reader.readNext();
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
            + " waveColumnName: "
            + waveColumnName //
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
      int waveColumnIdx = -1;
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

        if (null != waveColumnName
            && -1 == waveColumnIdx
            && waveColumnName.equals(columnNames[index])) {
          waveColumnIdx = index;
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
            + " waveColumnIdx: "
            + waveColumnIdx //
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

            if (eventDivisionColumnIdx < 0
                || null == data[eventDivisionColumnIdx]) {
              throw new FLLRuntimeException("Missing award group for team "
                  + teamNumber);
            }
            final String eventDivision = data[eventDivisionColumnIdx];

            if (judgingStationColumnIdx < 0
                || null == data[judgingStationColumnIdx]) {
              throw new FLLRuntimeException("Missing judging station for team "
                  + teamNumber);
            }
            final String judgingStation = data[judgingStationColumnIdx];

            final @Nullable String wave;
            if (waveColumnIdx < 0) {
              wave = null;
            } else {
              wave = data[waveColumnIdx];
            }

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Adding team "
                  + teamNumber
                  + " to tournament "
                  + tournament.getTournamentID());
            }

            if (!Queries.isTeamInTournament(connection, teamNumber, tournament.getTournamentID())) {
              Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), eventDivision,
                                          judgingStation, null == wave ? "" : wave);
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

}
