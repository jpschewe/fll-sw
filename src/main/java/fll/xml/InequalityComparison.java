/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Inequality values.
 */
public enum InequalityComparison {
  LESS_THAN("less-than", "less than"), //
  LESS_THAN_OR_EQUAL("less-than-or-equal", "less than or equal to"), //
  GREATER_THAN("greater-than", "greater than"), //
  GREATER_THAN_OR_EQUAL("greater-than-or-equal", "greater than or equal to"), //
  EQUAL_TO("equal-to", "equal to"), //
  NOT_EQUAL_TO("not-equal-to", "not equal to");

  private final String tagName;

  InequalityComparison(final String tagName,
                       final String display) {
    this.tagName = tagName;
    this.display = display;
  }

  /**
   * @return XML tag for the inequality
   */
  public String getTagName() {
    return tagName;
  }

  private final String display;

  /**
   * @return human readable string
   */
  public String getDisplay() {
    return display;
  }

}
