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

import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Check that a user is authenticated and redirect to the subjective web
 * application at index.html.
 */
@WebServlet("/subjective/Auth")
public class AuthServlet extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final boolean authenticated = WebUtils.checkAuthenticated(request, application);
    if (authenticated) {
      response.sendRedirect(response.encodeRedirectURL("index.html"));
    } else {
      // shouldn't get here due to the init filter, but put it in to be sure.
      session.setAttribute(SessionAttributes.REDIRECT_URL, request.getRequestURI());
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/login.jsp"));
    }
  }

}
