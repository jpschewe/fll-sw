/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Launcher for embedded tomcat.
 */
public class TomcatLauncher {

  private final Tomcat tomcat;

  /**
   * Create Tomcat launcher.
   */
  public TomcatLauncher() {
    final String webappDirLocation = "tomcat/webapps/fll-sw/";

    tomcat = new Tomcat();

    tomcat.setPort(Launcher.WEB_PORT);
    tomcat.getConnector(); // trigger the creation of the default connector

    // TODO: call tomcat.setBasedir() to specify the temporary directory to use

    final StandardContext ctx = (StandardContext) tomcat.addWebapp("/fll-sw",
                                                                   new File(webappDirLocation).getAbsolutePath());

    final WebResourceRoot resources = new StandardRoot(ctx);
    if (Boolean.getBoolean("inside.test")) {
      // when running inside testing we want to use the instrumented files

      final File additionWebInfClasses = new File("tomcat/webapps/TEST-fll-sw/WEB-INF/classes");
      resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                                                   additionWebInfClasses.getAbsolutePath(), "/"));
    }
    ctx.setResources(resources);

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
   * Stop the server.
   * 
   * @throws LifecycleException if an error occurs
   */
  public void stop() throws LifecycleException {
    tomcat.stop();
  }
}
