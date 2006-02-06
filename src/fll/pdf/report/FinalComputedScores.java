/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.pdf.report;

import fll.Queries;
import fll.Utilities;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;


/**
 * Code for finalComputedScores.jsp
 *
 * @version $Revision$
 */
public final class FinalComputedScores extends PdfPageEventHelper {
  
  private FinalComputedScores() {
     
  }
  
  private String m_tournament;
  private String m_challengeTitle;
  private String m_division;
  private PdfPTable m_header;
  private PdfTemplate m_tpl;
  private BaseFont m_headerFooterFont;
  
  public FinalComputedScores(org.w3c.dom.Document challengeDoc, String tournament) {
    Element root = challengeDoc.getDocumentElement();
    m_challengeTitle = root.getAttribute("title");
    m_tournament = tournament;
  }

  private void updateHeader(PdfWriter writer, Document document) {
    // initialization of the header table
    m_header = new PdfPTable(2);
    Phrase p = new Phrase();
    Chunk ck = new Chunk(m_challengeTitle + "\nFinal Computed Scores", times12ptNormal);
    p.add(ck);
    m_header.getDefaultCell().setBorderWidth(0);
    m_header.addCell(p);
    m_header.getDefaultCell().setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
    m_header.addCell(new Phrase(new Chunk("Tournament: " + m_tournament + "\nDivision: " + m_division, times12ptNormal)));
    PdfPCell blankCell = new PdfPCell();
    blankCell.setBorder(0);
    blankCell.setBorderWidthTop(1.0f);
    blankCell.setColspan(2);
    m_header.addCell(blankCell);
  }
  
  public void onOpenDocument(PdfWriter writer, Document document) {
    m_headerFooterFont = times12ptNormal.getBaseFont();

    // initialization of the footer template
    m_tpl = writer.getDirectContent().createTemplate(100, 100);
    m_tpl.setBoundingBox(new Rectangle(-20, -20, 100, 100));
  }
  
  public void onEndPage(PdfWriter writer, Document document) {
    PdfContentByte cb = writer.getDirectContent();
    cb.saveState();
    // write the headertable
    updateHeader(writer, document); // creates the header table with current division, etc.
    m_header.setTotalWidth(document.right() - document.left());
    m_header.writeSelectedRows(0, -1, document.left(), document.getPageSize().height() - 10, cb);
    
    // compose the footer
    String text = "Page " + writer.getPageNumber() + " of ";
    float textSize = m_headerFooterFont.getWidthPoint(text, 12);
    float textBase = document.bottom() - 20;
    cb.beginText();
    cb.setFontAndSize(m_headerFooterFont, 12);

    float adjust = m_headerFooterFont.getWidthPoint("0", 12);
    cb.setTextMatrix(document.right() - textSize - adjust, textBase);
    cb.showText(text);
    cb.endText();
    cb.addTemplate(m_tpl, document.right() - adjust, textBase);
    
    cb.restoreState();
  }

  public void onCloseDocument(PdfWriter writer, Document document) {
    m_tpl.beginText();
    m_tpl.setFontAndSize(m_headerFooterFont, 12);
    m_tpl.setTextMatrix(0, 0);
    m_tpl.showText("" + (writer.getPageNumber() - 1));
    m_tpl.endText();
  }

  private static final Font arial8ptBold = FontFactory.getFont(
      FontFactory.HELVETICA, 8, Font.BOLD);
  private static final Font arial8ptNormal = FontFactory.getFont(
      FontFactory.HELVETICA, 8, Font.NORMAL);
  private static final Font arial8ptNormalRed = FontFactory.getFont(
      FontFactory.HELVETICA, 8, Font.NORMAL,Color.RED);
  private static final Font times12ptNormal = FontFactory.getFont(
      FontFactory.TIMES, 12, Font.NORMAL);

