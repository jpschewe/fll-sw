/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import fll.Utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.w3c.dom.Document;

/**
 * Push an XML document into the database without recreating the database.
 * This can be somewhat dangerous and is mostly here for debugging.
 *
 * @version $Revision$
 */
public final class ImportDocument {

  private static final Logger LOG = Logger.getLogger(ImportDocument.class);
  
  public static void main(final String[] args) throws IOException {
    final Document document = ChallengeParser.parse(new FileInputStream("/home/jpschewe/projects/fll-sw/working-dir/challenge-descriptors/challenge-region-2006.xml"));
    if(null == document) {
      throw new RuntimeException("Error parsing challenge.xml");
    }

    PreparedStatement prep = null;
    Connection connection = null;
    try {
      connection = Utilities.createDBConnection("fll", "fll", "/home/jpschewe/projects/fll-sw/working-dir/scoring/build/tomcat/webapps/fll-sw/WEB-INF/flldb");
      prep = connection.prepareStatement("UPDATE TournamentParameters SET Value = ? WHERE Param = 'ChallengeDocument'");
      
      //dump the document into a byte array so we can push it into the database
      final XMLWriter xmlwriter = new XMLWriter();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlwriter.setOutput(baos, "UTF8");
      xmlwriter.write(document);
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      prep.setAsciiStream(1, bais, bytes.length);
      prep.executeUpdate();
    } catch(final UnsupportedEncodingException uee) {
      LOG.fatal("UTF8 not a supported encoding???", uee);
      System.exit(1);
    } catch(final SQLException sqle) {
      LOG.fatal("Error talking to database", sqle);
      System.exit(1);
    } finally {
      Utilities.closePreparedStatement(prep);
      Utilities.closeConnection(connection);
    }
    System.exit(0);
  }

  private ImportDocument() {}
  
}
