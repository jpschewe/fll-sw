/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import fll.Utilities;
import fll.db.DumpDB;
import fll.xml.ChallengeDescription;

/**
 * Gather up the data for a bug report.
 */
@WebServlet("/GatherBugReport")
public class GatherBugReport extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    final StringBuilder message = new StringBuilder();
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      final File fllWebInfDir = new File(application.getRealPath("/WEB-INF"));
      final String nowStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
      final File bugReportFile = File.createTempFile("bug_"
          + nowStr, ".zip", fllWebInfDir);

      try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(bugReportFile))) {

        final String description = request.getParameter("bug_description");
        if (null != description) {
          zipOut.putNextEntry(new ZipEntry("bug_description.txt"));
          zipOut.write(description.getBytes(Utilities.DEFAULT_CHARSET));
        }

        zipOut.putNextEntry(new ZipEntry("server_info.txt"));
        zipOut.write(String.format("Java version: %s%nJava vendor: %s%nOS Name: %s%nOS Arch: %s%nOS Version: %s%nServlet API: %d.%d%nServlet container: %s%n",
                                   System.getProperty("java.vendor"), //
                                   System.getProperty("java.version"), //
                                   System.getProperty("os.name"), //
                                   System.getProperty("os.arch"), //
                                   System.getProperty("os.version"), //
                                   application.getMajorVersion(), application.getMinorVersion(), //
                                   application.getServerInfo())
                           .getBytes(Utilities.DEFAULT_CHARSET));

        addDatabase(zipOut, connection, challengeDescription);
        addLogFiles(zipOut);
      }

      message.append(String.format("<i>Bug report saved to '%s', please notify the computer person in charge to look for bug report files.</i>",
                                   bugReportFile.getAbsolutePath()));
      SessionAttributes.appendToMessage(session, message.toString());

      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/"));

    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }

  }

  /**
   * Add the database to the zipfile.
   *
   * @throws SQLException on a database error
   */
  private static void addDatabase(final ZipOutputStream zipOut,
                                  final Connection connection,
                                  final ChallengeDescription description)
      throws IOException, SQLException {

    final File temp = File.createTempFile("database", ".flldb");

    try (ZipOutputStream dbZipOut = new ZipOutputStream(new FileOutputStream(temp))) {
      DumpDB.dumpDatabase(dbZipOut, connection, description, null);
      dbZipOut.close();
    }

    zipOut.putNextEntry(new ZipEntry("database.flldb"));
    try (FileInputStream fis = new FileInputStream(temp)) {
      fis.transferTo(zipOut);
    }

    if (!temp.delete()) {
      temp.deleteOnExit();
    }

  }

  /**
   * Prefix used in the zip files for logs. Has trailing Unix slash, which is also
   * appropriate for zip files.
   */
  public static final String LOGS_DIRECTORY = "logs/";

  /**
   * Add the web application and tomcat logs to the zipfile. These files are put
   * in a "logs" subdirectory in the zip file.
   *
   * @param zipOut the stream to write to.
   * @throws IOException if there is an error writing the log files to the zip
   *           stream
   */
  public static void addLogFiles(@Nonnull final ZipOutputStream zipOut) throws IOException {

    // add directory entry for the logs
    zipOut.putNextEntry(new ZipEntry(LOGS_DIRECTORY));

    // get logs from the application
    final Path logsDirectory = Paths.get("logs");
    if (Files.exists(logsDirectory)
        && Files.isDirectory(logsDirectory)) {
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(logsDirectory)) {
        for (final Path path : directoryStream) {
          if (Files.isRegularFile(path)) {
            zipOut.putNextEntry(new ZipEntry(normalizePathname(logsDirectory.resolve(path.getFileName()).toString())));
            try (InputStream is = Files.newInputStream(path)) {
              is.transferTo(zipOut);
            }
          }
        }
      }
    }

  }

  /**
   * Handle windows path names and make them match the zip file spec which is '/'.
   */
  private static String normalizePathname(final String name) {
    if (File.separatorChar != '/') {
      return name.replace('\\', '/');
    } else {
      return name;
    }
  }

}
