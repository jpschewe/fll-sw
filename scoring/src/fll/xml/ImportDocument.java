/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.GlobalParameters;

/**
 * Push an XML document into the database without recreating the database. This
 * can be somewhat dangerous and is mostly here for debugging.
 * 
 * @version $Revision$
 */
public final class ImportDocument {

  private static final Logger LOG = Logger.getLogger(ImportDocument.class);

  /**
   * Insert the specified document into the specified database.
   * <ul>
   * <li>arg[0] - the location of the database (eg. ./flldb)
   * <li>arg[1] - the location of the document to insert in the database
   * </ul>
   */
  public static void main(final String[] args) throws IOException {
    if (args.length < 2) {
      LOG.fatal("Usage ImportDocument <xml file> <db>");
      System.exit(1);
    }
    final File challengeFile = new File(args[0]);
    if (!challengeFile.exists()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " doesn't exist");
      System.exit(1);
    }
    if (!challengeFile.canRead()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " is not readable");
      System.exit(1);
    }
    if (!challengeFile.isFile()) {
      LOG.fatal(challengeFile.getAbsolutePath()
          + " is not a file");
      System.exit(1);
    }
    final Document document = ChallengeParser.parse(new FileReader(challengeFile));
    if (null == document) {
      LOG.fatal("Error parsing challenge descriptor");
      System.exit(1);
    }

    if (!Utilities.testHSQLDB(args[1])) {
      LOG.fatal("There is a problem with the database, see previous log messages");
      System.exit(1);
    }
    PreparedStatement prep = null;
    Connection connection = null;
    int exitCode = 0;
    try {
      connection = Utilities.createDataSource(args[1]).getConnection();
      prep = connection.prepareStatement("UPDATE global_parameters SET Value = ? WHERE Param = ?");
      prep.setString(2, GlobalParameters.CHALLENGE_DOCUMENT);

      // dump the document into a byte array so we can push it into the database
      final XMLWriter xmlwriter = new XMLWriter();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlwriter.setOutput(baos, "UTF8");
      xmlwriter.write(document);
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      prep.setAsciiStream(1, bais, bytes.length);
      prep.executeUpdate();
    } catch (final UnsupportedEncodingException uee) {
      LOG.fatal("UTF8 not a supported encoding???", uee);
      exitCode = 1;
    } catch (final SQLException sqle) {
      LOG.fatal("Error talking to database", sqle);
      exitCode = 1;
    } finally {
      SQLFunctions.closePreparedStatement(prep);
      SQLFunctions.closeConnection(connection);
    }
    LOG.info("Inserted document "
        + args[0] + " into database " + args[1]);
    System.exit(exitCode);
  }

  private ImportDocument() {
  }

}
