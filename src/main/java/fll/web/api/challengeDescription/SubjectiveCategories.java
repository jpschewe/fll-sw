/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.challengeDescription;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Get {@link ChallengeDescription#getSubjectiveCategories()}.
 */
@WebServlet("/api/ChallengeDescription/SubjectiveCategories/*")
public class SubjectiveCategories extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    final ServletContext application = getServletContext();

    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final List<SubjectiveScoreCategory> categories = challengeDescription.getSubjectiveCategories();

    jsonMapper.writeValue(writer, categories);
  }

}
