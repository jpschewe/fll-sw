/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import fll.db.GlobalParameters;

@WebServlet("/challenge.xml")
public class DisplayChallengeDescriptor extends BaseFLLServlet {

  /**
   * @see fll.web.BaseFLLServlet#processRequest(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext,
   *      javax.servlet.http.HttpSession)
   */
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      // get challenge document from the database as it will be modified to
      // include the stylesheet information and I don't want to propagate that.
      final Document challengeDocument = GlobalParameters.getChallengeDocument(connection);
      final ProcessingInstruction stylesheet = challengeDocument.createProcessingInstruction("xml-stylesheet", "type='text/css' href='fll.css'");
      challengeDocument.insertBefore(stylesheet, challengeDocument.getDocumentElement());

      response.reset();
      response.setContentType("text/xml");
      response.setHeader("Content-Disposition", "filename=challenge.xml");

      XMLUtils.writeXML(challengeDocument, response.getWriter(), "UTF-8");
    } catch (final SQLException sqle) {
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
