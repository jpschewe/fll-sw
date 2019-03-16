/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import fll.db.ImportDB;
import fll.util.LogUtils;

/**
 * Some utilities for writing tests.
 */
public final class TestUtils {

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/";

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

  /**
   * Delete data created by an import that we don't care about.
   * 
   * @param importResult the result of a database import
   * @throws IOException if there is an error deleting the files
   */
  public static void deleteImportData(final ImportDB.ImportResult importResult) throws IOException {
    if (Files.exists(importResult.getImportDirectory()))
      Files.walk(importResult.getImportDirectory()).sorted(Comparator.reverseOrder()).map(Path::toFile)
           .forEach(File::delete);
  }

  /**
   * Setup logging before a test.
   */
  public static class InitializeLogging implements BeforeTestExecutionCallback {

    @Override
    public void beforeTestExecution(final ExtensionContext context) throws Exception {
      LogUtils.initializeLogging();
    }

  }

}
