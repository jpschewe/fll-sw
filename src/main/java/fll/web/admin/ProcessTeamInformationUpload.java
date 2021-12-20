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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Team;
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

/**
 * Handle bulk team information changes.
 */
@WebServlet("/admin/ProcessTeamInformationUpload")
public final class ProcessTeamInformationUpload extends BaseFLLServlet {

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

    final int headerRowIndex = SessionAttributes.getNonNullAttribute(session, StoreColumnNames.HEADER_ROW_INDEX_KEY,
                                                                     Integer.class);

    final StringBuilder message = new StringBuilder();
    final String filename = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SPREADSHEET_FILE_KEY,
                                                                  String.class);
    final Path file = Paths.get(filename);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      if (!Files.exists(file)
          || !Files.isReadable(file)) {
        throw new FLLInternalException("Cannot read file: "
            + filename);
      }

      final String teamNumberColumnName = request.getParameter("teamNumber");
      if (null == teamNumberColumnName) {
        throw new FLLRuntimeException("Cannot find 'teamNumber' request parameter");
      }

      final String teamNameColumnName = WebUtils.getNonNullRequestParameter(request, "teamName");
      final String organizationColumnName = WebUtils.getNonNullRequestParameter(request, "organization");

      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      processFile(connection, message, file, sheetName, headerRowIndex, teamNumberColumnName, teamNameColumnName,
                  organizationColumnName);

    } catch (final SQLException | IOException | ParseException | InvalidFormatException e) {
      message.append("<p class='error'>Error saving team information into the database: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new FLLRuntimeException("Error saving team assignments into the database", e);
    } finally {
      SessionAttributes.appendToMessage(session, message.toString());

      try {
        Files.delete(file);
      } catch (final IOException e) {
        LOGGER.debug("Error deleting spreadsheet temp file, will get deleted on JVM exit", e);
      }

      response.sendRedirect(response.encodeRedirectURL("index.jsp"));
    }
  }

  /**
   * Make the changes.
   */
  private static void processFile(final Connection connection,
                                  final StringBuilder message,
                                  final Path file,
                                  final @Nullable String sheetName,
                                  final int headerRowIndex,
                                  final String teamNumberColumnName,
                                  final String teamNameColumnName,
                                  final String organizationColumnName)
      throws SQLException, IOException, ParseException, InvalidFormatException {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("File name: "
          + file.toString());
    }

    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);
    reader.skipRows(headerRowIndex);

    final @Nullable String @Nullable [] columnNames = reader.readNext();

    if (null == columnNames) {
      LOGGER.warn("No data in file");
      return;
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Column names size: "
          + columnNames.length //
          + " names: "
          + Arrays.asList(columnNames).toString() //
          + " teamNumber column: "
          + teamNumberColumnName);
    }

    int teamNumColumnIdx = -1;
    int teamNameColumnIdx = -1;
    int organizationColumnIdx = -1;
    int index = 0;
    while (index < columnNames.length
        && (-1 == teamNumColumnIdx
            || -1 == teamNameColumnIdx
            || -1 == organizationColumnIdx)) {
      if (-1 == teamNumColumnIdx
          && teamNumberColumnName.equals(columnNames[index])) {
        teamNumColumnIdx = index;
      }

      if (-1 == teamNameColumnIdx
          && teamNameColumnName.equals(columnNames[index])) {
        teamNameColumnIdx = index;
      }

      if (-1 == organizationColumnIdx
          && organizationColumnName.equals(columnNames[index])) {
        organizationColumnIdx = index;
      }

      ++index;
    }

    if (-1 == teamNumColumnIdx) {
      throw new FLLInternalException("Cannot find index for team number column '"
          + teamNumberColumnName
          + "'");
    }

    int rowsProcessed = 0;
    @Nullable
    String @Nullable [] data = reader.readNext();
    while (null != data) {
      if (teamNumColumnIdx < data.length) {
        final String teamNumStr = data[teamNumColumnIdx];
        if (null != teamNumStr
            && !"".equals(teamNumStr.trim())) {
          final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumStr).intValue();

          final Team team = Team.getTeamFromDatabase(connection, teamNumber);

          final String teamName;
          if (teamNameColumnIdx < 0) {
            teamName = team.getTeamName();
          } else {
            teamName = data[teamNameColumnIdx];
            if (null == teamName) {
              throw new FLLRuntimeException("Team name is missing for "
                  + teamNumber);
            }
          }

          final @Nullable String organization;
          if (organizationColumnIdx < 0) {
            organization = team.getOrganization();
          } else {
            organization = data[organizationColumnIdx];
          }

          Queries.updateTeam(connection, teamNumber, teamName, organization);

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
