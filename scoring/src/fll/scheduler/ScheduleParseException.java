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

  public ScheduleParseException() {
    super();
  }

  public ScheduleParseException(final String message) {
    super(message);
  }

  public ScheduleParseException(final Throwable cause) {
    super(cause);
  }

  public ScheduleParseException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
