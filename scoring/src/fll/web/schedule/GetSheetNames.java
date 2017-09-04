/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.util.ExcelCellReader;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.ProcessSelectedSheet;
import fll.web.SessionAttributes;
import fll.web.UploadSpreadsheet;
import fll.web.WebUtils;

/**
 * Reads the schedule file and figures out the sheet name. If a single sheet,
 * populates {@link UploadScheduleData#getSelectedSheet()}, otherwise stores
 * sheets in
 * session attribute {@link ProcessSelectedSheet#SHEET_NAMES_KEY} and
 * redirects to "/promptForSheetName.jsp" with a redirect URL back to this
 * servlet.
 * <p>
 * If redirected here with sheetName set in the request, store that in
 * {@link UploadScheduleData#setSelectedSheet(String)} and send on to
 * {@link CheckViolations}.
 * </p>
 */
@WebServlet("/schedule/GetSheetNames")
public class GetSheetNames extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    try {
      if (null != session.getAttribute(UploadSpreadsheet.SHEET_NAME_KEY)) {
        // handle case where we're redirected here from picking the sheet
        uploadScheduleData.setSelectedSheet(SessionAttributes.getAttribute(session, UploadSpreadsheet.SHEET_NAME_KEY,
                                                                           String.class));
        WebUtils.sendRedirect(application, response, "/schedule/scheduleConstraints.jsp");
        return;
      }

      try {
        final File file = uploadScheduleData.getScheduleFile();
        Objects.requireNonNull(file);

        final boolean isCsvFile = !ExcelCellReader.isExcelFile(file);
        if (isCsvFile) {
          // must be CSV file
          uploadScheduleData.setSelectedSheet(UploadScheduleData.CSV_SHEET_NAME);
          WebUtils.sendRedirect(application, response, "/schedule/scheduleConstraints.jsp");
          return;
        } else {
          // get list of sheet names
          final List<String> sheetNames = ExcelCellReader.getAllSheetNames(file);

          if (sheetNames.isEmpty()) {
            session.setAttribute(SessionAttributes.MESSAGE,
                                 "<p class='error'>The specified spreadsheet has no worksheets.</p>");
            WebUtils.sendRedirect(application, response, "/admin/index.jsp");
            return;
          } else if (1 == sheetNames.size()) {
            uploadScheduleData.setSelectedSheet(sheetNames.get(0));
            WebUtils.sendRedirect(application, response, "/schedule/scheduleConstraints.jsp");
            return;
          } else {
            session.setAttribute(ProcessSelectedSheet.SHEET_NAMES_KEY, sheetNames);
            session.setAttribute(UploadSpreadsheet.UPLOAD_REDIRECT_KEY,
                                 WebUtils.makeAbsoluteURL(application, "/schedule/GetSheetNames"));
            WebUtils.sendRedirect(application, response, "/promptForSheetName.jsp");
            return;
          }
        }

      } catch (final InvalidFormatException e) {
        final String message = "Error reading the spreadsheet";
        LOGGER.error(message, e);
        throw new FLLRuntimeException(message, e);
      }
    } finally {
      // ensure that the updated stored data is written into the session
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

}
