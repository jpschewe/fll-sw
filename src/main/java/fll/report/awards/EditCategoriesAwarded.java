/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.report.awards;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.TournamentLevel;
import fll.db.CategoriesIgnored;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for edit-categories-awarded.jsp.
 */
@WebServlet("/report/awards/EditCategoriesAwarded")
public class EditCategoriesAwarded extends BaseFLLServlet {

  /**
   * @param application application variables
   * @param page set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      final Collection<TournamentLevel> tournamentLevels = TournamentLevel.getAllLevels(connection);
      page.setAttribute("tournamentLevels", tournamentLevels);

      final Map<NonNumericCategory, Map<TournamentLevel, Boolean>> boxesChecked = new HashMap<>();
      for (final NonNumericCategory category : description.getNonNumericCategories()) {
        final Map<TournamentLevel, Boolean> categoryChecked = new HashMap<>();

        for (final TournamentLevel level : tournamentLevels) {
          final boolean checked = !CategoriesIgnored.isNonNumericCategoryIgnored(connection, level, category);
          categoryChecked.put(level, checked);
        }
        boxesChecked.put(category, categoryChecked);
      }
      page.setAttribute("boxesChecked", boxesChecked);

    } catch (final SQLException e) {
      throw new FLLInternalException(e);
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final Collection<TournamentLevel> tournamentLevels = TournamentLevel.getAllLevels(connection);
      for (final TournamentLevel level : tournamentLevels) {

        final String parameterName = Integer.toString(level.getId());
        final String @Nullable [] categoriesAwardedValue = request.getParameterValues(parameterName);
        final Collection<String> categoriesAwarded = null == categoriesAwardedValue ? Collections.emptySet()
            : Arrays.asList(categoriesAwardedValue);

        final Set<NonNumericCategory> categoriesIgnored = description.getNonNumericCategories().stream() //
                                                                     .filter(category -> !categoriesAwarded.contains(category.getTitle())) //
                                                                     .collect(Collectors.toSet());
        CategoriesIgnored.storeIgnoredNonNumericCategories(connection, level, categoriesIgnored);
      }

      SessionAttributes.appendToMessage(session, "<div class=\"success\">Stored changes to categories awarded</div>");
      response.sendRedirect(response.encodeRedirectURL("/report/awards/index.jsp"));
    } catch (final SQLException e) {
      throw new FLLInternalException("Error storing categories awarded", e);
    }

  }

}
