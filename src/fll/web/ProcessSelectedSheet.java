/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Grab the sheetName parameter and put it into the session as
 * {@link UploadSpreadsheet#SHEET_NAME_KEY}.
 */
@WebServlet("/ProcessSelectedSheet")
public final class ProcessSelectedSheet extends BaseFLLServlet {

  /**
   * Key in the sesession where <code>promptForSheetName.jsp</code> expects to
   * find a {@link List} of {@link String} objects to present to the user for
   * choosing the sheet name.
   */
  public static final String SHEET_NAMES_KEY = "sheetNames";

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();

    final String sheetName = request.getParameter(UploadSpreadsheet.SHEET_NAME_KEY);
    if (null == sheetName) {
      throw new RuntimeException("Missing parameter '"
          + UploadSpreadsheet.SHEET_NAME_KEY
          + "'");
    }
    session.setAttribute(UploadSpreadsheet.SHEET_NAME_KEY, sheetName);

    final String uploadRedirect = SessionAttributes.getNonNullAttribute(session, UploadSpreadsheet.UPLOAD_REDIRECT_KEY,
                                                                        String.class);

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(uploadRedirect));
  }

}
