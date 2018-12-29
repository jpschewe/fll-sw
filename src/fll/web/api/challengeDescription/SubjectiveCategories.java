/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.challengeDescription;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

@WebServlet("/api/ChallengeDescription/SubjectiveCategories/*")
public class SubjectiveCategories extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ObjectMapper jsonMapper = new ObjectMapper();

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final List<ScoreCategory> categories = challengeDescription.getSubjectiveCategories();

    jsonMapper.writeValue(writer, categories);
  }

}
