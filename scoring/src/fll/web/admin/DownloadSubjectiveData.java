/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fll.Team;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.XMLUtils;

/**
 * Downlaod the data file for the subjective score app.
 */
@WebServlet("/admin/subjective-data.fll")
public class DownloadSubjectiveData extends BaseFLLServlet {

  public static final String SUBJECTIVE_CATEGORY_NODE_NAME = "subjectiveCategory";

  public static final String SCORE_NODE_NAME = "score";

  public static final String SCORES_NODE_NAME = "scores";

  public static final String SUBSCORE_NODE_NAME = "subscore";

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      if (Queries.isJudgesProperlyAssigned(connection, challengeDescription)) {
        response.reset();
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "filename=subjective-data.fll");
        writeSubjectiveScores(connection, challengeDocument, challengeDescription, response.getOutputStream());
      } else {
        response.reset();
        response.setContentType("text/plain");
        final ServletOutputStream os = response.getOutputStream();
        os.println("Judges are not properly assigned, please go back to the administration page and assign judges");
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Create a document to hold subject scores for the tournament described in
   * challengeDocument.
   * 
   * @param teams the teams for this tournament
   * @param connection the database connection used to retrieve the judge
   *          information
   * @param currentTournament the tournament to generate the document for, used
   *          for deciding which set of judges to use
   * @return the document
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static Document createSubjectiveScoresDocument(final ChallengeDescription challengeDescription,
                                                        final Collection<? extends Team> teams,
                                                        final Connection connection,
                                                        final int currentTournament) throws SQLException {
    ResultSet rs = null;
    ResultSet rs2 = null;
    PreparedStatement prep = null;
    PreparedStatement prep2 = null;
    try {
      prep = connection.prepareStatement("SELECT id, station FROM Judges WHERE category = ? AND Tournament = ?");
      prep.setInt(2, currentTournament);

      final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();
      final Element top = document.createElementNS(null, SCORES_NODE_NAME);
      document.appendChild(top);

      for (final ScoreCategory categoryDescription : challengeDescription.getSubjectiveCategories()) {
        final String categoryName = categoryDescription.getName();
        final Element categoryElement = document.createElementNS(null, SUBJECTIVE_CATEGORY_NODE_NAME);
        top.appendChild(categoryElement);
        categoryElement.setAttributeNS(null, "name", categoryName);

        prep.setString(1, categoryName);
        rs = prep.executeQuery();
        while (rs.next()) {
          final String judge = rs.getString(1);
          final String judgingStation = rs.getString(2);

          for (final Team team : teams) {
            final String teamJudgingStation = Queries.getJudgingStation(connection, team.getTeamNumber(),
                                                                        currentTournament);
            if (judgingStation.equals(teamJudgingStation)) {
              final String teamDiv = Queries.getEventDivision(connection, team.getTeamNumber());

              final Element scoreElement = document.createElementNS(null, SCORE_NODE_NAME);
              categoryElement.appendChild(scoreElement);

              scoreElement.setAttributeNS(null, "teamName", team.getTeamName());
              scoreElement.setAttributeNS(null, "teamNumber", String.valueOf(team.getTeamNumber()));
              scoreElement.setAttributeNS(null, "division", teamDiv);
              scoreElement.setAttributeNS(null, "judging_station", teamJudgingStation);
              scoreElement.setAttributeNS(null, "organization", team.getOrganization());
              scoreElement.setAttributeNS(null, "judge", judge);

              prep2 = connection.prepareStatement("SELECT * FROM "
                  + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
              prep2.setInt(1, team.getTeamNumber());
              prep2.setInt(2, currentTournament);
              prep2.setString(3, judge);
              rs2 = prep2.executeQuery();
              if (rs2.next()) {
                for (final AbstractGoal goalDescription : categoryDescription.getGoals()) {
                  final String goalName = goalDescription.getName();
                  final String value = rs2.getString(goalName);
                  if (!rs2.wasNull()) {
                    final Element subscoreElement = document.createElementNS(null, SUBSCORE_NODE_NAME);
                    scoreElement.appendChild(subscoreElement);

                    subscoreElement.setAttributeNS(null, "name", goalName);
                    subscoreElement.setAttributeNS(null, "value", value);
                  }
                }
                scoreElement.setAttributeNS(null, "NoShow", rs2.getString("NoShow").toLowerCase());
              } else {
                scoreElement.setAttributeNS(null, "NoShow", "false");
              }
            }
          }
        }
      }
      return document;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(rs2);
      SQLFunctions.close(prep);
      SQLFunctions.close(prep2);
    }
  }

  /**
   * Write out the subjective scores data for the current tournament.
   * 
   * @param stream where to write the scores file
   * @throws IOException
   */
  public static void writeSubjectiveScores(final Connection connection,
                                           final Document challengeDocument,
                                           final ChallengeDescription challengeDescription,
                                           final OutputStream stream) throws IOException, SQLException {
    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);
    final int tournament = Queries.getCurrentTournament(connection);

    final ZipOutputStream zipOut = new ZipOutputStream(stream);
    final Charset charset = Charset.forName("UTF-8");
    final Writer writer = new OutputStreamWriter(zipOut, charset);

    zipOut.putNextEntry(new ZipEntry("challenge.xml"));
    XMLUtils.writeXML(challengeDocument, writer, "UTF-8");
    zipOut.closeEntry();

    final Document scoreDocument = createSubjectiveScoresDocument(challengeDescription, tournamentTeams.values(),
                                                                  connection, tournament);

    try {
      validateXML(scoreDocument);
    } catch (final SAXException e) {
      throw new FLLInternalException("Subjective XML document is invalid", e);
    }

    zipOut.putNextEntry(new ZipEntry("score.xml"));
    XMLUtils.writeXML(scoreDocument, writer, "UTF-8");
    zipOut.closeEntry();

    zipOut.close();
  }

  /**
   * Validate the schedule XML document.
   * 
   * @throws SAXException on an error
   */
  public static void validateXML(final org.w3c.dom.Document document) throws SAXException {
    try {
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final Source schemaFile = new StreamSource(classLoader.getResourceAsStream("fll/resources/subjective.xsd"));
      final Schema schema = factory.newSchema(schemaFile);

      final Validator validator = schema.newValidator();
      validator.validate(new DOMSource(document));
    } catch (final IOException e) {
      throw new RuntimeException("Internal error, should never get IOException here", e);
    }
  }

}
