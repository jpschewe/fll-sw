/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import fll.db.AwardDeterminationOrder;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.web.report.awards.AwardCategory;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * 
 */
@WebServlet("/report/EditAwardDeterminationOrder")
public class EditAwardDeterminationOrder extends BaseFLLServlet {

  public static void populateContext(final ServletContext application,
                                     final PageContext page) {

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final List<AwardCategory> awards = AwardDeterminationOrder.get(connection, description);
      page.setAttribute("awards", awards);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Map<AwardCategory, Integer> awardOrderMap = new HashMap<>();

      final List<AwardCategory> awards = AwardDeterminationOrder.get(connection, description);
      for (AwardCategory award : awards) {
        final int index = WebUtils.getIntRequestParameter(request, award.getTitle(), -1);
        if (index >= 0) {
          awardOrderMap.put(award, index);
        }
      }

      // sort awardGroups by the values in the map
      awards.sort((AwardCategory a1,
                   AwardCategory a2) -> {
        final Integer ag1Value = awardOrderMap.getOrDefault(a1, Integer.MAX_VALUE);
        final Integer ag2Value = awardOrderMap.getOrDefault(a2, Integer.MAX_VALUE);
        final int valueCompare = ag1Value.compareTo(ag2Value);
        if (0 == valueCompare) {
          return a1.getTitle().compareTo(a2.getTitle());
        } else {
          return valueCompare;
        }
      });

      AwardDeterminationOrder.save(connection, awards);

      SessionAttributes.appendToMessage(session,
                                        "<div class='success'>Saved changes to award determination order</div>");
      response.sendRedirect("index.jsp");

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
