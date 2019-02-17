/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;

/**
 * Some cookie utilities for FLL.
 */
public final class CookieUtils {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final String LOGIN_KEY = "fll-login";

  private CookieUtils() {
  }

  /**
   * Clear out all login cookies.
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

    final Collection<Cookie> loginCookies = CookieUtils.findLoginCookie(request);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      for (final Cookie loginCookie : loginCookies) {
        Cookie delCookie = new Cookie(loginCookie.getName(), "");
        delCookie.setMaxAge(0);
        delCookie.setDomain(domain);
        response.addCookie(delCookie);

        Queries.removeValidLoginByKey(connection, loginCookie.getValue());

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Removed cookie from DB: "
              + loginCookie.getValue());
        }
      }
  
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
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
    cookie.setMaxAge(7 * 24 * 60 * 60); // week year
    cookie.setPath("/");
    response.addCookie(cookie);
  }

  /**
   * Find all login cookies.
   * 
   * @param request where to find the cookies
   * @return the cookie or null if not found
   */
  public static Collection<Cookie> findLoginCookie(final HttpServletRequest request) {
    final Collection<Cookie> found = new LinkedList<Cookie>();

    final Cookie[] cookies = request.getCookies();
    if (null == cookies) {
      return found;
    }

    for (final Cookie cookie : cookies) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Checking cookie: "
            + cookie.getName());
      }
      if (LOGIN_KEY.equals(cookie.getName())) {
        found.add(cookie);
      }
    }
    return found;
  }

  /**
   * Find the login key(s)
   * 
   * @param request where to find the cookies
   * @return the string stored in the cookie or null if not found
   */
  public static Collection<String> findLoginKey(final HttpServletRequest request) {
    final Collection<String> retval = new LinkedList<String>();
    for (final Cookie cookie : findLoginCookie(request)) {
      if (null != cookie) {
        retval.add(cookie.getValue());
      }
    }
    return retval;
  }
}
