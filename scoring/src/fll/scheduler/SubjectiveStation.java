/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;

/**
 * Represents a subjective station. 
 */
public final class SubjectiveStation implements Serializable {
  public SubjectiveStation(final String name,
                        final long durationInMillis) {
    this.name = name;
    this.duration = durationInMillis;
  }

  private final String name;

  /**
   * Name of what is being judged.
   */
  public String getName() {
    return name;
  }

  private final long duration;

  /**
   * Duration of the judging session.
   */
  public long getDuration() {
    return duration;
  }
}