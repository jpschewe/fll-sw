/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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

  static {
    final String classname = SessionAttributes.class.getName();
    final int idx = classname.lastIndexOf('.');
    ID = classname.substring(idx + 1);
  }

  /**
   * The basename of the class.
   */
  public static final String ID;

  /** "Cache" holding all public static fields by it's field name */
  private static Map<String, Object> nameToValueMap = createNameToValueMap();

  /**
   * Puts all public static fields via introspection into the resulting Map.
   * Uses the name of the field as key to reference it's in the Map.
   * 
   * @return a Map of field names to field values of all public static fields of
   *         this class
   */
  private static Map<String, Object> createNameToValueMap() {
    final Map<String, Object> result = new HashMap<String, Object>();
    final Field[] publicFields = SessionAttributes.class.getFields();
    for (int i = 0; i < publicFields.length; i++) {
      final Field field = publicFields[i];
      final String name = field.getName();
      try {
        result.put(name, field.get(null));
      } catch (final IllegalArgumentException e) {
        throw new RuntimeException("Error initializing constants cache", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Error initializing constants cache", e);
      }
    }
    return result;
  }

  /**
   * Gets the Map of all public static fields. The field name is used as key for
   * the value of the field itself.
   * 
   * @return the Map of all public static fields
   */
  public static Map<String, Object> getNameToValueMap() {
    return nameToValueMap;
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
