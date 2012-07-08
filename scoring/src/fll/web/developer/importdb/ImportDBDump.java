package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.ImportDB;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.xml.ChallengeParser;

/**
 * Import a database dump into the existing database.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/ImportDBDump")
public class ImportDBDump extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  /**
   * Keep track of the number of database imports so that the database names are
   * unique.
   */
  private static int _importdbCount = 0;
  public static int getNextDBCount() {
    synchronized(ImportDBDump.class) {
      return _importdbCount++;
    }
  }
  
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Utilities.loadDBDriver();
    Connection destConnection = null;
    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      if (null != request.getAttribute("importdb")) {

        final String databaseName = "dbimport"
            + String.valueOf(getNextDBCount());
        // TODO ticket:88 should figure out how to clean up this database
        final DataSource importDataSource = Utilities.createMemoryDataSource(databaseName)
            ;

        // let other pages know where the connection is
        session.setAttribute("dbimport", importDataSource);

        final Connection memConnection = importDataSource.getConnection();

        // import the database
        final FileItem dumpFileItem = (FileItem) request.getAttribute("dbdump");
        final ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream());
        ImportDB.loadDatabaseDump(zipfile, memConnection);

        final DataSource destDataSource = ApplicationAttributes.getDataSource(application);
        destConnection = destDataSource.getConnection();
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
      } else {
        message.append("<p class='error'>Unknown form state, expected form fields not seen: "
            + request + "</p>");
      }
    } catch (final FileUploadException fue) {
      LOG.error(fue);
      throw new RuntimeException("Error handling the file upload", fue);
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error loading data into the database", sqle);
    } finally {
      SQLFunctions.close(destConnection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

  /**
   * @param memConnection
   * @param destConnection
   * @return message to the user if there are errors, null if everything is OK
   */
  private String checkChallengeDescriptors(final Connection sourceConnection,
                                           final Connection destConnection) throws SQLException {
    final Document sourceDoc = Queries.getChallengeDocument(sourceConnection);
    final Document destDoc = Queries.getChallengeDocument(destConnection);
    final String compareMessage = ChallengeParser.compareStructure(destDoc, sourceDoc);
    return compareMessage;
  }

}
