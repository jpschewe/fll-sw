/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

/**
 * Thrown when there is an error parsing the schedule.
 */
public class ScheduleParseException extends Exception {

  /**
   * Base constructor.
   */
  public ScheduleParseException() {
    super();
  }

  /**
   * @param message {@link #getMessage()}
   */
  public ScheduleParseException(final String message) {
    super(message);
  }

  /**
   * @param cause {@link #getCause()}
   */
  public ScheduleParseException(final Throwable cause) {
    super(cause);
  }

  /**
   * @param message {@link #getMessage()}
   * @param cause {@link #getCause()}
   */
  public ScheduleParseException(final String message,
                                final Throwable cause) {
    super(message, cause);
  }

}
