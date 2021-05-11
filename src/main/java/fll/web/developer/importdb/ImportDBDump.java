package fll.web.developer.importdb;

import java.io.IOException;
import java.nio.file.Files;
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
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;

/**
 * Import a database dump into the existing database.
 *
 * @author jpschewe
 */
@WebServlet("/developer/importdb/ImportDBDump")
public class ImportDBDump extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Key in the session to store an instance of {@link ImportDbSessionInfo}.
   */
  public static final String IMPORT_DB_SESSION_KEY = "importDbSessionInfo";

  /**
   * Keep track of the number of database imports so that the database names are
   * unique.
   */
  private static int importdbCount = 0;

  /**
   * @return an integer to differentiate in-memory databases.
   */
  public static int getNextDBCount() {
    synchronized (ImportDBDump.class) {
      return importdbCount++;
    }
  }

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

      if (null != request.getAttribute("importdb")) {

        final ImportDbSessionInfo sessionInfo = new ImportDbSessionInfo();

        final String databaseName = "dbimport"
            + String.valueOf(getNextDBCount());
        // TODO issue:123 should figure out how to clean up this database
        final DataSource importDataSource = Utilities.createMemoryDataSource(databaseName);

        sessionInfo.setImportDataSource(importDataSource);

        try (Connection memConnection = importDataSource.getConnection()) {

          // import the database
          final FileItem dumpFileItem = (FileItem) request.getAttribute("dbdump");
          if (null == dumpFileItem) {
            throw new MissingRequiredParameterException("dbdump");
          }
          try (ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream())) {
            final ImportDB.ImportResult importResult = ImportDB.loadDatabaseDump(zipfile, memConnection);

            if (importResult.hasBugs()) {
              message.append("<p id='bugs_found' class='warning'>Bug reports found in the import.</p>");
            }
            if (Files.exists(importResult.getImportDirectory())) {
              message.append(String.format("<p id='import_logs_dir'>See %s for bug reports and logs.</p>",
                                           importResult.getImportDirectory()));
            }

            final DataSource destDataSource = ApplicationAttributes.getDataSource(application);
            try (Connection destConnection = destDataSource.getConnection()) {
              final String docMessage = checkChallengeDescriptors(memConnection, destConnection);
              if (null == docMessage) {
                session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
              } else {
                message.append("<p class='error'>");
                message.append("Import failed: Challenge descriptors are incompatible. ");
                message.append(docMessage);
                message.append("</p>");
                session.setAttribute(SessionAttributes.REDIRECT_URL, "../index.jsp");
              }
            } // allocate destConnection
          } // allocate zipfile
        } // allocate memConnection

        session.setAttribute(IMPORT_DB_SESSION_KEY, sessionInfo);
      } else {
        message.append("<p class='error'>Unknown form state, expected form fields not seen: "
            + request
            + "</p>");
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

  /**
   * Compare challenge descriptions between 2 databases.
   *
   * @param sourceConnection the first database to compare
   * @param destConnection the second database to compare
   * @return message to the user if there are errors, null if everything is OK
   * @throws SQLException if there is an error talking to the database
   */
  public static @Nullable String checkChallengeDescriptors(final Connection sourceConnection,
                                                           final Connection destConnection)
      throws SQLException {
    final ChallengeDescription sourceDoc = GlobalParameters.getChallengeDescription(sourceConnection);
    final ChallengeDescription destDoc = GlobalParameters.getChallengeDescription(destConnection);
    final String compareMessage = ChallengeParser.compareStructure(destDoc, sourceDoc);
    return compareMessage;
  }

}
