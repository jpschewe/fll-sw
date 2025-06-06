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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.opencsv.CSVWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Launcher;
import fll.Tournament;
import fll.UserImages;
import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.GatherBugReport;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Dump the database.
 */
@WebServlet("/admin/database.flldb")
public final class DumpDB extends BaseFLLServlet {

  /**
   * Used as as replacement for null in the CSV files. See
   * {@link NullResultSetHelperService}.
   */
  public static final String FLL_SW_NULL_STRING = "FLL-SW-NULL";

  /**
   * Prefix used in the zip files for bugs. Has trailing Unix slash, which is also
   * appropriate for zip files.
   */
  public static final String BUGS_DIRECTORY = "bugs/";

  /**
   * Prefix used in the zip files for slideshow images. Has trailing Unix slash,
   * which is also
   * appropriate for zip files.
   */
  public static final String SLIDESHOW_IMAGES_DIRECTORY = "images/slideshow/";

  /**
   * Prefix used in the zip files for custom images. Has trailing Unix slash,
   * which is also
   * appropriate for zip files.
   */
  public static final String CUSTOM_IMAGES_DIRECTORY = "images/custom/";

  /**
   * Prefix used in the zip files for user images. Has trailing Unix slash,
   * which is also
   * appropriate for zip files.
   */
  public static final String USER_IMAGES_DIRECTORY = "images/user-images/";

  /**
   * Prefix used in the zip files for sponsor images. Has trailing Unix slash,
   * which is also
   * appropriate for zip files.
   */
  public static final String SPONSOR_IMAGES_DIRECTORY = "images/sponsor_logos/";

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

  private static final DateTimeFormatter DATABASE_DUMP_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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

    final String dateStr = LocalDateTime.now().format(DATABASE_DUMP_DATETIME_FORMATTER);

