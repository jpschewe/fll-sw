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
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for selectHeaderRow.jsp.
 */
public final class SelectHeaderRow {

  private SelectHeaderRow() {
  }

  private static final int DEFAULT_NUM_ROWS_TO_LOAD = 20;

  /**
   * @param request used to read how many rows to include
   * @param session used to read the spreadsheet information
   * @param pageContext populated with the data to display
   */
  public static void populateContext(final HttpServletRequest request,
                                     final HttpSession session,
                                     final PageContext pageContext) {

    try {
      final String fileName = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.SPREADSHEET_FILE_KEY,
                                                                    String.class);
      final Path file = Paths.get(fileName);
      final String sheetName = SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY, String.class);

      final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);

      final int numRowsToLoad = WebUtils.getIntRequestParameter(request, "numRowsToLoad", DEFAULT_NUM_ROWS_TO_LOAD);
      pageContext.setAttribute("numRowsToLoad", numRowsToLoad);

      final String[][] data = loadData(reader, numRowsToLoad, "&nbsp;");

      pageContext.setAttribute("data", data);
      pageContext.setAttribute("maxColumns", data.length > 0 ? data[0].length : 0);
    } catch (IOException | InvalidFormatException e) {
      throw new FLLRuntimeException("Error loading the data file", e);
    }
  }

  /**
   * Load up to the specified number of rows from the reader.
   * 
   * @param reader where to load the data from
   * @param numRowsToLoad the number of rows to load
   * @param nullString the string to replace nulls with
   * @return the data as a square array
   * @throws IOException if there is a problem reading the data
   */
  public static String[][] loadData(final CellFileReader reader,
                                    final int numRowsToLoad,
                                    final String nullString)
      throws IOException {
    int maxColumns = 0;
    final List<@Nullable String @Nullable []> rows = new LinkedList<>();
    while (reader.getLineNumber() < numRowsToLoad) {
      final @Nullable String @Nullable [] row = reader.readNext();
      if (row != null) {
        maxColumns = Math.max(maxColumns, row.length);
        rows.add(row);
      } else {
        // end of file
        break;
      }
    }

    // make the data square so that it's easy to process in the JSP
    final String[][] data = new String[rows.size()][maxColumns];
    int rowIndex = 0;
    for (final @Nullable String @Nullable [] row : rows) {
      final int fromIndex;
      if (null == row) {
        fromIndex = 0;
      } else {
        fromIndex = maxColumns
            - (maxColumns
                - row.length);
        for (int i = 0; i < row.length; ++i) {
          if (null == row[i]) {
            data[rowIndex][i] = nullString;
          } else {
            data[rowIndex][i] = row[i];
          }
        }
      }
      if (fromIndex < maxColumns) {
        Arrays.fill(data[rowIndex], fromIndex, maxColumns, nullString);
      }

      ++rowIndex;
    }

    return data;
  }

}
