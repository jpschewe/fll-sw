/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.util.List;

import fll.web.SessionAttributes;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for sponsors.jsp.
 */
public final class Sponsors {

  private Sponsors() {
  }

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext page) {
    final List<String> logoFiles = WebUtils.getSponsorLogos(application);
    
    final Number lastLogoIndexObj = SessionAttributes.getAttribute(session, "lastLogoIndex", Number.class);
    // This varible holds the index of the last image, relative to imagePath
    final int numLogos = logoFiles.size();
    int lastLogoIndex;
    if (numLogos < 1) {
      lastLogoIndex = -1;
    } else if (null != lastLogoIndexObj) {
      lastLogoIndex = lastLogoIndexObj.intValue();
    } else {
      lastLogoIndex = numLogos
          - 1;
    }
    page.setAttribute("numLogos", numLogos);

    if (numLogos > 0) {
      lastLogoIndex = (lastLogoIndex
          + 1)
          % numLogos;

      final String imageFileName = logoFiles.get(lastLogoIndex);
      page.setAttribute("imageFileName", imageFileName);

      session.setAttribute("lastLogoIndex", lastLogoIndex);
    }

  }
}
