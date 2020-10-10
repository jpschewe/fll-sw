/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.lang.Thread.UncaughtExceptionHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Log exeption information and then exit.
 */
public class ConsoleExceptionHandler implements UncaughtExceptionHandler {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Constructor.
   */
  public ConsoleExceptionHandler() {
  }

  /**
   * Needed for <code>sun.awt.exception.handler</code>.
   * 
   * @param throwable the error
   */
  public void handle(final Throwable throwable) {
    uncaughtException(Thread.currentThread(), throwable);
  }

  @Override
  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "We want the application to exit after an error")
  public void uncaughtException(final Thread t,
                                final Throwable ex) {
    final String message = String.format("An unexpected error occurred. Please send the log, this message and a description of what you were doing to the developer. Thread %s: %s",
                                         t.getName(), ex.getMessage());
    LOGGER.fatal(message, ex);
    System.exit(1);
  }

  /**
   * Register this class to handle all uncaught exceptions.
   */
  public static void registerExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new ConsoleExceptionHandler());
    System.setProperty("sun.awt.exception.handler", ConsoleExceptionHandler.class.getName());
  }

}
