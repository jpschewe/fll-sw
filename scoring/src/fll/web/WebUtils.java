/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import net.mtu.eggplant.util.sql.SQLFunctions;

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
                                  final String url)
      throws IOException {
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
      if (address instanceof Inet4Address) {
        // TODO skip IPv6 for now, need to figure out how to encode and get
        // Tomcat to listen on IPv6

        if (!address.isLoopbackAddress()) {
          // don't tell the user about connecting to localhost

          final String url = "http://"
              + addrStr
              + ":"
              + request.getLocalPort()
              + request.getContextPath();
          urls.add(url);

          // check for a name
          try {
            final String name = org.xbill.DNS.Address.getHostName(address);

            final String nameurl = "http://"
                + name
                + ":"
                + request.getLocalPort()
                + request.getContextPath();
            urls.add(nameurl);
          } catch (final UnknownHostException e) {
            LOGGER.trace("Could not resolve IP: "
                + addrStr, e);
          }
        }
      }
    }
    return urls;
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
      return '"'
          + escapeMatcher.replaceAll("\\\\$0")
          + '"';
    }
  }

  /**
   * Check if the web request is authenticated.
   * If the connection is from localhost it's
   * allowed.
   * 
   * @param request the web request
   * @param application the application context
   * @return true if authenticated, false otherwise
   */
  public static boolean checkAuthenticated(final HttpServletRequest request,
                                           final ServletContext application) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    // check request against all interfaces
    String requestAddressStr = request.getRemoteAddr();

    // remove zone from IPv6 addresses
    final int zoneIndex = requestAddressStr.indexOf('%');
    if (-1 != zoneIndex) {
      requestAddressStr = requestAddressStr.substring(0, zoneIndex);
    }

    try {
      final InetAddress requestAddress = InetAddress.getByName(requestAddressStr);
      if (requestAddress.isLoopbackAddress()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Returning true from checkAuthenticated for connection from local ip: "
              + requestAddressStr);
        }
        return true;
      }
    } catch (final UnknownHostException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Error converting request address string to address: "
            + requestAddressStr, e);
      }
    }

    if (null == datasource) {
      throw new FLLRuntimeException("Database is not initialized and security is required, you must initialize the database from localhost");
    }

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      if (Queries.isAuthenticationEmpty(connection)) {
        LOGGER.debug("Returning true from checkAuthenticated for empty auth");
        return true;
      }

      final Collection<String> loginKeys = CookieUtils.findLoginKey(request);
      final String user = Queries.checkValidLogin(connection, loginKeys);
      if (null != user) {
        LOGGER.debug("Returning true from checkSecurity for valid login: "
            + loginKeys
            + " user: "
            + user);
        return true;
      } else {
        LOGGER.debug("Returning false from checkAuthenticated");
        return false;
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Escape the string to be used in the value of a form field.
   */
  public static String escapeForHtmlFormValue(final String str) {
    if (null == str) {
      return str;
    } else {
      return str.replace("'", "&apos;");
    }
  }

  /**
   * Get a parameter value, trim it and if it's empty return null.
   * 
   * @param request the request to get the parameter from
   * @param parameterName the name of the parameter to find
   * @return the parameter value, null if the parameter isn't present OR the
   *         parameter value is the empty string
   */
  public static String getParameterOrNull(@Nonnull final ServletRequest request,
                                          @Nonnull final String parameterName) {
    final String rawValue = request.getParameter(parameterName);
    if (null == rawValue) {
      return null;
    } else {
      final String trimmed = rawValue.trim();
      if (trimmed.isEmpty()) {
        return null;
      } else {
        return trimmed;
      }
    }
  }
}
