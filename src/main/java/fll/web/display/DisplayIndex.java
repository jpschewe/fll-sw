/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;

/**
 * Helper for display/index.jsp.
 */
public final class DisplayIndex {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private DisplayIndex() {
  }

  public static void populateContext(final HttpServletRequest request,
                                     final PageContext page) {
    page.setAttribute("REGISTER_DISPLAY_MESSAGE_TYPE", Message.MessageType.REGISTER_DISPLAY.toString());
    page.setAttribute("PING_MESSAGE_TYPE", Message.MessageType.PING.toString());
    page.setAttribute("ASSIGN_UUID_MESSAGE_TYPE", Message.MessageType.ASSIGN_UUID.toString());
    page.setAttribute("DISPLAY_URL_MESSAGE_TYPE", Message.MessageType.DISPLAY_URL.toString());
    page.setAttribute("DISPLAY_UUID_PARAMETER_NAME", DisplayHandler.DISPLAY_UUID_PARAMETER_NAME);

    String name;
    final @Nullable String uuidParam = request.getParameter("display_uuid");
    if (!StringUtils.isBlank(uuidParam)) {
      try {
        final DisplayInfo info = DisplayHandler.getDisplay(uuidParam);
        name = info.getName();
      } catch (final UnknownDisplayException e) {
        LOGGER.warn("Display {} is unknown, resetting to new display", uuidParam);
        name = "";
      }
    } else {
      name = "";
    }
    page.setAttribute("displayName", name);
  }
}
