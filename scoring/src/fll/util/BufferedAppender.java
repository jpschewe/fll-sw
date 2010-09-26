/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Appender that buffers up all logging events until log4j is configured, at
 * which point the logging events are passed onto the configured log4j.
 * 
 * @see Logutils#isLog4jConfigured()
 */
public class BufferedAppender extends AppenderSkeleton {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final List<LoggingEvent> events = new LinkedList<LoggingEvent>();

  private boolean log4jConfigured = false;

  public static final BufferedAppender INSTANCE = new BufferedAppender();

  private BufferedAppender() {
    // singleton
    Runtime.getRuntime().addShutdownHook(new Thread("Flush Buffered Appender") {
      public void run() {
        shutdown();
      }
    });
  }

  /**
   * Store the event for later retrieval.
   */
  @Override
  protected void append(final LoggingEvent event) {
    synchronized (this) {
      if (log4jConfigured) {
        // shouldn't get here, pass through if we do though
        outputEvent(event);
      } else if (LogUtils.isLog4jConfigured()) {
        // log4j was configured, flush
        flushEvents();
        log4jConfigured = true;
      } else {
        // buffer events until configured
        events.add(event);
      }
    }
  }

  /**
   * Remove self from list of appenders on root.
   */
  public void removeSelf() {
    Logger.getRootLogger().removeAppender(this);
  }

  /**
   * Output all buffered events.
   */
  public void flushEvents() {
    synchronized (this) {
      if (!events.isEmpty()) {
        LOGGER.info("Starting to flush buffered events");
        for (final LoggingEvent event : events) {
          outputEvent(event);
        }
        events.clear();
        LOGGER.info("Finished flushing buffered events");
      }
    }
  }

  private void outputEvent(final LoggingEvent event) {
    removeSelf();
    event.getLogger().callAppenders(event);
  }

  private void shutdown() {
    synchronized (this) {
      if (!closed) {
        if (!events.isEmpty()) {
          if (!LogUtils.isLog4jConfigured()) {
            // add default appender to stdout
            LOGGER.warn("Closing down appender and log4j is not configured! Flushing to System.err");
            Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_ERR));
          }
          flushEvents();
        }
        closed = true;
      }
    }
  }

  public void close() {
    this.closed = true;
  }

  public boolean requiresLayout() {
    return false;
  }

}
