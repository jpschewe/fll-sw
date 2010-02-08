/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Utilities;

/**
 * Dump the challenge document out of a database.
 * 
 * @version $Revision$
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
    try {
      connection = Utilities.createDataSource(args[1]).getConnection();
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Value FROM TournamentParameters WHERE Param = 'ChallengeDocument'");
      if (rs.next()) {
        final Reader reader = rs.getCharacterStream(1);
        final FileWriter writer = new FileWriter(challengeFile);
        try {
          // copy to the file
          final CharBuffer buffer = CharBuffer.allocate(32 * 1024);
          while (reader.read(buffer) != -1
              || buffer.position() > 0) {
            buffer.flip();
            writer.append(buffer);
            buffer.clear();
          }
        } finally {
          reader.close();
          writer.close();
        }

      } else {
        throw new RuntimeException("Could not find challenge document in database");
      }

    } catch (final UnsupportedEncodingException uee) {
      LOG.fatal("UTF8 not a supported encoding???", uee);
      System.exit(1);
    } catch (final SQLException sqle) {
      LOG.fatal("Error talking to database", sqle);
      System.exit(1);
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closeConnection(connection);
    }
    System.exit(0);
  }

  private ExportDocument() {
  }

}
