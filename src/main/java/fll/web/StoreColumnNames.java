/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Store the column names and the header row index.
 */
@WebServlet("/StoreColumnNames")
public class StoreColumnNames extends BaseFLLServlet {

  /**
   * Session variable that stores an integer that is the index (zero-based) of the
   * header row.
   */
  public static final String HEADER_ROW_INDEX_KEY = "headerRowIndex";

  /**
   * Session variable that stores a possibly null {@link String} array of the
   * headers.
   */
  public static final String HEADER_NAMES_KEY = "spreadsheetHeaderNames";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final int headerRowIndex = WebUtils.getIntRequestParameter(request, "headerRowIndex");
    session.setAttribute(HEADER_ROW_INDEX_KEY, headerRowIndex);

    final String fileName = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SPREADSHEET_FILE_KEY,
                                                                  String.class);
    final Path file = Paths.get(fileName);
    final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

    try {
      final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);
      reader.skipRows(headerRowIndex);

      final @Nullable String @Nullable [] headerRow = reader.readNext();
      session.setAttribute(HEADER_NAMES_KEY, headerRow);

      final String uploadRedirect = SessionAttributes.getNonNullAttribute(session,
                                                                          UploadSpreadsheet.UPLOAD_REDIRECT_KEY,
                                                                          String.class);
      WebUtils.sendRedirect(application, response, uploadRedirect);
    } catch (IOException | InvalidFormatException e) {
      throw new FLLRuntimeException("Error reading the spreadsheet", e);
    }

  }

}
