/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Objects;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
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

    configureTomcat(tomcat);

    tomcat.setPort(Launcher.WEB_PORT);

    // trigger the creation of the default connector
    tomcat.getConnector();

  }

  /**
   * @return the directory that is the root of the classes directory
   */
  public static Path getClassesPath() {
    final ProtectionDomain classDomain = TomcatLauncher.class.getProtectionDomain();
    final CodeSource codeSource = classDomain.getCodeSource();
    final URL codeLocation = codeSource.getLocation();
    try {
      final File codeLocationFile = new File(codeLocation.toURI());
      final Path path = codeLocationFile.toPath();

      final Path classesPath;
      if (!Files.isDirectory(path)) {
        classesPath = path.getParent();
        LOGGER.debug("Path to class files is a file "
            + path
            + " using parent directory as the classes location");
      } else {
        classesPath = path;
      }

      return classesPath;
    } catch (final URISyntaxException e) {
      throw new RuntimeException("Error converting code location to a path", e);
    }
  }

  private void configureTomcat(final Tomcat tomcat) {
    final Path classesPath = getClassesPath();

    LOGGER.debug("Found classes path: "
        + classesPath.toAbsolutePath().toString());

    final Path webRoot = findWebappRoot(classesPath);
    Objects.requireNonNull(webRoot, "Could not find web root");
    LOGGER.info("Using web root: "
        + webRoot.toAbsolutePath().toString());

    // specify the temporary directory to use
    final Path tmpDir = webRoot.resolve("../webserver-temp");
    LOGGER.info("Using tomcat tmp dir: "
        + tmpDir.toAbsolutePath().toString());
    tomcat.setBaseDir(tmpDir.toAbsolutePath().toString());

    final StandardContext ctx = (StandardContext) tomcat.addWebapp("/", webRoot.toAbsolutePath().toString());

    // delegate to parent classloader first. This allows the web application
    // to use the application classpath.
    ctx.setDelegate(true);

    final WebResourceRoot resources = new StandardRoot(ctx);

    resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", classesPath.toAbsolutePath().toString(),
                                                 "/"));

    ctx.setResources(resources);
  }

  /**
   * @param classesPath where the root of the classes are
   * @return where the root of the web files are, null if it cannot be found
   */
  public static Path findWebappRoot(final Path classesPath) {
    // where to look relative to classesPath
    final String[] possibleWebLocations = {

                                            // shell script ->
                                            // fll-sw/lib/fll-sw.jar
                                            // root: (dir)/../web
                                            // classes: <empty>
                                            "../web", //

                                            // gradle ->
                                            // working-dir/build/classes/java/main/
                                            // root: ../../../web
                                            // classes: ../../
                                            // classpath
                                            "../../../web",

                                            // eclipse -> working-dir/bin/classes
                                            // root: ../web
                                            // classes: .
                                            "../web", };

    for (final String location : possibleWebLocations) {
      final Path web = classesPath.resolve(location);
      final Path check = web.resolve("fll.css");
      if (Files.exists(check)) {
        return web;
      } else {
        LOGGER.debug("No web root at '"
            + check.toAbsolutePath()
            + "'");
      }
    }
    return null;
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
