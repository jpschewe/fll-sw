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
import java.text.ParseException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import fll.Utilities;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.web.BaseFLLServlet;
import fll.web.UploadProcessor;

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
    try {
      UploadProcessor.processUpload(request);
      final FileItem advanceFileItem = (FileItem) request.getAttribute("advanceFile");      
      final String extension = Utilities.determineExtension(advanceFileItem);
      final File file = File.createTempFile("fll", extension);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Wrote advancing teams data to: "
            + file.getAbsolutePath());
      }
      advanceFileItem.write(file);
      
      // keep track of for later
      session.setAttribute("advanceFile", file.getAbsolutePath());
      
      // in case it doesn't get cleaned up after filtering
      file.deleteOnExit();
           
      putHeadersInSession(file, session);      
      
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving advancment data into the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving advancment data into the database", sqle);
    } catch (final ParseException e) {
      message.append("<p class='error'>Error saving team data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving advancment data into the database", e);
    } catch (final FileUploadException e) {
      message.append("<p class='error'>Error processing team data upload: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error processing advancment data upload", e);
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
   */
  public static void putHeadersInSession(final File file, final HttpSession session) throws SQLException, IOException {
    final CellFileReader reader;
    if (file.getName().endsWith(".xls")
        || file.getName().endsWith(".xslx")) {
      reader = new ExcelCellReader(file);
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
