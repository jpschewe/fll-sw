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
import fll.db.CategoriesIgnored;
import fll.db.GenerateDB;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
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

      final AwardsScript.Layer layer;
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
        layer = AwardsScript.Layer.TOURNAMENT_LEVEL;
      } else if (null != tournamentId) {
        final int id = Integer.parseInt(tournamentId);
        tournament = Tournament.findTournamentByID(connection, id);
        tournamentLevel = tournament.getLevel();
        page.setAttribute("descriptionText",
                          String.format("tournament %s - %s", tournament.getName(), tournament.getDescription()));
        page.setAttribute("tournament", tournament);
        layer = AwardsScript.Layer.TOURNAMENT;
      } else {
        page.setAttribute("descriptionText", "season");
        tournament = Tournament.findTournamentByID(connection, GenerateDB.INTERNAL_TOURNAMENT_ID);
        tournamentLevel = TournamentLevel.getById(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
        layer = AwardsScript.Layer.SEASON;
      }
      page.setAttribute("tournamentLevel", tournamentLevel);
      page.setAttribute("tournament", tournament);

      page.setAttribute("subjectiveCategories", description.getSubjectiveCategories());

      // FIXME populate
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

      // FIXME populate
      final Map<NonNumericCategory, String> nonNumericCategoryText = new HashMap<>();
      page.setAttribute("nonNumericCategoryText", nonNumericCategoryText);
      final Map<NonNumericCategory, Boolean> nonNumericCategorySpecified = new HashMap<>();
      page.setAttribute("nonNumericCategorySpecified", nonNumericCategorySpecified);

      // FIXME section names that need to have values populated in the script
      // sections:
      // front_matter
      // sponsors_intro
      // sponsors_recognition
      // volunteers
      // category_championship
      // category_performance
      // subjective category_${category.name}
      // non-numeric category_${category.title}
      // end_awards
      // footer
      // subjectiveCategoryText{}
      // nonNumericCategoryText{}

      // FIXME put default values into the GenerateDB code, possibly read text out of
      // files

      page.setAttribute("macros", AwardsScript.Macro.values());

      page.setAttribute("sections", AwardsScript.Section.values());
      Arrays.stream(AwardsScript.Section.values()).forEach(s -> page.setAttribute(s.name(), s));

      final Map<AwardsScript.Section, Boolean> sectionSpecified = new HashMap<>();
      final Map<AwardsScript.Section, String> sectionText = new HashMap<>();
      for (final AwardsScript.Section section : AwardsScript.Section.values()) {
        switch (layer) {
        case SEASON:
          sectionSpecified.put(section, AwardsScript.getSectionSpecifiedForSeason(connection, section));
          sectionText.put(section, AwardsScript.getSectionTextForSeason(connection, section));
          break;
        case TOURNAMENT:
          sectionSpecified.put(section, AwardsScript.getSectionSpecifiedForTournament(connection, tournament, section));
          sectionText.put(section, AwardsScript.getSectionTextForTournament(connection, tournament, section));
          break;
        case TOURNAMENT_LEVEL:
          sectionSpecified.put(section, AwardsScript.getSectionSpecifiedForTournamentLevel(connection, tournamentLevel,
                                                                                           section));
          sectionText.put(section, AwardsScript.getSectionTextForTournamentLevel(connection, tournamentLevel, section));
          break;
        default:
          throw new FLLInternalException("Unknown awards script layer: "
              + layer);
        }
      }
      page.setAttribute("sectionSpecified", sectionSpecified);
      page.setAttribute("sectionText", sectionText);

    } catch (

    final SQLException e) {
      throw new FLLInternalException(e);
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    // TODO Auto-generated method stub

  }

}
