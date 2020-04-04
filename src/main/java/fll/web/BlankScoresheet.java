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

import org.apache.commons.lang3.tuple.Pair;

import com.itextpdf.text.DocumentException;

import fll.util.FLLRuntimeException;
import fll.web.playoff.ScoresheetGenerator;
import fll.xml.ChallengeDescription;

/**
 * Generate a blank score sheet.
 */
@WebServlet("/playoff/BlankScoresheet")
public class BlankScoresheet extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=blank-scoreSheet.pdf");

      final Pair<Boolean, Float> orientationResult = ScoresheetGenerator.guessOrientation(challengeDescription);
      final boolean orientationIsPortrait = orientationResult.getLeft();
      final float pagesPerScoreSheet = orientationResult.getRight();

      final ScoresheetGenerator gen = new ScoresheetGenerator(challengeDescription);
      gen.writeFile(response.getOutputStream(), orientationIsPortrait, pagesPerScoreSheet);

    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    } catch (final DocumentException e) {
      final String errorMessage = "There was an error creating the PDF document - perhaps you didn't select any scoresheets to print?";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    }
  }

}
