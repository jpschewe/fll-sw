/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

/**
 * Some utilities for dealing with the web.
 */
public final class WebUtils {

  private WebUtils() {
    // no instances
  }

  /**
   * Take a URL and make it absolute by prepending the context path.
   * 
   * @param application the servlet context
   * @param url expected to begin with a slash
   * @return the absolute URL
   */
  public static String makeAbsoluteURL(final ServletContext application,
                                       final String url) {
    return application.getContextPath()
        + url;
  }

  /**
   * Redirect to the specified URL relative to the context path. The context
   * path is prepended to the URL. Takes care of encoding the URL and if it
   * starts with a slash, the context path is prepended to ensure the correct
   * URL is created.
   * 
   * <p>The method calling this method should return right away.</p>
   * 
   * @param application
   * @param url
   * @throws IOException See {@link HttpServletResponse#sendRedirect(String)} 
   * @see #makeAbsoluteURL(ServletContext, String)
   * @see HttpServletResponse#sendRedirect(String)
   */
  public static void sendRedirect(final ServletContext application,
                                  final HttpServletResponse response,
                                  final String url) throws IOException {
    final String newURL;
    if(null != url && url.startsWith("/")) {
      newURL = makeAbsoluteURL(application, url);
    } else {
      newURL = url;
    }
   
    response.sendRedirect(response.encodeRedirectURL(newURL));
  }
}
