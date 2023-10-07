/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
   * Session variable that stores a possibly empty immutable {@link List} of
   * {@link String} elements that are the headers. Indices of this list match the
   * indices of the data.
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
      final List<String> headerNames = extractHeaderNames(file, sheetName, headerRowIndex);

      session.setAttribute(HEADER_NAMES_KEY, headerNames);

      final String uploadRedirect = SessionAttributes.getNonNullAttribute(session,
                                                                          UploadSpreadsheet.UPLOAD_REDIRECT_KEY,
                                                                          String.class);
      WebUtils.sendRedirect(application, response, uploadRedirect);
    } catch (IOException | InvalidFormatException e) {
      throw new FLLRuntimeException("Error reading the spreadsheet", e);
    }

  }

  /**
   * Get the names of the headers. Empty headers are removed.
   * 
   * @param file the file to read with {@link CellFileReader}
   * @param sheetName the name of the sheet if loading a spreadsheet
   * @param headerRowIndex the index of the header row
   * @return the header names as an immutable list
   * @throws InvalidFormatException if there is an error parsing the spreadsheet
   * @throws IOException if there is an error reading the file
   */
  public static List<String> extractHeaderNames(final Path file,
                                                final @Nullable String sheetName,
                                                final int headerRowIndex)
      throws InvalidFormatException, IOException {
    final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);
    reader.skipRows(headerRowIndex);
    final @Nullable String @Nullable [] headerRow = reader.readNext();

    final List<String> headerNames;
    if (null == headerRow) {
      headerNames = Collections.emptyList();
    } else {
      final String[] headerArray = new String[headerRow.length];
      for (int i = 0; i < headerRow.length; ++i) {
        if (StringUtils.isBlank(headerRow[i])) {
          headerArray[i] = String.valueOf(i);
        } else {
          headerArray[i] = String.format("%d_%s", i, sanitizeColumnName(headerRow[i].trim()));
        }
      }
      headerNames = Collections.unmodifiableList(Arrays.asList(headerArray));
    }
    return headerNames;
  }

  /**
   * Create a string that's a valid database column name.
   * 
   * @param str the string to sanitize
   * @return a string that can be used as a database column name
   */
  public static String sanitizeColumnName(final String str) {
    if ("constraint".equalsIgnoreCase(str)) {
      return "CONSTRAINT_";
    } else {
      String ret = str.trim();
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9_]");
}
