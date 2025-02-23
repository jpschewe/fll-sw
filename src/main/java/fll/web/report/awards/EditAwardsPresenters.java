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
import java.util.Set;
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
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
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
 * Support for edit-awards-presenters.jsp.
 */
@WebServlet("/report/awards/EditAwardsPresenters")
public class EditAwardsPresenters extends BaseFLLServlet {

  /**
   * @param application read application variables
   * @param page set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final TournamentLevel tournamentLevel = tournament.getLevel();
      page.setAttribute("descriptionText",
                        String.format("tournament %s - %s", tournament.getName(), tournament.getDescription()));

      page.setAttribute("INTERNAL_TOURNAMENT_ID", GenerateDB.INTERNAL_TOURNAMENT_ID);
      page.setAttribute("tournamentLevel", tournamentLevel);
      page.setAttribute("tournament", tournament);

      loadSubjectiveCategoryPresenters(page, description, connection, tournament);

      loadNonNumericCategoryPresenters(page, description, connection, tournamentLevel, tournament);

      loadSectionPresenters(page, connection, tournament);

      page.setAttribute("championshipAwardTitle", ChampionshipCategory.CHAMPIONSHIP_AWARD_TITLE);
      page.setAttribute("head2headAwardTitle", HeadToHeadCategory.AWARD_TITLE);
      page.setAttribute("performanceAwardTitle", PerformanceScoreCategory.CATEGORY_TITLE);

    } catch (final SQLException e) {
      throw new FLLInternalException("Error getting values for editing awards presenters", e);
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

  private static void loadSubjectiveCategoryPresenters(final PageContext page,
                                                       final ChallengeDescription description,
                                                       final Connection connection,
                                                       final Tournament tournament)
      throws SQLException {
    final List<SubjectiveScoreCategory> subjectiveCategories = getSubjectiveCategories(description);
    page.setAttribute("subjectiveCategories", subjectiveCategories);

    final Map<SubjectiveScoreCategory, String> subjectiveCategoryPresenter = new HashMap<>();
    final Map<SubjectiveScoreCategory, Boolean> subjectiveCategoryPresenterSpecified = new HashMap<>();

    for (final SubjectiveScoreCategory category : subjectiveCategories) {
      final boolean presenterSpecified = AwardsScript.isPresenterSpecifiedForTournament(connection, tournament,
                                                                                        category);

      final String presenter;
      if (!presenterSpecified) {
        // display the value that would be used to the user
        presenter = AwardsScript.getPresenter(connection, tournament, category);
      } else {
        presenter = AwardsScript.getPresenterForTournament(connection, tournament, category);
      }

      subjectiveCategoryPresenter.put(category, presenter);
      subjectiveCategoryPresenterSpecified.put(category, presenterSpecified);
    }

    page.setAttribute("subjectiveCategoryPresenter", subjectiveCategoryPresenter);
    page.setAttribute("subjectiveCategoryPresenterSpecified", subjectiveCategoryPresenterSpecified);
  }

  private static void loadNonNumericCategoryPresenters(final PageContext page,
                                                       final ChallengeDescription description,
                                                       final Connection connection,
                                                       final TournamentLevel tournamentLevel,
                                                       final Tournament tournament)
      throws SQLException {
    final List<NonNumericCategory> nonNumericCategories = getNonNumericCategories(description, connection,
                                                                                  tournamentLevel);
    page.setAttribute("nonNumericCategories", nonNumericCategories);

    final Map<NonNumericCategory, String> nonNumericCategoryPresenter = new HashMap<>();
    final Map<NonNumericCategory, Boolean> nonNumericCategoryPresenterSpecified = new HashMap<>();

    for (final NonNumericCategory category : nonNumericCategories) {
      final boolean presenterSpecified = AwardsScript.isPresenterSpecifiedForTournament(connection, tournament,
                                                                                        category);
      final String presenter;
      if (!presenterSpecified) {
        // display the value that would be used to the user
        presenter = AwardsScript.getPresenter(connection, tournament, category);
      } else {
        presenter = AwardsScript.getPresenterForTournament(connection, tournament, category);
      }

      nonNumericCategoryPresenter.put(category, presenter);
      nonNumericCategoryPresenterSpecified.put(category, presenterSpecified);
    }

    page.setAttribute("nonNumericCategoryPresenter", nonNumericCategoryPresenter);
    page.setAttribute("nonNumericCategoryPresenterSpecified", nonNumericCategoryPresenterSpecified);
  }

  private static void loadSectionPresenters(final PageContext page,
                                            final Connection connection,
                                            final Tournament tournament)
      throws SQLException {
    page.setAttribute("sections", AwardsScript.Section.values());
    Arrays.stream(AwardsScript.Section.values()).forEach(s -> page.setAttribute(s.name(), s));

    final Map<AwardsScript.Section, Boolean> sectionSpecified = new HashMap<>();
    final Map<AwardsScript.Section, String> sectionText = new HashMap<>();
    for (final AwardsScript.Section section : Arrays.asList(AwardsScript.Section.CATEGORY_CHAMPIONSHIP_PRESENTER,
                                                            AwardsScript.Section.CATEGORY_HEAD2HEAD_PRESENTER,
                                                            AwardsScript.Section.CATEGORY_PERFORMANCE_PRESENTER)) {
      final boolean specified = AwardsScript.isSectionSpecifiedForTournament(connection, tournament, section);
      final String text;
      if (!specified) {
        // display the value that would be used to the user
        text = AwardsScript.getSectionText(connection, tournament, section);
      } else {
        text = AwardsScript.getSectionTextForTournament(connection, tournament, section);
      }
      sectionText.put(section, text);
      sectionSpecified.put(section, specified);
    }
    page.setAttribute("sectionSpecified", sectionSpecified);
    page.setAttribute("sectionText", sectionText);
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final int tournamentLevelId = WebUtils.getIntRequestParameter(request, "level");
      final TournamentLevel tournamentLevel = TournamentLevel.getById(connection, tournamentLevelId);

      final int tournamentId = WebUtils.getIntRequestParameter(request, "tournament");
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);

      storeSpecialCategoryPresenters(request, connection, tournament);

      storeSubjectiveCategoryPresenters(request, description, connection, tournament);

      storeNonNumericCategoryPresenters(request, description, connection, tournamentLevel, tournament);

      final String layerText = String.format("tournament %s", tournament.getName());

      SessionAttributes.appendToMessage(session,
                                        String.format("<div class='success'>Saved changes to awards presenters for %s</div>",
                                                      layerText));
      response.sendRedirect(response.encodeRedirectURL("/report/index.jsp"));
    } catch (final SQLException e) {
      throw new FLLInternalException("Error storing values for award script", e);
    }

  }

  private void storeSpecialCategoryPresenters(final HttpServletRequest request,
                                              final Connection connection,
                                              final Tournament tournament)
      throws SQLException {
    for (final AwardsScript.Section section : new AwardsScript.Section[] { AwardsScript.Section.CATEGORY_CHAMPIONSHIP_PRESENTER,
                                                                           AwardsScript.Section.CATEGORY_HEAD2HEAD_PRESENTER,
                                                                           AwardsScript.Section.CATEGORY_PERFORMANCE_PRESENTER }) {
      final @Nullable String specifiedStr = request.getParameter(String.format("%s_specified",
                                                                               section.getIdentifier()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request,
                                                                String.format("%s_text", section.getIdentifier()));
        AwardsScript.updateSectionTextForTournament(connection, tournament, section, text);
      } else {
        AwardsScript.clearSectionTextForTournament(connection, tournament, section);
      }
    }
  }

  private void storeSubjectiveCategoryPresenters(final HttpServletRequest request,
                                                 final ChallengeDescription description,
                                                 final Connection connection,
                                                 final Tournament tournament)
      throws SQLException {
    final List<SubjectiveScoreCategory> categories = getSubjectiveCategories(description);

    for (final SubjectiveScoreCategory category : categories) {
      final @Nullable String specifiedStr = request.getParameter(String.format("category_%s_presenter_specified",
                                                                               category.getName()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request, String.format("category_%s_presenter_text",
                                                                                       category.getName()));

        AwardsScript.updatePresenterForTournament(connection, tournament, category, text);
      } else {
        AwardsScript.clearPresenterForTournament(connection, tournament, category);
      }
    }
  }

  private void storeNonNumericCategoryPresenters(final HttpServletRequest request,
                                                 final ChallengeDescription description,
                                                 final Connection connection,
                                                 final TournamentLevel tournamentLevel,
                                                 final Tournament tournament)
      throws SQLException {
    final List<NonNumericCategory> categories = getNonNumericCategories(description, connection, tournamentLevel);

    for (final NonNumericCategory category : categories) {
      final @Nullable String specifiedStr = request.getParameter(String.format("category_%s_presenter_specified",
                                                                               category.getTitle()));
      if (null != specifiedStr) {
        final String text = WebUtils.getNonNullRequestParameter(request, String.format("category_%s_presenter_text",
                                                                                       category.getTitle()));

        AwardsScript.updatePresenterForTournament(connection, tournament, category, text);
      } else {
        AwardsScript.clearPresenterForTournament(connection, tournament, category);
      }
    }
  }

}
