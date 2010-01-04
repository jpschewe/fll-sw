/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import au.com.bytecode.opencsv.CSVWriter;
import fll.Utilities;
import fll.xml.XMLWriter;

/**
 * @author jpschewe
 * @version $Revision$
 */
public final class DumpDB {

  private static final Logger LOGGER = Logger.getLogger(DumpDB.class);
  
  public static void main(final String[] args) throws SQLException, IOException {
    final String database = "/home/jpschewe/projects/fll-sw/working-dir/scoring/build/tomcat/webapps/fll-sw/WEB-INF/flldb";
    final Connection connection = Utilities.createDataSource(database).getConnection();
    final ZipOutputStream output = new ZipOutputStream(new FileOutputStream("/home/jpschewe/download/foo/database.zip"));
    final Document challengeDocument = Queries.getChallengeDocument(connection);
    dumpDatabase(output, connection, challengeDocument);
    output.close();
  }
  
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

      // can't use Queries.getTablesInDB because it lowercases names and we need
      // all names to be the same as the database is expecting them
      final DatabaseMetaData metadata = connection.getMetaData();
      rs = metadata.getTables(null, null, "%", new String[]{"TABLE"});
      while (rs.next()) {
        final String tableName = rs.getString("TABLE_NAME"); 
        dumpTable(output, connection, metadata, outputWriter, tableName);        
      }
      SQLFunctions.closeResultSet(rs);
      
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
    }
  }

  private static void dumpTable(final ZipOutputStream output,
                                final Connection connection,
                                final DatabaseMetaData metadata,
                                final OutputStreamWriter outputWriter,
                                final String tableName) throws IOException, SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      CSVWriter csvwriter;
      
      // write table type information
      if(LOGGER.isTraceEnabled()) {
        LOGGER.trace("Dumping type information for " + tableName);
      }
      output.putNextEntry(new ZipEntry(tableName + ".types"));      
      csvwriter = new CSVWriter(outputWriter);
      rs = metadata.getColumns(null, null, tableName, "%");
      while(rs.next()) {
        final String typeName = rs.getString("TYPE_NAME");
        final String columnName = rs.getString("COLUMN_NAME");
        csvwriter.writeNext(new String[] {columnName, typeName});
        if(LOGGER.isTraceEnabled()) {
          final String name = rs.getString("TABLE_NAME"); 
          LOGGER.trace(new Formatter().format("Table %s Column %s has type %s", name, columnName, typeName));
        }
      }
      csvwriter.flush();
      output.closeEntry();
      SQLFunctions.closeResultSet(rs);
      rs = null;

      output.putNextEntry(new ZipEntry(tableName + ".csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM " + tableName);
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      output.closeEntry();
      SQLFunctions.closeResultSet(rs);
      rs = null;
            
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
    }
  }

}
