/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

/**
 * Judge information.
 */
public final class JudgeInformation {
  private final String id;

  public String getId() {
    return id;
  }

  private final String category;

  public String getCategory() {
    return category;
  }

  private final String division;

  public String getDivision() {
    return division;
  }

  public JudgeInformation(final String id,
                          final String category,
                          final String division) {
    this.id = id;
    this.category = category;
    this.division = division;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (null == o) {
      return false;
    } else if (o == this) {
      return true;
    } else if (o.getClass().equals(JudgeInformation.class)) {
      final JudgeInformation other = (JudgeInformation) o;
      return getId().equals(other.getId())
          && getCategory().equals(other.getCategory()) && getDivision().equals(other.getDivision());
    } else {
      return false;
    }
  }
}