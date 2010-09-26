/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.util.ExcelCellReader;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Reads uploadSchedule_file and figures out the sheet name. If a single sheet,
 * stores in uploadSchedule_sheet ({@link String}), otherwise stores sheets in
 * uploadSchedule_sheetNames ({@link List}).
 * <p>
 * If redirected here with sheetName set in the request, store that in
 * uploadSchedule_sheet.
 * </p>
 * 
 * @web.servlet name="GetSheetNames"
 * @web.servlet-mapping url-pattern="/schedule/GetSheetNames"
 */
public class GetSheetNames extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if(null != session.getAttribute("sheetName")) {
      // handle case where we're redirected here from picking the sheet
      session.setAttribute("uploadSchedule_sheet", session.getAttribute("sheetName"));
      WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
      return;   
    }
    
    try {
      final File file = SessionAttributes.getNonNullAttribute(session, "uploadSchedule_file", File.class);

      // get list of sheet names
      final List<String> sheetNames = ExcelCellReader.getAllSheetNames(file);

      if (sheetNames.isEmpty()) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>The specified spreadsheet has no worksheets.</p>");
        WebUtils.sendRedirect(application, response, "/admin/index.jsp");
        return;
      } else if (1 == sheetNames.size()) {
        session.setAttribute("uploadSchedule_sheet", sheetNames.get(0));
        WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
        return;
      } else {
        session.setAttribute("sheetNames", sheetNames);
        session.setAttribute("uploadRedirect", WebUtils.makeAbsoluteURL(application, "/schedule/GetSheetNames"));
        WebUtils.sendRedirect(application, response, "/promptForSheetName.jsp");
        return;
      }

    } catch (final InvalidFormatException e) {
      final String message = "Error reading the spreadsheet";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    }

  }

}
