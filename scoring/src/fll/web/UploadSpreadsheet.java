/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import fll.Utilities;
import fll.util.ExcelCellReader;
import fll.util.LogUtils;

/**
 * Handle uploading a spreadsheet (csv or Excel). The filename is stored in the
 * session attribute {@link #SPREADSHEET_FILE_KEY}. This file needs to be
 * deleted once it's
 * been processed. The file is expected to be in the request parameter "file".
 * The page to redirect to when done is expected to be in the request parameter
 * {@link #UPLOAD_REDIRECT_KEY}. The sheet name will be stored in the session
 * attribute
 * {@link #SHEET_NAME_KEY}.
 */
@WebServlet("/UploadSpreadsheet")
public final class UploadSpreadsheet extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Session key for the name of the file that was saved.
   */
  public static final String SPREADSHEET_FILE_KEY = "spreadsheetFile";

  /**
   * Session key for the name of the page to redirect to after the spreadsheet has
   * been uploaded and the sheet name chosen.
   */
  public static final String UPLOAD_REDIRECT_KEY = "uploadRedirect";

  /**
   * Session key for the name of the sheet in the spreadsheet to load.
   * This will be null if a CSV file was loaded.
   * The data type is {@link String}.
   */
  public static final String SHEET_NAME_KEY = "sheetName";

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();
    String nextPage;
    try {
      UploadProcessor.processUpload(request);
      final String uploadRedirect = (String) request.getAttribute(UploadSpreadsheet.UPLOAD_REDIRECT_KEY);
      if (null == uploadRedirect) {
        throw new RuntimeException("Missing parameter '"
            + UploadSpreadsheet.UPLOAD_REDIRECT_KEY
            + "' params: "
            + request.getParameterMap());
      }
      session.setAttribute(UploadSpreadsheet.UPLOAD_REDIRECT_KEY, uploadRedirect);

      final FileItem fileItem = (FileItem) request.getAttribute("file");
      final String extension = Utilities.determineExtension(fileItem.getName());
      final File file = File.createTempFile("fll", extension);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Wrote data to: "
            + file.getAbsolutePath());
      }
      fileItem.write(file);
      session.setAttribute(SPREADSHEET_FILE_KEY, file.getAbsolutePath());

      if (ExcelCellReader.isExcelFile(file)) {
        final List<String> sheetNames = ExcelCellReader.getAllSheetNames(file);
        if (sheetNames.size() > 1) {
          session.setAttribute(ProcessSelectedSheet.SHEET_NAMES_KEY, sheetNames);
          nextPage = "promptForSheetName.jsp";
        } else {
          session.setAttribute(SHEET_NAME_KEY, sheetNames.get(0));
          nextPage = uploadRedirect;
        }
      } else {
        nextPage = uploadRedirect;
      }

    } catch (final FileUploadException e) {
      message.append("<p class='error'>Error processing team data upload: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error processing team data upload", e);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving team data into the database: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving team data into the database", e);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(nextPage));
  }

}
