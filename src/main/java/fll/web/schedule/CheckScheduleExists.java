/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadSpreadsheet;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Check if a schedule exists for the current tournament.
 */
@WebServlet("/schedule/CheckScheduleExists")
public class CheckScheduleExists extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static void clearSesionVariables(final HttpSession session) {
    session.removeAttribute(UploadScheduleData.KEY);
  }

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

    clearSesionVariables(session);

    final UploadScheduleData uploadScheduleData = new UploadScheduleData();

    final String fileName = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SPREADSHEET_FILE_KEY,
                                                                  String.class);
    uploadScheduleData.setScheduleFile(new File(fileName));

    final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);
    if (null != sheetName) {
      uploadScheduleData.setSelectedSheet(sheetName);
    }

    LOGGER.debug("File: {} Sheet: {}", uploadScheduleData.getScheduleFile(), uploadScheduleData.getSelectedSheet());

    session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);

      if (TournamentSchedule.scheduleExistsInDatabase(connection, tournamentID)) {
        // redirect to prompt page
        WebUtils.sendRedirect(application, response, "/schedule/promptForOverwrite.jsp");
        return;
      } else {
        // redirect to check teams against DB servlet
        WebUtils.sendRedirect(application, response, "/schedule/scheduleConstraints.jsp");
        return;
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    }

  }

}
