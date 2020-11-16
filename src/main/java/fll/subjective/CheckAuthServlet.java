/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Check if the user is logged in as a judge. This is used by the subjective application.
 * GET: AuthResult
 */
@WebServlet("/subjective/CheckAuth")
public class CheckAuthServlet extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    final boolean authenticated = auth.isJudge();
    final AuthResult result = new AuthResult(authenticated);

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    jsonMapper.writeValue(writer, result);
  }

  /**
   * Result of checking authentication.
   */
  public static class AuthResult {
    /**
     * @param authenticated {@link #getAuthenticated()}
     */
    public AuthResult(final boolean authenticated) {
      mAuthenticated = authenticated;
    }

    private final boolean mAuthenticated;

    /**
     * @return if the user is authenticated
     */
    public boolean getAuthenticated() {
      return mAuthenticated;
    }
  }

}
