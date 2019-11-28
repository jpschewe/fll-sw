/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.tomcat;

import java.io.CharArrayWriter;

import org.apache.catalina.valves.AccessLogValve;

/**
 * Access log that writes to log4j.
 */
public class Log4jAccessLog extends AccessLogValve {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  public void log(final CharArrayWriter message) {
    LOGGER.info(message.toString());
  }

  @Override
  protected synchronized void open() {
    // do nothing
  }

  @Override
  public synchronized void backgroundProcess() {
    // do nothing
  }

}
