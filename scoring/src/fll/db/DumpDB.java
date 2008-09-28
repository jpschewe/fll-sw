/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVWriter;
import fll.xml.XMLWriter;

/**
 * @author jpschewe
 * @version $Revision$
 *
 */
public final class DumpDB {

  private DumpDB() {
    // no instances
  }
  
  /**
   * Dump the database to a zip file.
   * 
   * @param output where to dump the database
   * @param connection the database connection to dump
   */
  public static void dumpDatabase(final ZipOutputStream output, final Connection connection, final Document challengeDocument) throws SQLException, IOException {
    ResultSet rs = null;
    Statement stmt = null;
    
    try {
      stmt = connection.createStatement();
      
      final OutputStreamWriter outputWriter = new OutputStreamWriter(output);
      
      // output the challenge descriptor
      output.putNextEntry(new ZipEntry("challenge.xml"));
      final XMLWriter xmlwriter = new XMLWriter();
      xmlwriter.setOutput(outputWriter);
      xmlwriter.write(challengeDocument);
      output.closeEntry();

      CSVWriter csvwriter;

      // TODO output TournamentParameters once 1853081 is completed
      
      // teams
      output.putNextEntry(new ZipEntry("Teams.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM Teams");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();

      // judges
      output.putNextEntry(new ZipEntry("Judges.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM Judges");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();

      // TournamentTeams
      output.putNextEntry(new ZipEntry("TournamentTeams.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM TournamentTeams");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();

      // Tournaments
      output.putNextEntry(new ZipEntry("Tournaments.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM Tournaments");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();
      
      // performance
      output.putNextEntry(new ZipEntry("Performance.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM Performance");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();
      
      // each subjective category
      final Element rootElement = challengeDocument.getDocumentElement();
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
        final Element categoryElement = (Element)subjectiveCategories.item(cat);
        final String tableName = categoryElement.getAttribute("name");
        output.putNextEntry(new ZipEntry(tableName + ".csv"));
        csvwriter = new CSVWriter(outputWriter);
        rs = stmt.executeQuery("SELECT * FROM " + tableName);
        csvwriter.writeAll(rs, true);
        csvwriter.flush();
        SQLFunctions.closeResultSet(rs);
        output.closeEntry();
      }
      
      // PlayoffData
      output.putNextEntry(new ZipEntry("PlayoffData.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM PlayoffData");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();
      
      // tablenames
      output.putNextEntry(new ZipEntry("tablenames.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM tablenames");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();

      // FinalScores
      output.putNextEntry(new ZipEntry("FinalScores.csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM FinalScores");
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      SQLFunctions.closeResultSet(rs);
      output.closeEntry();
      
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
    }
  }
  
  
}
