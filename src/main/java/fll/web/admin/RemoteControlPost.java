/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.web.display.DisplayHandler;
import fll.web.display.DisplayInfo;
import fll.web.playoff.H2HUpdateWebSocket;
import fll.web.scoreboard.ScoreboardUpdates;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handle changes to the remote control parameters.
 */
@WebServlet("/admin/RemoteControlPost")
public class RemoteControlPost extends BaseFLLServlet {

  private static final Logger LOGGER = LogManager.getLogger();

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

    final Collection<DisplayInfo> displays = DisplayHandler.getAllRemoteControlDisplays();

    if (LOGGER.isTraceEnabled()) {
      for (final DisplayInfo display : displays) {
        LOGGER.trace("display name: {} uuid: {} ", display.getName(), display.getUuid());
        LOGGER.trace("\tremotePage {}", request.getParameter(display.getRemotePageFormParamName()));
        LOGGER.trace("\tremoteURL {}", request.getParameter(display.getRemoteUrlFormParamName()));

        final int numBrackets = WebUtils.getIntRequestParameter(request,
                                                                display.getHead2HeadNumBracketsFormParamName());
        LOGGER.trace("\tnum brackets:");
        for (int i = 0; i < numBrackets; ++i) {
          LOGGER.trace("\t\tplayoffDivision {}: {}", i,
                       request.getParameter(display.getHead2HeadBracketFormParamName(i)));

          LOGGER.trace("\t\tplayoffRoundNumber {}: {}", i,
                       request.getParameter(display.getHead2HeadFirstRoundFormParamName(i)));
        }
        LOGGER.trace("\tdelete? {}", request.getParameter(display.getDeleteFormParamName()));

        LOGGER.trace("\tfinalistDivision {}",
                     request.getParameter(display.getFinalistScheduleAwardGroupFormParamName()));

      } // foreach display
    } // if trace enabled

    final String slideIntervalStr = request.getParameter("slideInterval");
    if (null != slideIntervalStr) {
      application.setAttribute("slideShowInterval", Integer.valueOf(slideIntervalStr));
    }

    final List<DisplayInfo> toDelete = new LinkedList<>();
    for (final DisplayInfo display : displays) {
      if (null != request.getParameter(display.getDeleteFormParamName())) {
        toDelete.add(display);
      } else {
        if (DisplayInfo.DEFAULT_DISPLAY_NAME.equals(request.getParameter(display.getRemotePageFormParamName()))) {
          display.setFollowDefault();
        } else {
          display.setRemotePage(WebUtils.getNonNullRequestParameter(request, display.getRemotePageFormParamName()));
        }

        display.setSpecialUrl(request.getParameter(display.getSpecialUrlFormParamName()));

        display.setFinalistScheduleAwardGroup(request.getParameter(display.getFinalistScheduleAwardGroupFormParamName()));

        if (display.isHeadToHead()) {
          final List<DisplayInfo.H2HBracketDisplay> brackets = new LinkedList<>();
          final int numBrackets = WebUtils.getIntRequestParameter(request,
                                                                  display.getHead2HeadNumBracketsFormParamName());
          for (int bracketIdx = 0; bracketIdx < numBrackets; ++bracketIdx) {
            final String bracket = WebUtils.getNonNullRequestParameter(request,
                                                                       display.getHead2HeadBracketFormParamName(bracketIdx));

            final String firstRoundStr = request.getParameter(display.getHead2HeadFirstRoundFormParamName(bracketIdx));
            final int firstRound;
            if (null == firstRoundStr) {
              // there are no head to head rounds yet, just use 1
              firstRound = 1;
            } else {
              firstRound = Integer.parseInt(firstRoundStr);
            }

            final DisplayInfo.H2HBracketDisplay bracketInfo = new DisplayInfo.H2HBracketDisplay(display, bracketIdx,
                                                                                                bracket, firstRound);
            brackets.add(bracketInfo);
          }
          display.setBrackets(brackets);
        } // head to head

        // award groups to display
        final String awardGroupsFormParamName = display.getAwardGroupsFormParamName();
        final String @Nullable [] awardGroupsParamValues = request.getParameterValues(awardGroupsFormParamName);
        if (null == awardGroupsParamValues) {
          throw new MissingRequiredParameterException(awardGroupsFormParamName);
        }
        final List<String> awardGroupsToDisplay = Arrays.asList(awardGroupsParamValues);
        final List<String> currentAwardGroupsToDisplay = display.getScoreboardAwardGroups();
        if (!awardGroupsToDisplay.equals(currentAwardGroupsToDisplay)) {
          display.setScoreboardAwardGroups(awardGroupsToDisplay);
          ScoreboardUpdates.awardGroupChange();
        }

      } // display to keep
    } // foreach display

    for (final DisplayInfo display : toDelete) {
      DisplayHandler.removeDisplay(display.getUuid());
    }

    // notify brackets that there may be changes
    for (final DisplayInfo display : displays) {
      if (!toDelete.contains(display)) {
        H2HUpdateWebSocket.updateDisplayedBracket(display);
      }
    }

    DisplayHandler.sendUpdateUrl();

    SessionAttributes.appendToMessage(session, "<i id='success'>Successfully set remote control parameters</i>");
    response.sendRedirect(response.encodeRedirectURL("remoteControl.jsp"));
  }

}