    final String filename = String.format("%s_%s%s.flldb", tournament.getName(), dateStr, (null == label ? "" : label));
    response.reset();
    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=\""
        + filename
        + "\"");

    try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
      DumpDB.dumpDatabase(zipOut, connection, challengeDescription, application);
    }
  }

  private static final String AUTOMATIC_BACKUP_DIRECTORY_NAME = "database-backups";

  /**
   * @return path to the database backups directory
   */
  public static Path getDatabaseBackupPath() {
    return Paths.get(AUTOMATIC_BACKUP_DIRECTORY_NAME);
  }

  /**
   * Create an automatic backup of the specified database. Create the file in the
   * directory "database-backups" relative to the current directory. The file is
   * named based on the current date and time and the specified label.
   * <p>
   * All exceptions are caught and turned into logging errors.
   * </p>
   * 
   * @param connection database to backup
   * @param label the label to use, may be null. Appended to the end of the
   *          filename before the suffix.
   */
  public static void automaticBackup(final Connection connection,
                                     final String label) {
    try {
      if (!Utilities.testDatabaseInitialized(connection)) {
        return;
      }
    } catch (final SQLException e) {
      LOGGER.error("Error checking if database is initialized, assuming it's not and not creating an automatic backup",
                   e);
    }

    final Path backupDirectory = getDatabaseBackupPath();

    try {
      Files.createDirectories(backupDirectory);
    } catch (final FileAlreadyExistsException e) {
      LOGGER.error("Unable to create automatic database backup because the output directory %s exists and is not a directory");
      return;
    } catch (final IOException e) {
      LOGGER.error("Error creating automatic database backups directory", e);
      return;
    }

    try {
      final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(connection);

      final String dateStr = LocalDateTime.now().format(DATABASE_DUMP_DATETIME_FORMATTER);
      final String dumpFilename = String.format("%s-%s.flldb", dateStr, label);
      final Path outputFile = backupDirectory.resolve(dumpFilename);
      try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(outputFile))) {
        DumpDB.dumpDatabase(zipOut, connection, challengeDescription, null);
      }

    } catch (final SQLException e) {
      LOGGER.error("Error reading database to create automatic backup", e);
    } catch (final IOException e) {
      LOGGER.error("Error writing automatic database backup", e);
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
                                  final @Nullable ServletContext application)
      throws SQLException, IOException {
    try (Statement stmt = connection.createStatement();
        OutputStreamWriter outputWriter = new OutputStreamWriter(output, Utilities.DEFAULT_CHARSET)) {

      // output the challenge descriptor
      output.putNextEntry(new ZipEntry("challenge.xml"));
      XMLUtils.writeXML(description.toXml(), outputWriter, Utilities.DEFAULT_CHARSET.name());
      outputWriter.flush();
      output.closeEntry();

      output.putNextEntry(new ZipEntry("dump_version.txt"));
      outputWriter.write(String.format("%d%n", ImportDB.DUMP_VERSION));
      outputWriter.flush();
      output.closeEntry();

      // can't use Queries.getTablesInDB because it lowercases names and we need
      // all names to be the same as the database is expecting them
      final DatabaseMetaData metadata = connection.getMetaData();
      try (ResultSet rs = metadata.getTables(null, null, "%", new String[] { "TABLE" })) {
        while (rs.next()) {
          final String tableName = rs.getString("TABLE_NAME");
          if (null != tableName) {
            dumpTable(output, connection, metadata, outputWriter, tableName.toLowerCase());
          }
        }
      } // ResultSet try

      if (null != application) {
        GatherBugReport.addLogFiles(output);

        // find the bug reports
        addBugReports(output, application);
      }

      addImages(output);

    } // stmt & outputWriter
  }

  private static void addImages(final ZipOutputStream output) throws IOException {
    addSlideshowImages(output);
    addCustomImages(output);
    addUserImages(output);
    addSponsorImages(output);
  }

  private static void addSlideshowImages(final ZipOutputStream output) throws IOException {
    final @Nullable Path slideshowPath = Launcher.getSlideshowDirectory();
    if (null == slideshowPath) {
      return;
    }

    Utilities.addFilesToZip(output, SLIDESHOW_IMAGES_DIRECTORY, slideshowPath);
  }

  private static void addCustomImages(final ZipOutputStream output) throws IOException {
    final @Nullable Path path = Launcher.getCustomDirectory();
    if (null == path) {
      return;
    }

    Utilities.addFilesToZip(output, CUSTOM_IMAGES_DIRECTORY, path);
  }

  private static void addUserImages(final ZipOutputStream output) throws IOException {
    Utilities.addFilesToZip(output, USER_IMAGES_DIRECTORY, UserImages.getImagesPath());
  }

  private static void addSponsorImages(final ZipOutputStream output) throws IOException {
    final @Nullable Path path = Launcher.getSponsorLogosDirectory();
    if (null == path) {
      return;
    }

    Utilities.addFilesToZip(output, SPONSOR_IMAGES_DIRECTORY, path);
  }

  /**
   * Add the bug reports to the zipfile. These files are put
   * in a "bugs" subdirectory in the zip file.
   *
   * @param zipOut the stream to write to.
   * @param application used to find the bug report files.
   */
  private static void addBugReports(final ZipOutputStream zipOut,
                                    final ServletContext application)
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
    // can't close the CSVwriter because that will close outputWriter, which is
    // actually the zip output stream
    final CSVWriter csvwriter = new CSVWriter(outputWriter);
    try (ResultSet rs = metadata.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        retval = true;

        String typeName = rs.getString("TYPE_NAME");
        final String columnName = rs.getString("COLUMN_NAME");
        if (null != columnName
            && null != typeName) {
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
            LOGGER.trace("Table {} Column {} has type {}", name == null ? "null" : name, columnName, typeName);
          }
        }
      }
    }
    csvwriter.flush();
    return retval;
  }

  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Dynamic based upon tables in the database")
  private static void dumpTable(final ZipOutputStream output,
                                final Connection connection,
                                final DatabaseMetaData metadata,
                                final OutputStreamWriter outputWriter,
                                final String tableName)
      throws IOException, SQLException {
    try (Statement stmt = connection.createStatement()) {
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

      // can't close the CSVwriter because that will close outputWriter, which is
      // actually the zip output stream
      final CSVWriter csvwriter = new CSVWriter(outputWriter);
      csvwriter.setResultService(new NullResultSetHelperService(FLL_SW_NULL_STRING));
      try (ResultSet rs = stmt.executeQuery("SELECT * FROM "
          + tableName)) {
        csvwriter.writeAll(rs, true);
        csvwriter.flush();
        output.closeEntry();
      }
    }
  }

}
