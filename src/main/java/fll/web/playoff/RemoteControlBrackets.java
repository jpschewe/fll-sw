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
import fll.web.WebUtils;
import fll.web.display.DisplayHandler;
import fll.web.display.DisplayInfo;
import fll.web.display.UnknownDisplayException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Data for remoteControlBrackets.jsp.
 */
public final class RemoteControlBrackets {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Number of rounds to display on the remote control brackets page.
   */
  public static final int NUM_ROUNDS_TO_DISPLAY = 2;

  private RemoteControlBrackets() {
  }

  /**
   * @param application application variables
   * @param session session variables
   * @param pageContext page variables
   * @param request used to get parameters
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final HttpServletRequest request,
                                     final PageContext pageContext) {
    final String displayUuid = request.getParameter(DisplayHandler.DISPLAY_UUID_PARAMETER_NAME);

    DisplayInfo displayInfo;
    try {
      displayInfo = DisplayHandler.resolveDisplay(displayUuid);
    } catch (final UnknownDisplayException e) {
      LOGGER.warn("Unable to find display {}, using default display", displayUuid);
      displayInfo = DisplayHandler.getDefaultDisplay();
    }

    // store the brackets to know when a refresh is required
    session.setAttribute("brackets", displayInfo.getBrackets());

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      pageContext.setAttribute("maxNameLength", Team.MAX_TEAM_NAME_LEN);

      final List<BracketData> allBracketData = new LinkedList<>();

      for (final DisplayInfo.H2HBracketDisplay h2hBracket : displayInfo.getBrackets()) {
        final BracketData bracketData = BracketData.constructRemoteControlBrackets(connection, h2hBracket.getBracket(),
                                                                                   h2hBracket.getFirstRound(),
                                                                                   h2hBracket.getFirstRound()
                                                                                       + NUM_ROUNDS_TO_DISPLAY
                                                                                       - 1,
                                                                                   h2hBracket.getIndex());
        allBracketData.add(bracketData);
      }

      pageContext.setAttribute("allBracketData", allBracketData);

      // expose allBracketData to the javascript
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      final String allBracketDataJson = WebUtils.escapeStringForJsonParse(jsonMapper.writeValueAsString(allBracketData));
      pageContext.setAttribute("allBracketDataJson", allBracketDataJson);

      final double scrollRate = GlobalParameters.getHeadToHeadScrollRate(connection);
      pageContext.setAttribute("scrollRate", scrollRate);

      Message.setPageVariables(pageContext);
    } catch (final SQLException sqle) {
      throw new FLLRuntimeException("Error talking to the database", sqle);
    } catch (final JsonProcessingException e) {
      throw new FLLRuntimeException("Error converting data to JSON", e);
    }

  }

}
