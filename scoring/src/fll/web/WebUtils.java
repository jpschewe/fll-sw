/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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
   * <p>
   * The method calling this method should return right away.
   * </p>
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
    if (null != url
        && url.startsWith("/")) {
      newURL = makeAbsoluteURL(application, url);
    } else {
      newURL = url;
    }

    response.sendRedirect(response.encodeRedirectURL(newURL));
  }

  /**
   * Get a list of the URLs that can be used to access the server. This gets all
   * network interfaces and their IP addresses and filters out localhost.
   * 
   * @return the list of URLs, will be empty if no interfaces (other than
   *         localhost) are found
   */
  public static Collection<String> getAllURLs(final HttpServletRequest request) throws IOException {
    final Collection<String> urls = new LinkedList<String>();
    final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    if (null != interfaces) {
      while (interfaces.hasMoreElements()) {
        final NetworkInterface ifce = interfaces.nextElement();
        final Enumeration<InetAddress> addresses = ifce.getInetAddresses();
        while (addresses.hasMoreElements()) {
          final InetAddress addr = addresses.nextElement();
          final String addrStr = addr.getHostAddress();
          if (!addrStr.contains(":")) {
            // skip IPv6 for now, need to figure out how to encode and get
            // Tomcat to listen on IPv6
            if (!addr.isLoopbackAddress()) {
              final String url = "http://"
                  + addr.getHostAddress() + ":" + request.getLocalPort() + "/" + request.getContextPath();
              urls.add(url);
            }
          }
        }
      }
    }
    return urls;
  }

}