  /**
   * Generate the actual report.
   */
  public void generateReport(
      final org.w3c.dom.Document document,
      final Connection connection,
      final OutputStream out) throws SQLException, IOException {

    Statement stmt = null;
    Statement teamsStmt = null;
    ResultSet rawScoreRS = null;
    ResultSet teamsRS = null;
    ResultSet scaledScoreRS = null;
    try {
      // This creates our new PDF document and declares it to be in landscape orientation
      Document pdfDoc = new Document(PageSize.LETTER);
      PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
      writer.setPageEvent(this);
      
      // Measurements are always in points (72 per inch) - This sets up 1/2 inch margins
      pdfDoc.setMargins(0.5f * 72, 0.5f * 72, 0.5f * 72, 0.5f * 72);
      pdfDoc.open();

      final Element rootElement = document.getDocumentElement();

      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      stmt = connection.createStatement();
      teamsStmt = connection.createStatement();

      final Iterator divisionIter = Queries.getDivisions(connection).iterator();
      while(divisionIter.hasNext()) {
        m_division = (String)divisionIter.next();

        // Figure out how many subjective categories have weights > 0.
        double[] weights = new double[subjectiveCategories.getLength()];
        Element[] catElements = new Element[subjectiveCategories.getLength()];
        int nonZeroWeights = 0;
        for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
          catElements[cat] = (Element)subjectiveCategories.item(cat);
          weights[cat] = Utilities.NUMBER_FORMAT_INSTANCE.parse(catElements[cat].getAttribute("weight")).doubleValue();
          if(weights[cat] > 0.0) {
            nonZeroWeights++;
          }
        }
        // Array of relative widths for the columns of the score page
        // Array length varies with the number of subjective scores weighted > 0.
        float[] relativeWidths = new float[nonZeroWeights + 4];
        relativeWidths[0] = 4f;
        relativeWidths[1] = 1.0f;
        relativeWidths[relativeWidths.length - 2] = 1.5f;
        relativeWidths[relativeWidths.length - 1] = 1.5f;
        for(int i = 2; i < 2 + nonZeroWeights; i++) {
          relativeWidths[i] = 1.5f;
        }
        
        // Create a table to hold all the scores for this division
        PdfPTable divTable = new PdfPTable(relativeWidths);
        divTable.getDefaultCell().setBorder(0);
        divTable.setWidthPercentage(100);

        ///////////////////////////////////////////////////////////////////////
        // Write the table column headers
        ///////////////////////////////////////////////////////////////////////
        PdfPCell teamCell = new PdfPCell(new Phrase("Team # / Organization / Team Name", arial8ptBold));
        teamCell.setBorder(0);
        teamCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        divTable.addCell(teamCell);
        divTable.addCell("");

        PdfPCell[] catCells = new PdfPCell[subjectiveCategories.getLength()];
        Paragraph[] catPars = new Paragraph[subjectiveCategories.getLength()];
        for(int cat=0; cat<catElements.length; cat++) {
          if(weights[cat] > 0.0) {
            final String catTitle = catElements[cat].getAttribute("title");

            catPars[cat] = new Paragraph(catTitle, arial8ptBold);
            catCells[cat] = new PdfPCell(catPars[cat]);
            catCells[cat].setBorder(0);
            catCells[cat].setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            catCells[cat].setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
            divTable.addCell(catCells[cat]);
          }
        }

        Paragraph perfPar = new Paragraph("Performance", arial8ptBold);
        PdfPCell perfCell = new PdfPCell(perfPar);
        perfCell.setBorder(0);
        perfCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        perfCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        divTable.addCell(perfCell);

        Paragraph overallScore = new Paragraph("Overall\nScore", arial8ptBold);
        PdfPCell osCell = new PdfPCell(overallScore);
        osCell.setBorder(0);
        osCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        osCell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        divTable.addCell(osCell);

        ///////////////////////////////////////////////////////////////////////
        // Write a table row with the relative weights of the subjective scores
        ///////////////////////////////////////////////////////////////////////

        Paragraph wPar = new Paragraph("Weight:", arial8ptNormal);
        PdfPCell wCell = new PdfPCell(wPar);
        wCell.setColspan(2);
        wCell.setBorder(0);
        wCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        divTable.addCell(wCell);

        PdfPCell[] wCells = new PdfPCell[subjectiveCategories.getLength()];
        Paragraph[] wPars = new Paragraph[subjectiveCategories.getLength()];
        for(int cat=0; cat<catElements.length; cat++) {
          if(weights[cat] > 0.0) {
            wPars[cat] = new Paragraph(Double.toString(weights[cat]), arial8ptNormal);
            wCells[cat] = new PdfPCell(wPars[cat]);
            wCells[cat].setBorder(0);
            wCells[cat].setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            divTable.addCell(wCells[cat]);
          }
        }

        final Element performanceElement = (Element) rootElement
            .getElementsByTagName("Performance").item(0);
        final double perfWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(
            performanceElement.getAttribute("weight")).doubleValue();
        Paragraph perfWeightPar = new Paragraph(Double.toString(perfWeight),
            arial8ptNormal);
        PdfPCell perfWeightCell = new PdfPCell(perfWeightPar);
        perfWeightCell
            .setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        perfWeightCell.setBorder(0);
        divTable.addCell(perfWeightCell);
        
        divTable.addCell("");

        PdfPCell blankCell = new PdfPCell();
        blankCell.setBorder(0);
        blankCell.setBorderWidthBottom(1.0f);
        blankCell.setColspan(4 + nonZeroWeights);
        divTable.addCell(blankCell);

        divTable.setHeaderRows(3); // Cause the first 3 rows to be repeated on
                                    // each page - 2 rows text headers and 1 for
                                    // the horizontal line.

        teamsRS = teamsStmt.executeQuery(
            "SELECT Teams.Organization,Teams.TeamName,Teams.TeamNumber,FinalScores.OverallScore"
            + " FROM Teams,FinalScores"
            + " WHERE FinalScores.TeamNumber = Teams.TeamNumber"
            + " AND FinalScores.Tournament = '" + m_tournament + "'"
            + " AND Teams.Division = '" + m_division + "'"
            + " ORDER BY FinalScores.OverallScore DESC, Teams.TeamNumber");
        while(teamsRS.next()) {
          final int teamNumber = teamsRS.getInt(3);
          final String organization = teamsRS.getString(1);
          final String teamName = teamsRS.getString(2);

          final double totalScore;
          final double ts = teamsRS.getDouble(4);
          if(teamsRS.wasNull()) {
            totalScore = Double.NaN;
          } else {
            totalScore = ts;
          }

          /////////////////////////////////////////////////////////////////////
          // Build a table of data for this team
          /////////////////////////////////////////////////////////////////////
          final PdfPTable curteam = new PdfPTable(relativeWidths);
          curteam.getDefaultCell().setBorder(0);

          // The first row of the team table...
          // First column is organization name
          PdfPCell teamCol = new PdfPCell(new Phrase(Integer
              .toString(teamNumber)
              + " " + organization, arial8ptNormal));
          teamCol.setBorder(0);
          teamCol.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
          curteam.addCell(teamCol);
          curteam.getDefaultCell().setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
          
          // Second column is "Raw:"
          curteam.addCell(new Phrase("Raw:", arial8ptNormal));

          // Next, one column containing the raw score for each subjective category with weight > 0
          for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
            final Element catElement = (Element)subjectiveCategories.item(cat);
            final double catWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(
                catElement.getAttribute("weight")).doubleValue();
            if(catWeight > 0.0) {
              final String catName = catElement.getAttribute("name");
              rawScoreRS = stmt
                  .executeQuery("SELECT RawScore FROM SummarizedScores WHERE TeamNumber = "
                      + teamNumber
                      + " AND Category = '"
                      + catName
                      + "' AND Tournament = '" + m_tournament + "'");
              final double rawScore;
              if(rawScoreRS.next()) {
                final double v = rawScoreRS.getDouble(1);
                if(rawScoreRS.wasNull()) {
                  rawScore = Double.NaN;
                } else {
                  rawScore = v;
                }
              } else {
                rawScore = Double.NaN;
              }
              PdfPCell subjCell = new PdfPCell(
                  (Double.isNaN(rawScore) ?
                      new Phrase("No Score", arial8ptNormalRed) :
                      new Phrase(SCORE_FORMAT.format(rawScore), arial8ptNormal)));
              subjCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
              subjCell.setBorder(0);
              curteam.addCell(subjCell);
              rawScoreRS.close();
            }
          }

          // Column for the highest performance score of the first 3 rounds
          rawScoreRS = stmt
              .executeQuery("SELECT RawScore FROM SummarizedScores WHERE TeamNumber = "
                  + teamNumber
                  + " AND Category = 'Performance' AND Tournament = '"
                  + m_tournament + "'");
          final double rawScore;
          if(rawScoreRS.next()) {
            final double v = rawScoreRS.getDouble(1);
            if(rawScoreRS.wasNull()) {
              rawScore = Double.NaN;
            } else {
              rawScore = v;
            }
          } else {
            rawScore = Double.NaN;
          }
          PdfPCell pCell = new PdfPCell(
              (Double.isNaN(rawScore) ?
                  new Phrase("No Score", arial8ptNormalRed) :
                  new Phrase(SCORE_FORMAT.format(rawScore), arial8ptNormal)));
          pCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
          pCell.setBorder(0);
          curteam.addCell(pCell);
          rawScoreRS.close();
          
          // The "Overall score" column is not filled in for raw scores
          curteam.addCell("");

          // The second row of the team table...
          // First column contains the team # and name
          PdfPCell teamNameCol = new PdfPCell(new Phrase(teamName, arial8ptNormal));
          teamNameCol.setBorder(0);
          teamNameCol.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
          curteam.addCell(teamNameCol);
          
          // Second column contains "Scaled:"
          curteam.getDefaultCell().setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
          curteam.addCell(new Phrase("Scaled:", arial8ptNormal));

          // Next, one column containing the scaled score for each subjective category with weight > 0
          for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
            final Element catElement = (Element)subjectiveCategories.item(cat);
            final double catWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(
                catElement.getAttribute("weight")).doubleValue();
            if(catWeight > 0.0) {
              final String catName = catElement.getAttribute("name");
              scaledScoreRS = stmt
                  .executeQuery("SELECT StandardizedScore FROM SummarizedScores WHERE TeamNumber = "
                      + teamNumber
                      + " AND Category = '"
                      + catName
                      + "' AND Tournament = '" + m_tournament + "'");
              final double scaledScore;
              if(scaledScoreRS.next()) {
                final double v = scaledScoreRS.getDouble(1);
                if(scaledScoreRS.wasNull()) {
                  scaledScore = Double.NaN;
                } else {
                  scaledScore = v;
                }
              } else {
                scaledScore = Double.NaN;
              }

              PdfPCell subjCell = new PdfPCell(
                  (Double.isNaN(scaledScore) ?
                      new Phrase("No Score", arial8ptNormalRed) :
                      new Phrase(SCORE_FORMAT.format(scaledScore), arial8ptNormal)));
              subjCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
              subjCell.setBorder(0);
              curteam.addCell(subjCell);

              scaledScoreRS.close();
            }
          }

          // 2nd to last column has the scaled performance score
          {
            scaledScoreRS = stmt
                .executeQuery("SELECT StandardizedScore FROM SummarizedScores WHERE TeamNumber = "
                    + teamNumber
                    + " AND Category = 'Performance' AND Tournament = '"
                    + m_tournament + "'");
            final double scaledScore;
            if(scaledScoreRS.next()) {
              final double v = scaledScoreRS.getDouble(1);
              if(scaledScoreRS.wasNull()) {
                scaledScore = Double.NaN;
              } else {
                scaledScore = v;
              }
            } else {
              scaledScore = Double.NaN;
            }

            pCell = new PdfPCell((Double.isNaN(scaledScore) ? new Phrase(
                "No Score", arial8ptNormalRed) : new Phrase(SCORE_FORMAT
                .format(scaledScore), arial8ptNormal)));
            pCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            pCell.setBorder(0);
            curteam.addCell(pCell);
          }
          
          scaledScoreRS.close();

          // Last column contains the overall scaled score
          pCell = new PdfPCell((Double.isNaN(totalScore) ? new Phrase(
              "No Score", arial8ptNormalRed) : new Phrase(SCORE_FORMAT
              .format(totalScore), arial8ptNormal)));
          pCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
          pCell.setBorder(0);
          curteam.addCell(pCell);

          // This is an empty row in the team table that is added to put a
          // horizontal rule under the team's score in the display
          blankCell = new PdfPCell();
          blankCell.setBorder(0);
          blankCell.setBorderWidthBottom(0.5f);
          blankCell.setBorderColorBottom(Color.GRAY);
          blankCell.setColspan(4 + nonZeroWeights);
          curteam.addCell(blankCell);

          // Create a new cell and add it to the division table - this cell will
          // contain the entire team table we just built above
          PdfPCell curteamCell = new PdfPCell(curteam);
          curteamCell.setBorder(0);
          curteamCell.setColspan(relativeWidths.length);
          divTable.addCell(curteamCell);
        }

        teamsRS.close();

        // Add the division table to the document
        pdfDoc.add(divTable);
        
        // If there is another division to process, start it on a new page
        if(divisionIter.hasNext()) pdfDoc.newPage();

      } //end while(divisionIter.next())

      pdfDoc.close();
    } catch(final ParseException pe) {
      throw new RuntimeException("Error parsing category weight!", pe);
    } catch(final DocumentException de) {
      throw new RuntimeException("Error creating PDF document!", de);
    } finally {
      Utilities.closeResultSet(rawScoreRS);
      Utilities.closeResultSet(teamsRS);
      Utilities.closeResultSet(scaledScoreRS);
      
      Utilities.closeStatement(stmt);
      Utilities.closeStatement(teamsStmt);
    }
  }

  private static final NumberFormat SCORE_FORMAT = NumberFormat.getInstance();
  static {
    SCORE_FORMAT.setMaximumFractionDigits(2);
    SCORE_FORMAT.setMinimumFractionDigits(2);
  }    

}
