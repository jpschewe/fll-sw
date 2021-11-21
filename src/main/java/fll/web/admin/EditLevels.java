/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.diffplug.common.base.Errors;

import fll.TournamentLevel;
import fll.TournamentLevel.NoSuchTournamentLevelException;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support code for edit_levels.jsp.
 */
@WebServlet("/admin/EditLevels")
public class EditLevels extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String NEW_LEVEL_ID_PREFIX = "new_";

  private static final String NEXT_PREFIX = "next_";

  private static final String NONE_OPTION_VALUE = "none";

  private static final String NONE_OPTION_TITLE = "None";

  /**
   * @param application read application variables
   * @param page set variables for rendering the page
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection(); Statement stmt = connection.createStatement()) {

      final Set<TournamentLevel> levels = TournamentLevel.getAllLevels(connection);
      page.setAttribute("levels", levels);

      page.setAttribute("NO_NEXT_LEVEL_ID", TournamentLevel.NO_NEXT_LEVEL_ID);
      page.setAttribute("NEW_LEVEL_ID_PREFIX", NEW_LEVEL_ID_PREFIX);
      page.setAttribute("NEXT_PREFIX", NEXT_PREFIX);
      page.setAttribute("NONE_OPTION_VALUE", NONE_OPTION_VALUE);
      page.setAttribute("NONE_OPTION_TITLE", NONE_OPTION_TITLE);

      final Set<TournamentLevel> referencedLevels = levels.stream() //
                                                          .filter(Errors.rethrow()
                                                                        .wrapPredicate(l -> TournamentLevel.getNumTournamentsAtLevel(connection,
                                                                                                                                     l) > 0)) //
                                                          .collect(Collectors.toSet());
      page.setAttribute("referencedLevels", referencedLevels);

    } catch (final SQLException e) {
      throw new FLLInternalException("Error populating context for editing of tournament levels", e);
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection(); Statement stmt = connection.createStatement()) {

      final Set<TournamentLevel> prevLevels = TournamentLevel.getAllLevels(connection);

      final Set<Integer> levelsSeen = new HashSet<>();
      final Map<String, String[]> parameterMap = request.getParameterMap();
      final Map<String, LevelInfo> webIdToInfo = new HashMap<>();

      for (final Map.Entry<String, String[]> parameterEntry : parameterMap.entrySet()) {
        final String parameterName = parameterEntry.getKey();

        final String nextLevelParameterName = String.format("%s%s", NEXT_PREFIX, parameterName);
        if (parameterMap.containsKey(nextLevelParameterName)) {
          // parameterName must be the ID of a tournament level

          final String levelName = parameterEntry.getValue()[0];
          final String nextLevelWebId = parameterMap.get(nextLevelParameterName)[0];

          final TournamentLevel level;
          if (parameterName.startsWith(NEW_LEVEL_ID_PREFIX)) {
            LOGGER.debug("Creating tournament level '{}' from id '{}'", levelName, parameterName);

            // create a new tournament level
            level = TournamentLevel.createTournamentLevel(connection, levelName);
          } else {
            // lookup the tournament level by id
            LOGGER.debug("Looking up tournament level '{}' from id '{}'", levelName, parameterName);

            try {
              final int levelId = Integer.parseInt(parameterName);
              level = TournamentLevel.getById(connection, levelId);
              levelsSeen.add(levelId);
            } catch (final NumberFormatException e) {
              throw new FLLInternalException("Found existing tournament level id that isn't an integer", e);
            } catch (final NoSuchTournamentLevelException e) {
              throw new FLLInternalException("Invalid tournament level id found", e);
            }
          }

          LOGGER.debug("Next level for '{}' is '{}'", parameterName, nextLevelWebId);
          webIdToInfo.put(parameterName, new LevelInfo(level, nextLevelWebId, levelName));

        } else {
          LOGGER.debug("'{}' is not the parameter of a tournament level", parameterName);
        }

      } // foreach parameter

      // set next tournament levels
      for (final Map.Entry<String, LevelInfo> entry : webIdToInfo.entrySet()) {
        final LevelInfo info = entry.getValue();
        final TournamentLevel level = info.getLevel();
        final String nextLevelWebId = info.getNextWebId();

        if (!NONE_OPTION_VALUE.equals(nextLevelWebId)) {
          if (!webIdToInfo.containsKey(nextLevelWebId)) {
            throw new FLLInternalException("Cannot find tournament for id '"
                + nextLevelWebId
                + "'. This suggests there is an error in the web page.");
          }

          final TournamentLevel nextLevel = webIdToInfo.get(nextLevelWebId).getLevel();
          TournamentLevel.updateTournamentLevel(connection, level.getId(), info.getNewName(), nextLevel);
        } else {
          LOGGER.debug("Not setting next tournament for '{}' as it is the none value '{}'", level.getName(),
                       nextLevelWebId);
        }
      }

      // delete any levels not seen
      for (final TournamentLevel level : prevLevels) {
        if (!levelsSeen.contains(level.getId())) {
          TournamentLevel.deleteLevel(connection, level);
        }
      }

      SessionAttributes.appendToMessage(session, "<div class='success'>Saved trournament levels</div>");
      response.sendRedirect(response.encodeRedirectURL("/admin/index.jsp"));
    } catch (final SQLException e) {
      throw new FLLInternalException("Error saving tournament level information to the database", e);
    }

  }

  private static final class LevelInfo {
    LevelInfo(final TournamentLevel level,
              final String nextWebId,
              final String newName) {
      this.level = level;
      this.nextWebId = nextWebId;
      this.newName = newName;
    }

    private final TournamentLevel level;

    public TournamentLevel getLevel() {
      return level;
    }

    private final String nextWebId;

    public String getNextWebId() {
      return nextWebId;
    }

    private final String newName;

    public String getNewName() {
      return newName;
    }
  }

}
