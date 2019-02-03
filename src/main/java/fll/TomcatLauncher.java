/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.File;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

//TODO: get logging into log4j
//TODO: launch this from the regular launcher
//TODO: get classpath right
/**
 * 
 */
public class TomcatLauncher {

  public static void main(String[] args) throws Exception {

      final String webappDirLocation = "tomcat/webapps/fll-sw/";
      final Tomcat tomcat = new Tomcat();

      tomcat.setPort(8080);
      tomcat.getConnector(); // trigger the creation of the default connector

      final StandardContext ctx = (StandardContext) tomcat.addWebapp("/fll-sw", new File(webappDirLocation).getAbsolutePath());
      System.out.println("configuring app with basedir: " + new File("./" + webappDirLocation).getAbsolutePath());

      // Declare an alternative location for your "WEB-INF/classes" dir
      // Servlet 3.0 annotation will work
      //TODO: probably don't need this since we have them all internally
      final File additionWebInfClasses = new File("tomcat/webapps/fll-sw/WEB-INF/classes");
      final WebResourceRoot resources = new StandardRoot(ctx);
      resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
              additionWebInfClasses.getAbsolutePath(), "/"));
      ctx.setResources(resources);
      
      tomcat.addWebapp("/", new File("tomcat/webapps/ROOT").getAbsolutePath());     
      

      tomcat.start();
      tomcat.getServer().await();
  }
}
