/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Support for permission-denied.jsp.
 */
public final class PermissionDenied {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private PermissionDenied() {
  }

  /**
   * Populate the redirect url in the session if this page was visited due to a
   * forward.
   * 
   * @param session session variables
   * @param request browser request
   */
  public static void populateContext(final HttpSession session,
                                     final HttpServletRequest request) {
    final Object origUri = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    if (null != origUri) {
      final String originalUri = origUri.toString();
      session.setAttribute(SessionAttributes.REDIRECT_URL, originalUri);
    }
    LOGGER.debug("Redirect URL is {}", SessionAttributes.getRedirectURL(session));
    LOGGER.debug("Request is for {}", request.getRequestURL());
    LOGGER.debug("forward.request_uri: {}", request.getAttribute("javax.servlet.forward.request_uri"));
    LOGGER.debug("forward.context_path: {}", request.getAttribute("javax.servlet.forward.context_path"));
    LOGGER.debug("forward.servlet_path: {}", request.getAttribute("javax.servlet.forward.servlet_path"));
    LOGGER.debug("forward.path_info: {}", request.getAttribute("javax.servlet.forward.path_info"));
    LOGGER.debug("forward.query_string: {}", request.getAttribute("javax.servlet.forward.query_string"));
  }

}
