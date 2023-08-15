/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;

/**
 * Populate context for dynamic scoreboard.
 */
public final class Dynamic {

  private Dynamic() {
  }

  /**
   * Setup variables for the page.
   *
   * @param request to get parameters
   * @param application application context
   * @param page page context
   */
  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final List<String> awardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
      final Map<String, String> awardGroupColors = new HashMap<>();
      for (int index = 0; index < awardGroups.size(); ++index) {
        final String awardGroup = awardGroups.get(index);
        final String color = AllTeams.getColorForAwardGroup(awardGroup, index);
        awardGroupColors.put(awardGroup, color);
      }
      final ObjectMapper mapper = Utilities.createJsonMapper();
      page.setAttribute("awardGroupColors", mapper.writeValueAsString(awardGroupColors));

      final int divisionFlipRate = GlobalParameters.getIntGlobalParameter(connection,
                                                                          GlobalParameters.DIVISION_FLIP_RATE);
      page.setAttribute("divisionFlipRate", divisionFlipRate);

      page.setAttribute("layout", request.getParameter("layout"));
    } catch (final SQLException e) {
      throw new FLLInternalException("Error talking to the database", e);
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Error mapping data to JSON", e);
    }

  }

}
