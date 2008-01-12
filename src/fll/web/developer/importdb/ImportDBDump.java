package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import fll.Utilities;
import fll.db.ImportDB;
import fll.web.UploadProcessor;

/**
 * Import a database dump into the existing database.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class ImportDBDump extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(ImportDBDump.class);

  /**
   * Keep track of the number of database imports so that the database names are
   * unique.
   */
  private static int _importdbCount = 0;

  /**
   * 
   * @param request
   * @param response
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final StringBuilder message = new StringBuilder();
    final HttpSession session = request.getSession();

    Connection memConnection = null;
    Statement memStmt = null;

    Utilities.loadDBDriver();

    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      if(null != request.getAttribute("importdb")) {

        final String url = "jdbc:hsqldb:mem:dbimport" + String.valueOf(_importdbCount++);
        memConnection = DriverManager.getConnection(url);
        memStmt = memConnection.createStatement();

        // import the database
        final FileItem dumpFileItem = (FileItem)request.getAttribute("dbdump");
        final ZipInputStream zipfile = new ZipInputStream(dumpFileItem.getInputStream());
        /* final Document challengeDocument = */ImportDB.loadDatabaseDump(zipfile, memConnection);
        // GenerateDB.generateDB(challengeDocument, database, true);

        // let the jsp code know where to find the database
        session.setAttribute("dbimport_url", url);
        session.setAttribute("redirect_url", "selectTournament.jsp");
      } else {
        message.append("<p class='error'>Unknown form state, expected form fields not seen: " + request + "</p>");
      }
    } catch(final FileUploadException fue) {
      message.append("<p class='error'>Error handling the file upload: " + fue.getMessage() + "</p>");
      LOG.error(fue);
    } catch(final SQLException sqle) {
      message.append("<p class='error'>Error loading data into the database: " + sqle.getMessage() + "</p>");
      LOG.error(sqle);
    } finally {
      Utilities.closeStatement(memStmt);
      Utilities.closeConnection(memConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL((String)session.getAttribute("redirect_url")));
  }

  /**
   * Create the tournament that is in the session variable
   * <code>selected_tournament</code>.
   * 
   * @param session
   *          the session
   */
  public static void createSelectedTournament(final HttpSession session) {
    final StringBuilder message = new StringBuilder();
    
    final String selectedTournament = (String)session.getAttribute("selected_tournament");
    final Connection connection = (Connection)session.getAttribute("connection");
    
    Utilities.loadDBDriver();
    Connection memConnection = null;
    PreparedStatement memPrep= null;
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      memConnection = DriverManager.getConnection((String)session.getAttribute("dbimport_url"));
      memPrep = memConnection.prepareStatement("SELECT Name, Location, NextTournament FROM Tournaments WHERE Name = ?");
      memPrep.setString(1, selectedTournament);
      if(rs.next()) {
        prep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location, NextTournament) VALUES (?, ?, ?)");        
        prep.setString(1, rs.getString(1));
        prep.setString(2, rs.getString(2));
        prep.setString(3, rs.getString(3));
        prep.executeUpdate();
      } else {
        message.append("Cannot find tournament " + selectedTournament + " in imported database!");
        session.setAttribute("redirect_url", "selectTournament.jsp");
      }
      
    } catch(final SQLException sqle) {
      message.append("<p class='error'>Error adding tournament to database: " + sqle.getMessage() + "</p>");
      LOG.error(sqle, sqle);
      session.setAttribute("redirect_url", "selectTournament.jsp");
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(memPrep);
      Utilities.closeConnection(memConnection);
      Utilities.closePreparedStatement(prep);
    }

    session.setAttribute("message", message.toString());
  }
}
