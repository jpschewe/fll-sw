/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import javax.servlet.http.HttpSession;

import org.checkerframework.checker.nullness.qual.Nullable;

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
   * Append some text to the message in the session.
   *
   * @param session where to get/store the information
   * @param msg the text to append to the message
   */
  public static void appendToMessage(final HttpSession session,
                                     final String msg) {
    final String prevMessage = getMessage(session);
    final StringBuilder builder = new StringBuilder();
    if (null != prevMessage) {
      builder.append(prevMessage);
    }
    builder.append(msg);
    session.setAttribute(MESSAGE, builder.toString());
  }

  /**
   * Key in the session used to store the URL to redirect to after the current
   * operation completes.
   * The type is a {@link String}.
   */
  public static final String REDIRECT_URL = "redirect_url";

  /**
   * @param session where to get the information
   * @return the URL to send the user to after the current operation completes
   */
  public static @Nullable String getRedirectURL(final HttpSession session) {
    return getAttribute(session, REDIRECT_URL, String.class);
  }

  /**
   * Key in the session used to store the display name.
   *
   * @see fll.web.DisplayInfo
   */
  public static final String DISPLAY_NAME = "displayName";

  /**
   * Get the name for the current display.
   *
   * @param session where to get the information from
   * @return may be null
   */
  public static @Nullable String getDisplayName(final HttpSession session) {
    return getAttribute(session, DISPLAY_NAME, String.class);
  }

  /**
   * Get session attribute and send appropriate error if type is wrong. Note
   * that null is always valid.
   *
   * @param session where to get the attribute
   * @param attribute the attribute to get
   * @param clazz the expected type
   * @param <T> the expected type
   * @return the attribute value
   */
  public static <T> @Nullable T getAttribute(final HttpSession session,
                                             final String attribute,
                                             final Class<T> clazz) {
    final Object o = session.getAttribute(attribute);
    if (o == null
        || clazz.isInstance(o)) {
      return clazz.cast(o);
    } else {
      throw new ClassCastException(String.format("Expecting session attribute '%s' to be of type '%s', but was of type '%s'",
                                                 attribute, clazz, o.getClass()));
    }
  }

  /**
   * Get a session attribute and throw a {@link NullPointerException} if it's
   * null.
   *
   * @param session where to get the attribute from
   * @param attribute the name of the attribute to retrieve
   * @param <T> the type of value stored in the attribute
   * @param clazz the type of value stored in the attribute
   * @return the attribute value
   * @see #getAttribute(HttpSession, String, Class)
   */
  public static <T> T getNonNullAttribute(final HttpSession session,
                                          final String attribute,
                                          final Class<T> clazz) {
    final T retval = getAttribute(session, attribute, clazz);
    if (null == retval) {
      throw new NullPointerException(String.format("Session attribute %s is null when it's not expected to be",
                                                   attribute));
    }
    return retval;
  }

  /**
   * Stores the current login information. This is an instance of
   * {@link AuthenticationContext}.
   */
  public static final String AUTHENTICATION = "authentication";

  /**
   * Get the authentication information. If there is no authentication information
   * {@link AuthenticationContext#notLoggedIn()} is used.
   * 
   * @param session used to get the variable
   * @return the authentication information
   */
  public static AuthenticationContext getAuthentication(final HttpSession session) {
    final AuthenticationContext auth = getAttribute(session, AUTHENTICATION, AuthenticationContext.class);
    if (null == auth) {
      final AuthenticationContext newAuth = AuthenticationContext.notLoggedIn();
      session.setAttribute(AUTHENTICATION, newAuth);
      return newAuth;
    } else {
      return auth;
    }
  }

  /**
   * The key in the session to get form parameters stored by login and should be
   * passed to the next page.
   */
  public static final String STORED_PARAMETERS = "stored_parameters";

}
