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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
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
import fll.xml.Category;
import fll.xml.ChallengeDescription;
import fll.xml.ChampionshipCategory;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
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

      loadSubjectiveCategoryInfo(page, description, connection, tournamentLevel, tournament, layer);

      loadNonNumericCategoryInfo(page, description, connection, tournamentLevel, tournament, layer);

      loadMacroInformation(page, connection, tournamentLevel, tournament, layer);

      loadSectionInformation(page, connection, tournamentLevel, tournament, layer);

      loadSponsors(page, connection, tournamentLevel, tournament, layer);

      loadAwardOrder(description, page, connection, tournamentLevel, tournament, layer);

      loadNumPerformanceAwards(page, connection, tournamentLevel, tournament, layer);

      page.setAttribute("championshipAwardTitle", ChampionshipCategory.CHAMPIONSHIP_AWARD_TITLE);
      page.setAttribute("performanceAwardTitle", PerformanceScoreCategory.CATEGORY_TITLE);

    } catch (final SQLException e) {
      throw new FLLInternalException("Error getting values for editing awards script", e);
    }
  }

  private static List<NonNumericCategory> getNonNumericCategories(final ChallengeDescription description,
                                                                  final Connection connection,
                                                                  final TournamentLevel tournamentLevel) {
    if (null != tournamentLevel) {
      return description.getNonNumericCategories().stream() //
                        .filter(Errors.rethrow()
                                      .wrapPredicate(c -> !CategoriesIgnored.isNonNumericCategoryIgnored(connection,
                                                                                                         tournamentLevel,
                                                                                                         c))) //
                        .collect(Collectors.toList());
    } else {
      return description.getNonNumericCategories();
    }
  }

  private static List<SubjectiveScoreCategory> getSubjectiveCategories(final ChallengeDescription description) {
    // extra method in case we later ignore some categories like the non-numeric
    // categories
    return description.getSubjectiveCategories();
  }

  private static void loadSubjectiveCategoryInfo(final PageContext page,
                                                 final ChallengeDescription description,
                                                 final Connection connection,
                                                 final TournamentLevel tournamentLevel,
                                                 final Tournament tournament,
                                                 final AwardsScript.Layer layer)
      throws SQLException {
    final List<SubjectiveScoreCategory> subjectiveCategories = getSubjectiveCategories(description);
    page.setAttribute("subjectiveCategories", subjectiveCategories);

    final Map<SubjectiveScoreCategory, String> subjectiveCategoryText = new HashMap<>();
    final Map<SubjectiveScoreCategory, Boolean> subjectiveCategorySpecified = new HashMap<>();
    final Map<SubjectiveScoreCategory, String> subjectiveCategoryPresenter = new HashMap<>();
    final Map<SubjectiveScoreCategory, Boolean> subjectiveCategoryPresenterSpecified = new HashMap<>();

    for (final SubjectiveScoreCategory category : subjectiveCategories) {
      final boolean textSpecified;
      String text;
      final boolean presenterSpecified;
      String presenter;
      switch (layer) {
      case SEASON:
        textSpecified = AwardsScript.isCategoryTextSpecifiedForSeason(connection, category);
        text = AwardsScript.getCategoryTextForSeason(connection, category);
        presenterSpecified = AwardsScript.isPresenterSpecifiedForSeason(connection, category);
        presenter = AwardsScript.getPresenterForSeason(connection, category);
        break;
      case TOURNAMENT:
        textSpecified = AwardsScript.isCategoryTextSpecifiedForTournament(connection, tournament, category);
        text = AwardsScript.getCategoryTextForTournament(connection, tournament, category);
        presenterSpecified = AwardsScript.isPresenterSpecifiedForTournament(connection, tournament, category);
        presenter = AwardsScript.getPresenterForTournament(connection, tournament, category);
        break;
      case TOURNAMENT_LEVEL:
        textSpecified = AwardsScript.isCategoryTextSpecifiedForTournamentLevel(connection, tournamentLevel, category);
        text = AwardsScript.getCategoryTextForTournamentLevel(connection, tournamentLevel, category);
        presenterSpecified = AwardsScript.isPresenterSpecifiedForTournamentLevel(connection, tournamentLevel, category);
        presenter = AwardsScript.getPresenterForTournamentLevel(connection, tournamentLevel, category);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }

      if (!textSpecified) {
        // display the value that would be used to the user
        text = AwardsScript.getCategoryText(connection, tournament, category);
      }
      if (!presenterSpecified) {
        // display the value that would be used to the user
        presenter = AwardsScript.getPresenter(connection, tournament, category);
      }

      subjectiveCategoryText.put(category, text);
      subjectiveCategorySpecified.put(category, textSpecified);
      subjectiveCategoryPresenter.put(category, presenter);
      subjectiveCategoryPresenterSpecified.put(category, presenterSpecified);
    }

    page.setAttribute("subjectiveCategoryText", subjectiveCategoryText);
    page.setAttribute("subjectiveCategorySpecified", subjectiveCategorySpecified);
    page.setAttribute("subjectiveCategoryPresenter", subjectiveCategoryPresenter);
    page.setAttribute("subjectiveCategoryPresenterSpecified", subjectiveCategoryPresenterSpecified);
  }

  private static void loadNonNumericCategoryInfo(final PageContext page,
                                                 final ChallengeDescription description,
                                                 final Connection connection,
                                                 final TournamentLevel tournamentLevel,
                                                 final Tournament tournament,
                                                 final AwardsScript.Layer layer)
      throws SQLException {
    final List<NonNumericCategory> nonNumericCategories = getNonNumericCategories(description, connection,
                                                                                  tournamentLevel);
    page.setAttribute("nonNumericCategories", nonNumericCategories);

    final Map<NonNumericCategory, String> nonNumericCategoryText = new HashMap<>();
    final Map<NonNumericCategory, Boolean> nonNumericCategorySpecified = new HashMap<>();
    final Map<NonNumericCategory, String> nonNumericCategoryPresenter = new HashMap<>();
    final Map<NonNumericCategory, Boolean> nonNumericCategoryPresenterSpecified = new HashMap<>();

    for (final NonNumericCategory category : nonNumericCategories) {
      final boolean textSpecified;
      String text;
      final boolean presenterSpecified;
      String presenter;
      switch (layer) {
      case SEASON:
        textSpecified = AwardsScript.isCategoryTextSpecifiedForSeason(connection, category);
        text = AwardsScript.getCategoryTextForSeason(connection, category);
        presenterSpecified = AwardsScript.isPresenterSpecifiedForSeason(connection, category);
        presenter = AwardsScript.getPresenterForSeason(connection, category);
        break;
      case TOURNAMENT:
        textSpecified = AwardsScript.isCategoryTextSpecifiedForTournament(connection, tournament, category);
        text = AwardsScript.getCategoryTextForTournament(connection, tournament, category);
        presenterSpecified = AwardsScript.isPresenterSpecifiedForTournament(connection, tournament, category);
        presenter = AwardsScript.getPresenterForTournament(connection, tournament, category);
        break;
      case TOURNAMENT_LEVEL:
        textSpecified = AwardsScript.isCategoryTextSpecifiedForTournamentLevel(connection, tournamentLevel, category);
        text = AwardsScript.getCategoryTextForTournamentLevel(connection, tournamentLevel, category);
        presenterSpecified = AwardsScript.isPresenterSpecifiedForTournamentLevel(connection, tournamentLevel, category);
        presenter = AwardsScript.getPresenterForTournamentLevel(connection, tournamentLevel, category);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }

      if (!textSpecified) {
        // display the value that would be used to the user
        text = AwardsScript.getCategoryText(connection, tournament, category);
      }
      if (!presenterSpecified) {
        // display the value that would be used to the user
        presenter = AwardsScript.getPresenter(connection, tournament, category);
      }

      nonNumericCategoryText.put(category, text);
      nonNumericCategorySpecified.put(category, textSpecified);
      nonNumericCategoryPresenter.put(category, presenter);
      nonNumericCategoryPresenterSpecified.put(category, presenterSpecified);
    }

    page.setAttribute("nonNumericCategoryText", nonNumericCategoryText);
    page.setAttribute("nonNumericCategorySpecified", nonNumericCategorySpecified);
    page.setAttribute("nonNumericCategoryPresenter", nonNumericCategoryPresenter);
    page.setAttribute("nonNumericCategoryPresenterSpecified", nonNumericCategoryPresenterSpecified);
  }

  private static void loadSectionInformation(final PageContext page,
                                             final Connection connection,
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
      final boolean specified;
      String value;

      switch (layer) {
      case SEASON:
        specified = AwardsScript.isMacroSpecifiedForSeason(connection, macro);
        value = AwardsScript.getMacroValueForSeason(connection, macro);
        break;
      case TOURNAMENT:
        specified = AwardsScript.isMacroSpecifiedForTournament(connection, tournament, macro);
        value = AwardsScript.getMacroValueForTournament(connection, tournament, macro);
        break;
      case TOURNAMENT_LEVEL:
        specified = AwardsScript.isMacroSpecifiedForTournamentLevel(connection, tournamentLevel, macro);
        value = AwardsScript.getMacroValueForTournamentLevel(connection, tournamentLevel, macro);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }

      if (!specified) {
        value = AwardsScript.getMacroValue(connection, tournament, macro);
      }

      macroSpecified.put(macro, specified);
      macroValue.put(macro, value);

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

      storeSubjectiveCategoryText(request, description, connection, tournamentLevel, tournament, layer);
      storeSubjectiveCategoryPresenters(request, description, connection, tournamentLevel, tournament, layer);

      storeNonNumericCategoryText(request, description, connection, tournamentLevel, tournament, layer);
      storeNonNumericCategoryPresenters(request, description, connection, tournamentLevel, tournament, layer);

      storeMacroValues(request, connection, tournamentLevel, tournament, layer);

      storeSponsors(request, connection, tournamentLevel, tournament, layer);

      storeAwardOrder(description, request, connection, tournamentLevel, tournament, layer);

      storeNumPerformanceAwards(request, connection, tournamentLevel, tournament, layer);

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
                                final Connection connection,
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
                                final Connection connection,
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

  private void storeSubjectiveCategoryText(final HttpServletRequest request,
                                           final ChallengeDescription description,
                                           final Connection connection,
                                           final TournamentLevel tournamentLevel,
                                           final Tournament tournament,
                                           final AwardsScript.Layer layer)
      throws SQLException {
    final List<SubjectiveScoreCategory> categories = getSubjectiveCategories(description);

    for (final SubjectiveScoreCategory category : categories) {
      final @Nullable String specifiedStr = request.getParameter(String.format("category_%s_specified",
                                                                               category.getName()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request,
                                                                String.format("category_%s_text", category.getName()));

        switch (layer) {
        case SEASON:
          AwardsScript.updateCategoryTextForSeason(connection, category, text);
          break;
        case TOURNAMENT:
          AwardsScript.updateCategoryTextForTournament(connection, tournament, category, text);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.updateCategoryTextForTournamentLevel(connection, tournamentLevel, category, text);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);

        }
      } else {
        switch (layer) {
        case SEASON:
          AwardsScript.clearCategoryTextForSeason(connection, category);
          break;
        case TOURNAMENT:
          AwardsScript.clearCategoryTextForTournament(connection, tournament, category);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.clearCategoryTextForTournamentLevel(connection, tournamentLevel, category);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
    }
  }

  private void storeNonNumericCategoryText(final HttpServletRequest request,
                                           final ChallengeDescription description,
                                           final Connection connection,
                                           final TournamentLevel tournamentLevel,
                                           final Tournament tournament,
                                           final AwardsScript.Layer layer)
      throws SQLException {
    final List<NonNumericCategory> categories = getNonNumericCategories(description, connection, tournamentLevel);

    for (final NonNumericCategory category : categories) {
      final @Nullable String specifiedStr = request.getParameter(String.format("category_%s_specified",
                                                                               category.getTitle()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request,
                                                                String.format("category_%s_text", category.getTitle()));

        switch (layer) {
        case SEASON:
          AwardsScript.updateCategoryTextForSeason(connection, category, text);
          break;
        case TOURNAMENT:
          AwardsScript.updateCategoryTextForTournament(connection, tournament, category, text);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.updateCategoryTextForTournamentLevel(connection, tournamentLevel, category, text);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);

        }
      } else {
        switch (layer) {
        case SEASON:
          AwardsScript.clearCategoryTextForSeason(connection, category);
          break;
        case TOURNAMENT:
          AwardsScript.clearCategoryTextForTournament(connection, tournament, category);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.clearCategoryTextForTournamentLevel(connection, tournamentLevel, category);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
    }
  }

  private void storeSubjectiveCategoryPresenters(final HttpServletRequest request,
                                                 final ChallengeDescription description,
                                                 final Connection connection,
                                                 final TournamentLevel tournamentLevel,
                                                 final Tournament tournament,
                                                 final AwardsScript.Layer layer)
      throws SQLException {
    final List<SubjectiveScoreCategory> categories = getSubjectiveCategories(description);

    for (final SubjectiveScoreCategory category : categories) {
      final @Nullable String specifiedStr = request.getParameter(String.format("category_%s_presenter_specified",
                                                                               category.getName()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request, String.format("category_%s_presenter_text",
                                                                                       category.getName()));

        switch (layer) {
        case SEASON:
          AwardsScript.updatePresenterForSeason(connection, category, text);
          break;
        case TOURNAMENT:
          AwardsScript.updatePresenterForTournament(connection, tournament, category, text);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.updatePresenterForTournamentLevel(connection, tournamentLevel, category, text);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);

        }
      } else {
        switch (layer) {
        case SEASON:
          AwardsScript.clearPresenterForSeason(connection, category);
          break;
        case TOURNAMENT:
          AwardsScript.clearPresenterForTournament(connection, tournament, category);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.clearPresenterForTournamentLevel(connection, tournamentLevel, category);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
    }
  }

  private void storeNonNumericCategoryPresenters(final HttpServletRequest request,
                                                 final ChallengeDescription description,
                                                 final Connection connection,
                                                 final TournamentLevel tournamentLevel,
                                                 final Tournament tournament,
                                                 final AwardsScript.Layer layer)
      throws SQLException {
    final List<NonNumericCategory> categories = getNonNumericCategories(description, connection, tournamentLevel);

    for (final NonNumericCategory category : categories) {
      final @Nullable String specifiedStr = request.getParameter(String.format("category_%s_presenter_specified",
                                                                               category.getTitle()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request, String.format("category_%s_presenter_text",
                                                                                       category.getTitle()));

        switch (layer) {
        case SEASON:
          AwardsScript.updatePresenterForSeason(connection, category, text);
          break;
        case TOURNAMENT:
          AwardsScript.updatePresenterForTournament(connection, tournament, category, text);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.updatePresenterForTournamentLevel(connection, tournamentLevel, category, text);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);

        }
      } else {
        switch (layer) {
        case SEASON:
          AwardsScript.clearPresenterForSeason(connection, category);
          break;
        case TOURNAMENT:
          AwardsScript.clearPresenterForTournament(connection, tournament, category);
          break;
        case TOURNAMENT_LEVEL:
          AwardsScript.clearPresenterForTournamentLevel(connection, tournamentLevel, category);
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
    }
  }

  private static void loadSponsors(final PageContext page,
                                   final Connection connection,
                                   final TournamentLevel tournamentLevel,
                                   final Tournament tournament,
                                   final AwardsScript.Layer layer)
      throws SQLException {

    final boolean sponsorsSpecified;
    List<String> sponsors;
    switch (layer) {
    case SEASON:
      sponsorsSpecified = AwardsScript.isSponsorsSpecifiedForSeason(connection);
      sponsors = AwardsScript.getSponsorsForSeason(connection);
      break;
    case TOURNAMENT:
      sponsorsSpecified = AwardsScript.isSponsorsSpecifiedForTournament(connection, tournament);
      sponsors = AwardsScript.getSponsorsForTournament(connection, tournament);
      break;
    case TOURNAMENT_LEVEL:
      sponsorsSpecified = AwardsScript.isSponsorsSpecifiedForTournamentLevel(connection, tournamentLevel);
      sponsors = AwardsScript.getSponsorsForTournamentLevel(connection, tournamentLevel);
      break;
    default:
      throw new FLLInternalException("Unknown awards script layer: "
          + layer);
    }

    if (!sponsorsSpecified) {
      // display the value that would be used to the user
      sponsors = AwardsScript.getSponsors(connection, tournament);
    }

    page.setAttribute("sponsorsSpecified", sponsorsSpecified);
    page.setAttribute("sponsors", sponsors);
  }

  private static final String SPONSOR_PARAMETER_PREFIX = "sponsor_";

  private static List<String> findSponsors(final HttpServletRequest request) {

    // Find all request parameters that start with the right prefix and parse them
    // into integers.
    // store the values in a SortedMap so that walking the map sorts the sponsor
    // names by rank.
    final SortedMap<Integer, String> sponsorMap = new TreeMap<>();
    for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      final String key = entry.getKey();
      if (key.startsWith(SPONSOR_PARAMETER_PREFIX)) {
        final String value = entry.getValue()[0];
        if (!StringUtils.isBlank(value)) {
          final String rankStr = key.substring(SPONSOR_PARAMETER_PREFIX.length(), key.length());
          try {
            final Integer rank = Integer.valueOf(rankStr);
            sponsorMap.put(rank, value);
          } catch (final NumberFormatException nfe) {
            throw new FLLInternalException(String.format("Got error parsing sponsor rank from '%s', skipping", key));
          }
        }
      }
    }

    final List<String> sponsors = new LinkedList<>(sponsorMap.values());
    return sponsors;
  }

  private static void storeSponsors(final HttpServletRequest request,
                                    final Connection connection,
                                    final TournamentLevel tournamentLevel,
                                    final Tournament tournament,
                                    final AwardsScript.Layer layer)
      throws SQLException {

    final @Nullable String specifiedStr = request.getParameter(String.format("sponsors_specified"));
    if (null != specifiedStr) {
      final List<String> sponsors = findSponsors(request);

      switch (layer) {
      case SEASON:
        AwardsScript.updateSponsorsForSeason(connection, sponsors);
        break;
      case TOURNAMENT:
        AwardsScript.updateSponsorsForTournament(connection, tournament, sponsors);
        break;
      case TOURNAMENT_LEVEL:
        AwardsScript.updateSponsorsForTournamentLevel(connection, tournamentLevel, sponsors);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);

      }
    } else {
      switch (layer) {
      case SEASON:
        AwardsScript.clearSponsorsForSeason(connection);
        break;
      case TOURNAMENT:
        AwardsScript.clearSponsorsForTournament(connection, tournament);
        break;
      case TOURNAMENT_LEVEL:
        AwardsScript.clearSponsorsForTournamentLevel(connection, tournamentLevel);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }
    }
  }

  private static void loadNumPerformanceAwards(final PageContext page,
                                               final Connection connection,
                                               final TournamentLevel tournamentLevel,
                                               final Tournament tournament,
                                               final AwardsScript.Layer layer)
      throws SQLException {

    final boolean specified;
    int value;
    switch (layer) {
    case SEASON:
      specified = AwardsScript.isNumPerformanceAwardsSpecifiedForSeason(connection);
      value = AwardsScript.getNumPerformanceAwardsForSeason(connection);
      break;
    case TOURNAMENT:
      specified = AwardsScript.isNumPerformanceAwardsSpecifiedForTournament(connection, tournament);
      value = AwardsScript.getNumPerformanceAwardsForTournament(connection, tournament);
      break;
    case TOURNAMENT_LEVEL:
      specified = AwardsScript.isNumPerformanceAwardsSpecifiedForTournamentLevel(connection, tournamentLevel);
      value = AwardsScript.getNumPerformanceAwardsForTournamentLevel(connection, tournamentLevel);
      break;
    default:
      throw new FLLInternalException("Unknown awards script layer: "
          + layer);
    }

    if (!specified) {
      // display the value that would be used to the user
      value = AwardsScript.getNumPerformanceAwards(connection, tournament);
    }

    page.setAttribute("numPerformanceAwardsSpecified", specified);
    page.setAttribute("numPerformanceAwardsValue", value);
  }

  private static void loadAwardOrder(final ChallengeDescription description,
                                     final PageContext page,
                                     final Connection connection,
                                     final TournamentLevel tournamentLevel,
                                     final Tournament tournament,
                                     final AwardsScript.Layer layer)
      throws SQLException {

    final boolean awardOrderSpecified;
    List<Category> awardOrder;
    switch (layer) {
    case SEASON:
      awardOrderSpecified = AwardsScript.isAwardOrderSpecifiedForSeason(connection);
      awardOrder = AwardsScript.getAwardOrderForSeason(description, connection);
      break;
    case TOURNAMENT:
      awardOrderSpecified = AwardsScript.isAwardOrderSpecifiedForTournament(connection, tournament);
      awardOrder = AwardsScript.getAwardOrderForTournament(description, connection, tournament);
      break;
    case TOURNAMENT_LEVEL:
      awardOrderSpecified = AwardsScript.isAwardOrderSpecifiedForTournamentLevel(connection, tournamentLevel);
      awardOrder = AwardsScript.getAwardOrderForTournamentLevel(description, connection, tournamentLevel);
      break;
    default:
      throw new FLLInternalException("Unknown awards script layer: "
          + layer);
    }

    if (!awardOrderSpecified) {
      // display the value that would be used to the user
      awardOrder = AwardsScript.getAwardOrder(description, connection, tournament);
    }

    page.setAttribute("awardOrderSpecified", awardOrderSpecified);
    page.setAttribute("awardOrder", awardOrder);
  }

  private static final String AWARD_ORDER_PARAMETER_PREFIX = "award_order_";

  private static List<Category> findAwardOrder(final ChallengeDescription description,
                                               final HttpServletRequest request) {

    // Find all request parameters that start with the right prefix and parse them
    // into integers.
    // store the values in a SortedMap so that walking the map sorts the sponsor
    // names by rank.
    final SortedMap<Integer, String> sponsorMap = new TreeMap<>();
    for (final Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      final String key = entry.getKey();
      if (key.startsWith(AWARD_ORDER_PARAMETER_PREFIX)) {
        final String value = entry.getValue()[0];
        if (!StringUtils.isBlank(value)) {
          final String rankStr = key.substring(AWARD_ORDER_PARAMETER_PREFIX.length(), key.length());
          try {
            final Integer rank = Integer.valueOf(rankStr);
            sponsorMap.put(rank, value);
          } catch (final NumberFormatException nfe) {
            throw new FLLInternalException(String.format("Got error parsing award order rank from '%s', skipping",
                                                         key));
          }
        }
      }
    }

    final List<Category> awardOrder = sponsorMap.values().stream()
                                                .map(s -> AwardsScript.getCategoryByTitle(description, s))
                                                .collect(Collectors.toList());
    return awardOrder;
  }

  private static void storeAwardOrder(final ChallengeDescription description,
                                      final HttpServletRequest request,
                                      final Connection connection,
                                      final TournamentLevel tournamentLevel,
                                      final Tournament tournament,
                                      final AwardsScript.Layer layer)
      throws SQLException {

    final @Nullable String specifiedStr = request.getParameter(String.format("awardOrder_specified"));
    if (null != specifiedStr) {
      final List<Category> awardOrder = findAwardOrder(description, request);

      switch (layer) {
      case SEASON:
        AwardsScript.updateAwardOrderForSeason(connection, awardOrder);
        break;
      case TOURNAMENT:
        AwardsScript.updateAwardOrderForTournament(connection, tournament, awardOrder);
        break;
      case TOURNAMENT_LEVEL:
        AwardsScript.updateAwardOrderForTournamentLevel(connection, tournamentLevel, awardOrder);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);

      }
    } else {
      switch (layer) {
      case SEASON:
        AwardsScript.clearAwardOrderForSeason(connection);
        break;
      case TOURNAMENT:
        AwardsScript.clearAwardOrderForTournament(connection, tournament);
        break;
      case TOURNAMENT_LEVEL:
        AwardsScript.clearAwardOrderForTournamentLevel(connection, tournamentLevel);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }
    }
  }

  private static void storeNumPerformanceAwards(final HttpServletRequest request,
                                                final Connection connection,
                                                final TournamentLevel tournamentLevel,
                                                final Tournament tournament,
                                                final AwardsScript.Layer layer)
      throws SQLException {

    final @Nullable String specifiedStr = request.getParameter(String.format("num_performance_awards_specified"));
    if (null != specifiedStr) {
      final int value = WebUtils.getIntRequestParameter(request, "num_performance_awards_value");

      switch (layer) {
      case SEASON:
        AwardsScript.updateNumPerformanceAwardsForSeason(connection, value);
        break;
      case TOURNAMENT:
        AwardsScript.updateNumPerformanceAwardsForTournament(connection, tournament, value);
        break;
      case TOURNAMENT_LEVEL:
        AwardsScript.updateNumPerformanceAwardsForTournamentLevel(connection, tournamentLevel, value);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }
    } else {
      switch (layer) {
      case SEASON:
        AwardsScript.clearNumPerformanceAwardsForSeason(connection);
        break;
      case TOURNAMENT:
        AwardsScript.clearNumPerformanceAwardsForTournament(connection, tournament);
        break;
      case TOURNAMENT_LEVEL:
        AwardsScript.clearNumPerformanceAwardsForTournamentLevel(connection, tournamentLevel);
        break;
      default:
        throw new FLLInternalException("Unknown awards script layer: "
            + layer);
      }
    }
  }

}
