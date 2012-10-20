/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.web.WebUtils;

/**
 * Saves an uploaded schedule into a temporary file referenced by the session
 * variable "uploadSchedule_file" (type {@link File}).
 * 
 */
@WebServlet("/schedule/UploadSchedule")
public class UploadSchedule extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final String SCHEDULE_KEY = "uploadSchedule_schedule";
  
  /**
   * Clear out session variables used by the schedule upload workflow.
   */
  public static void clearSesionVariables(final HttpSession session) {
    session.removeAttribute("uploadSchedule_file");
    session.removeAttribute("uploadSchedule_sheet");
    session.removeAttribute("uploadSchedule_schedule");
    session.removeAttribute("uploadSchedule_violations");
    session.removeAttribute(SCHEDULE_KEY);
    session.removeAttribute(GatherEventDivisionChanges.EVENT_DIVISION_INFO_KEY);
    session.removeAttribute("sheetName");
    session.removeAttribute("sheetNames");
    session.removeAttribute(CheckViolations.SUBJECTIVE_STATIONS);
    session.removeAttribute(CheckViolations.UNUSED_HEADERS);
  }
  
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    clearSesionVariables(session);
    
    final File file = File.createTempFile("fll", null);
    file.deleteOnExit();
    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      // process file and keep track of filename in session.scheduleFilename
      final FileItem scheduleFileItem = (FileItem) request.getAttribute("scheduleFile");
      if (null == scheduleFileItem
          || scheduleFileItem.getSize() == 0) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>A file containing a schedule must be specified</p>");
        WebUtils.sendRedirect(application, response, "/admin/index.jsp");
        return;
      } else {
        scheduleFileItem.write(file);
        session.setAttribute("uploadSchedule_file", file);

        WebUtils.sendRedirect(application, response, "/schedule/CheckScheduleExists");
        return;
      }
    } catch (final FileUploadException e) {
      LOGGER.error("There was an error processing the file upload", e);
      throw new FLLRuntimeException("There was an error processing the file upload", e);
    } catch (final Exception e) {
      final String message = "There was an error writing the uploaded file to the filesystem";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    }

  }

}
