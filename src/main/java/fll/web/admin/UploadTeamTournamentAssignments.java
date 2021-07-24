/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.CellFileReader;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadSpreadsheet;
import fll.web.UserRole;

/**
 * Upload the teams to be advanced.
 * Pass off to chooseAdvancementColumns.jsp.
 */
@WebServlet("/admin/UploadTeamTournamentAssignments")
public final class UploadTeamTournamentAssignments extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Key in the session to get the {@link String} object that is the spreadsheet
   * to load.
   */
  public static final String ADVANCE_FILE_KEY = "advanceFile";

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

    final String fileName = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SPREADSHEET_FILE_KEY,
                                                                  String.class);
    final File file = new File(fileName);
    try {
      // keep track of for later
      session.setAttribute(ADVANCE_FILE_KEY, file.getAbsolutePath());

      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      putHeadersInSession(file, sheetName, session);
    } catch (final IOException | InvalidFormatException e) {
      message.append("<p class='error'>Error reading headers from file: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error reading headers from file", e);
    }
    SessionAttributes.appendToMessage(session, message.toString());
    response.sendRedirect(response.encodeRedirectURL("chooseAdvancementColumns.jsp"));

  }

  /**
   * Get the headers from the file.
   *
   * @param file the file that the data is in
   * @param sheetName the sheet in an Excel file to read, null if reading a CSV
   *          file
   * @param session used to store the headers, attribute is "fileHeaders" and is
   *          of type String[]
   * @throws IOException if an error occurs reading from file
   * @throws InvalidFormatException if there is an error parsing the file
   */
  public static void putHeadersInSession(final File file,
                                         final @Nullable String sheetName,
                                         final HttpSession session)
      throws IOException, InvalidFormatException {
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

    // save this for other pages to use
    session.setAttribute("fileHeaders", columnNames);
  }

}
