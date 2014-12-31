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

import javax.servlet.ServletContext;

/**
 * Keep track of the named displays.
 */
public final class DisplayNames {

  private DisplayNames() {
    // utility class
  }

  /**
   * Add a display name to the list of known displays.
   * This sets the current time as the last seen time. So this method can also
   * be used to update the last seen time.
   */
  public static void appendDisplayName(final ServletContext application,
                                       final String name) {

    synchronized (DisplayNames.class) {
      // ServletContext isn't type safe
      @SuppressWarnings("unchecked")
      Map<String, Date> displayNames = ApplicationAttributes.getAttribute(application,
                                                                          ApplicationAttributes.DISPLAY_NAMES,
                                                                          Map.class);
      if (null == displayNames) {
        displayNames = new HashMap<>();
      }
      displayNames.put(name, new Date());
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
   * Delete a named display. It will reppear if the display is still active.
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
    }
  }

}
