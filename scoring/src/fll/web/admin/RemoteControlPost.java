/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.icepush.PushContext;

import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.DisplayNames;
import fll.web.SessionAttributes;

@WebServlet("/admin/RemoteControlPost")
public class RemoteControlPost extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(RemoteControlPost.class.getName());

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final Set<String> displayNames = DisplayNames.getDisplayNames(application);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("remotePage "
          + request.getParameter("remotePage"));
      LOGGER.trace("remoteURL "
          + request.getParameter("remoteURL"));
      LOGGER.trace("playoffDivision "
          + request.getParameter("playoffDivision"));
      LOGGER.trace("playoffRoundNumber "
          + request.getParameter("playoffRoundNumber"));
      LOGGER.trace("finalistDivision "
          + request.getParameter("finalistDivision"));

      if (null != displayNames) {
        for (final String displayName : displayNames) {
          LOGGER.trace("display "
              + displayName);
          LOGGER.trace("\tremotePage "
              + request.getParameter(displayName
                  + "_remotePage"));
          LOGGER.trace("\tremoteURL "
              + request.getParameter(displayName
                  + "_remoteURL"));
          LOGGER.trace("\tplayoffDivision "
              + request.getParameter(displayName
                  + "_playoffDivision"));
          LOGGER.trace("\tplayoffRoundNumber "
              + request.getParameter(displayName
                  + "_playoffRoundNumber"));
          LOGGER.trace("\tdelete? "
              + request.getParameter(displayName
                  + "_delete"));
        }
      }
    } // if trace enabled

    // default display
    final String slideIntervalStr = request.getParameter("slideInterval");
    if (null != slideIntervalStr) {
      application.setAttribute("slideShowInterval", Integer.valueOf(slideIntervalStr));
    }
    application.setAttribute(ApplicationAttributes.DISPLAY_PAGE, request.getParameter("remotePage"));
    application.setAttribute("displayURL", request.getParameter("remoteURL"));
    final String playoffRoundNumberStr = request.getParameter("playoffRoundNumber");
    if (null != playoffRoundNumberStr) {
      application.setAttribute("playoffRoundNumber", Integer.valueOf(playoffRoundNumberStr));
    }
    application.setAttribute("playoffDivision", request.getParameter("playoffDivision"));

    application.setAttribute("finalistDivision", request.getParameter("finalistDivision"));

    // named displays
    if (null != displayNames) {

      final List<String> toDelete = new LinkedList<>();
      for (final String displayName : displayNames) {
        if (null != request.getParameter(displayName
            + "_delete")) {
          toDelete.add(displayName);
        } else if ("default".equals(request.getParameter(displayName
            + "_remotePage"))) {
          application.removeAttribute(displayName
              + "_displayPage");
          application.removeAttribute(displayName
              + "_displayURL");
          application.removeAttribute(displayName
              + "_playoffRoundNumber");
          application.removeAttribute(displayName
              + "_playoffDivision");
          application.removeAttribute(displayName
              + "_finalistDivision");
        } else {
          application.setAttribute(displayName
              + "_displayPage", request.getParameter(displayName
                  + "_remotePage"));
          application.setAttribute(displayName
              + "_displayURL", request.getParameter("remoteURL"));
          final String displayPlayoffRoundNumberStr = request.getParameter(displayName
              + "_playoffRoundNumber");
          if (null != displayPlayoffRoundNumberStr) {

            application.setAttribute(displayName
                + "_playoffRoundNumber", Integer.valueOf(displayPlayoffRoundNumberStr));
          }
          application.setAttribute(displayName
              + "_playoffDivision", request.getParameter(displayName
                  + "_playoffDivision"));
          application.setAttribute(displayName
              + "_finalistDivision",
                                   request.getParameter(displayName
                                       + "_finalistDivision"));
        }
      } // foreach display

      for (final String displayName : toDelete) {
        DisplayNames.deleteNamedDisplay(application, displayName);
      }

    } // if non-null display names

    PushContext pc = PushContext.getInstance(application);
    pc.push("playoffs");
    pc.push("display");
    session.setAttribute(SessionAttributes.MESSAGE, "<i id='success'>Successfully set remote control parameters</i>");

    response.sendRedirect(response.encodeRedirectURL("remoteControl.jsp"));

  }

}
