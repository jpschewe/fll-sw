/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

import fll.Utilities;
import fll.util.ExcelCellReader;
import fll.util.FLLRuntimeException;
import fll.web.schedule.UploadScheduleData;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Session key for the name of the file that was saved. The file is marked for
   * delete on exit, however it is helpful if the code processing the file deletes
   * it once it is no longer needed.
   */
  public static final String SPREADSHEET_FILE_KEY = "spreadsheetFile";

  /**
   * Session key for the name of the page to redirect to after the spreadsheet has
   * been uploaded and the sheet name chosen.
   */
  public static final String UPLOAD_REDIRECT_KEY = "uploadRedirect";

  /**
   * Clear out session variables used by the upload spreadsheet workflow.
   */
  private static void clearSesionVariables(final HttpSession session) {
    session.removeAttribute(UploadScheduleData.KEY);
    session.removeAttribute(ProcessSelectedSheet.SHEET_NAMES_KEY);
    session.removeAttribute(UploadSpreadsheet.SHEET_NAME_KEY);
    session.removeAttribute(StoreColumnNames.HEADER_NAMES_KEY);
    session.removeAttribute(StoreColumnNames.HEADER_ROW_INDEX_KEY);
  }

  /**
   * Session key for the name of the sheet in the spreadsheet to load.
   * This will be null if a CSV file was loaded.
   * The data type is {@link String}.
   */
  public static final String SHEET_NAME_KEY = "sheetName";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    clearSesionVariables(session);

    final StringBuilder message = new StringBuilder();
    String nextPage;
    try {
      UploadProcessor.processUpload(request);
      final String uploadRedirect = (String) request.getAttribute(UploadSpreadsheet.UPLOAD_REDIRECT_KEY);
      if (null == uploadRedirect) {
        throw new MissingRequiredParameterException(UploadSpreadsheet.UPLOAD_REDIRECT_KEY);
      }
      session.setAttribute(UploadSpreadsheet.UPLOAD_REDIRECT_KEY, uploadRedirect);
      LOGGER.debug("Redirect: {}", uploadRedirect);

      final FileItem fileItem = (FileItem) request.getAttribute("file");
      if (null == fileItem) {
        throw new MissingRequiredParameterException("file");
      }

      final String extension = Utilities.determineExtension(fileItem.getName());
      final File file = File.createTempFile("fll", extension);
      file.deleteOnExit();
      LOGGER.debug("Wrote data to: {}", file.getAbsolutePath());

      // the write call below will fail if the file already exists
      if (!file.delete()) {
        final String errorMessage = "Unable to delete temporary file: "
            + file;
        LOGGER.error(errorMessage);
        throw new FLLRuntimeException(errorMessage);
      }
      fileItem.write(file);
      session.setAttribute(SPREADSHEET_FILE_KEY, file.getAbsolutePath());

      if (ExcelCellReader.isExcelFile(file)) {
        final List<String> sheetNames = ExcelCellReader.getAllSheetNames(file);

        LOGGER.debug("Excel file with sheets: {}", sheetNames);

        if (sheetNames.size() > 1) {
          session.setAttribute(ProcessSelectedSheet.SHEET_NAMES_KEY, sheetNames);
          nextPage = "promptForSheetName.jsp";
        } else {
          session.setAttribute(SHEET_NAME_KEY, sheetNames.get(0));
          nextPage = "selectHeaderRow.jsp";
        }
      } else {
        nextPage = "selectHeaderRow.jsp";
      }

    } catch (final FileUploadException e) {
      final String baseMessage = "Error processing spreadsheet upload";
      message.append("<p class='error'>"
          + baseMessage
          + ": "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException(baseMessage, e);
    } catch (final IOException e) {
      final String baseMessage = "Error reading spreadsheet";
      message.append("<p class='error'>"
          + baseMessage
          + ": "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException(baseMessage, e);
    } catch (final Exception e) {
      final String baseMessage = "Error saving file to disk";
      message.append("<p class='error'>"
          + baseMessage
          + ": "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException(baseMessage, e);
    }

    LOGGER.debug("Redirecting to {}", nextPage);
    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(nextPage));
  }

}
