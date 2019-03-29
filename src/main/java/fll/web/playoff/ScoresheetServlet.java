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



import com.itextpdf.text.DocumentException;

import fll.db.Queries;
import fll.util.FLLRuntimeException;

import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.sql.SQLFunctions;

@WebServlet("/playoff/ScoresheetServlet")
public class ScoresheetServlet extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournament = Queries.getCurrentTournament(connection);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");

      final boolean orientationIsPortrait = ScoresheetGenerator.guessOrientation(challengeDescription);

      // Create the scoresheet generator - must provide correct number of
      // scoresheets
      final ScoresheetGenerator gen = new ScoresheetGenerator(request, connection, tournament, challengeDescription);

      gen.writeFile(response.getOutputStream(), orientationIsPortrait);

    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    } catch (final DocumentException e) {
      final String errorMessage = "There was an error creating the PDF document - perhaps you didn't select any scoresheets to print?";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
