/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import javax.servlet.http.HttpSession;

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

  /**
   * @param session where to get the information
   * @return the message to display to the user
   */
  public static String getMessage(final HttpSession session) {
    return getAttribute(session, MESSAGE, String.class);
  }

  /**
   * Key in the session used to store the URL to redirect to after the current operation completes.
   */
  public static final String REDIRECT_URL = "redirect_url";

  /**
   * @param session where to get the information
   * @return the URL to send the user to after the current operation completes
   */
  public static String getRedirectURL(final HttpSession session) {
    return getAttribute(session, REDIRECT_URL, String.class);
  }

  /**
   * Key in the session used to store the display name.
   * @see fll.web.DisplayInfo
   */
  public static final String DISPLAY_NAME = "displayName";
  
  /**
   * Get the name for the current display.
   *  
   * @param session where to get the information from
   * @return may be null
   */
  public static String getDisplayName(final HttpSession session) {
    return getAttribute(session, DISPLAY_NAME, String.class);
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
   * Get a session attribute and throw a {@link NullPointerException} if it's
   * null.
   * 
   * @see #getAttribute(HttpSession, String, Class)
   */
  public static <T> T getNonNullAttribute(final HttpSession session, final String attribute, final Class<T> clazz) {
    final T retval = getAttribute(session, attribute, clazz);
    if (null == retval) {
      throw new NullPointerException(String.format("Session attribute %s is null when it's not expected to be", attribute));
    }
    return retval;
  }

}
