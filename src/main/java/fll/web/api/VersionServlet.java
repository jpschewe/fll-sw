/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.Version;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;

/**
 * GET: "version".
 */
@WebServlet("/api/Version")
public class VersionServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isPublic()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    jsonMapper.writeValue(writer, Version.getVersion());
  }

}
