/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.util.List;

import fll.xml.DescriptionInfo;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for /challenge-descriptions.jsp.
 */
public final class ChallengeDescriptions {

  private ChallengeDescriptions() {

  }

  /**
   * @param page store variables for the page
   */
  public static void populateContext(final PageContext page) {
    final List<DescriptionInfo> descriptions = DescriptionInfo.getAllKnownChallengeDescriptionInfo();
    page.setAttribute("descriptions", descriptions);
  }

}
