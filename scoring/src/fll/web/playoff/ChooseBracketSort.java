/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import fll.util.FLLRuntimeException;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.BracketSortType;

/**
 * Work with choose_bracket_sort.jsp.
 */
@WebServlet("/playoff/ChooseBracketSort")
public class ChooseBracketSort extends BaseFLLServlet {

  /**
   * Setup variables needed for the page.
   * <ul>
   * <li>sortOptions - BracketSortType[]</li>
   * <li>defaultSort - BracketSortType</li>
   * </ul>
   */
  public static void populateContext(final PageContext pageContext) {
    pageContext.setAttribute("sortOptions", BracketSortType.class.getEnumConstants());
    pageContext.setAttribute("defaultSort", BracketSortType.SEEDING);
  }

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                          PlayoffSessionData.class);

    final String sortStr = request.getParameter("sort");
    if (null == sortStr
        || "" == sortStr) {
      throw new FLLRuntimeException("Missing parameter 'sort'");
    }

    final BracketSortType sort = BracketSortType.valueOf(sortStr);

    data.setSort(sort);

    session.setAttribute(PlayoffIndex.SESSION_DATA, data);
    
    response.sendRedirect(response.encodeRedirectURL("InitializeBrackets"));

  }
}
