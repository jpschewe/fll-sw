/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import jakarta.servlet.jsp.PageContext;

/**
 * Helper for resolveAwardsScriptDifferences.jsp.
 */
public final class ResolveAwardsScriptDifferences {

  private ResolveAwardsScriptDifferences() {
  }

  /**
   * Setup context variables for the page.
   * 
   * @param page page context
   */
  public static void populateContext(final PageContext page) {
    page.setAttribute("awardsScriptDifferenceActionValues", AwardsScriptDifferenceAction.values());
  }

}
