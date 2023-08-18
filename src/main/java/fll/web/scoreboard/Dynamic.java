/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.TournamentTeam;
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

  private static final int TEAMS_BETWEEN_LOGOS = 2;

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
      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection,
                                                                                      tournament.getTournamentID());

      final Map<String, String> awardGroupColors = new HashMap<>();
      for (int index = 0; index < awardGroups.size(); ++index) {
        final String awardGroup = awardGroups.get(index);
        final String color = Dynamic.getColorForAwardGroup(awardGroup, index);
        awardGroupColors.put(awardGroup, color);
      }
      final ObjectMapper mapper = Utilities.createJsonMapper();
      page.setAttribute("awardGroupColors", mapper.writeValueAsString(awardGroupColors));

      final List<TournamentTeam> allTeams = new LinkedList<>(tournamentTeams.values());
      final Map<Integer, String> teamHeaderColor = new HashMap<>();
      for (final Map.Entry<Integer, TournamentTeam> entry : tournamentTeams.entrySet()) {
        final TournamentTeam team = entry.getValue();

        final String headerColor = Dynamic.getColorForAwardGroup(team.getAwardGroup(),
                                                                 awardGroups.indexOf(team.getAwardGroup()));
        teamHeaderColor.put(entry.getKey(), headerColor);
        allTeams.add(entry.getValue());
      } // foreach tournament team

      final int divisionFlipRate = GlobalParameters.getIntGlobalParameter(connection,
                                                                          GlobalParameters.DIVISION_FLIP_RATE);
      page.setAttribute("divisionFlipRate", divisionFlipRate);

      final @Nullable String layout = request.getParameter("layout");
      page.setAttribute("layout", layout);
      if (!StringUtils.isEmpty(layout)) {
        page.setAttribute("additionalTitle", String.format(" - %s", layout));
      } else {
        page.setAttribute("additionalTitle", "");
      }

      final double scrollRate = GlobalParameters.getAllTeamScrollRate(connection);
      page.setAttribute("scrollRate", scrollRate);

      final List<String> sponsorLogos = getSponsorLogos(application);
      page.setAttribute("sponsorLogos", sponsorLogos);

      page.setAttribute("teamsBetweenLogos", Integer.valueOf(TEAMS_BETWEEN_LOGOS));
      page.setAttribute("allTeams", allTeams);
      page.setAttribute("teamHeaderColor", teamHeaderColor);
    } catch (final SQLException e) {
      throw new FLLInternalException("Error talking to the database", e);
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Error mapping data to JSON", e);
    }

  }

  /**
   * Colors for award groups.
   *
   * @param awardGroup the name of the award group, used to recognize regularly
   *          used award groups
   * @param index the award group index
   * @return color in a format suitable for use in an HTML document
   */
  public static String getColorForAwardGroup(final String awardGroup,
                                             final int index) {
    if ("lakes".equals(awardGroup.toLowerCase())) {
      return "#800000"; // maroon
    } else if ("woods".equals(awardGroup.toLowerCase())) {
      return "#008000"; // green
    } else if ("prairie".equals(awardGroup.toLowerCase())) {
      return "#CC6600"; // orange
    } else if ("marsh".equals(awardGroup.toLowerCase())) {
      return "#FF00FF"; // magenta
    } else if ("rivers".equals(awardGroup.toLowerCase())) {
      return "#800080"; // purple
    } else if ("snow".equals(awardGroup.toLowerCase())) {
      return "#808080"; // grey
    } else {
      final int idx = index
          % 6;
      switch (idx) {
      case 0:
        return "#800000"; // maroon
      case 1:
        return "#008000"; // green
      case 2:
        return "#CC6600"; // orange
      case 3:
        return "#FF00FF"; // magenta
      case 4:
        return "#800080"; // purple
      case 5:
        return "#808080"; // grey
      default:
        throw new FLLInternalException("Internal error, cannot choose color");
      }
    }
  }

  /**
   * Get the URsponsor logo filenames relative to "/sponsor_logos".
   *
   * @return sorted sponsor logos list
   */
  private static List<String> getSponsorLogos(final ServletContext application) {
    final String imagePath = application.getRealPath("/sponsor_logos");

    final List<String> logoFiles = Utilities.getGraphicFiles(new File(imagePath));

    return logoFiles;
  }

}
