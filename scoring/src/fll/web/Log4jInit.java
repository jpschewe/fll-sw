/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * @author jpschewe
 * @version $Revision$
 */
public class Log4jInit extends HttpServlet {

  @Override
  public void init() {
    final String prefix = getServletContext().getRealPath("/");
    final String file = getInitParameter("log4j-init-file");

    // if the log4j-init-file is not set, then no point in trying
    if (file != null) {

      // set some properties that are used in the log4j config file
      System.setProperty("app.name", "web");
      System.setProperty("logroot", prefix
          + "/"); // need the trailing slash because of how the log4j.xml file
                  // is built

      DOMConfigurator.configure(prefix
          + file);

      Logger.getRootLogger().info("Logging initialized");
    }
  }

}
