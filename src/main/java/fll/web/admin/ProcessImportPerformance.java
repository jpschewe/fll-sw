/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;

import org.apache.tomcat.util.http.fileupload.FileUploadException;

import fll.Tournament;
import fll.Utilities;
import fll.db.ImportDB;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.web.developer.importdb.ImportDBDump;
import fll.web.developer.importdb.ImportDbSessionInfo;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/**
 * Start the import of performance data by setting up the expected parameters
 * and then calling into the import database workflow.
 */
@WebServlet("/admin/ProcessImportPerformance")
@MultipartConfig()
public class ProcessImportPerformance extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

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

    session.setAttribute(ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY, request.getHeader("Referer"));

    final StringBuilder message = new StringBuilder();

    Utilities.loadDBDriver();
    try {
      if (null != request.getPart("performanceFile")) {

        final String databaseName = "dbimport"
            + String.valueOf(ImportDBDump.getNextDBCount());

        // TODO issue:123 should figure out how to clean up this database
        final DataSource importDataSource = Utilities.createMemoryDataSource(databaseName);

        // set redirect page to be the judges room index
        final String finalRedirectUrl = String.format("%s/admin/performance-area.jsp", request.getContextPath());

        final ImportDbSessionInfo sessionInfo = new ImportDbSessionInfo(importDataSource, finalRedirectUrl);

        try (Connection memConnection = importDataSource.getConnection()) {

          // import the database
          final Part dumpFileItem = request.getPart("performanceFile");
          if (null == dumpFileItem) {
            throw new MissingRequiredParameterException("performanceFile");
          }
          try (ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream())) {
            ImportDB.loadDatabaseDump(zipfile, memConnection, false);

            final String sourceTournamentName = Tournament.getCurrentTournament(memConnection).getName();

            final DataSource destDataSource = ApplicationAttributes.getDataSource(application);
            try (Connection destConnection = destDataSource.getConnection()) {
              final String docMessage = ImportDBDump.checkChallengeDescriptors(memConnection, destConnection);
              if (null == docMessage) {

                // check that the current tournament is the same in the imported database
                // and this database
                final String destTournamentName = Tournament.getCurrentTournament(destConnection).getName();
                if (!destTournamentName.equals(sourceTournamentName)) {
                  message.append(String.format("<p class='error'>The tournament being imported is %s, but the selected tournament is %s. Import of performance data cannot continue.</p>",
                                               sourceTournamentName, destTournamentName));
                  session.setAttribute(SessionAttributes.REDIRECT_URL,
                                       SessionAttributes.getAttribute(session,
                                                                      ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY,
                                                                      String.class));
                  session.removeAttribute(ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY);
                } else {

                  sessionInfo.setTournamentName(sourceTournamentName);
                  sessionInfo.setImportFinalist(false);
                  sessionInfo.setImportPerformance(true);
                  sessionInfo.setImportSubjective(false);
                  sessionInfo.setImportAwardsScript(false);

                  session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckPerformanceEmpty");
                }
              } else {
                message.append("<p class='error'>");
                message.append("Import failed: Challenge descriptors are incompatible. This performance dump is not from the same tournament.");
                message.append(docMessage);
                message.append("</p>");
                session.setAttribute(SessionAttributes.REDIRECT_URL,
                                     SessionAttributes.getAttribute(session, ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY,
                                                                    String.class));
                session.removeAttribute(ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY);
              }
            } // allocate destConnection
          } // allocate zipfile
        } // allocate memConnection

        session.setAttribute(ImportDBDump.IMPORT_DB_SESSION_KEY, sessionInfo);
      } else {
        message.append("<p class='error'>Missing performance data file</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL,
                             SessionAttributes.getAttribute(session, ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY,
                                                            String.class));
        session.removeAttribute(ImportDBDump.IMPORT_DB_FINAL_REDIRECT_KEY);
      }
    } catch (final FileUploadException fue) {
      LOG.error(fue);
      throw new RuntimeException("Error handling the file upload", fue);
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error loading data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());
    WebUtils.sendRedirect(response, session);
  }

}
