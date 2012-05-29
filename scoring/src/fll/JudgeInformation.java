/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.Serializable;

/**
 * Judge information.
 */
public final class JudgeInformation implements Serializable {
  private final String id;

  public String getId() {
    return id;
  }

  private final String category;

  public String getCategory() {
    return category;
  }

  private final String station;

  public String getStation() {
    return station;
  }

  public JudgeInformation(final String id,
                          final String category,
                          final String station) {
    this.id = id;
    this.category = category;
    this.station = station;
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
          && getCategory().equals(other.getCategory()) && getStation().equals(other.getStation());
    } else {
      return false;
    }
  }
}