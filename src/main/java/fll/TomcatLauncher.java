/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;

import fll.util.LogUtils;

/**
 * Launcher for embedded tomcat.
 */
public class TomcatLauncher {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final Tomcat tomcat;

  /**
   * Create Tomcat launcher.
   */
  public TomcatLauncher() {
    tomcat = new Tomcat();

    tomcat.setPort(Launcher.WEB_PORT);
    tomcat.getConnector(); // trigger the creation of the default connector

    // TODO: call tomcat.setBasedir() to specify the temporary directory to use

    tomcat.addWebapp("/fll-sw", new File("tomcat/webapps/fll-sw/").getAbsolutePath());

    if (Boolean.getBoolean("inside.test")) {
      // setup test webapp
      tomcat.addWebapp("/TEST-fll-sw", new File("tomcat/webapps/TEST-fll-sw/").getAbsolutePath());
    }

    // tomcat.addWebapp("/", new File("tomcat/webapps/ROOT").getAbsolutePath());
  }

  /**
   * Start the server.
   * 
   * @throws LifecycleException if an error occurs
   */
  public void start() throws LifecycleException {
    tomcat.start();
  }

  /**
   * Stop the server. This object cannot be used after this method has been
   * called.
   * 
   * @throws LifecycleException if an error occurs
   */
  public void stop() throws LifecycleException {
    LOGGER.info("Stopping tomcat");
    tomcat.stop();
    LOGGER.info("Stopped tomcat");
    tomcat.destroy();
    LOGGER.info("Destroyed tomcat");
  }
}
