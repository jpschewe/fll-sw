/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.fest.swing.image.ScreenshotTaker;

import fll.util.LogUtils;

/**
 * Some utilities for writing tests.
 */
public final class TestUtils {

  private static final Logger LOG = LogUtils.getLogger();

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/fll-sw/";

  private TestUtils() {
    // no instances
  }

  /**
   * Delete all files that would be associated with the specified database.
   */
  public static void deleteDatabase(final String database) {
    for (final String extension : Utilities.HSQL_DB_EXTENSIONS) {
      final String filename = database
          + extension;
      final File file = new File(filename);
      if (file.exists()) {
        if (!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
  }

  private static final ScreenshotTaker SCREENSHOT_TAKER = new ScreenshotTaker();

  public static void saveScreenshot() throws IOException {
    final File screenshot = File.createTempFile("fll", ".png", new File("screenshots"));
    LOG.error("Screenshot saved to "
        + screenshot.getAbsolutePath());
    // file can't exist when calling save desktop as png
    screenshot.delete();
    SCREENSHOT_TAKER.saveDesktopAsPng(screenshot.getAbsolutePath());
  }
}
