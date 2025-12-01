/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Get version information about the FLL software.
 *
 * @author jpschewe
 */
public final class Version {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  private static final Map<String, String> VERSION_INFORMATION;

  private static final String VERSION;
  static {
    final InputStream stream = Version.class.getResourceAsStream("/fll/resources/git.properties");
    if (null == stream) {
      LOG.error("Unable to find version.properties!");
      VERSION = "NO-PROPERTIES-FILE";
      VERSION_INFORMATION = Collections.emptyMap();
    } else {
      final Properties versionProps = new Properties();
      String version;
      try {
        versionProps.load(stream);
        version = versionProps.getProperty("git.build.version", "NO-PROPERTY");
      } catch (final IOException ioe) {
        LOG.error("Error loading version properties", ioe);
        version = "EXCEPTION";
      } finally {
        try {
          stream.close();
        } catch (final IOException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Error closing stream", e);
          }
        }
      }

      // convert to String, String
      final Map<String, String> map = new HashMap<>();
      for (final String name : versionProps.stringPropertyNames()) {
        final String value = versionProps.getProperty(name);
        if (null != value) {
          map.put(name, value);
        }
      }

      VERSION_INFORMATION = Collections.unmodifiableMap(map);
      VERSION = version;
    }
  }

  private Version() {
    // no instances
  }

  /**
   * @param args ignored
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

  /**
   * @return all version information stored in version.properties, unmodifiable
   *         map
   */
  public static Map<String, String> getAllVersionInformation() {
    return VERSION_INFORMATION;
  }

}
