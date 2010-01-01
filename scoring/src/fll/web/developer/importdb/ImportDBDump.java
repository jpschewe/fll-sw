package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import fll.Utilities;
import fll.db.ImportDB;
import fll.web.BaseFLLServlet;
import fll.web.Init;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;

/**
 * Import a database dump into the existing database.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class ImportDBDump extends BaseFLLServlet {

  private static final Logger LOG = Logger.getLogger(ImportDBDump.class);

  /**
   * Keep track of the number of database imports so that the database names are
   * unique.
   */
  private static int _importdbCount = 0;

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    Utilities.loadDBDriver();

    try {
      Init.initialize(request, response);

      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      if (null != request.getAttribute("importdb")) {

        final String databaseName = "dbimport" + String.valueOf(_importdbCount++); 
        final String url = "jdbc:hsqldb:mem:" + databaseName;
        // FIXME should figure out how to clean up this database
        final DataSource importDataSource = Utilities.createDataSource(databaseName, url);

        // let other pages know where the connection is 
        session.setAttribute("dbimport", importDataSource);

        final Connection memConnection = importDataSource.getConnection();        

        // import the database
        final FileItem dumpFileItem = (FileItem) request.getAttribute("dbdump");
        final ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream());
        ImportDB.loadDatabaseDump(zipfile, memConnection);

        session.setAttribute(SessionAttributes.REDIRECT_URL, "selectTournament.jsp");
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
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));    
  }

}
