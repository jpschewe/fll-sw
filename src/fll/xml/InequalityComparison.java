/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

public enum InequalityComparison {
  LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, EQUAL_TO, NOT_EQUAL_TO;

  public static final String LESS_THAN_TAG_NAME = "less-than";

  public static final String LESS_THAN_OR_EQUAL_TAG_NAME = "less-than-or-equal";

  public static final String GREATER_THAN_TAG_NAME = "greater-than";

  public static final String GREATER_THAN_OR_EQUAL_TAG_NAME = "greater-than-or-equal";

  public static final String EQUAL_TO_TAG_NAME = "equal-to";

  public static final String NOT_EQUAL_TO_TAG_NAME = "not-equal-to";
}
