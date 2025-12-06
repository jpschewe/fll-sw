/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import fll.db.Queries;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.TournamentData;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Java code for scoreEntry/select_team.jsp.
 */
public final class SelectTeam {

  private SelectTeam() {
  }

  /**
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {

    final List<RunMetadata> editMetadata = new LinkedList<>();

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final RunMetadataFactory runMetadataFactory = tournamentData.getRunMetadataFactory();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?")) {
        prep.setInt(1, Queries.getCurrentTournament(connection));
        try (ResultSet rs = prep.executeQuery()) {
          final int maxRunNumber;
          if (rs.next()) {
            maxRunNumber = rs.getInt(1);
          } else {
            maxRunNumber = 1;
          }
          for (int runNumber = 1; runNumber <= maxRunNumber; ++runNumber) {
            final RunMetadata runMetadata = runMetadataFactory.getRunMetadata(runNumber);
            editMetadata.add(runMetadata);
          }
          pageContext.setAttribute("editMetadata", editMetadata);
        } // result set
      } // prepared statement

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
