/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;

import fll.Utilities;

/**
 * Represents a subjective station.
 */
public final class SubjectiveStation implements Serializable {
  public SubjectiveStation(final String name,
                           final long durationInMinutes) {
    this.name = name;
    this.durationMinutes = durationInMinutes;
  }

  private final String name;

  /**
   * Name of what is being judged.
   */
  public String getName() {
    return name;
  }

  private final long durationMinutes;

  /**
   * Duration of the judging session.
   */
  public long getDurationInMillis() {
    return Utilities.convertMinutesToMilliseconds(durationMinutes);
  }

  /**
   * Duration of the judging session.
   */
  public long getDurationMinutes() {
    return durationMinutes;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (o == null) {
      return false;
    } else if (o instanceof SubjectiveStation) {
      final SubjectiveStation other = (SubjectiveStation) o;
      return other.getName().equals(getName());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }
}