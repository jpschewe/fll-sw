/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.RunMetadata;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.TournamentData;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for editing {@link RunMetadata}.
 */
@WebServlet("/admin/EditRunMetadata")
public class EditRunMetadata extends BaseFLLServlet {

  /**
   * @param application application scope
   * @param page page scope
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final List<RunMetadata> allRunMetadata = tournamentData.getAllRunMetadata();
    page.setAttribute("allRunMetadata", allRunMetadata);

    try (Connection connection = tournamentData.getDataSource().getConnection()) {
      final Tournament tournament = tournamentData.getCurrentTournament();

      // check which runs can be deleted
      final Map<Integer, Boolean> canDelete = new HashMap<>();
      for (final RunMetadata metadata : allRunMetadata) {
        canDelete.put(metadata.getRunNumber(), RunMetadata.canDelete(connection, tournament, metadata.getRunNumber()));
      }
      page.setAttribute("canDelete", canDelete);
    } catch (final SQLException e) {
      throw new FLLInternalException("Unable to check which run metadata can be deleted", e);
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);

    // FIXME check for deletes first and do from top down
    final List<Integer> toDelete = new LinkedList<>();
    for (int round = 1; round < Integer.MAX_VALUE; ++round) {
      final @Nullable String displayName = request.getParameter(String.format("%d_name", round));
      if (null == displayName) {
        break;
      }

      if (null != request.getParameter(String.format("%d_delete", round))) {
        toDelete.add(round);
      }
    }
    // delete in reverse order so that it doesn't fail from trying to delete and
    // leave a gap
    Collections.sort(toDelete, Collections.reverseOrder());
    for (final Integer round : toDelete) {
      tournamentData.deleteRunMetadata(round);
    }

    for (int round = 1; round < Integer.MAX_VALUE; ++round) {
      final @Nullable String displayName = request.getParameter(String.format("%d_name", round));
      if (null == displayName) {
        // processed the last run
        break;
      }

      if (null != request.getParameter(String.format("%d_delete", round))) {
        // deletes handled first, if a run is deleted, all later runs are also deleted,
        // so we're done looking
        break;
      } else {
        if (StringUtils.isBlank(displayName)) {
          throw new MissingFormatArgumentException(String.format("Round %d has an empty display name, this is not allowed, all rounds must have a display name",
                                                                 round));
        }

        final boolean regularMatchPlay = null != request.getParameter(String.format("%d_regularMatchPlay", round));
        final boolean scoreboardDisplay = null != request.getParameter(String.format("%d_scoreboardDisplay", round));
        final RunMetadata metadata = new RunMetadata(round, displayName, regularMatchPlay, scoreboardDisplay);
        tournamentData.storeRunMetadata(metadata);
      }
    }

  }

}
