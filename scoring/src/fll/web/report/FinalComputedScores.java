/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * Final computed scores report.
 * 
 * @web.servlet name="FinalComputedScores"
 * @web.servlet-mapping url-pattern="/report/finalComputedScores.pdf"
 */
public final class FinalComputedScores extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    try {
      final DataSource datasource = SessionAttributes.getDataSource(session);
      final Connection connection = datasource.getConnection();
      final org.w3c.dom.Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=finalComputedScores.pdf");

      final Element root = challengeDocument.getDocumentElement();
      final String challengeTitle = root.getAttribute("title");
      final SimpleFooterHandler pageHandler = new SimpleFooterHandler();

      generateReport(connection, response.getOutputStream(), challengeDocument, challengeTitle, tournament, pageHandler);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Font ARIAL_8PT_BOLD = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD);

  private static final Font ARIAL_8PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

  private static final Font ARIAL_8PT_NORMAL_RED = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL,
                                                                       BaseColor.RED);

  private static final Font TIMES_12PT_NORMAL = FontFactory.getFont(FontFactory.TIMES, 12, Font.NORMAL);

  /**
   * Generate the actual report.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                                             "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Sort determined by winner criteria, category name determines table name")
  private void generateReport(final Connection connection,
                              final OutputStream out,
                              final org.w3c.dom.Document challengeDocument,
                              final String challengeTitle,
                              final Tournament tournament,
                              final SimpleFooterHandler pageHandler) throws SQLException, IOException {
    if (tournament.getTournamentID() != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException(
                                    "Cannot generate final score report for a tournament other than the current tournament");
    }

    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);
    final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

    final TournamentSchedule schedule;
    if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
      if(LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found a schedule for tournament: " + tournament);
      }
      schedule = new TournamentSchedule(connection, tournament.getTournamentID());
    } else {
      schedule = null;
    }

    Statement stmt = null;
    Statement teamsStmt = null;
    ResultSet rawScoreRS = null;
    ResultSet teamsRS = null;
    PreparedStatement prep = null;
    try {
      // This creates our new PDF document and declares it to be in landscape
      // orientation
      final Document pdfDoc = new Document(PageSize.LETTER);
      final PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
      writer.setPageEvent(pageHandler);

      // Measurements are always in points (72 per inch) - This sets up 1/2 inch
      // margins
      pdfDoc.setMargins(0.5f * 72, 0.5f * 72, 0.5f * 72, 0.5f * 72);
      pdfDoc.open();

      final Element rootElement = challengeDocument.getDocumentElement();

      final List<Element> subjectiveCategories = new NodelistElementCollectionAdapter(
                                                                                      rootElement
                                                                                                 .getElementsByTagName("subjectiveCategory"))
                                                                                                                                             .asList();
      stmt = connection.createStatement();
      teamsStmt = connection.createStatement();

      final Iterator<String> divisionIter = Queries.getEventDivisions(connection).iterator();
      while (divisionIter.hasNext()) {
        final String division = divisionIter.next();

        // Figure out how many subjective categories have weights > 0.
        final double[] weights = new double[subjectiveCategories.size()];
        final Element[] catElements = new Element[subjectiveCategories.size()];
        int nonZeroWeights = 0;
        for (int cat = 0; cat < subjectiveCategories.size(); cat++) {
          catElements[cat] = subjectiveCategories.get(cat);
          weights[cat] = Utilities.NUMBER_FORMAT_INSTANCE.parse(catElements[cat].getAttribute("weight")).doubleValue();
          if (weights[cat] > 0.0) {
            nonZeroWeights++;
          }
        }
        // Array of relative widths for the columns of the score page
        // Array length varies with the number of subjective scores weighted >
        // 0.
        final int numColumnsLeftOfSubjective;
        if (null == schedule) {
          numColumnsLeftOfSubjective = 2;
        } else {
          numColumnsLeftOfSubjective = 3;
        }
        final int numColumnsRightOfSubjective = 2;
        final float[] relativeWidths = new float[nonZeroWeights
            + numColumnsLeftOfSubjective + numColumnsRightOfSubjective];
        relativeWidths[0] = 4f;
        relativeWidths[1] = 1.0f;
        if(null != schedule) {
          relativeWidths[2] = 1.0f;
        }
        relativeWidths[relativeWidths.length
            - numColumnsRightOfSubjective] = 1.5f;
        relativeWidths[relativeWidths.length
            - numColumnsRightOfSubjective + 1] = 1.5f;
        for (int i = numColumnsLeftOfSubjective; i < numColumnsRightOfSubjective
            + nonZeroWeights; i++) {
          relativeWidths[i] = 1.5f;
        }

        // Create a table to hold all the scores for this division
        final PdfPTable divTable = new PdfPTable(relativeWidths);
        divTable.getDefaultCell().setBorder(0);
        divTable.setWidthPercentage(100);

        final PdfPTable header = createHeader(challengeTitle, tournament.getName(), division);
        final PdfPCell headerCell = new PdfPCell(header);
        headerCell.setColspan(relativeWidths.length);
        divTable.addCell(headerCell);

        // /////////////////////////////////////////////////////////////////////
        // Write the table column headers
        // /////////////////////////////////////////////////////////////////////
        final PdfPCell teamCell = new PdfPCell(new Phrase("Organization / Team # / Team Name", ARIAL_8PT_BOLD));
        teamCell.setBorder(0);
        teamCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        divTable.addCell(teamCell);

        // judging group
        if (null != schedule) {
          final Paragraph judgingStation = new Paragraph("Judging", ARIAL_8PT_BOLD);
          judgingStation.add(Chunk.NEWLINE);
          judgingStation.add(new Chunk("Station"));
          final PdfPCell osCell = new PdfPCell(judgingStation);
          osCell.setBorder(0);
          osCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          osCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
          divTable.addCell(osCell);
        }

        divTable.addCell(""); // weight/raw&scaled

        for (int cat = 0; cat < catElements.length; cat++) {
          if (weights[cat] > 0.0) {
            final String catTitle = catElements[cat].getAttribute("title");

            final Paragraph catPar = new Paragraph(catTitle, ARIAL_8PT_BOLD);
            final PdfPCell catCell = new PdfPCell(catPar);
            catCell.setBorder(0);
            catCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            catCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
            divTable.addCell(catCell);
          }
        }

        final Paragraph perfPar = new Paragraph("Performance", ARIAL_8PT_BOLD);
        final PdfPCell perfCell = new PdfPCell(perfPar);
        perfCell.setBorder(0);
        perfCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        perfCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        divTable.addCell(perfCell);

        final Paragraph overallScore = new Paragraph("Overall", ARIAL_8PT_BOLD);
        overallScore.add(Chunk.NEWLINE);
        overallScore.add(new Chunk("Score"));
        final PdfPCell osCell = new PdfPCell(overallScore);
        osCell.setBorder(0);
        osCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        osCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        divTable.addCell(osCell);

        // /////////////////////////////////////////////////////////////////////
        // Write a table row with the relative weights of the subjective scores
        // /////////////////////////////////////////////////////////////////////

        final Paragraph wPar = new Paragraph("Weight:", ARIAL_8PT_NORMAL);
        final PdfPCell wCell = new PdfPCell(wPar);
        wCell.setColspan(numColumnsLeftOfSubjective);
        wCell.setBorder(0);
        wCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        divTable.addCell(wCell);

        final PdfPCell[] wCells = new PdfPCell[subjectiveCategories.size()];
        final Paragraph[] wPars = new Paragraph[subjectiveCategories.size()];
        for (int cat = 0; cat < catElements.length; cat++) {
          if (weights[cat] > 0.0) {
            wPars[cat] = new Paragraph(Double.toString(weights[cat]), ARIAL_8PT_NORMAL);
            wCells[cat] = new PdfPCell(wPars[cat]);
            wCells[cat].setBorder(0);
            wCells[cat].setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            divTable.addCell(wCells[cat]);
          }
        }

        final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
        final double perfWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(performanceElement.getAttribute("weight"))
                                                                  .doubleValue();
        final Paragraph perfWeightPar = new Paragraph(Double.toString(perfWeight), ARIAL_8PT_NORMAL);
        final PdfPCell perfWeightCell = new PdfPCell(perfWeightPar);
        perfWeightCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        perfWeightCell.setBorder(0);
        divTable.addCell(perfWeightCell);

        divTable.addCell("");

        PdfPCell blankCell = new PdfPCell();
        blankCell.setBorder(0);
        blankCell.setBorderWidthBottom(1.0f);
        blankCell.setColspan(relativeWidths.length);
        divTable.addCell(blankCell);

        divTable.setHeaderRows(4); // Cause the first 4 rows to be repeated on
        // each page - 1 row for box header, 2 rows text headers and 1 for
        // the horizontal line.

        final StringBuilder query = new StringBuilder();
        query
             .append("SELECT Teams.Organization,Teams.TeamName,Teams.TeamNumber,FinalScores.OverallScore,FinalScores.performance");
        for (int cat = 0; cat < catElements.length; cat++) {
          if (weights[cat] > 0.0) {
            final String catName = catElements[cat].getAttribute("name");
            query.append(",FinalScores."
                + catName);
          }
        }
        // TODO make prepared statement
        query.append(" FROM Teams,FinalScores,current_tournament_teams");
        query.append(" WHERE FinalScores.TeamNumber = Teams.TeamNumber");
        query.append(" AND FinalScores.Tournament = "
            + tournament.getTournamentID());
        query.append(" AND current_tournament_teams.event_division = ?");
        query.append(" AND current_tournament_teams.TeamNumber = Teams.TeamNumber");
        query.append(" ORDER BY FinalScores.OverallScore "
            + ascDesc + ", Teams.TeamNumber");
        prep = connection.prepareStatement(query.toString());
        prep.setString(1, division);
        teamsRS = prep.executeQuery();
        while (teamsRS.next()) {
          final int teamNumber = teamsRS.getInt(3);
          final String organization = teamsRS.getString(1);
          final String teamName = teamsRS.getString(2);

          final double totalScore;
          final double ts = teamsRS.getDouble(4);
          if (teamsRS.wasNull()) {
            totalScore = Double.NaN;
          } else {
            totalScore = ts;
          }

          // ///////////////////////////////////////////////////////////////////
          // Build a table of data for this team
          // ///////////////////////////////////////////////////////////////////
          final PdfPTable curteam = new PdfPTable(relativeWidths);
          curteam.getDefaultCell().setBorder(0);

          // The first row of the team table...
          // First column is organization name
          final PdfPCell teamCol = new PdfPCell(new Phrase(organization, ARIAL_8PT_NORMAL));
          teamCol.setBorder(0);
          teamCol.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
          curteam.addCell(teamCol);
          curteam.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

          if (null != schedule) {
            // insert judging station here
            final TeamScheduleInfo si = schedule.getSchedInfoForTeam(teamNumber);
            if (null == si) {
              if(LOGGER.isTraceEnabled()) {
                LOGGER.trace("Got null sched info for team " + teamNumber);
              }
              curteam.addCell("");
            } else {
              if(LOGGER.isTraceEnabled()) {
                LOGGER.trace("Found judging station " + si.getJudgingStation() + " for team " + teamNumber);
              }
              curteam.addCell(new Phrase(si.getJudgingStation(), ARIAL_8PT_NORMAL));
            }
          }

          // Second column is "Raw:"
          curteam.addCell(new Phrase("Raw:", ARIAL_8PT_NORMAL));

          // Next, one column containing the raw score for each subjective
          // category with weight > 0
          for (int cat = 0; cat < subjectiveCategories.size(); cat++) {
            final Element catElement = subjectiveCategories.get(cat);
            final double catWeight = weights[cat];
            if (catWeight > 0.0) {
              final String catName = catElement.getAttribute("name");
              rawScoreRS = stmt.executeQuery("SELECT ComputedTotal"
                  // TODO make prepared statement
                  + " FROM " + catName + " WHERE TeamNumber = " + teamNumber + " AND Tournament = "
                  + tournament.getTournamentID() + " ORDER BY ComputedTotal " + ascDesc);
              boolean scoreSeen = false;
              final StringBuilder rawScoreText = new StringBuilder();
              while (rawScoreRS.next()) {
                final double v = rawScoreRS.getDouble(1);
                if (!rawScoreRS.wasNull()) {
                  if (scoreSeen) {
                    rawScoreText.append(", ");
                  } else {
                    scoreSeen = true;
                  }
                  rawScoreText.append(Utilities.NUMBER_FORMAT_INSTANCE.format(v));
                }
              }
              final PdfPCell subjCell = new PdfPCell((!scoreSeen ? new Phrase("No Score", ARIAL_8PT_NORMAL_RED)
                  : new Phrase(rawScoreText.toString(), ARIAL_8PT_NORMAL)));
              subjCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
              subjCell.setBorder(0);
              curteam.addCell(subjCell);
              rawScoreRS.close();
            }
          }

          // Column for the highest performance score of the seeding rounds
          rawScoreRS = stmt.executeQuery("SELECT score FROM performance_seeding_max"
              // TODO make prepared statement
              + " WHERE TeamNumber = " + teamNumber + " AND Tournament = " + tournament.getTournamentID());
          final double rawScore;
          if (rawScoreRS.next()) {
            final double v = rawScoreRS.getDouble(1);
            if (rawScoreRS.wasNull()) {
              rawScore = Double.NaN;
            } else {
              rawScore = v;
            }
          } else {
            rawScore = Double.NaN;
          }
          PdfPCell pCell = new PdfPCell((Double.isNaN(rawScore) ? new Phrase("No Score", ARIAL_8PT_NORMAL_RED)
              : new Phrase(Utilities.NUMBER_FORMAT_INSTANCE.format(rawScore), ARIAL_8PT_NORMAL)));
          pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          pCell.setBorder(0);
          curteam.addCell(pCell);
          rawScoreRS.close();

          // The "Overall score" column is not filled in for raw scores
          curteam.addCell("");

          // The second row of the team table...
          // First column contains the team # and name
          final PdfPCell teamNameCol = new PdfPCell(new Phrase(Integer.toString(teamNumber)
              + " " + teamName, ARIAL_8PT_NORMAL));
          teamNameCol.setBorder(0);
          teamNameCol.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
          curteam.addCell(teamNameCol);

          if (null != schedule) {
            // nothing to show in second row for judging station
            curteam.addCell("");
          }

          // Second column contains "Scaled:"
          curteam.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
          curteam.addCell(new Phrase("Scaled:", ARIAL_8PT_NORMAL));

          // Next, one column containing the scaled score for each subjective
          // category with weight > 0
          for (int cat = 0; cat < subjectiveCategories.size(); cat++) {
            final double catWeight = weights[cat];
            if (catWeight > 0.0) {
              final double scaledScore;
              final double v = teamsRS.getDouble(5 + cat + 1);
              if (teamsRS.wasNull()) {
                scaledScore = Double.NaN;
              } else {
                scaledScore = v;
              }

              final PdfPCell subjCell = new PdfPCell((Double.isNaN(scaledScore) ? new Phrase("No Score",
                                                                                             ARIAL_8PT_NORMAL_RED)
                  : new Phrase(Utilities.NUMBER_FORMAT_INSTANCE.format(scaledScore), ARIAL_8PT_NORMAL)));
              subjCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
              subjCell.setBorder(0);
              curteam.addCell(subjCell);
            }
          }

          // 2nd to last column has the scaled performance score
          {
            final double scaledScore;
            final double v = teamsRS.getDouble(5);
            if (teamsRS.wasNull()) {
              scaledScore = Double.NaN;
            } else {
              scaledScore = v;
            }

            pCell = new PdfPCell((Double.isNaN(scaledScore) ? new Phrase("No Score", ARIAL_8PT_NORMAL_RED)
                : new Phrase(Utilities.NUMBER_FORMAT_INSTANCE.format(scaledScore), ARIAL_8PT_NORMAL)));
            pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            pCell.setBorder(0);
            curteam.addCell(pCell);
          }

          // Last column contains the overall scaled score
          pCell = new PdfPCell((Double.isNaN(totalScore) ? new Phrase("No Score", ARIAL_8PT_NORMAL_RED)
              : new Phrase(Utilities.NUMBER_FORMAT_INSTANCE.format(totalScore), ARIAL_8PT_NORMAL)));
          pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          pCell.setBorder(0);
          curteam.addCell(pCell);

          // This is an empty row in the team table that is added to put a
          // horizontal rule under the team's score in the display
          blankCell = new PdfPCell();
          blankCell.setBorder(0);
          blankCell.setBorderWidthBottom(0.5f);
          blankCell.setBorderColorBottom(BaseColor.GRAY);
          blankCell.setColspan(relativeWidths.length);
          curteam.addCell(blankCell);

          // Create a new cell and add it to the division table - this cell will
          // contain the entire team table we just built above
          final PdfPCell curteamCell = new PdfPCell(curteam);
          curteamCell.setBorder(0);
          curteamCell.setColspan(relativeWidths.length);
          divTable.addCell(curteamCell);
        }

        teamsRS.close();

        // Add the division table to the document
        pdfDoc.add(divTable);

        // If there is another division to process, start it on a new page
        if (divisionIter.hasNext()) {
          pdfDoc.newPage();
        }

      } // end while(divisionIter.next())

      pdfDoc.close();
    } catch (final ParseException pe) {
      throw new RuntimeException("Error parsing category weight!", pe);
    } catch (final DocumentException de) {
      throw new RuntimeException("Error creating PDF document!", de);
    } finally {
      SQLFunctions.close(rawScoreRS);
      SQLFunctions.close(teamsRS);

      SQLFunctions.close(stmt);
      SQLFunctions.close(teamsStmt);
      SQLFunctions.close(prep);
    }
  }

  private PdfPTable createHeader(final String challengeTitle,
                                 final String tournamentName,
                                 final String division) {
    // initialization of the header table
    final PdfPTable header = new PdfPTable(2);

    final Phrase p = new Phrase();
    p.add(new Chunk(challengeTitle, TIMES_12PT_NORMAL));
    p.add(Chunk.NEWLINE);
    p.add(new Chunk("Final Computed Scores", TIMES_12PT_NORMAL));
    header.getDefaultCell().setBorderWidth(0);
    header.addCell(p);
    header.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

    final Phrase p2 = new Phrase();
    p2.add(new Chunk("Tournament: "
        + tournamentName, TIMES_12PT_NORMAL));
    p2.add(Chunk.NEWLINE);
    p2.add(new Chunk("Division: "
        + division, TIMES_12PT_NORMAL));
    header.addCell(p2);

    return header;
  }
}
