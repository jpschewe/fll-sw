/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Some cookie utilities for FLL.
 */
public final class CookieUtils {

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
    response.addCookie(cookie);
  }

  /**
   * Find the login cookie.
   * 
   * @param request where to find the cookies
   * @return the cookie or null if not found
   */
  public static Cookie findLoginCookie(final HttpServletRequest request) {
    for (final Cookie cookie : request.getCookies()) {
      if (LOGIN_KEY.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * Find the login key.
   * 
   * @param request where to find the cookies
   * @return the string stored in the cookie or null if not found
   */
  public static String findLoginKey(final HttpServletRequest request) {
    final Cookie cookie = findLoginCookie(request);
    if (null == cookie) {
      return null;
    } else {
      return cookie.getValue();
    }
  }
}
