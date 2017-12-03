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
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import fll.util.LogUtils;

/**
 * Some utilities for writing tests.
 */
public final class TestUtils {

  private static final Logger LOG = LogUtils.getLogger();

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/TEST-fll-sw/";

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
    final File screenshotDir = new File("screenshots");
    if (!screenshotDir.exists()) {
      screenshotDir.mkdirs();
    }

    final File screenshot = File.createTempFile("fll", ".png", screenshotDir);
    LOG.error("Screenshot saved to "
        + screenshot.getAbsolutePath());
    // file can't exist when calling save desktop as png
    screenshot.delete();
    SCREENSHOT_TAKER.saveDesktopAsPng(screenshot.getAbsolutePath());
  }

  /**
   * JUnit test rule that reruns filaed tests some number of times.
   */
  public static class Retry implements TestRule {
    private final int retryCount;

    public Retry(final int retryCount) {
      this.retryCount = retryCount;
    }

    @Override
    public Statement apply(final Statement base,
                           final Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          Throwable caughtThrowable = null;

          for (int i = 0; i < retryCount; ++i) {
            try {
              base.evaluate();
              return;
            } catch (final Throwable t) {
              caughtThrowable = t;
              LOG.error(description.getDisplayName()
                  + ": run "
                  + (i
                      + 1)
                  + " failed");
            }
          }
          LOG.error(description.getDisplayName()
              + ": giving up after "
              + retryCount
              + "failures");
          throw caughtThrowable;
        }
      };
    }
  } // class Retry

}
