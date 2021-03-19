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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import fll.Tournament;
import fll.Utilities;
import fll.db.ImportDB;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.web.UserRole;
import fll.web.developer.importdb.ImportDBDump;
import fll.web.developer.importdb.ImportDbSessionInfo;

/**
 * Start the import of finalist data by setting up the expected parameters
 * and then calling into the import database workflow.
 */
@WebServlet("/admin/ProcessImportFinalist")
public class ProcessImportFinalist extends BaseFLLServlet {

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

    final StringBuilder message = new StringBuilder();

    Utilities.loadDBDriver();
    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      if (null != request.getAttribute("finalistFile")) {

        final ImportDbSessionInfo sessionInfo = new ImportDbSessionInfo();

        final String databaseName = "dbimport"
            + String.valueOf(ImportDBDump.getNextDBCount());

        // TODO issue:123 should figure out how to clean up this database
        final DataSource importDataSource = Utilities.createMemoryDataSource(databaseName);

        sessionInfo.setImportDataSource(importDataSource);

        // set redirect page to be the admin index
        final String finalRedirectUrl = String.format("%s/admin/index.jsp", request.getContextPath());
        sessionInfo.setRedirectURL(finalRedirectUrl);

        try (Connection memConnection = importDataSource.getConnection()) {

          // import the database
          final FileItem dumpFileItem = (FileItem) request.getAttribute("finalistFile");
          try (ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream())) {
            ImportDB.loadDatabaseDump(zipfile, memConnection);

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
                  session.setAttribute(SessionAttributes.REDIRECT_URL, "index.jsp");
                } else {

                  sessionInfo.setTournamentName(sourceTournamentName);
                  sessionInfo.setImportFinalist(true);
                  sessionInfo.setImportPerformance(false);
                  sessionInfo.setImportSubjective(true);

                  // insert into the import workflow after tournament verification
                  final String redirect = String.format("%s/developer/importdb/FindMissingTeams",
                                                        request.getContextPath());
                  session.setAttribute(SessionAttributes.REDIRECT_URL, redirect);
                  session.setAttribute(ImportDBDump.IMPORT_DB_SESSION_KEY, sessionInfo);
                }
              } else {
                message.append("<p class='error'>");
                message.append("Import failed: Challenge descriptors are incompatible. This finalist dump is not from the same tournament.");
                message.append(docMessage);
                message.append("</p>");
                session.setAttribute(SessionAttributes.REDIRECT_URL, "index.jsp");
              }
            } // allocate destConnection
          } // allocate zipfile
        } // allocate memConnection
      } else {
        message.append("<p class='error'>Missing finalist data file</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "index.jsp");
      }

      SessionAttributes.appendToMessage(session, message.toString());
      response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
    } catch (final FileUploadException fue) {
      LOG.error(fue);
      throw new RuntimeException("Error handling the file upload", fue);
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error loading data into the database", sqle);
    }
  }

}
