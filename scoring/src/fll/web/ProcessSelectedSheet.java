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
 * Grap the sheetName parameter and put it into the session.
 * 
 */
@WebServlet("/ProcessSelectedSheet")
public final class ProcessSelectedSheet extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();
    
    final String sheetName = request.getParameter("sheetName");
    if (null == sheetName) {
      throw new RuntimeException("Missing parameter 'sheetName'");
    }
    session.setAttribute("sheetName", sheetName);

    final String uploadRedirect = SessionAttributes.getAttribute(session, "uploadRedirect", String.class);

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(uploadRedirect));
  }

}
