/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.opencsv.CSVWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.GatherBugReport;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Dump the database.
 */
@WebServlet("/admin/database.flldb")
public final class DumpDB extends BaseFLLServlet {

  /**
   * Prefix used in the zip files for bugs. Has trailing Unix slash, which is also
   * appropriate for zip files.
   */
  public static final String BUGS_DIRECTORY = "bugs/";

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final String label = "";

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {

      exportDatabase(response, application, label, challengeDescription, connection);
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

  /**
   * Export the current database using a standard filename format.
   *
   * @param response where to write the database
   * @param application used to get some information for the dump
   * @param label the label to use, may be null. Appended to the end of the
   *          filename before the suffix.
   * @param challengeDescription the challenge
   * @param connection database connection
   * @throws SQLException if there is an error reading from the database
   * @throws IOException if there is an error writing to the response
   */
  public static void exportDatabase(final HttpServletResponse response,
                                    final ServletContext application,
                                    final @Nullable String label,
                                    final ChallengeDescription challengeDescription,
                                    final Connection connection)
      throws SQLException, IOException {
    final int tournamentId = Queries.getCurrentTournament(connection);
    final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);

    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    final String dateStr = df.format(new Date());

    final String filename = String.format("%s_%s%s.flldb", tournament.getName(), dateStr, (null == label ? "" : label));
    response.reset();
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=\""
        + filename
        + "\"");

    final ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
    try {
      DumpDB.dumpDatabase(zipOut, connection, challengeDescription, application);
    } finally {
      zipOut.close();
    }
  }

  /**
   * Dump the database to a zip file.
   *
   * @param output where to dump the database
   * @param connection the database connection to dump
   * @param description the challenge to write out
   * @param application if not null, then add the log files and bug reports to the
   *          database
   * @throws SQLException on a database error
   * @throws IOException on an error writing to the stream
   */
  public static void dumpDatabase(final ZipOutputStream output,
                                  final Connection connection,
                                  final ChallengeDescription description,
                                  final ServletContext application)
      throws SQLException, IOException {
    try (Statement stmt = connection.createStatement();
        OutputStreamWriter outputWriter = new OutputStreamWriter(output, Utilities.DEFAULT_CHARSET)) {

      // output the challenge descriptor
      output.putNextEntry(new ZipEntry("challenge.xml"));
      XMLUtils.writeXML(description.toXml(), outputWriter, Utilities.DEFAULT_CHARSET.name());
      output.closeEntry();

      // can't use Queries.getTablesInDB because it lowercases names and we need
      // all names to be the same as the database is expecting them
      final DatabaseMetaData metadata = connection.getMetaData();
      try (ResultSet rs = metadata.getTables(null, null, "%", new String[] { "TABLE" })) {
        while (rs.next()) {
          final String tableName = rs.getString("TABLE_NAME");
          dumpTable(output, connection, metadata, outputWriter, tableName.toLowerCase());
        }
      } // ResultSet try

      if (null != application) {
        GatherBugReport.addLogFiles(output);

        // find the bug reports
        addBugReports(output, application);
      }

    } // stmt & outputWriter
  }

  /**
   * Add the bug reports to the zipfile. These files are put
   * in a "bugs" subdirectory in the zip file.
   *
   * @param zipOut the stream to write to.
   * @param application used to find the bug report files.
   */
  private static void addBugReports(@Nonnull final ZipOutputStream zipOut,
                                    @Nonnull final ServletContext application)
      throws IOException {

    // add directory entry for the logs
    final String directory = BUGS_DIRECTORY;
    zipOut.putNextEntry(new ZipEntry(directory));

    final File fllWebInfDir = new File(application.getRealPath("/WEB-INF"));
    final File[] bugReports = fllWebInfDir.listFiles((FilenameFilter) (dir,
                                                                       name) -> name.startsWith("bug_")
                                                                           && name.endsWith(".zip"));

    if (null != bugReports) {
      for (final File f : bugReports) {
        if (f.isFile()) {
          zipOut.putNextEntry(new ZipEntry(directory
              + f.getName()));
          try (FileInputStream fis = new FileInputStream(f)) {
            fis.transferTo(zipOut);
            fis.close();
          }
        }
      }
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
                                        final OutputStreamWriter outputWriter)
      throws SQLException, IOException {
    boolean retval = false;
    ResultSet rs = null;
    try {
      final CSVWriter csvwriter = new CSVWriter(outputWriter);
      rs = metadata.getColumns(null, null, tableName, "%");
      while (rs.next()) {
        retval = true;

        String typeName = rs.getString("TYPE_NAME");
        final String columnName = rs.getString("COLUMN_NAME");
        if ("varchar".equalsIgnoreCase(typeName)
            || "char".equalsIgnoreCase(typeName)
            || "character".equalsIgnoreCase(typeName)) {
          typeName = typeName
              + "("
              + rs.getInt("COLUMN_SIZE")
              + ")";
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

  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Dynamic based upon tables in the database")
  private static void dumpTable(final ZipOutputStream output,
                                final Connection connection,
                                final DatabaseMetaData metadata,
                                final OutputStreamWriter outputWriter,
                                final String tableName)
      throws IOException, SQLException {
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
