/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Upload the teams to be advanced.
 */
public final class UploadAdvancingTeams extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(UploadAdvancingTeams.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();
    
    final String fileName = SessionAttributes.getNonNullAttribute(session, "spreadsheetFile", String.class);
    final File file = new File(fileName);
    try {      
      // keep track of for later
      session.setAttribute("advanceFile", file.getAbsolutePath());
      
      final String sheetName = SessionAttributes.getAttribute(session, "sheetName", String.class);

      putHeadersInSession(file, sheetName, session);            
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving advancment data into the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving advancment data into the database", sqle);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving advancment data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving advancment data into the database", e);
    }
    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL("chooseAdvancementColumns.jsp"));

  }

  /**
   * Get the headers from the file.
   * 
   * @param file the file that the data is in
   * @param session used to store the headers, attribute is "fileHeaders" and is of type String[]
   * @throws IOException if an error occurs reading from file
   * @throws InvalidFormatException 
   */
  public static void putHeadersInSession(final File file, final String sheetName, final HttpSession session) throws SQLException, IOException, InvalidFormatException {
    final CellFileReader reader;
    if (file.getName().endsWith(".xls")
        || file.getName().endsWith(".xslx")) {
      reader = new ExcelCellReader(file, sheetName);
    } else {
      // determine if the file is tab separated or comma separated, check the
      // first line for tabs and if they aren't found, assume it's comma
      // separated
      final BufferedReader breader = new BufferedReader(new FileReader(file));
      try {
        final String firstLine = breader.readLine();
        if (null == firstLine) {
          LOGGER.error("Empty file");
          return;
        }
        if (firstLine.indexOf("\t") != -1) {
          reader = new CSVCellReader(file, '\t');
        } else {
          reader = new CSVCellReader(file);
        }
      } finally {
        breader.close();
      }
    }

    // parse out the first non-blank line as the names of the columns
    String[] columnNames = reader.readNext();
    while(columnNames.length < 1) {
      columnNames = reader.readNext();  
    }
    

    // save this for other pages to use
    session.setAttribute("fileHeaders", columnNames);
  }

}
