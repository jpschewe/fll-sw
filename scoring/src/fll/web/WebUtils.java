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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some utilities for dealing with the web.
 */
public final class WebUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(WebUtils.class);

  private static final Pattern needsEscape = Pattern.compile("[\'\"\\\\]");

  private static final Collection<InetAddress> ips = new LinkedList<InetAddress>();

  private static long ipsExpiration = 0;

  private static final long IP_CACHE_LIFETIME = 300000;

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
  public static Collection<String> getAllURLs(final HttpServletRequest request) {
    final Collection<String> urls = new LinkedList<String>();
    for (final InetAddress address : getAllIPs()) {
      final String addrStr = address.getHostAddress();
      if (!addrStr.contains(":")) {
        // skip IPv6 for now, need to figure out how to encode and get
        // Tomcat to listen on IPv6
        if (!address.isLoopbackAddress()) {
          final String url = "http://"
              + addrStr + ":" + request.getLocalPort() + "/" + request.getContextPath();
          urls.add(url);
        }
      }
    }
    return urls;
  }

  public static Collection<String> getAllIPStrings() {
    final Collection<String> ips = new LinkedList<String>();
    for (final InetAddress address : getAllIPs()) {
      String addr = address.getHostAddress();
      
      // remove IPv6 zone information 
      if (addr.contains("%")) {
        addr = addr.substring(0, addr.indexOf('%'));
      }
      
      ips.add(addr);
    }
    return ips;
  }

  /**
   * Get all IP addresses associated with this host.
   * Thread safe.
   * 
   * @return unmodifiable collection of IP addresses
   */
  public static Collection<InetAddress> getAllIPs() {
    synchronized (ips) {
      if (System.currentTimeMillis() > ipsExpiration) {
        ips.clear();

        try {
          final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
          if (null != interfaces) {
            while (interfaces.hasMoreElements()) {
              final NetworkInterface ifce = interfaces.nextElement();
              final Enumeration<InetAddress> addresses = ifce.getInetAddresses();
              while (addresses.hasMoreElements()) {
                ips.add(addresses.nextElement());
              }
            }
          }
        } catch (final IOException e) {
          LOGGER.error("Error getting list of IP addresses for this host", e);
        }

        ipsExpiration = System.currentTimeMillis()
            + IP_CACHE_LIFETIME;

      } // end if
    } // end synchronized
    return Collections.unmodifiableCollection(ips);
  }

  /**
   * Take a string and quote it for Javascript.
   */
  public static String quoteJavascriptString(final String str) {
    if (null == str
        || "".equals(str)) {
      return "\"\"";
    } else {
      final Matcher escapeMatcher = WebUtils.needsEscape.matcher(str);
      return '"' + escapeMatcher.replaceAll("\\\\$0") + '"';
    }
  }

}
