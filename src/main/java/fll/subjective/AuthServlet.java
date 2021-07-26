/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

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

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    if (!auth.isJudge()) {
      session.setAttribute(SessionAttributes.REDIRECT_URL, request.getRequestURI());
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/login.jsp"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("index.html"));
    }
  }

}
