/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import jakarta.servlet.jsp.PageContext;

import fll.Version;

/**
 * Data needed for developer/index.jsp.
 */
public final class DeveloperIndex {

  private DeveloperIndex() {
  }

  /**
   * @param pageContext used to set variables
   */
  public static void populateContext(final PageContext pageContext) {
    pageContext.setAttribute("versionInfo", Version.getAllVersionInformation());
  }

}
