/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/**
 * Keep track of the named displays.
 */
public final class DisplayNames {

  private DisplayNames() {
    // utility class
  }

  /**
   * Create a string that's a valid HTML name.
   */
  private static String sanitizeDisplayName(final String str) {
    if (null == str
        || "".equals(str)) {
      return null;
    } else {
      String ret = str;
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9_-]");

  /**
   * Add a display name to the list of known displays.
   * This sets the current time as the last seen time. So this method can also
   * be used to update the last seen time.
   * 
   * @param application used to track the list of all display names
   * @param session used to store the display name for the page to see. Variable
   *          is "displayName".
   *          This name will be a legal HTML identifier.
   * @param name the name to set for the display, may be different from what is
   *          stored
   */
  public static void appendDisplayName(final ServletContext application,
                                       final HttpSession session,
                                       final String name) {
    session.removeAttribute("displayName");

    final String sanitized = sanitizeDisplayName(name);
    if (null == sanitized) {
      // nothing to store
      return;
    }
    session.setAttribute("displayName", sanitized);

    synchronized (DisplayNames.class) {
      // ServletContext isn't type safe
      @SuppressWarnings("unchecked")
      Map<String, Date> displayNames = ApplicationAttributes.getAttribute(application,
                                                                          ApplicationAttributes.DISPLAY_NAMES,
                                                                          Map.class);
      if (null == displayNames) {
        displayNames = new HashMap<>();
      }
      displayNames.put(sanitized, new Date());
      application.setAttribute(ApplicationAttributes.DISPLAY_NAMES, displayNames);
    }
  }

  /**
   * Get all display names that we know about.
   */
  public static Set<String> getDisplayNames(final ServletContext application) {

    synchronized (DisplayNames.class) {
      // ServletContext isn't type safe
      @SuppressWarnings("unchecked")
      final Map<String, Date> displayNames = ApplicationAttributes.getAttribute(application,
                                                                                ApplicationAttributes.DISPLAY_NAMES,
                                                                                Map.class);
      if (null == displayNames) {
        return Collections.emptySet();
      } else {
        return displayNames.keySet();
      }
    }

  }

  /**
   * Delete a named display. It will reappear if the display is still active.
   * Also cleans up the related application attributes.
   */
  public static void deleteNamedDisplay(final ServletContext application,
                                        final String name) {
    // ServletContext isn't type safe
    @SuppressWarnings("unchecked")
    final Map<String, Date> displayNames = ApplicationAttributes.getAttribute(application,
                                                                              ApplicationAttributes.DISPLAY_NAMES,
                                                                              Map.class);
    if (null != displayNames) {
      displayNames.remove(name);
      application.setAttribute(ApplicationAttributes.DISPLAY_NAMES, displayNames);

      application.removeAttribute(name
          + "_displayPage");
      application.removeAttribute(name
          + "_displayURL");
      application.removeAttribute(name
          + "_playoffRoundNumber");
      application.removeAttribute(name
          + "_playoffDivision");
      application.removeAttribute(name
          + "_finalistDivision");

    }
  }

}
