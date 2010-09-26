/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.itextpdf.text.DocumentException;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * @web.servlet name="ScoresheetServlet"
 * @web.servlet-mapping url-pattern="/playoff/ScoresheetServlet"
 */
public class ScoresheetServlet extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  /**
   * @see fll.web.BaseFLLServlet#processRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, javax.servlet.http.HttpSession)
   */
  @Override
  protected void processRequest(final HttpServletRequest request, 
                                final HttpServletResponse response, 
                                final ServletContext application, 
                                final HttpSession session) throws IOException,
      ServletException {
    try {
      final DataSource datasource = SessionAttributes.getDataSource(session);
      final Connection connection = datasource.getConnection();
      final org.w3c.dom.Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
      final int tournament = Queries.getCurrentTournament(connection);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");

      // Create the scoresheet generator - must provide correct number of
      // scoresheets
      final ScoresheetGenerator gen = new ScoresheetGenerator(request, connection, tournament, challengeDocument);

      gen.writeFile(connection, response.getOutputStream());
      
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }
  }

}
