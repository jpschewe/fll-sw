/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.JudgeInformation;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Verify the judges information
 */
@WebServlet("/admin/VerifyJudges")
public class VerifyJudges extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "XSS_REQUEST_PARAMETER_TO_JSP_WRITER", justification = "Checking category name retrieved from request against valid category names")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of VerifyJudges.processRequest");
    }

    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

    // keep track of any errors
    final StringBuffer error = new StringBuffer();

    // keep track of which categories have judges
    final Map<String, Set<String>> hash = new HashMap<String, Set<String>>();

    // populate a hash where key is category name and value is an empty
    // Set. Use set so there are no duplicates
    final List<Element> subjectiveCategories = new NodelistElementCollectionAdapter(
                                                                                    challengeDocument.getDocumentElement()
                                                                                                     .getElementsByTagName("subjectiveCategory")).asList();

    for (final Element element : subjectiveCategories) {
      final String categoryName = element.getAttribute("name");
      hash.put(categoryName, new HashSet<String>());
    }

    final Collection<JudgeInformation> judges = new LinkedList<JudgeInformation>();
    
    // walk request and push judge id into the Set, if not null or empty,
    // in the value for each category in the hash.
    int row = 1;
    String id = request.getParameter("id"
        + row);
    String category = request.getParameter("cat"
        + row);
    String division = request.getParameter("div"
        + row);
    while (null != category) {
      if (null != id) {
        id = id.trim();
        id = id.toUpperCase();
        if (id.length() > 0) {
          final Set<String> set = hash.get(category);
          set.add(id);

          final JudgeInformation judge = new JudgeInformation(id, category, division);
          judges.add(judge);
        }
      }

      row++;
      id = request.getParameter("id"
          + row);
      category = request.getParameter("cat"
          + row);
      division = request.getParameter("div"
          + row);
    }
    
    session.setAttribute(GatherJudgeInformation.JUDGES_KEY, judges);

    // now walk the keys of the hash and make sure that all values have a list
    // of size > 0, otherwise append an error to error.
    for (final Map.Entry<String, Set<String>> entry : hash.entrySet()) {
      final String categoryName = entry.getKey();
      final Set<String> set = entry.getValue();
      if (set.isEmpty()) {
        error.append("You must specify at least one judge for "
            + categoryName + "<br>");
      }
    }

    if (error.length() > 0) {
      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error' id='error'>"
          + error.toString() + "</p>");
      response.sendRedirect(response.encodeRedirectURL("GatherJudgeInformation"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("displayJudges.jsp"));
    }

  }

}
