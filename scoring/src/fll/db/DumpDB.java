/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import au.com.bytecode.opencsv.CSVWriter;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Dump the database.
 */
@WebServlet("/admin/database.flldb")
public final class DumpDB extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

      response.reset();
      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "filename=database.flldb");

      final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
      try {
        DumpDB.dumpDatabase(zipOut, connection, challengeDocument);
      } finally {
        zipOut.close();
      }
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Dump the database to a zip file.
   * 
   * @param output where to dump the database
   * @param connection the database connection to dump
   */
  public static void dumpDatabase(final ZipOutputStream output,
                                  final Connection connection,
                                  final Document challengeDocument) throws SQLException, IOException {
    ResultSet rs = null;
    Statement stmt = null;

    try {
      stmt = connection.createStatement();

      final Charset charset = Charset.forName("UTF-8");
      final OutputStreamWriter outputWriter = new OutputStreamWriter(output, charset);

      // output the challenge descriptor
      output.putNextEntry(new ZipEntry("challenge.xml"));
      XMLUtils.writeXML(challengeDocument, outputWriter, "UTF-8");
      output.closeEntry();

      // can't use Queries.getTablesInDB because it lowercases names and we need
      // all names to be the same as the database is expecting them
      final DatabaseMetaData metadata = connection.getMetaData();
      rs = metadata.getTables(null, null, "%", new String[] { "TABLE" });
      while (rs.next()) {
        final String tableName = rs.getString("TABLE_NAME");
        dumpTable(output, connection, metadata, outputWriter, tableName.toLowerCase());
      }
      SQLFunctions.close(rs);

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Dump the type information for a table to outputWriter.
   * 
   * @param tableName
   * @param metadata
   * @param outputWriter
   * @return true if column information for the specified table name was found
   * @throws SQLException
   * @throws IOException
   */
  private static boolean dumpTableTypes(final String tableName,
                                        final DatabaseMetaData metadata,
                                        final OutputStreamWriter outputWriter) throws SQLException, IOException {
    boolean retval = false;
    ResultSet rs = null;
    try {
      final CSVWriter csvwriter = new CSVWriter(outputWriter);
      rs = metadata.getColumns(null, null, tableName, "%");
      while (rs.next()) {
        retval = true;

        String typeName = rs.getString("TYPE_NAME");
        final String columnName = rs.getString("COLUMN_NAME");
        if ("varchar".equalsIgnoreCase(typeName)) {
          typeName = typeName
              + "(" + rs.getInt("COLUMN_SIZE") + ")";
        }
        csvwriter.writeNext(new String[] { columnName.toLowerCase(), typeName });
        if (LOGGER.isTraceEnabled()) {
          final String name = rs.getString("TABLE_NAME");
          LOGGER.trace(new Formatter().format("Table %s Column %s has type %s", name, columnName, typeName));
        }
      }
      csvwriter.flush();
    } finally {
      SQLFunctions.close(rs);
    }
    return retval;
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Dynamic based upon tables in the database")
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
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Dumping type information for "
            + tableName);
      }
      output.putNextEntry(new ZipEntry(tableName
          + ".types"));
      boolean dumpedTypes = dumpTableTypes(tableName, metadata, outputWriter);
      if (!dumpedTypes) {
        dumpedTypes = dumpTableTypes(tableName.toUpperCase(), metadata, outputWriter);
      }
      if (!dumpedTypes) {
        dumpTableTypes(tableName.toLowerCase(), metadata, outputWriter);
      }
      output.closeEntry();

      output.putNextEntry(new ZipEntry(tableName
          + ".csv"));
      csvwriter = new CSVWriter(outputWriter);
      rs = stmt.executeQuery("SELECT * FROM "
          + tableName);
      csvwriter.writeAll(rs, true);
      csvwriter.flush();
      output.closeEntry();
      SQLFunctions.close(rs);
      rs = null;

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

}
