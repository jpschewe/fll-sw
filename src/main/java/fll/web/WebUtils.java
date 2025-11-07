/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.Utilities;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;

/**
 * Some utilities for dealing with the web.
 */
public final class WebUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebUtils.class);

  private static final Pattern NEEDS_ESCAPE = Pattern.compile("[\'\"\\\\]");

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
   * @param application used to create an absolute URL
   * @param response used to send the redirect
   * @param url the url to redirect to
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
   * Get the host address for the specified address. For IPv4 addresses this just
   * calls {@link InetAddress#getHostAddress()}. For IPv6 addresses, the same
   * method is called, however the scope ID needs to be removed.
   *
   * @param address the address to convert
   * @return the IP address as a string.
   */
  public static String getHostAddress(final InetAddress address) {
    final String addrStr = address.getHostAddress();
    if (address instanceof Inet6Address) {
      final int index = addrStr.indexOf('%');
      if (index >= 0) {
        final String trimmed = addrStr.substring(0, index);
        return trimmed;
      } else {
        return addrStr;
      }
    } else {
      return addrStr;
    }
  }

  /**
   * Get all IP addresses associated with this host.
   *
   * @return collection of IP addresses
   */
  public static Collection<InetAddress> getAllIPs() {
    final Collection<InetAddress> ips = new LinkedList<>();
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

    return ips;
  }

  /**
   * Get all names that this computer is known as. This includes all IP addresses
   * and all host names that are found by doing a reverse lookup on each IP
   * address. Due to the DNS lookups, this method may be slow.
   *
   * @return collection of names
   * @see #getAllIPs()
   */
  public static Collection<String> getAllHostNames() {
    final Collection<String> hostNames = new LinkedList<>();

    for (final InetAddress address : getAllIPs()) {
      if (address instanceof Inet4Address) {
        // TODO skip IPv6 for now, need to figure out how to encode and get
        // Tomcat to listen on IPv6. Issue #233.

        if (!address.isLoopbackAddress()) {
          // don't tell the user about connecting to localhost

          final String addrStr = getHostAddress(address);
          hostNames.add(addrStr);

          // check for a name
          try {
            final String name = org.xbill.DNS.Address.getHostName(address);
            hostNames.add(name);
          } catch (final UnknownHostException e) {
            LOGGER.trace("Could not resolve IP: "
                + addrStr, e);
          }
        }
      } // IPv4
    } // foreach IP

    return hostNames;
  }

  /**
   * @param str the string to quote
   * @return a string suitable to be used in javascript
   */
  public static String quoteJavascriptString(final @Nullable String str) {
    if (null == str
        || "".equals(str)) {
      return "\"\"";
    } else {
      final Matcher escapeMatcher = WebUtils.NEEDS_ESCAPE.matcher(str);
      return '"'
          + escapeMatcher.replaceAll("\\\\$0")
          + '"';
    }
  }

  /**
   * Escape a string that will be put into single quotes in javascript and parsed
   * with {@code JSON.parse}.
   * 
   * @param str the string
   * @return the escaped string
   */
  public static String escapeStringForJsonParse(final String str) {
    return str
              // escape the single quotes
              .replace("'", "\\'")
              // double quotes need to have 3 slashes in front of them
              .replace("\\\"", "\\\\\\\"");
  }

  /**
   * @param str the string that needs to be escaped
   * @return A string suitable to be used in a form field
   */
  public static @PolyNull String escapeForHtmlFormValue(final @PolyNull String str) {
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
  public static @Nullable String getParameterOrNull(@Nonnull final ServletRequest request,
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

  /**
   * Based on
   * https://stackoverflow.com/questions/2222238/httpservletrequest-to-complete-url.
   *
   * @param request a request
   * @return the full url, including query string
   * @see HttpServletRequest#getQueryString()
   * @see HttpServletRequest#getRequestURL()
   */
  public static String getFullURL(final HttpServletRequest request) {
    final StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
    final String queryString = request.getQueryString();

    if (null == queryString) {
      return requestURL.toString();
    } else {
      return requestURL.append('?').append(queryString).toString();
    }
  }

  /**
   * Format of the value from a time form field.
   */
  public static final DateTimeFormatter WEB_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  /**
   * @param request where to get the parameter from
   * @param parameter the parameter to get
   * @return the value
   * @throws MissingRequiredParameterException if the parameter is missing
   * @throws DateTimeParseException if the value isn't parsable as a time
   */
  public static LocalTime getTimeRequestParameter(final HttpServletRequest request,
                                                  final String parameter)
      throws MissingRequiredParameterException, DateTimeParseException {
    final String str = request.getParameter(parameter);
    if (null == str) {
      throw new MissingRequiredParameterException(parameter);
    }
    final LocalTime value = LocalTime.parse(str, WEB_TIME_FORMAT);
    return value;
  }

  /**
   * @param request where to get the parameter from
   * @param parameter the parameter to get
   * @return the value
   * @throws MissingRequiredParameterException if the parameter is missing
   * @throws NumberFormatException if the value isn't parsable as an integer
   */
  public static int getIntRequestParameter(final HttpServletRequest request,
                                           final String parameter)
      throws MissingRequiredParameterException, NumberFormatException {
    final String str = request.getParameter(parameter);
    if (null == str) {
      throw new MissingRequiredParameterException(parameter);
    }
    final int value = Integer.parseInt(str);
    return value;
  }

  /**
   * @param request where to get the parameter from
   * @param parameter the parameter to get
   * @param defaultValue the value to return if the parameter is not present
   * @return the value
   * @throws NumberFormatException if the value isn't parsable as an integer
   */
  public static int getIntRequestParameter(final HttpServletRequest request,
                                           final String parameter,
                                           final int defaultValue)
      throws NumberFormatException {
    final String str = request.getParameter(parameter);
    if (null == str) {
      return defaultValue;
    }
    final int value = Integer.parseInt(str);
    return value;
  }

  /**
   * @param request where to get the parameter from
   * @param parameter the parameter to get
   * @return the value
   * @throws MissingRequiredParameterException if the parameter is missing
   * @throws NumberFormatException if the value isn't parsable as a double
   */
  public static double getDoubleRequestParameter(final HttpServletRequest request,
                                                 final String parameter)
      throws MissingRequiredParameterException, NumberFormatException {
    final String str = request.getParameter(parameter);
    if (null == str) {
      throw new MissingRequiredParameterException(parameter);
    }
    final double value = Double.parseDouble(str);
    return value;
  }

  /**
   * @param request where to get the parameter from
   * @param parameter the parameter to get
   * @param defaultValue the value to return if the parameter is not present
   * @return the value
   * @throws NumberFormatException if the value isn't parsable as an integer
   */
  public static boolean getBooleanRequestParameter(final HttpServletRequest request,
                                                   final String parameter,
                                                   final boolean defaultValue)
      throws NumberFormatException {
    final String str = request.getParameter(parameter);
    if (null == str) {
      return defaultValue;
    }
    final boolean value = Boolean.parseBoolean(str);
    return value;
  }

  /**
   * @param request where to get the parameter from
   * @param parameter the parameter to get
   * @return the value
   * @throws MissingRequiredParameterException if the parameter is missing
   */
  public static String getNonNullRequestParameter(final HttpServletRequest request,
                                                  final String parameter)
      throws MissingRequiredParameterException {
    final String str = request.getParameter(parameter);
    if (null == str) {
      throw new MissingRequiredParameterException(parameter);
    }
    return str;
  }

  /**
   * Redirect to the page in {@link SessionAttributes#getRedirectURL(HttpSession)}
   * or "/" if nothing is stored in the session.
   * 
   * @param response the response to redirect
   * @param session the session to get the redirect from, this attribute is
   *          cleared by the method
   * @throws IOException {@link HttpServletResponse#sendRedirect(String)}
   */
  public static void sendRedirect(final HttpServletResponse response,
                                  final HttpSession session)
      throws IOException {
    final String redirect = SessionAttributes.getRedirectURL(session);

    // clear variable since it's been used
    SessionAttributes.clearRedirectURL(session);

    sendRedirect(response, redirect);
  }

  /**
   * Redirect to the page specified
   * or "/" if {@code redirect} doesn't specify a location.
   * 
   * @param response the response to redirect
   * @param redirect where to send the user
   * @throws IOException {@link HttpServletResponse#sendRedirect(String)}
   */
  public static void sendRedirect(final HttpServletResponse response,
                                  final @Nullable String redirect)
      throws IOException {
    final String whereTo;
    if (null != redirect) {
      whereTo = redirect;
    } else {
      whereTo = "/";
    }

    LOGGER.trace("Redirecting to {}", redirect);
    response.sendRedirect(response.encodeRedirectURL(whereTo));
  }

  /**
   * The path to the sponsor logos relative to the root of the web application.
   * Does not have a leading or trailing slash.
   */
  public static final String SPONSOR_LOGOS_PATH = "sponsor_logos";

  /**
   * The path to the slideshow images relative to the root of the web application.
   * Does not have a leading or trailing slash.
   */
  public static final String SLIDESHOW_PATH = "slideshow";

  /**
   * The path to the custom images relattive to the root of the web application.
   * Does not have a leading or trailing slash.
   */
  public static final String CUSTOM_PATH = "custom";

  /**
   * Get the sponsor logo filenames.
   *
   * @param application used to get the absolute path of the sponsor logos
   *          directory
   * @return sorted sponsor logos filenames relative to the root of the web
   *         application
   * @see Utilities#getGraphicFiles(File)
   */
  public static List<String> getSponsorLogos(final ServletContext application) {
    final String imagePath = application.getRealPath("/"
        + SPONSOR_LOGOS_PATH);

    final List<String> logoFiles = Utilities.getGraphicFiles(new File(imagePath));

    return logoFiles;
  }

  /**
   * Send a text message to a client over the specified websocket.
   * In most cases the websocket should be discarded if false is returned.
   * 
   * @param session the websocket
   * @param messageText the message to send
   * @return true if the message was sent, false if not.
   */
  public static boolean sendWebsocketTextMessage(final Session session,
                                                 final String messageText) {
    if (session.isOpen()) {
      try {
        synchronized (session) {
          session.getBasicRemote().sendText(messageText);
        }
        return true;
      } catch (final EOFException e) {
        LOGGER.debug("Caught EOF sending message to {}, dropping session", session.getId(), e);
        return false;
      } catch (final IOException ioe) {
        LOGGER.error("Got error sending message to session ({}), dropping session", session.getId(), ioe);
        return false;
      } catch (final IllegalStateException e) {
        LOGGER.warn("Illegal state exception writing to client, dropping: {}", session.getId(), e);
        return false;
      }
    } else {
      return false;
    }
  }
}
