/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import fll.db.DumpDB;
import jakarta.servlet.jsp.PageContext;

/**
 * Helper for database backup index.
 */
public final class DatabaseBackupsIndex {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private DatabaseBackupsIndex() {
  }

  /**
   * @param page add page variables
   */
  public static void populateContext(final PageContext page) {
    final String baseWeb = String.valueOf(DumpDB.getDatabaseBackupPath().getFileName());
    page.setAttribute("BASE_URL", baseWeb);

    final List<String> backups = new LinkedList<>();
    try (DirectoryStream<Path> directories = Files.newDirectoryStream(DumpDB.getDatabaseBackupPath())) {
      for (final Path p : directories) {
        if (Files.isRegularFile(p)) {
          final String name = String.valueOf(p.getFileName());
          backups.add(name);
        }
      }
    } catch (final IOException e) {
      LOGGER.error("Got error getting list of backups", e);
    }
    Collections.sort(backups);
    page.setAttribute("backups", backups);
  }

}
