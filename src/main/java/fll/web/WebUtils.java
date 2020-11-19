/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   * The key to use with {@link ServletContext#getAttribute(String)} to find the
   * list of hostnames.
   * This is a collection of strings.
   */
  private static final String HOSTNAMES_KEY = "HOSTNAMES";

  /**
   * The key to use with {@link ServletContext#getAttribute(String)} to find when
   * the hostname should expire.
   * This is a {@link LocalTime} object.
   */
  private static final String HOSTNAMES_EXPIRATION_KEY = "HOSTNAMES_EXPIRATION";

  /**
   * How long to cache the hostnames.
   */
  private static final Duration HOSTNAME_LIFETIME = Duration.ofMinutes(5);

  /**
   * Compute the host names and store them in the application. This is done in the
   * background to avoid issues with DNS timeouts.
   *
   * @param application where to store the hostnames
   * @see #HOSTNAMES_KEY
   * @see #HOSTNAMES_EXPIRATION_KEY
   * @see #HOSTNAME_LIFETIME
   */
  public static void updateHostNamesInBackground(final ServletContext application) {
    ForkJoinPool.commonPool().execute(() -> {
      final Collection<String> urls = WebUtils.getAllHostNames();
      application.setAttribute(HOSTNAMES_KEY, urls);
      final LocalTime expire = LocalTime.now().plus(HOSTNAME_LIFETIME);
      application.setAttribute(HOSTNAMES_EXPIRATION_KEY, expire);
    });
  }

  /**
   * Call {@link #updateHostNamesInBackground(ServletContext)} if the hostnames
   * have expired or have not been set.
   * 
   * @param application application variable store
   */
  public static void scheduleHostnameUpdateIfNeeded(final ServletContext application) {
    final LocalTime expiration = ApplicationAttributes.getAttribute(application, HOSTNAMES_EXPIRATION_KEY,
                                                                    LocalTime.class);
    if (null == expiration
        || LocalTime.now().isAfter(expiration)) {
      updateHostNamesInBackground(application);
    }
  }

  /**
   * Get all URLs that this host can be access via. The scheme of the URLs is
   * determined by the scheme of the request.
   *
   * @param request the request
   * @param application where to get the stored hostnames and optionally update
   *          the hostnames
   * @return the urls for this host
   */
  public static Collection<String> getAllUrls(final HttpServletRequest request,
                                              final ServletContext application) {
    scheduleHostnameUpdateIfNeeded(application);

    @SuppressWarnings("unchecked") // can't store generics in ServletContext
    final Collection<String> hostNames = ApplicationAttributes.getAttribute(application, HOSTNAMES_KEY,
                                                                            Collection.class);
    if (null == hostNames) {
      // no data yet
      return Collections.emptyList();
    }

    final Collection<String> urls = hostNames.stream().map(hostName -> {
      return String.format("%s://%s:%d%s", request.getScheme(), hostName, request.getLocalPort(),
                           request.getContextPath());
    }).collect(Collectors.toList());
    return urls;
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
   * @param str the string that needs to be escaped
   * @return A string suitable to be used in a form field
   */
  public static String escapeForHtmlFormValue(final @Nullable String str) {
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
}
