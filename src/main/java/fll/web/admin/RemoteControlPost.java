/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN, UserRole.SCORING_COORDINATOR), false)) {
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

    // collect all of the displays to delete
    final Set<DisplayInfo> toDelete = new HashSet<>();
    for (final DisplayInfo display : displays) {
      if (null != request.getParameter(display.getDeleteFormParamName())) {
        toDelete.add(display);
      }
    }

    // handle all of the setting of follow default, then we can be smarter about
    // which displays get notified.
    final Set<DisplayInfo> followingDefault = new HashSet<>();
    final Set<DisplayInfo> newlyfollowingDefault = new HashSet<>();
    for (final DisplayInfo display : displays) {
      if (toDelete.contains(display)) {
        continue;
      }

      final boolean displayFollowDefault = DisplayInfo.DEFAULT_DISPLAY_NAME.equals(request.getParameter(display.getRemotePageFormParamName()));
      if (displayFollowDefault) {
        followingDefault.add(display);

        if (displayFollowDefault != display.isFollowDefault()) {
          display.setFollowDefault();
          newlyfollowingDefault.add(display);
        }
      }
    }

    // Handle all parameters other than the URL change.
    // The idea is that this will get all of the parameters on the DisplayInfo
    // object set before it's used to load the initial page.
    for (final DisplayInfo display : displays) {
      if (toDelete.contains(display)) {
        continue;
      }

      if (followingDefault.contains(display)) {
        // nothing to do
        continue;
      }

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

      final boolean scoreBoardClockEnabled = WebUtils.getBooleanRequestParameter(request,
                                                                                 display.getScoreboardClockEnabledParamName(),
                                                                                 false);
      LOGGER.warn("Display {} clock {}", display.getUuid(), scoreBoardClockEnabled);
      if (scoreBoardClockEnabled != display.isScoreboardClockEnabled()) {
        display.setScoreboardClockEnabled(scoreBoardClockEnabled);
        ScoreboardUpdates.sendClockEnabledMessage(display, scoreBoardClockEnabled);

        if (display.isDefaultDisplay()) {
          for (final DisplayInfo d : followingDefault) {
            ScoreboardUpdates.sendClockEnabledMessage(d, scoreBoardClockEnabled);
          }
        }
      }
    }

    // remove displays
    for (final DisplayInfo display : toDelete) {
      DisplayHandler.removeDisplay(display.getUuid());
    }

    // notify brackets that there may be changes
    for (final DisplayInfo display : displays) {
      if (!toDelete.contains(display)) {
        H2HUpdateWebSocket.updateDisplayedBracket(display);
      }
    }

    // notify displays that changed URLs
    for (final DisplayInfo display : displays) {
      if (toDelete.contains(display)) {
        continue;
      }

      if (newlyfollowingDefault.contains(display)) {
        // update the page displayed as we don't know what is currently on this display
        DisplayHandler.sendDisplayUrl(display);
        continue;
      }
      if (followingDefault.contains(display)) {
        // this will be handled when the default display is visited
        continue;
      }

      boolean needsUpdate = false;
      final String displayRemotePage = WebUtils.getNonNullRequestParameter(request,
                                                                           display.getRemotePageFormParamName());
      if (!displayRemotePage.equals(display.getRemotePage())) {
        display.setRemotePage(displayRemotePage);
        needsUpdate = true;
      }

      final @Nullable String specialUrl = request.getParameter(display.getSpecialUrlFormParamName());
      if (!Objects.equals(specialUrl, display.getSpecialUrl())) {
        display.setSpecialUrl(specialUrl);
        needsUpdate = true;
      }

      if (needsUpdate) {
        DisplayHandler.sendDisplayUrl(display);
      }
    }

    SessionAttributes.appendToMessage(session, "<i id='success'>Successfully set remote control parameters</i>");
    response.sendRedirect(response.encodeRedirectURL("remoteControl.jsp"));
  }

}
