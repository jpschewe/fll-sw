/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import fll.xml.XMLWriter;

/**
 * 
 */
public class DisplayChallengeDescriptor extends BaseFLLServlet {

  /**
   * @see fll.web.BaseFLLServlet#processRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.ServletContext, javax.servlet.http.HttpSession)
   */
  @Override
  protected void processRequest(final HttpServletRequest request, 
                                final HttpServletResponse response, 
                                final ServletContext application, 
                                final HttpSession session) throws IOException,
      ServletException {
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

    final XMLWriter xmlwriter = new XMLWriter();

    response.reset();
    response.setContentType("text/xml");
    response.setHeader("Content-Disposition", "filename=challenge.xml");
    xmlwriter.setOutput(response.getOutputStream(), null);
    xmlwriter.setStyleSheet("fll.css");
    xmlwriter.write(challengeDocument);
  }

}
