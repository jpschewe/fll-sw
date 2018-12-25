/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Document;

import fll.db.DumpDB;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Export the database using a name with the tournament name, date and label
 * that it's the performance seeding export.
 */
@WebServlet("/admin/ExportPerformanceData")
public class ExportPerformanceData extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    try (Connection connection = datasource.getConnection()) {
      DumpDB.exportDatabase(response, application, "_perf-after-seeding", challengeDocument, connection);
    } catch (final SQLException sqle) {
      throw new RuntimeException(sqle);
    }
  }

}
