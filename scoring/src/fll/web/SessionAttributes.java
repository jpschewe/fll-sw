/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

/**
 * Keys for session variables. Each key has an associated accessor function as
 * well that helps with type safety.
 * 
 * @author jpschewe
 */
public final class SessionAttributes {

  private SessionAttributes() {
    // no instances
  }

  /**
   * A {@link String} that is a message to display. This is set in many pages
   * and servlets to pass a message onto the next page to display to the user.
   */
  public static final String MESSAGE = "message";

  public static String getMessage(final HttpSession session) {
    return getAttribute(session, MESSAGE, String.class);
  }

  /**
   * {@link javax.sql.DataSource} that is connected to the tournament database.
   * Initialized in 'jspf/init.jspf'.
   */
  public static final String DATASOURCE = "datasource";

  public static DataSource getDataSource(final HttpSession session) {
    return getAttribute(session, DATASOURCE, DataSource.class);
  }

  /**
   * {@link String} that keeps track of what page is being shown on the big
   * screen. Used in conjunction with {@link ApplicationAttributes#DISPLAY_PAGE}
   * to keep from refreshing the display too often.
   */
  public static final String SESSION_DISPLAY_PAGE = "sessionDisplayPage";
  public static String getSessionDisplayPage(final HttpSession session) {
    return getAttribute(session, SESSION_DISPLAY_PAGE, String.class);
  }
  
  public static final String REDIRECT_URL = "redirect_url";
  public static String getRedirectURL(final HttpSession session) {
    return getAttribute(session, REDIRECT_URL, String.class);
  }
  
  /**
   * Get session attribute and send appropriate error if type is wrong. Note
   * that null is always valid.
   * 
   * @param session where to get the attribute
   * @param attribute the attribute to get
   * @param clazz the expected type
   */
  public static <T> T getAttribute(final HttpSession session, final String attribute, final Class<T> clazz) {
    final Object o = session.getAttribute(attribute);
    if (o == null
        || clazz.isInstance(o)) {
      return clazz.cast(o);
    } else {
      throw new ClassCastException(String.format("Expecting session attribute '%s' to be of type '%s', but was of type '%s'", attribute, clazz, o.getClass()));
    }
  }
  
  /**
   * Get a session attribute and throw a {@link NullPointerException} if it's null.
   * 
   *  @see #getAttribute(HttpSession, String, Class)
   */
  public static <T> T getNonNullAttribute(final HttpSession session, final String attribute, final Class<T> clazz) {
    final T retval = getAttribute(session, attribute, clazz);
    if(null == retval) {
      throw new NullPointerException(String.format("Session attribute %s is null when it's not expected to be", attribute));
    }
    return retval;
  }
  
}
