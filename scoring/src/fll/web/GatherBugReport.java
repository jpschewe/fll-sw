/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.DumpDB;
import fll.util.LogUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Gather up the data for a bug report.
 */
@WebServlet("/GatherBugReport")
public class GatherBugReport extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    ZipOutputStream zipOut = null;
    final StringBuilder message = new StringBuilder();
    try {
      if (null != SessionAttributes.getMessage(session)) {
        message.append(SessionAttributes.getMessage(session));
      }

      connection = datasource.getConnection();
      final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

      final File fllWebInfDir = new File(application.getRealPath("/WEB-INF"));
      final String nowStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
      final File bugReportFile = File.createTempFile("bug_" + nowStr, ".zip", fllWebInfDir);

      zipOut = new ZipOutputStream(new FileOutputStream(bugReportFile));

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

      addDatabase(zipOut, connection, challengeDocument);
      addLogFiles(zipOut, application);

      message.append(String.format("<i>Bug report saved to '%s', please notify the computer person in charge to look for bug report files.</i>",
                                   bugReportFile.getAbsolutePath()));
      session.setAttribute(SessionAttributes.MESSAGE, message);

      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/"));

    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    } finally {
      SQLFunctions.close(connection);
      IOUtils.closeQuietly(zipOut);
    }

  }

  /**
   * Add the database to the zipfile.
   * 
   * @throws SQLException
   */
  private static void addDatabase(final ZipOutputStream zipOut,
                                  final Connection connection,
                                  final Document challengeDocument)
      throws IOException, SQLException {

    ZipOutputStream dbZipOut = null;
    FileInputStream fis = null;
    try {
      final File temp = File.createTempFile("database", ".flldb");

      dbZipOut = new ZipOutputStream(new FileOutputStream(temp));
      DumpDB.dumpDatabase(dbZipOut, connection, challengeDocument, null);
      dbZipOut.close();

      zipOut.putNextEntry(new ZipEntry("database.flldb"));
      fis = new FileInputStream(temp);
      IOUtils.copy(fis, zipOut);
      fis.close();

      if (!temp.delete()) {
        temp.deleteOnExit();
      }

    } finally {
      IOUtils.closeQuietly(dbZipOut);
      IOUtils.closeQuietly(fis);
    }

  }

  /**
   * Prefix used in the zip files for logs.
   */
  public static final String LOGS_DIRECTORY = "logs/";
  
  /**
   * Add the web application and tomcat logs to the zipfile. These files are put
   * in a "logs" subdirectory in the zip file.
   * 
   * @param zipOut the stream to write to.
   * @param application used to find the log files.
   */
  public static void addLogFiles(@Nonnull final ZipOutputStream zipOut,
                                 @Nonnull final ServletContext application)
      throws IOException {

    // add directory entry for the logs
    final String logsDirectory = LOGS_DIRECTORY;
    zipOut.putNextEntry(new ZipEntry(logsDirectory));

    // get logs from the webapp
    final File fllAppDir = new File(application.getRealPath("/"));
    final File[] webLogs = fllAppDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(final File dir,
                            final String name) {
        return name.startsWith("fllweb.log");
      }
    });
    if (null != webLogs) {
      for (final File f : webLogs) {
        if (f.isFile()) {
          FileInputStream fis = null;
          try {
            zipOut.putNextEntry(new ZipEntry(logsDirectory
                + f.getName()));
            fis = new FileInputStream(f);
            IOUtils.copy(fis, zipOut);
            fis.close();
          } finally {
            IOUtils.closeQuietly(fis);
          }
        }
      }
    }

    // get tomcat logs
    final File webappsDir = fllAppDir.getParentFile();
    LOGGER.trace("Webapps dir: "
        + webappsDir.getAbsolutePath());
    final File tomcatDir = webappsDir.getParentFile();
    LOGGER.trace("Tomcat dir: "
        + tomcatDir.getAbsolutePath());
    final File tomcatLogDir = new File(tomcatDir, "logs");
    LOGGER.trace("tomcat log dir: "
        + tomcatLogDir.getAbsolutePath());
    final File[] tomcatLogs = tomcatLogDir.listFiles();
    if (null != tomcatLogs) {
      for (final File f : tomcatLogs) {
        if (f.isFile()) {
          FileInputStream fis = null;
          try {
            zipOut.putNextEntry(new ZipEntry(logsDirectory
                + f.getName()));
            fis = new FileInputStream(f);
            IOUtils.copy(fis, zipOut);
            fis.close();
          } finally {
            IOUtils.closeQuietly(fis);
          }
        }
      }
    }

  }

}
