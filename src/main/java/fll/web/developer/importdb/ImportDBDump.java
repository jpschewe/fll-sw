package fll.web.developer.importdb;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.checkerframework.checker.nullness.qual.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

/**
 * Import a database dump into the existing database.
 *
 * @author jpschewe
 */
@WebServlet("/developer/importdb/ImportDBDump")
@MultipartConfig()
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
   * Where to send the user at the end of the import.
   */
  public static final String IMPORT_DB_FINAL_REDIRECT_KEY = "ImportDBFinalRedirect";

  /**
   * @return an integer to differentiate in-memory databases.
   */
  @SuppressFBWarnings(value = "SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA", justification = "https://github.com/spotbugs/spotbugs/issues/1978")
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

    session.setAttribute(IMPORT_DB_FINAL_REDIRECT_KEY, request.getHeader("Referer"));

    final StringBuilder message = new StringBuilder();

    Utilities.loadDBDriver();
    final String redirectUrl;
    try {
      if (null != request.getPart("importdb")) {

        final String databaseName = "dbimport"
            + String.valueOf(getNextDBCount());
        // TODO issue:123 should figure out how to clean up this database
        final DataSource importDataSource = Utilities.createMemoryDataSource(databaseName);

        final ImportDbSessionInfo sessionInfo = new ImportDbSessionInfo(importDataSource);

        try (Connection memConnection = importDataSource.getConnection()) {

          // import the database
          final Part dumpFileItem = request.getPart("dbdump");
          if (null == dumpFileItem) {
            throw new MissingRequiredParameterException("dbdump");
          }
          try (ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream())) {
            final ImportDB.ImportResult importResult = ImportDB.loadDatabaseDump(zipfile, memConnection, false);

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
                redirectUrl = "selectTournament.jsp";
                session.setAttribute(IMPORT_DB_SESSION_KEY, sessionInfo);
              } else {
                message.append("<p class='error'>");
                message.append("Import failed: Challenge descriptors are incompatible. ");
                message.append(docMessage);
                message.append("</p>");

                final String redirect = SessionAttributes.getAttribute(session, IMPORT_DB_FINAL_REDIRECT_KEY,
                                                                       String.class);
                session.removeAttribute(IMPORT_DB_FINAL_REDIRECT_KEY);
                redirectUrl = redirect;
              }
            } // allocate destConnection
          } // allocate zipfile
        } // allocate memConnection

      } else {
        throw new FLLRuntimeException("Unknown form state, expected form fields not seen: "
            + request);
      }
    } catch (final FileUploadException fue) {
      LOG.error(fue);
      throw new RuntimeException("Error handling the file upload", fue);
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error loading data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());
    WebUtils.sendRedirect(response, redirectUrl);
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
