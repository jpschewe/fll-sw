/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Initialize the web attributes and then call the process method.
 */
public abstract class BaseFLLServlet extends HttpServlet {

  @Override
  protected final void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    doGet(request, response);
  }

  @Override
  protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    try {
      Init.initialize(request, response);
    } catch (final SQLException e) {
      throw new RuntimeException("Error in initialization", e);
    }

    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();

    processRequest(request, response, application, session);
  }

  protected abstract void processRequest(HttpServletRequest request, HttpServletResponse response, ServletContext application, HttpSession session)
      throws IOException, ServletException;

}
