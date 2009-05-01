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

  private static final String _version;
  static {
    final InputStream stream = Version.class.getResourceAsStream("/fll/resources/version.properties");
    if (null == stream) {
      LOG.error("Unable to find version.properties!");
      _version = "NO-PROPERTIES-FILE";
    } else {
      final Properties versionProps = new Properties();
      String version;
      try {
        versionProps.load(stream);
        stream.close();
        version = versionProps.getProperty("version", "NO-PROPERTY");
      } catch (final IOException ioe) {
        LOG.error("Error loading version properties", ioe);
        version = "EXCEPTION";
      }

      _version = version;
    }
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
    if (_version.contains("APP-VERSION")) {
      return "devel";
    } else {
      return _version;
    }
  }
}
