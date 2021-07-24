/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Initialize the web attributes and then call the process method.
 */
public abstract class BaseFLLServlet extends HttpServlet {

  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    doGet(request, response);
  }

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();

    processRequest(request, response, application, session);
  }

  /**
   * Called from both {@link #doGet(HttpServletRequest, HttpServletResponse)} and
   * {@link #doPost(HttpServletRequest, HttpServletResponse)} methods.
   *
   * @param request http request
   * @param response http response
   * @param application application variable store
   * @param session session variable store
   * @throws IOException errors talking to the request or response
   * @throws ServletException base servlet error
   */
  protected abstract void processRequest(HttpServletRequest request,
                                         HttpServletResponse response,
                                         ServletContext application,
                                         HttpSession session)
      throws IOException, ServletException;

}
