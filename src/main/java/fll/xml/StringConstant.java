/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.web.playoff.TeamScore;

/**
 * A mutable string that implements {@link StringValue}.
 */
public class StringConstant implements StringValue {

  public StringConstant(final String value) {
    this.value = value;
  }

  private String value;

  public void setValue(final String v) {
    value = v;
  }

  /**
   * @return see {@link #getValue()}
   */
  public String getValue() {
    return value;
  }

  @Override
  public String getStringValue(final TeamScore ignored) {
    return getValue();
  }

  @Override
  public String getRawStringValue() {
    return getValue();
  }

  @Override
  public boolean isStringConstant() {
    return true;
  }

  @Override
  public boolean isGoalRef() {
    return false;
  }

}
