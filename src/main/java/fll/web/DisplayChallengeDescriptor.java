/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import fll.Utilities;
import fll.db.GlobalParameters;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Display the challenge description.
 */
@WebServlet("/challenge.xml")
public class DisplayChallengeDescriptor extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      // get challenge document from the database as it will be modified to
      // include the stylesheet information and I don't want to propagate that.
      final Document challengeDocument = GlobalParameters.getChallengeDescription(connection).toXml();
      final ProcessingInstruction stylesheet = challengeDocument.createProcessingInstruction("xml-stylesheet",
                                                                                             "type='text/css' href='fll.css'");
      challengeDocument.insertBefore(stylesheet, challengeDocument.getDocumentElement());

      response.reset();
      response.setContentType("text/xml");
      response.setHeader("Content-Disposition", "filename=challenge.xml");

      XMLUtils.writeXML(challengeDocument, response.getWriter(), Utilities.DEFAULT_CHARSET.name());
    } catch (final SQLException sqle) {
      throw new RuntimeException("Error talking to the database", sqle);
    }

  }

}
