/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.setup;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.db.GenerateDB;
import fll.db.ImportDB;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.xml.ChallengeParser;

/**
 * Create a new database either from an xml descriptor or from a database dump.
 * 
 * @author jpschewe
 * @web.servlet name="CreateDB"
 * @web.servlet-mapping url-pattern="/setup/CreateDB"
 */
public class CreateDB extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      if (null != request.getAttribute("reinitializeDatabase")) {
        // create a new empty database from an XML descriptor
        final FileItem xmlFileItem = (FileItem) request.getAttribute("xmldocument");

        final boolean forceRebuild = "1".equals(request.getAttribute("force_rebuild"));
        if (null == xmlFileItem) {
          message.append("<p class='error'>XML description document not specified</p>");
        } else {
          final Document document = ChallengeParser.parse(new InputStreamReader(xmlFileItem.getInputStream()));

          final String db = getServletConfig().getServletContext().getRealPath("/WEB-INF/flldb");
          GenerateDB.generateDB(document, db, forceRebuild);

          // remove application & session variables that depend on the database
          session.removeAttribute(SessionAttributes.DATASOURCE);
          application.removeAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT);
          application.removeAttribute(ApplicationAttributes.DATABASE);

          message.append("<p id='success'><i>Successfully initialized database</i></p>");
        }
      } else if (null != request.getAttribute("createdb")) {
        // import a database from a dump
        final FileItem dumpFileItem = (FileItem) request.getAttribute("dbdump");

        final String database = application.getRealPath("/WEB-INF/flldb");

        ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileItem.getInputStream()), database);

        // remove application variables that depend on the database
        session.removeAttribute(SessionAttributes.DATASOURCE);
        application.removeAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT);
        application.removeAttribute(ApplicationAttributes.DATABASE);

        message.append("<p id='success'><i>Successfully initialized database from dump</i></p>");
      } else {
        message.append("<p class='error'>Unknown form state, expected form fields not seen: "
            + request + "</p>");
      }
    } catch (final FileUploadException fue) {
      message.append("<p class='error'>Error handling the file upload: "
          + fue.getMessage() + "</p>");
      LOG.error(fue, fue);
    } catch (final IOException ioe) {
      message.append("<p class='error'>Error reading challenge descriptor: "
          + ioe.getMessage() + "</p>");
      LOG.error(ioe, ioe);
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error loading data into the database: "
          + sqle.getMessage() + "</p>");
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error loading data into the database", sqle);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

}
