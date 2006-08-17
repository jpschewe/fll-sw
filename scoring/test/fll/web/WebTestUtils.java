/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import org.apache.log4j.Logger;

/**
 * Utilities
 *
 * @version $Revision$
 */
public class WebTestUtils {
  
  private static final Logger LOG = Logger.getLogger(WebTestUtils.class);

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/fll-sw/";
  
  private WebTestUtils() {
    // no instances
  }
}
