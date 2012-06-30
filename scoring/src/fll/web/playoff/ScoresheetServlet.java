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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.icepush.PushContext;

import com.itextpdf.text.DocumentException;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

@WebServlet("/playoff/ScoresheetServlet")
public class ScoresheetServlet extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

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
      final DataSource datasource = SessionAttributes.getDataSource(session);
      connection = datasource.getConnection();
      final org.w3c.dom.Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
      final int tournament = Queries.getCurrentTournament(connection);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");

      PushContext pc = PushContext.getInstance(application);
      pc.push("playoffs");

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
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
