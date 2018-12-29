/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.util.CellFileReader;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadSpreadsheet;

/**
 * Upload the teams to be advanced.
 * Pass off to chooseAdvancementColumns.jsp.
 */
@WebServlet("/admin/UploadTeamTournamentAssignments")
public final class UploadTeamTournamentAssignments extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Key in the session to get the {@link String} object that is the spreadsheet
   * to load.
   */
  public static final String ADVANCE_FILE_KEY = "advanceFile";

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

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
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error reading headers from file", e);
    }
    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("chooseAdvancementColumns.jsp"));

  }

  /**
   * Get the headers from the file.
   * 
   * @param file the file that the data is in
   * @param session used to store the headers, attribute is "fileHeaders" and is
   *          of type String[]
   * @throws IOException if an error occurs reading from file
   * @throws InvalidFormatException
   */
  public static void putHeadersInSession(final File file,
                                         final String sheetName,
                                         final HttpSession session)
      throws IOException, InvalidFormatException {
    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);

    // parse out the first non-blank line as the names of the columns
    String[] columnNames = reader.readNext();
    while (columnNames.length < 1) {
      columnNames = reader.readNext();
    }

    // save this for other pages to use
    session.setAttribute("fileHeaders", columnNames);
  }

}
