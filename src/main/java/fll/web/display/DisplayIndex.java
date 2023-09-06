/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.web.DisplayInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;

/**
 * Helper for display/index.jsp.
 */
public final class DisplayIndex {

  private DisplayIndex() {
  }

  public static void populateContext(final HttpServletRequest request,
                                     final PageContext page) {
    page.setAttribute("REGISTER_DISPLAY_MESSAGE_TYPE", Message.MessageType.REGISTER_DISPLAY.toString());
    page.setAttribute("PING_MESSAGE_TYPE", Message.MessageType.PING.toString());
    page.setAttribute("ASSIGN_UUID_MESSAGE_TYPE", Message.MessageType.ASSIGN_UUID.toString());
    page.setAttribute("DISPLAY_URL_MESSAGE_TYPE", Message.MessageType.DISPLAY_URL.toString());
    page.setAttribute("DISPLAY_UUID_PARAMETER_NAME", DisplayHandler.DISPLAY_UUID_PARAMETER_NAME);

    final String name;
    final @Nullable String uuidParam = request.getParameter("display_uuid");
    if (!StringUtils.isBlank(uuidParam)) {
      final DisplayInfo info = DisplayHandler.getDisplay(uuidParam);
      name = info.getName();
    } else {
      name = "";
    }
    page.setAttribute("displayName", name);
  }
}
