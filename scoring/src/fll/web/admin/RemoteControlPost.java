/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.util.Collection;

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
import fll.web.SessionAttributes;

@WebServlet("/admin/RemoteControlPost")
public class RemoteControlPost extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(RemoteControlPost.class.getName());

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    @SuppressWarnings("unchecked")
    final Collection<String> displayNames = (Collection<String>) ApplicationAttributes.getAttribute(application,
                                                                                                    "displayNames",
                                                                                                    Collection.class);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("remotePage "
          + request.getParameter("remotePage"));
      LOGGER.trace("remoteURL "
          + request.getParameter("remoteURL"));
      LOGGER.trace("playoffDivision "
          + request.getParameter("playoffDivision"));
      LOGGER.trace("playoffRoundNumber "
          + request.getParameter("playoffRoundNumber"));

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
        }
      }
    }

    // default display
    final String slideIntervalStr = request.getParameter("slideInterval");
    if (null != slideIntervalStr) {
      application.setAttribute("slideShowInterval", Integer.valueOf(slideIntervalStr));
    }
    application.setAttribute("displayPage", request.getParameter("remotePage"));
    application.setAttribute("displayURL", request.getParameter("remoteURL"));
    final String playoffRoundNumberStr = request.getParameter("playoffRoundNumber");
    if (null != playoffRoundNumberStr) {
      application.setAttribute("playoffRoundNumber", Integer.valueOf(playoffRoundNumberStr));
    }
    application.setAttribute("playoffDivision", request.getParameter("playoffDivision"));

    // named displays
    if (null != displayNames) {
      for (final String displayName : displayNames) {
        if ("default".equals(request.getParameter(displayName
            + "_remotePage"))) {
          application.removeAttribute(displayName
              + "_displayPage");
          application.removeAttribute(displayName
              + "_displayURL");
          application.removeAttribute(displayName
              + "_playoffRoundNumber");
          application.removeAttribute(displayName
              + "_playoffDivision");
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
        }
      }
    }

    PushContext.getInstance(application).push("playoffs");
    session.setAttribute(SessionAttributes.MESSAGE, "<i id='success'>Successfully set remote control parameters</i>");

    response.sendRedirect(response.encodeRedirectURL("remoteControl.jsp"));

  }

}
