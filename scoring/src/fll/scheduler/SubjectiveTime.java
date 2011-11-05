/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a subjective judging time.
 */
public final class SubjectiveTime implements Serializable {
  public SubjectiveTime(final String name,
                        final Date time) {
    this.name = name;
    this.time = time == null ? null : new Date(time.getTime());
  }

  private final String name;

  /**
   * Name of what is being judged.
   */
  public String getName() {
    return name;
  }

  private final Date time;

  /**
   * Time of the judging session.
   */
  public Date getTime() {
    return null == time ? null : new Date(time.getTime());
  }
}