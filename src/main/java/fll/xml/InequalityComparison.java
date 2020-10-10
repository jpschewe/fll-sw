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
  LESS_THAN("less-than"), //
  LESS_THAN_OR_EQUAL("less-than-or-equal"), //
  GREATER_THAN("greater-than"), //
  GREATER_THAN_OR_EQUAL("greater-than-or-equal"), //
  EQUAL_TO("equal-to"), //
  NOT_EQUAL_TO("not-equal-to");

  private final String tagName;

  InequalityComparison(final String tagName) {
    this.tagName = tagName;
  }

  /**
   * @return XML tag for the inequality
   */
  public String getTagName() {
    return tagName;
  }

}
