/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import fll.db.Queries;
import fll.db.TableInformation;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.BracketSortType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Work with bracket_parameters.jsp.
 */
@WebServlet("/playoff/BracketParameters")
public class BracketParameters extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Setup variables needed for the page.
   * <ul>
   * <li>sortOptions - BracketSortType[]</li>
   * <li>defaultSort - BracketSortType</li>
   * <li>tableInfo - List&lt;TableInformation&gt;</li>
   * </ul>
   * 
   * @param application the application context
   * @param session the session context
   * @param pageContext the page context
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    pageContext.setAttribute("sortOptions", BracketSortType.class.getEnumConstants());
    pageContext.setAttribute("defaultSort", BracketSortType.SEEDING);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final int currentTournament = data.getCurrentTournament().getTournamentID();

      final String bracket = data.getBracket();
      if (null == bracket) {
        throw new FLLRuntimeException("Playoff session data has a null bracket");
      }

      final List<TableInformation> tableInfo = TableInformation.getTournamentTableInformation(connection,
                                                                                              currentTournament,
                                                                                              bracket);
      pageContext.setAttribute("tableInfo", tableInfo);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }
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
    try (Connection connection = datasource.getConnection()) {

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final String sortStr = WebUtils.getNonNullRequestParameter(request, "sort");
      final BracketSortType sort = BracketSortType.valueOf(sortStr);

      if (BracketSortType.CUSTOM.equals(sort)) {
        final String customSortOrderStr = WebUtils.getNonNullRequestParameter(request, "custom_order");
        final List<Integer> customSortOrder = Stream.of(customSortOrderStr.split("[,\\s]+")) //
                                                    .map(Integer::valueOf) //
                                                    .toList();

        // check that all teams are in the sort order
        final String bracket = data.getBracket();
        if (null == bracket) {
          throw new CustomSortOrderException("No playoff bracket specified");
        } else if (Queries.isPlayoffDataInitialized(connection, bracket)) {
          throw new CustomSortOrderException(String.format("Playoffs have already been initialized for playoff bracket %s",
                                                           bracket));
        }

        final List<Integer> teamNumbersInBracket = Playoff.getTeamNumbersForPlayoffBracket(connection,
                                                                                           data.getCurrentTournament()
                                                                                               .getTournamentID(),
                                                                                           bracket);
        if (customSortOrder.size() != teamNumbersInBracket.size()) {
          throw new CustomSortOrderException(String.format("Custom order must contain the exact list of teams in the bracket. Provided size: %d Actual size: %d",
                                                           customSortOrder.size(), teamNumbersInBracket.size()));
        }

        for (final Integer teamNum : customSortOrder) {
          if (!teamNumbersInBracket.contains(teamNum)) {
            throw new CustomSortOrderException(String.format("Specified team %d is not in the bracket", teamNum));
          }
        }

        data.setCustomSortOrder(customSortOrder);
      }
      data.setSort(sort);

      try (PreparedStatement delete = connection.prepareStatement("DELETE FROM table_division" //
          + " WHERE tournament = ?" //
          + " AND playoff_division = ?"//
      )) {
        delete.setInt(1, data.getCurrentTournament().getTournamentID());
        delete.setString(2, data.getBracket());
        delete.executeUpdate();
      }

      try (
          PreparedStatement insert = connection.prepareStatement("INSERT INTO table_division (tournament, playoff_division, table_id) VALUES (?, ?, ?)")) {
        insert.setInt(1, data.getCurrentTournament().getTournamentID());
        insert.setString(2, data.getBracket());
        final String[] tables = request.getParameterValues("tables");
        if (null != tables) {
          for (final String idStr : tables) {
            final int id = Integer.parseInt(idStr);
            insert.setInt(3, id);
            insert.executeUpdate();
          }
        }
      }

      session.setAttribute(PlayoffIndex.SESSION_DATA, data);

      response.sendRedirect(response.encodeRedirectURL("InitializeBrackets"));
    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    } catch (final MissingRequiredParameterException e) {
      SessionAttributes.appendToMessage(session, String.format("<p class='error'>%s</p>", e.getMessage()));
      WebUtils.sendRedirect(response, "bracket_parameters.jsp");
    } catch (final NumberFormatException e) {
      SessionAttributes.appendToMessage(session,
                                        String.format("<p class='error'>The custom sort order must be a comma or space separated list of numbers: %s</p>",
                                                      e.getMessage()));
      WebUtils.sendRedirect(response, "bracket_parameters.jsp");
    } catch (final CustomSortOrderException e) {
      SessionAttributes.appendToMessage(session, String.format("<p class='error'>%s</p>", e.getMessage()));
      WebUtils.sendRedirect(response, "bracket_parameters.jsp");
    }
  }

  private static final class CustomSortOrderException extends FLLRuntimeException {
    CustomSortOrderException(final String msg) {
      super(msg);
    }
  }
}
