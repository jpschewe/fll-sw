/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.diffplug.common.base.Errors;

import fll.Tournament;
import fll.TournamentLevel;
import fll.db.AwardsScript;
import fll.db.AwardsScript.Layer;
import fll.db.CategoriesIgnored;
import fll.db.GenerateDB;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for edit-awards-script.jsp.
 */
@WebServlet("/report/awards/EditAwardsScript")
public class EditAwardsScript extends BaseFLLServlet {

  /**
   * @param request read the parameters passed in
   * @param application read application variables
   * @param page set page variables
   */
  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext page) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {

      final TournamentLevel tournamentLevel;
      final Tournament tournament;
      final @Nullable String levelId = request.getParameter("level");
      final @Nullable String tournamentId = request.getParameter("tournament");
      if (null != levelId
          && null != tournamentId) {
        throw new FLLRuntimeException("Cannot specify both a tournament id and a tournament level id");
      } else if (null != levelId) {
        final int id = Integer.parseInt(levelId);
        tournamentLevel = TournamentLevel.getById(connection, id);
        tournament = Tournament.findTournamentByID(connection, GenerateDB.INTERNAL_TOURNAMENT_ID);
        page.setAttribute("descriptionText", String.format("tournament level %s", tournamentLevel.getName()));
        page.setAttribute("tournamentLevel", tournamentLevel);
      } else if (null != tournamentId) {
        final int id = Integer.parseInt(tournamentId);
        tournament = Tournament.findTournamentByID(connection, id);
        tournamentLevel = tournament.getLevel();
        page.setAttribute("descriptionText",
                          String.format("tournament %s - %s", tournament.getName(), tournament.getDescription()));
        page.setAttribute("tournament", tournament);
      } else {
        page.setAttribute("descriptionText", "season");
        tournament = Tournament.findTournamentByID(connection, GenerateDB.INTERNAL_TOURNAMENT_ID);
        tournamentLevel = TournamentLevel.getById(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
      }

      final AwardsScript.Layer layer = determineAwardsScriptLayer(tournamentLevel, tournament);

      page.setAttribute("tournamentLevel", tournamentLevel);
      page.setAttribute("tournament", tournament);

      page.setAttribute("subjectiveCategories", description.getSubjectiveCategories());

      // FIXME populate 8/31/21 pending answer from Jeannie
      final Map<SubjectiveScoreCategory, String> subjectiveCategoryText = new HashMap<>();
      page.setAttribute("subjectiveCategoryText", subjectiveCategoryText);
      final Map<SubjectiveScoreCategory, Boolean> subjectiveCategorySpecified = new HashMap<>();
      page.setAttribute("subjectiveCategorySpecified", subjectiveCategorySpecified);

      final List<NonNumericCategory> nonNumericCategories;
      if (null != tournamentLevel) {
        nonNumericCategories = description.getNonNumericCategories().stream() //
                                          .filter(Errors.rethrow()
                                                        .wrapPredicate(c -> !CategoriesIgnored.isNonNumericCategoryIgnored(connection,
                                                                                                                           tournamentLevel,
                                                                                                                           c))) //
                                          .collect(Collectors.toList());
      } else {
        nonNumericCategories = description.getNonNumericCategories();
      }
      page.setAttribute("nonNumericCategories", nonNumericCategories);

      // FIXME populate 8/31/21 pending answer from Jeannie
      final Map<NonNumericCategory, String> nonNumericCategoryText = new HashMap<>();
      page.setAttribute("nonNumericCategoryText", nonNumericCategoryText);
      final Map<NonNumericCategory, Boolean> nonNumericCategorySpecified = new HashMap<>();
      page.setAttribute("nonNumericCategorySpecified", nonNumericCategorySpecified);

      loadMacroInformation(page, connection, tournamentLevel, tournament, layer);

      loadSectionInformation(page, connection, tournamentLevel, tournament, layer);

    } catch (final SQLException e) {
      throw new FLLInternalException("Error getting values for editing awards script", e);
    }
  }

  private static void loadSectionInformation(final PageContext page,
                                             Connection connection,
                                             final TournamentLevel tournamentLevel,
                                             final Tournament tournament,
                                             final AwardsScript.Layer layer)
      throws SQLException {
    page.setAttribute("sections", AwardsScript.Section.values());
    Arrays.stream(AwardsScript.Section.values()).forEach(s -> page.setAttribute(s.name(), s));

    final Map<AwardsScript.Section, Boolean> sectionSpecified = new HashMap<>();
    final Map<AwardsScript.Section, String> sectionText = new HashMap<>();
    for (final AwardsScript.Section section : AwardsScript.Section.values()) {
      final boolean specified;
      String text;
      switch (layer) {
      case SEASON:
        specified = AwardsScript.isSectionSpecifiedForSeason(connection, section);
        text = AwardsScript.getSectionTextForSeason(connection, section);
        break;
      case TOURNAMENT:
        specified = AwardsScript.isSectionSpecifiedForTournament(connection, tournament, section);
        text = AwardsScript.getSectionTextForTournament(connection, tournament, section);
        break;
      case TOURNAMENT_LEVEL:
        specified = AwardsScript.isSectionSpecifiedForTournamentLevel(connection, tournamentLevel, section);
        text = AwardsScript.getSectionTextForTournamentLevel(connection, tournamentLevel, section);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }

      if (!specified) {
        // display the value that would be used to the user
        text = AwardsScript.getSectionText(connection, tournament, section);
      }
      sectionText.put(section, text);
      sectionSpecified.put(section, specified);
    }
    page.setAttribute("sectionSpecified", sectionSpecified);
    page.setAttribute("sectionText", sectionText);
  }

  private static void loadMacroInformation(final PageContext page,
                                           final Connection connection,
                                           final TournamentLevel tournamentLevel,
                                           final Tournament tournament,
                                           final AwardsScript.Layer layer)
      throws SQLException {
    page.setAttribute("macros", AwardsScript.Macro.values());
    Arrays.stream(AwardsScript.Macro.values()).forEach(s -> page.setAttribute(s.name(), s));

    // Look for all macro values in the database. Not all macro values are in the
    // database, but this is OK as the page won't check these maps for macros that
    // aren't in the database and the lookup functions handle the missing values
    final Map<AwardsScript.Macro, Boolean> macroSpecified = new HashMap<>();
    final Map<AwardsScript.Macro, String> macroValue = new HashMap<>();
    for (final AwardsScript.Macro macro : AwardsScript.Macro.values()) {
      switch (layer) {
      case SEASON:
        macroSpecified.put(macro, AwardsScript.isMacroSpecifiedForSeason(connection, macro));
        macroValue.put(macro, AwardsScript.getMacroValueForSeason(connection, macro));
        break;
      case TOURNAMENT:
        macroSpecified.put(macro, AwardsScript.isMacroSpecifiedForTournament(connection, tournament, macro));
        macroValue.put(macro, AwardsScript.getMacroValueForTournament(connection, tournament, macro));
        break;
      case TOURNAMENT_LEVEL:
        macroSpecified.put(macro, AwardsScript.isMacroSpecifiedForTournamentLevel(connection, tournamentLevel, macro));
        macroValue.put(macro, AwardsScript.getMacroValueForTournamentLevel(connection, tournamentLevel, macro));
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }
    }
    page.setAttribute("macroSpecified", macroSpecified);
    page.setAttribute("macroValue", macroValue);
  }

  /**
   * Determine which awards script layer is being dealt with based on the
   * tournament level and tournament information.
   * 
   * @param tournamentLevel the tournament level
   * @param tournament the tournament
   * @return the awards script layer
   */
  private static Layer determineAwardsScriptLayer(final TournamentLevel tournamentLevel,
                                                  final Tournament tournament) {
    final AwardsScript.Layer layer;
    if (tournamentLevel.getId() == GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID
        && tournament.getTournamentID() == GenerateDB.INTERNAL_TOURNAMENT_ID) {
      layer = AwardsScript.Layer.SEASON;
    } else if (tournament.getTournamentID() == GenerateDB.INTERNAL_TOURNAMENT_ID) {
      layer = AwardsScript.Layer.TOURNAMENT_LEVEL;
    } else {
      layer = AwardsScript.Layer.TOURNAMENT;
    }
    return layer;
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final int tournamentLevelId = WebUtils.getIntRequestParameter(request, "level");
      final TournamentLevel tournamentLevel = TournamentLevel.getById(connection, tournamentLevelId);

      final int tournamentId = WebUtils.getIntRequestParameter(request, "tournament");
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);

      final AwardsScript.Layer layer = determineAwardsScriptLayer(tournamentLevel, tournament);

      storeSectionText(request, connection, tournamentLevel, tournament, layer);

      storeMacroValues(request, connection, tournamentLevel, tournament, layer);

      final String layerText;
      switch (layer) {
      case SEASON:
        layerText = "the season";
        break;
      case TOURNAMENT:
        layerText = String.format("tournament %s", tournament.getName());
        break;
      case TOURNAMENT_LEVEL:
        layerText = String.format("tournament level %s", tournamentLevel.getName());
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }

      SessionAttributes.appendToMessage(session,
                                        String.format("<div class='success'>Saved changes to awards script for %s</div>",
                                                      layerText));
      response.sendRedirect("index.jsp");
    } catch (final SQLException e) {
      throw new FLLInternalException("Error storing values for award script", e);
    }

  }

  private void storeSectionText(final HttpServletRequest request,
                                Connection connection,
                                final TournamentLevel tournamentLevel,
                                final Tournament tournament,
                                final AwardsScript.Layer layer)
      throws SQLException {
    for (final AwardsScript.Section section : AwardsScript.Section.values()) {
      final @Nullable String specifiedStr = request.getParameter(String.format("%s_specified",
                                                                               section.getIdentifier()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request,
                                                                String.format("%s_text", section.getIdentifier()));

        switch (layer) {
        case SEASON:
          AwardsScript.updateSectionTextForSeason(connection, section, text);
          break;
        case TOURNAMENT:
          AwardsScript.updateSectionTextForTournament(connection, tournament, section, text);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.updateSectionTextForTournamentLevel(connection, tournamentLevel, section, text);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);

        }
      } else {
        switch (layer) {
        case SEASON:
          AwardsScript.clearSectionTextForSeason(connection, section);
          break;
        case TOURNAMENT:
          AwardsScript.clearSectionTextForTournament(connection, tournament, section);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.clearSectionTextForTournamentLevel(connection, tournamentLevel, section);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
    }
  }

  private void storeMacroValues(final HttpServletRequest request,
                                Connection connection,
                                final TournamentLevel tournamentLevel,
                                final Tournament tournament,
                                final AwardsScript.Layer layer)
      throws SQLException {
    for (final AwardsScript.Macro macro : AwardsScript.Macro.values()) {
      final @Nullable String specifiedStr = request.getParameter(String.format("%s_specified", macro.getText()));
      if (null != specifiedStr) {
        final String value = WebUtils.getNonNullRequestParameter(request, String.format("%s_value", macro.getText()));

        switch (layer) {
        case SEASON:
          AwardsScript.updateMacroValueForSeason(connection, macro, value);
          break;
        case TOURNAMENT:
          AwardsScript.updateMacroValueForTournament(connection, tournament, macro, value);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.updateMacroValueForTournamentLevel(connection, tournamentLevel, macro, value);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);

        }
      } else {
        switch (layer) {
        case SEASON:
          AwardsScript.clearMacroValueForSeason(connection, macro);
          break;
        case TOURNAMENT:
          AwardsScript.clearMacroValueForTournament(connection, tournament, macro);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.clearMacroValueForTournamentLevel(connection, tournamentLevel, macro);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
    }
  }

}
