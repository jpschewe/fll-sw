/*
 * Copyright (c) 2008
 *      Jon Schewe.  All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * I'd appreciate comments/suggestions on the code jpschewe@mtu.net
 */
package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;

/**
 * Initialize web attributes.
 * 
 * @author jpschewe
 */
public final class Init {

  private static final Logger LOGGER = Logger.getLogger(Init.class);

  private Init() {
    // no instances
  }
  
  /**
   * @param request
   * @param response
   * @return the URL to redirect to if there was trouble. This should be passed
   *         to encodeRedirectURL. If this is null, then everything initialized
   *         OK
   * @throws IOException
   * @throws RuntimeException
   * @throws SQLException
   */
  public static String initialize(final HttpServletRequest request, final HttpServletResponse response) throws IOException, SQLException, RuntimeException {
    final HttpSession session = request.getSession();
    final ServletContext application = session.getServletContext();

    final String database = ApplicationAttributes.getDatabase(application);
    
    // FIXME put all of this in a filter and don't put the filter on setup/*
    
    final boolean dbok = Utilities.testHSQLDB(database);
    if (!dbok) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Database files not ok, redirecting to setup");
      }
      session
      .setAttribute(SessionAttributes.MESSAGE,
                    "<p class='error'>The database does not exist yet or there is a problem with the database files. Please create the database.<br/></p>");
      return request.getContextPath()
      + "/setup";
    }      


    // initialize the datasource
    final DataSource datasource;
    if (null == session.getAttribute(SessionAttributes.DATASOURCE)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Datasource not available, creating");
      }
      datasource = Utilities.createDataSource(database);
      session.setAttribute(SessionAttributes.DATASOURCE, datasource);
    } else {
      datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);
    }

    // Initialize the connection
    final Connection connection = datasource.getConnection();

    // check if the database is initialized
    final boolean dbinitialized = Utilities.testDatabaseInitialized(connection);
    if (!dbinitialized) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Database not initialized, redirecting to setup");
      }
      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>The database is not yet initialized. Please create the database.<br/></p>");
      return request.getContextPath()
          + "/setup";
    }

    // load the challenge descriptor
    if (null == application.getAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loading challenge descriptor");
      }
      final Document document = Queries.getChallengeDocument(connection);
      if (null == document) {
        throw new RuntimeException("Could not find xml challenge description in the database!");
      }
      application.setAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT, document);
    }

    // keep browser from caching any content
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", 0); // prevents caching at the proxy
    // server

    return null;
  }
}
