/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.time.Duration;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is general information for a particular judging station.
 * A {@link SubjectiveTime} the use of a {@link SubjectiveStation} at a
 * particular point in time.
 */
public final class SubjectiveStation implements Serializable {

  /**
   * @param name {@link #getName()}
   * @param durationInMinutes {@link #getDurationMinutes()}
   */
  public SubjectiveStation(final String name,
                           final int durationInMinutes) {
    this.name = name;
    this.durationMinutes = durationInMinutes;
  }

  private final String name;

  /**
   * @return Name of what is being judged.
   */
  public String getName() {
    return name;
  }

  private final int durationMinutes;

  /**
   * @return Duration of the judging session.
   */
  public long getDurationInMillis() {
    return Duration.ofMinutes(durationMinutes).toMillis();
  }

  /**
   * @return Duration of the judging session.
   */
  public int getDurationMinutes() {
    return durationMinutes;
  }

  /**
   * @return {@link #getDurationMinutes()} as a {@link Duration} object
   */
  public Duration getDuration() {
    return Duration.ofMinutes(durationMinutes);
  }

  @Override
  @EnsuresNonNullIf(expression="#1", result=true)
  public boolean equals(final @Nullable Object o) {
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