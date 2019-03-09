/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import org.openqa.selenium.By;

/**
 * Selenium selectors for finding elements.
 */
public final class ElementBy {
  private ElementBy() {
  }

  public static By partialText(final String text) {
    return By.xpath("//*[contains(normalize-space(.),'"
        + text + "')]");
  }
}
