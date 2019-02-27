/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;

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

    tomcat.setPort(Launcher.WEB_PORT);

    // trigger the creation of the default connector
    tomcat.getConnector();

    // // TODO: call tomcat.setBasedir() to specify the temporary directory to use
    // final Path webRoot = findWebappRoot();
    //
    // final StandardContext ctx = (StandardContext) tomcat.addWebapp("/",
    // webRoot.toString());
    //
    // // TODO add this as another run location
    // final WebResourceRoot resources = new StandardRoot(ctx);
    // if (Boolean.getBoolean("inside.test")) {
    // // Use instrumented classes when under test
    //
    // final File additionWebInfClasses = new File("coverage/classes");
    // resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
    // additionWebInfClasses.getAbsolutePath(), "/"));
    // }
    // ctx.setResources(resources);

    if (DistRunLocation.INSTANCE.isRunning()) {
      DistRunLocation.INSTANCE.configureTomcat(tomcat);
    } else {
      LOGGER.error("Cannot determine a known run location");
    }

  }

  // private Path findClasses() {
  //
  // }
  //
  // private Path findInstrumentedClasses() {
  //
  // }
  //
  // private Path findLibs() {
  //
  // }

  private Path findWebappRoot() {
    final ProtectionDomain classDomain = TomcatLauncher.class.getProtectionDomain();
    final CodeSource codeSource = classDomain.getCodeSource();
    final URL codeLocation = codeSource.getLocation();
    final Path path = Paths.get(codeLocation.getPath());
    LOGGER.info("Found base path '"
        + path
        + "'");

    final Path classesPath;
    if (!Files.isDirectory(path)) {
      classesPath = path.getParent();
      LOGGER.debug("Path to class files is a file "
          + path
          + " using parent directory as the classes location");
    } else {
      classesPath = path;
    }

    LOGGER.debug("Found classes path "
        + classesPath);

    // FIXME decide where to put files in gradle
    // current thought is to use a war file in gradle and the distribution
    // - this means that people can't easily edit the jsp files
    // - they can't reference the directories to add images

    // look for fll.css in the potential directories

    // where to look relative to classesPath
    final String[] possibleWebLocations = {
                                            // FIXME make a data class that has these values in it
                                            // it should have a check method to evaluate if this instance applies
                                            // accessors for all of the paths that are needed, or perhaps a method that
                                            // takes in the StandardContext... and modifies it.

                                            // shell script ->
                                            // /home/jpschewe/projects/fll-sw/working-dir/build/distributions/foo/fll-sw/lib/fll-sw.jar
                                            // root: (dir)/../web
                                            // classes: <empty>
                                            // lib: (dir)/../lib
                                            "../web", //
                                            // gradle run ->
                                            // /home/jpschewe/projects/fll-sw/working-dir/build/classes/java/main/
                                            // root: ../../../web
                                            // classes: ../../
                                            // lib: FIXME see if there is a way for Tomcat to use the application
                                            // classpath
                                            "../../../web",

                                            // eclipse -> /home/jpschewe/projects/fll-sw/working-dir/bin/classes
                                            // root: ../web
                                            // classes: .
                                            // lib: ../build/???
                                            "../web",

                                            // ant, fll-sw.sh ->
                                            // /home/jpschewe/projects/fll-sw/working-dir/build.ant/webapps/fll-sw/WEB-INF/classes/
                                            // root: ../..
                                            // classes: .
                                            // lib: ../lib
                                            "../.." };

    // FIXME
    return classesPath.resolve("../../../web");
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

  /**
   * Base class for determining the location of various files used for the web
   * server.
   */
  private static abstract class RunLocation {

    private final String name;

    /**
     * @return the name used for debugging which run location is in use
     */
    public String getName() {
      return name;
    }

    public RunLocation(final String name) {
      this.name = name;
    }

    /**
     * @param tomcat the webserve to configure
     */
    public abstract void configureTomcat(final Tomcat tomcat);

    /**
     * @return if this run location is being used
     */
    public abstract boolean isRunning();

    /**
     * @param classesPath
     */
    public abstract void configureWebApp(final Path classesPath);
  }

  private static final class DistRunLocation extends RunLocation {
    public static final DistRunLocation INSTANCE = new DistRunLocation();

    public DistRunLocation() {
      super("Dist");
    }

    @Override
    public void configureTomcat(final Tomcat tomcat) {
      final ProtectionDomain classDomain = TomcatLauncher.class.getProtectionDomain();
      final CodeSource codeSource = classDomain.getCodeSource();
      final URL codeLocation = codeSource.getLocation();
      final Path path = Paths.get(codeLocation.getPath());

      final Path classesPath;
      if (!Files.isDirectory(path)) {
        classesPath = path.getParent();
        LOGGER.debug("Path to class files is a file "
            + path
            + " using parent directory as the classes location");
      } else {
        classesPath = path;
      }

      // TODO: call tomcat.setBasedir() to specify the temporary directory to use
      final Path webRoot = classesPath.resolve("../web");

      final StandardContext ctx = (StandardContext) tomcat.addWebapp("/", webRoot.toAbsolutePath().toString());

      final WebResourceRoot resources = new StandardRoot(ctx);
      final Path libDir = webRoot.resolve("../lib");
      
      LOGGER.info("Using lib dir: " + libDir.toAbsolutePath());

      resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/lib", libDir.toAbsolutePath().toString(), "/"));

      ctx.setResources(resources);
    }

    @Override
    public boolean isRunning() {
      final ProtectionDomain classDomain = TomcatLauncher.class.getProtectionDomain();
      final CodeSource codeSource = classDomain.getCodeSource();
      final URL codeLocation = codeSource.getLocation();
      final Path path = Paths.get(codeLocation.getPath());

      final Path classesPath;
      if (!Files.isDirectory(path)) {
        // assume that if the class was loaded from a jar file, then this is the
        // distribution
        return true;
      }

      return false;
    }

    /**
     * @see fll.TomcatLauncher.RunLocation#configureWebApp(java.nio.file.Path)
     */
    @Override
    public void configureWebApp(Path classesPath) {
      // TODO Auto-generated method stub

    }

  }
}
