/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.eval.NotImplementedException;

import fll.util.LogUtils;

@WebServlet("/api/ChallengeDescription")
public class ChallengeDescriptionServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
//    final ServletContext application = getServletContext();

//    response.reset();
//    response.setContentType("application/json");
//    final PrintWriter writer = response.getWriter();
//
//    final Gson gson = new Gson();

//    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
//
//    final String json = gson.toJson(challengeDescription);
//    writer.print(json);

    throw new NotImplementedException("Not currently implemented as this causes infinite recursion");
    
  }

  
}
