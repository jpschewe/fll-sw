/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Get version information about the FLL software.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public final class Version {

  private static final Logger LOG = Logger.getLogger(Version.class);

  private static final String VERSION;
  static {
    final InputStream stream = Version.class.getResourceAsStream("/fll/resources/version.properties");
    if (null == stream) {
      LOG.error("Unable to find version.properties!");
      VERSION = "NO-PROPERTIES-FILE";
    } else {
      final Properties versionProps = new Properties();
      String version;
      try {
        versionProps.load(stream);
        version = versionProps.getProperty("version", "NO-PROPERTY");
      } catch (final IOException ioe) {
        LOG.error("Error loading version properties", ioe);
        version = "EXCEPTION";
      } finally {
        try {
          stream.close();
        } catch(final IOException e) {
          if(LOG.isDebugEnabled()) {
            LOG.debug("Error closing stream", e);
          }
        }
      }

      VERSION = version;
    }
  }

  private Version() {
    // no instances
  }
  
  /**
   * @param args
   */
  public static void main(final String[] args) {
    LOG.info("Version is: "
        + getVersion());
  }

  /**
   * Get the version of the software being used.
   * 
   * @return the version
   */
  public static String getVersion() {
    if (VERSION.contains("APP-VERSION")) {
      return "devel";
    } else {
      return VERSION;
    }
  }
}
