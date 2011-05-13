/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Some cookie utilities for FLL.
 */
public final class CookieUtils {

  private static final Logger LOG = LogUtils.getLogger();

  public static final String LOGIN_KEY = "fll-login";

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
    for (final Cookie cookie : request.getCookies()) {
      LOG.info("Checking cookie: "
          + cookie.getName());
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
