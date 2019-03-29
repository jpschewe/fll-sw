/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import org.apache.juli.logging.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Send Tomcat log data to log4j.
 */
public class TomcatLog4jLogging implements Log {

  private final Logger logger;

  // this constructor is important, otherwise the ServiceLoader cannot start
  public TomcatLog4jLogging() {
    logger = LogManager.getLogger(TomcatLog4jLogging.class);
  }

  // this constructor is needed by the LogFactory implementation
  public TomcatLog4jLogging(final String name) {
    logger = LogManager.getLogger(name);
  }

  // now we have to implement the `Log` interface
  @Override
  public boolean isFatalEnabled() {
    return logger.isFatalEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void fatal(final Object msg) {
    logger.fatal(msg);
  }

  @Override
  public void fatal(final Object msg,
                    final Throwable throwable) {
    logger.fatal(msg, throwable);
  }

  @Override
  public void debug(final Object msg) {
    logger.debug(msg);
  }

  @Override
  public void debug(final Object msg,
                    final Throwable throwable) {
    logger.debug(msg, throwable);
  }

  @Override
  public void error(final Object msg) {
    logger.error(msg);
  }

  @Override
  public void error(final Object msg,
                    final Throwable throwable) {
    logger.error(msg, throwable);
  }

  @Override
  public void info(final Object msg) {
    logger.info(msg);
  }

  @Override
  public void info(final Object msg,
                   final Throwable throwable) {
    logger.info(msg, throwable);
  }

  @Override
  public void trace(final Object msg) {
    logger.trace(msg);
  }

  @Override
  public void trace(final Object msg,
                    final Throwable throwable) {
    logger.trace(msg, throwable);
  }

  @Override
  public void warn(final Object msg) {
    logger.warn(msg);
  }

  @Override
  public void warn(final Object msg,
                   final Throwable throwable) {
    logger.warn(msg, throwable);
  }
}
