/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;

/**
 * Dump the challenge document out of a database.
 */
public final class ExportDocument {

  private static final Logger LOG = Logger.getLogger(ExportDocument.class);

  /**
   * Export the challenge document from the specified database.
   * <ul>
   * <li>arg[0] - the location of the database (eg. ./flldb)
   * <li>arg[1] - where to store the xml file
   * </ul>
   */
  public static void main(final String[] args) throws IOException {
    if (args.length < 2) {
      LOG.fatal("Usage: ExportDocument <xml file> <db>");
      System.exit(1);
    }
    final File challengeFile = new File(args[0]);
    if (challengeFile.exists()
        && !challengeFile.canWrite()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " exists and is not writable");
      System.exit(1);
    }
    if (challengeFile.isDirectory()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " is a directory");
      System.exit(1);
    }

    if (!Utilities.testHSQLDB(args[1])) {
      LOG.fatal("There is a problem with the database, see previous log messages");
      System.exit(1);
    }
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    int exitCode = 0;
    try {
      connection = Utilities.createDataSource(args[1]).getConnection();
      final FileWriter writer = new FileWriter(challengeFile);
      
      final Document doc = Queries.getChallengeDocument(connection);
      final XMLWriter xmlwriter = new XMLWriter();
      xmlwriter.setOutput(writer);
      xmlwriter.write(doc);
      writer.close();

    } catch (final UnsupportedEncodingException uee) {
      LOG.fatal("UTF8 not a supported encoding???", uee);
      exitCode = 1;
    } catch (final SQLException sqle) {
      LOG.fatal("Error talking to database", sqle);
      exitCode = 1;
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closeConnection(connection);
    }
    System.exit(exitCode);
  }

  private ExportDocument() {
  }

}
