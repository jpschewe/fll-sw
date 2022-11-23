/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Team;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;
import fll.web.playoff.BracketData.TopRightCornerStyle;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Data for remoteControlBrackets.jsp.
 */
public final class RemoteControlBrackets {

  private RemoteControlBrackets() {
  }

  /**
   * @param application application variables
   * @param session session variables
   * @param pageContext page variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);

    // store the brackets to know when a refresh is required
    session.setAttribute("brackets", displayInfo.getBrackets());

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      pageContext.setAttribute("maxNameLength", Team.MAX_TEAM_NAME_LEN);

      final List<BracketData> allBracketData = new LinkedList<>();

      for (final DisplayInfo.H2HBracketDisplay h2hBracket : displayInfo.getBrackets()) {
        final BracketData bracketData = new BracketData(connection, h2hBracket.getBracket(), h2hBracket.getFirstRound(),
                                                        h2hBracket.getFirstRound()
                                                            + 2,
                                                        4, false, true, h2hBracket.getIndex(), false);

        bracketData.addBracketLabels(h2hBracket.getFirstRound());
        bracketData.addStaticTableLabels(connection);

        bracketData.generateBracketOutput(connection, TopRightCornerStyle.MEET_TOP_OF_CELL);

        allBracketData.add(bracketData);
      }

      pageContext.setAttribute("allBracketData", allBracketData);

      // expose allBracketData to the javascript
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      final String allBracketDataJson = jsonMapper.writeValueAsString(allBracketData).replace("'", "\\'");
      pageContext.setAttribute("allBracketDataJson", allBracketDataJson);

      final double scrollRate = GlobalParameters.getHeadToHeadScrollRate(connection);
      pageContext.setAttribute("scrollRate", scrollRate);
    } catch (final SQLException sqle) {
      throw new FLLRuntimeException("Error talking to the database", sqle);
    } catch (final JsonProcessingException e) {
      throw new FLLRuntimeException("Error converting data to JSON", e);
    }

  }

}
