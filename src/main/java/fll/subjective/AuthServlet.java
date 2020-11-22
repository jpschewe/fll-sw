/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Check that a user is authenticated as a judge and redirect to the subjective
 * web application at index.html or the login page.
 */
@WebServlet("/subjective/Auth")
public class AuthServlet extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.isJudge()) {
      session.setAttribute(SessionAttributes.REDIRECT_URL, request.getRequestURI());
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/login.jsp"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("index.html"));
    }
  }

}
