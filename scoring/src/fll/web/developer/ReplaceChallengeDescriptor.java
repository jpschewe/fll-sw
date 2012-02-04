/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.xml.ChallengeParser;

@WebServlet("/developer/ReplaceChallengeDescriptor")
public class ReplaceChallengeDescriptor extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of ReplaceChallengeDescriptor.doPost");
    }

    final StringBuilder message = new StringBuilder();

    try {
      final DataSource datasource = SessionAttributes.getDataSource(session);
      final Connection connection = datasource.getConnection();

      
      final Document curDoc = ApplicationAttributes.getChallengeDocument(application);
      
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      // create a new empty database from an XML descriptor
      final FileItem xmlFileItem = (FileItem) request.getAttribute("xmldoc");
      
      final Document newDoc= ChallengeParser.parse(new InputStreamReader(xmlFileItem.getInputStream()));
      
      final String compareMessage = ChallengeParser.compareStructure(curDoc, newDoc);
      if(null == compareMessage) {
        GenerateDB.insertOrUpdateChallengeDocument(newDoc, connection);
        application.setAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT, newDoc);
        message.append("<p><i>Successfully replaced challenge descriptor</i></p>");
      } else {
        message.append("<p class='error'>");
        message.append(compareMessage);
        message.append("</p>");
      }
    } catch (final FileUploadException fue) {
      message.append("<p class='error'>Error handling the file upload: "
          + fue.getMessage() + "</p>");
      LOGGER.error(fue, fue);
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of ReplaceChallengeDescriptor.doPost");
    }
    
    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }

}
