/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * @author jpschewe
 */
@WebServlet(urlPatterns="/log4j-init", loadOnStartup=1)
public class Log4jInit extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  public void init() {
    final String prefix = getServletContext().getRealPath("/");

    // set some properties that are used in the log4j config file
    System.setProperty("app.name", "web");
    System.setProperty("logroot", prefix
        + "/"); // need the trailing slash because of how the log4j.xml file
    // is built

    LogUtils.initializeLogging();

    LOGGER.info("Logging initialized");
  }

}
