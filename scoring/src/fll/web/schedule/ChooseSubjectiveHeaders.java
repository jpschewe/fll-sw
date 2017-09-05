/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import javax.servlet.jsp.PageContext;

import fll.scheduler.SchedParams;

/**
 * Support for chooseSubjectiveHeaders.jsp.
 */
public class ChooseSubjectiveHeaders {

  /**
   * Setup page variables used by the JSP.
   */
  public static void populateContext(final PageContext pageContext) {
    pageContext.setAttribute("default_duration", SchedParams.DEFAULT_SUBJECTIVE_MINUTES);
  }
}
