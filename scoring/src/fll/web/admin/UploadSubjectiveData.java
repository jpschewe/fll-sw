/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.subjective.SubjectiveUtils;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.UploadProcessor;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.XMLUtils;

/**
 * Java code behind uploading subjective scores
 */
@WebServlet("/admin/UploadSubjectiveData")
public final class UploadSubjectiveData extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();

    final File file = File.createTempFile("fll", null);
    Connection connection = null;
    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      final FileItem subjectiveFileItem = (FileItem) request.getAttribute("subjectiveFile");
      subjectiveFileItem.write(file);

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();
      saveSubjectiveData(file, Queries.getCurrentTournament(connection),
                         ApplicationAttributes.getChallengeDescription(application), connection, application);
      message.append("<p id='success'><i>Subjective data uploaded successfully</i></p>");
    } catch (final SAXParseException spe) {
      final String errorMessage = String.format("Error parsing file line: %d column: %d%n Message: %s%n This may be caused by using the wrong version of the software attempting to parse a file that is not subjective data.",
                                                spe.getLineNumber(), spe.getColumnNumber(), spe.getMessage());
      message.append("<p class='error'>"
          + errorMessage + "</p>");
      LOGGER.error(errorMessage, spe);
    } catch (final SAXException se) {
      final String errorMessage = "The subjective scores file was found to be invalid, check that you are parsing a subjective scores file and not something else";
      message.append("<p class='error'>"
          + errorMessage + "</p>");
      LOGGER.error(errorMessage, se);
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving subjective data into the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving subjective data into the database", sqle);
    } catch (final ParseException e) {
      message.append("<p class='error'>Error saving subjective data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving subjective data into the database", e);
    } catch (final FileUploadException e) {
      message.append("<p class='error'>Error processing subjective data upload: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error processing subjective data upload", e);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving subjective data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving subjective data into the database", e);
    } finally {
      if (!file.delete()) {
        LOGGER.warn("Unable to delete file "
            + file.getAbsolutePath() + ", setting to delete on exit");
        file.deleteOnExit();
      }
      SQLFunctions.close(connection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

  private static final ThreadLocal<DateFormat> DATE_TIME_FORMAT = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
    }
  };

  /**
   * Save the data stored in file to the database and update the subjective
   * score totals.
   * 
   * @param file the file to read the data from
   * @param connection the database connection to write to
   * @throws SAXException if there is an error parsing the document
   */
  public static void saveSubjectiveData(final File file,
                                        final int currentTournament,
                                        final ChallengeDescription challengeDescription,
                                        final Connection connection,
                                        final ServletContext application)
                                            throws SQLException, IOException, ParseException, SAXException {
    if (LOGGER.isDebugEnabled()) {
      try {
        LOGGER.debug("Saving uploaded file to ");
        final String baseFilename = "subjective-upload_"
            + DATE_TIME_FORMAT.get().format(new Date());
        final String filename = application.getRealPath("/WEB-INF/"
            + baseFilename);

        final File copy = new File(filename);
        FileOutputStream output = null;
        FileInputStream input = null;
        try {
          input = new FileInputStream(file);
          output = new FileOutputStream(copy);
          IOUtils.copy(input, output);
        } finally {
          IOUtils.closeQuietly(input);
          IOUtils.closeQuietly(output);
        }
      } catch (final IOException e) {
        LOGGER.debug("Error creating copy of subjective datafile", e);
      }
    }
    ZipFile zipfile = null;
    Document scoreDocument = null;
    try {
      try {
        zipfile = new ZipFile(file);

        // read in score data
        final ZipEntry scoreZipEntry = zipfile.getEntry("score.xml");
        if (null == scoreZipEntry) {
          throw new RuntimeException("Zipfile does not contain score.xml as expected");
        }
        final InputStream scoreStream = zipfile.getInputStream(scoreZipEntry);
        scoreDocument = XMLUtils.parseXMLDocument(scoreStream);
        scoreStream.close();
        zipfile.close();

      } catch (final ZipException ze) {
        LOGGER.info("Subjective upload is not a zip file, trying as an XML file");

        // not a zip file, parse as just the XML file
        FileInputStream fis = null;
        try {
          fis = new FileInputStream(file);
          scoreDocument = XMLUtils.parseXMLDocument(fis);
        } finally {
          IOUtils.closeQuietly(fis);
        }
      }

      if (null == scoreDocument) {
        throw new FLLRuntimeException("Cannot parse input as a compressed subjective data file or an uncompressed XML file");
      }

      saveSubjectiveData(scoreDocument, currentTournament, challengeDescription, connection);
    } finally {
      if (null != zipfile) {
        zipfile.close();
      }
    }
  }

  /**
   * Save the subjective data in scoreDocument to the database.
   */
  public static void saveSubjectiveData(final Document scoreDocument,
                                        final int currentTournament,
                                        final ChallengeDescription challengeDescription,
                                        final Connection connection) throws SQLException, IOException, ParseException {

    // make sure all judges exist in the database first
    addMissingJudges(connection, currentTournament, scoreDocument);
    
    final Element scoresElement = scoreDocument.getDocumentElement();
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("first element: "
          + scoresElement);
    }

    for (final Element scoreCategoryNode : new NodelistElementCollectionAdapter(scoresElement.getChildNodes())) {
      final Element scoreCategoryElement = scoreCategoryNode; // "subjectiveCategory"
      final String categoryName = scoreCategoryElement.getAttribute("name");

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Saving category: "
            + categoryName);
      }

      ScoreCategory categoryElement = null;
      for (final ScoreCategory cat : challengeDescription.getSubjectiveCategories()) {
        if (cat.getName().equals(categoryName)) {
          categoryElement = cat;
        }
      }
      if (null == categoryElement) {
        throw new RuntimeException("Cannot find subjective category description for category in score document category: "
            + categoryName);
      }

      saveCategoryData(currentTournament, connection, scoreCategoryElement, categoryName, categoryElement);
    }

    removeNullSubjectiveRows(connection, currentTournament, challengeDescription);

    final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament);
    tournament.recordSubjectiveModified(connection);
  }

  /**
   * Remove subjective score rows from database that are empty. These
   * are rows that have null for all scores and is not a no show.
   * 
   * @param connection database connection
   * @param tournamentId which tournament to work on
   * @param challengeDescription the challenge description
   */
  public static void removeNullSubjectiveRows(final Connection connection,
                                              final int tournamentId,
                                              final ChallengeDescription challengeDescription) throws SQLException {
    for (final ScoreCategory cat : challengeDescription.getSubjectiveCategories()) {
      removeNullRows(tournamentId, connection, cat.getName(), cat);
    }
  }

  /**
   * Remove rows from the specified subjective category that are empty. These
   * are rows that have null for all scores and is not a no show.
   * 
   * @param currentTournament
   * @param connection
   * @param categoryName
   * @param categoryElement
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns are dynamic")
  private static void removeNullRows(final int currentTournament,
                                     final Connection connection,
                                     final String categoryName,
                                     final ScoreCategory categoryElement) throws SQLException {
    final List<AbstractGoal> goalDescriptions = categoryElement.getGoals();
    PreparedStatement prep = null;
    try {
      final StringBuffer sql = new StringBuffer();
      sql.append("DELETE FROM "
          + categoryName + " WHERE NoShow <> ? ");
      for (final AbstractGoal goalDescription : goalDescriptions) {
        sql.append(" AND "
            + goalDescription.getName() + " IS NULL ");
      }

      sql.append(" AND Tournament = ?");
      prep = connection.prepareStatement(sql.toString());
      prep.setBoolean(1, true);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();

    } finally {
      SQLFunctions.close(prep);
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "columns are dynamic")
  private static void saveCategoryData(final int currentTournament,
                                       final Connection connection,
                                       final Element scoreCategoryElement,
                                       final String categoryName,
                                       final ScoreCategory categoryElement) throws SQLException, ParseException {
    final List<AbstractGoal> goalDescriptions = categoryElement.getGoals();

    PreparedStatement insertPrep = null;
    PreparedStatement updatePrep = null;
    try {
      // prepare statements for update and insert

      final StringBuffer updateStmt = new StringBuffer();
      final StringBuffer insertSQLColumns = new StringBuffer();
      insertSQLColumns.append("INSERT INTO "
          + categoryName + " (TeamNumber, Tournament, Judge, NoShow");
      final StringBuffer insertSQLValues = new StringBuffer();
      insertSQLValues.append(") VALUES ( ?, ?, ?, ?");
      updateStmt.append("UPDATE "
          + categoryName + " SET NoShow = ? ");
      final int numGoals = goalDescriptions.size();
      for (final AbstractGoal goalDescription : goalDescriptions) {
        insertSQLColumns.append(", "
            + goalDescription.getName());
        insertSQLValues.append(", ?");
        updateStmt.append(", "
            + goalDescription.getName() + " = ?");
      }

      updateStmt.append(" WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
      updatePrep = connection.prepareStatement(updateStmt.toString());
      insertPrep = connection.prepareStatement(insertSQLColumns.toString()
          + insertSQLValues.toString() + ")");
      // initialze the tournament
      insertPrep.setInt(2, currentTournament);
      updatePrep.setInt(numGoals
          + 3, currentTournament);

      for (final Element scoreElement : new NodelistElementCollectionAdapter(scoreCategoryElement.getElementsByTagName("score"))) {

        if (scoreElement.hasAttribute("modified")
            && "true".equalsIgnoreCase(scoreElement.getAttribute("modified"))) {
          final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(scoreElement.getAttribute("teamNumber"))
                                                                 .intValue();

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving score data for team: "
                + teamNumber);
          }

          final String judgeId = scoreElement.getAttribute("judge");
          final boolean noShow = Boolean.parseBoolean(scoreElement.getAttribute("NoShow"));
          updatePrep.setBoolean(1, noShow);
          insertPrep.setBoolean(4, noShow);

          insertPrep.setInt(1, teamNumber);
          updatePrep.setInt(numGoals
              + 2, teamNumber);
          insertPrep.setString(3, judgeId);
          updatePrep.setString(numGoals
              + 4, judgeId);

          int goalIndex = 0;
          for (final AbstractGoal goalDescription : goalDescriptions) {
            final String goalName = goalDescription.getName();

            final Element subscoreElement = SubjectiveUtils.getSubscoreElement(scoreElement, goalName);
            if (null == subscoreElement) {
              // no subscore element, no show or deleted
              insertPrep.setNull(goalIndex
                  + 5, Types.DOUBLE);
              updatePrep.setNull(goalIndex
                  + 2, Types.DOUBLE);
            } else {
              final String value = subscoreElement.getAttribute("value");
              if (!value.trim().isEmpty()) {
                insertPrep.setString(goalIndex
                    + 5, value.trim());
                updatePrep.setString(goalIndex
                    + 2, value.trim());
              } else {
                insertPrep.setNull(goalIndex
                    + 5, Types.DOUBLE);
                updatePrep.setNull(goalIndex
                    + 2, Types.DOUBLE);
              }
            }

            ++goalIndex;
          } // end for

          // attempt the update first
          final int modifiedRows = updatePrep.executeUpdate();
          if (modifiedRows < 1) {
            // do insert if nothing was updated
            insertPrep.executeUpdate();
          }
        }
      }

    } finally {
      SQLFunctions.close(insertPrep);
      SQLFunctions.close(updatePrep);
    }

  }

  /**
   * Add any judges to the database that are referenced in the score file that
   * aren't already in the database.
   */
  private static void addMissingJudges(final Connection connection,
                                       final int tournamentId,
                                       final Document scoreDocument) throws SQLException {

    PreparedStatement insertJudge = null;
    try {
      insertJudge = connection.prepareStatement("INSERT INTO Judges (id, category, Tournament, station) VALUES (?, ?, ?, ?)");
      insertJudge.setInt(3, tournamentId);

      final Collection<JudgeInformation> currentJudges = JudgeInformation.getJudges(connection, tournamentId);

      final Element scoresElement = scoreDocument.getDocumentElement();

      for (final Element scoreCategoryNode : new NodelistElementCollectionAdapter(scoresElement.getChildNodes())) {
        final Element scoreCategoryElement = scoreCategoryNode; // "subjectiveCategory"
        final String categoryName = scoreCategoryElement.getAttribute("name");

        for (final Element scoreElement : new NodelistElementCollectionAdapter(scoreCategoryElement.getElementsByTagName("score"))) {
          final String judgeId = scoreElement.getAttribute("judge");
          final String station = scoreElement.getAttribute("judging_station");
          final JudgeInformation judge = new JudgeInformation(judgeId, categoryName, station);
          if (!doesJudgeExist(currentJudges, judge)) {
            // add judge
            insertJudge.setString(1, judge.getId());
            insertJudge.setString(2, judge.getCategory());
            insertJudge.setString(4, judge.getGroup());
            insertJudge.executeUpdate();

            currentJudges.add(judge);
          }
        } // foreach score
      } // foreach category

    } finally {
      SQLFunctions.close(insertJudge);
    }

  }

  /**
   * Is judge in currentJudges?
   */
  private static boolean doesJudgeExist(final Collection<JudgeInformation> currentJudges,
                                        final JudgeInformation judge) {
    for (final JudgeInformation cjudge : currentJudges) {
      if (ComparisonUtils.safeEquals(cjudge, judge)) {
        return true;
      }
    }
    return false;
  }

}
