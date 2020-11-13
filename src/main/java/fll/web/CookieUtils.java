/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.diffplug.common.base.Errors;

import fll.db.Authentication;

/**
 * Some cookie utilities for FLL.
 */
public final class CookieUtils {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String LOGIN_KEY = "fll-login";

  private CookieUtils() {
  }

  /**
   * Clear out all login cookies.
   *
   * @param application the application context
   * @param request the servlet request
   * @param response the servlet response
   */
  public static void clearLoginCookies(final ServletContext application,
                                       final HttpServletRequest request,
                                       final HttpServletResponse response) {
    final String hostHeader = request.getHeader("host");
    final int colonIndex = hostHeader.indexOf(':');
    final String domain;
    if (-1 != colonIndex) {
      domain = hostHeader.substring(0, colonIndex);
    } else {
      domain = hostHeader;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("domain: "
          + domain);
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      CookieUtils.findLoginCookies(request).forEach(Errors.rethrow().wrap(loginCookie -> {
        final Cookie delCookie = new Cookie(loginCookie.getName(), "");
        delCookie.setMaxAge(0);
        delCookie.setDomain(domain);
        response.addCookie(delCookie);

        Authentication.removeValidLoginByKey(connection, loginCookie.getValue());

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Removed cookie from DB: "
              + loginCookie.getValue());
        }
      }));

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Set the login cookie.
   *
   * @param response the response
   * @param magicKey the key to store
   */
  public static void setLoginCookie(final HttpServletResponse response,
                                    final String magicKey) {
    final Cookie cookie = new Cookie(LOGIN_KEY, magicKey);
    cookie.setMaxAge(7
        * 24
        * 60
        * 60); // week year
    cookie.setPath("/");
    response.addCookie(cookie);
  }

  /**
   * Find all login cookies.
   *
   * @param request where to find the cookies
   * @return the login cookies
   */
  private static Stream<Cookie> findLoginCookies(final HttpServletRequest request) {
    final Cookie[] cookies = request.getCookies();
    if (null == cookies) {
      return Stream.of();
    } else {
      return Arrays.stream(cookies).filter(c -> LOGIN_KEY.equals(c.getName()));
    }
  }

  /**
   * Find the login key(s).
   *
   * @param request where to find the cookies
   * @return the string stored in the cookie or null if not found
   */
  public static Collection<String> findLoginKey(final HttpServletRequest request) {
    return findLoginCookies(request).map(Cookie::getValue).collect(Collectors.toUnmodifiableList());
  }
}
