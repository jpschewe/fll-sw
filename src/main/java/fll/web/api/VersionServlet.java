/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Version;

/**
 * GET: "version"
 */
@WebServlet("/api/Version")
public class VersionServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ObjectMapper jsonMapper = new ObjectMapper();

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    jsonMapper.writeValue(writer, Version.getVersion());
  }

}
