/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.util.List;

import fll.Utilities;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for slideshow.jsp.
 */
public final class Slideshow {

  private Slideshow() {
  }

  /**
   * @param application application variables
   * @param session session variables
   * @param pageContext page variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    // All images are be located under slideshow/ in the fll web folder.
    final String imagePath = application.getRealPath("/"
        + WebUtils.SLIDESHOW_PATH);

    // This variable holds the name of the last image, relative to imagePath
    String lastImage = SessionAttributes.getAttribute(session, "slideShowLastImage", String.class);
    final List<String> files = Utilities.getGraphicFiles(new File(imagePath));
    if (files.size() == 0) {
      lastImage = "";
    } else if (null == lastImage) {
      lastImage = files.get(0);
    } else {
      final int oldFileIdx = files.indexOf(lastImage);
      if (oldFileIdx < 0
          || oldFileIdx >= files.size()
              - 1) {
        lastImage = files.get(0);
      } else {
        lastImage = files.get(oldFileIdx
            + 1);
      }
    }
    session.setAttribute("slideShowLastImage", lastImage);

    final Number slideShowIntervalObj = ApplicationAttributes.getAttribute(application, "slideShowInterval",
                                                                           Number.class);
    int slideShowInterval;
    if (null == slideShowIntervalObj) {
      slideShowInterval = 10000;
    } else {
      slideShowInterval = slideShowIntervalObj.intValue()
          * 1000;
    }

    if (slideShowInterval < 1) {
      slideShowInterval = 1
          * 1000;
    }
    pageContext.setAttribute("slideShowInterval", slideShowInterval);
  }
}
