/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Change the text on the scoreboard pages.
 */
@WebServlet("/admin/ChangeScorePageText")
public class ChangeScorePageText extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
      final String newText = request.getParameter("ScorePageText");
      if (null != newText
          && !"".equals(newText)) {
        application.setAttribute(ApplicationAttributes.SCORE_PAGE_TEXT, newText);
      }

    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }
}
