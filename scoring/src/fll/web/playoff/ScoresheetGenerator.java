/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.Version;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.util.FP;
import fll.util.LogUtils;
import fll.util.PdfUtils;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.EnumeratedValue;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * @author Dan Churchill
 */
public class ScoresheetGenerator {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String LONG_BLANK = "_________________________";

  private static final String SHORT_BLANK = "______";

  private final Font f6i = new Font(Font.FontFamily.HELVETICA, 6, Font.ITALIC);

  private String m_copyright;

  private final String tournamentName;

  /**
   * Create document with the specified number of sheets. Initially all sheets
   * are empty. They should be filled in using the set methods.
   * 
   * @param numSheets the number of sheets on a page
   * @param tournamentName the name of the tournament to display
   * @param description the challenge description to get the goals from
   */
  public ScoresheetGenerator(final int numSheets,
                             final ChallengeDescription description,
                             final String tournamentName) {
    this.tournamentName = tournamentName;
    m_numSheets = numSheets;
    initializeArrays();

    setPageTitle("");
    for (int i = 0; i < m_numSheets; i++) {
      m_table[i] = SHORT_BLANK;
      m_name[i] = LONG_BLANK;
      m_round[i] = SHORT_BLANK;
      m_number[i] = null;
      m_time[i] = null;
    }

    setChallengeInfo(description);
  }

  /**
   * Create a new ScoresheetGenerator object populated with form header data
   * provided in the given Map. The map should contain String[] objects, each of
   * length 1, keyed by the String objects listed below (this matches the
   * expected format of the Map returned by
   * javax.servlet.ServletRequest.getParameterMap):
   * <ul>
   * <li><b>"numMatches"</b> - The number of matches in the form.
   * <li><b>"checkX"</b> - Present only for matches that should be printed.
   * <li><b>"roundX"</b> - For X = 1, 2, ... numMatches. Playoff round number
   * for scoresheet X.
   * <li><b>"teamAX"</b> - For X = 1, 2, ... numMatches. Team number of team on
   * table A for match X.
   * <li><b>"tableAX"</b> - For X = 1, 2, ... numTeams. Table assignment for
   * table A on match X.
   * <li><b>"teamBX"</b> - For X = 1, 2, ... numMatches. Team number of team on
   * table B for match X.
   * <li><b>"tableBX"</b> - For X = 1, 2, ... numTeams. Table assignment for
   * table B on match X.
   * </ul>
   * If any of the above objects don't exist in the Map, blank lines will be
   * placed in the generated form for that field. This also updates the
   * PlayoffData table in the assumption that the created scoresheet will be
   * printed, so table assignments are stored into the database and the match is
   * marked as printed. It is very questionable whether this is where this
   * should happen, but I don't feel like breaking it out.
   */
  public ScoresheetGenerator(final HttpServletRequest request,
                             final Connection connection,
                             final int tournament,
                             final ChallengeDescription description)
      throws SQLException {
    final Tournament tournamentObj = Tournament.findTournamentByID(connection, tournament);
    this.tournamentName = tournamentObj.getName();
    final ScoreType performanceScoreType = description.getPerformance().getScoreType();

    final String numMatchesStr = request.getParameter("numMatches");
    if (null == numMatchesStr) {
      // must have been called asking for blank
      m_numSheets = 1;
      initializeArrays();

      setPageTitle("");
      for (int i = 0; i < m_numSheets; i++) {
        m_table[i] = SHORT_BLANK;
        m_name[i] = LONG_BLANK;
        m_round[i] = SHORT_BLANK;
        m_divisionLabel[i] = AWARD_GROUP_LABEL;
        m_division[i] = SHORT_BLANK;
        m_number[i] = null;
        m_time[i] = null;
      }
    } else {
      final String division = request.getParameter("division");

      // called with specific sheets to print
      final int numMatches = Integer.parseInt(numMatchesStr);
      final boolean[] checkedMatches = new boolean[numMatches
          + 1]; // ignore
      // slot
      // index 0
      int checkedMatchCount = 0;
      // Build array of out how many matches we are printing
      for (int i = 1; i <= numMatches; i++) {
        final String checkX = "print"
            + i;
        checkedMatches[i] = null != request.getParameter(checkX);
        if (checkedMatches[i]) {
          checkedMatchCount++;
        }
      }

      if (checkedMatchCount == 0) {
        throw new FLLRuntimeException("No matches were found checked. Please go back and select the checkboxes for the scoresheets that you want to print");
      }

      m_numSheets = checkedMatchCount
          * 2;

      initializeArrays();
      setPageTitle(m_pageTitle);

      // Loop through checked matches, populate data, and update database to
      // track
      // printed status and remember assigned tables.
      PreparedStatement updatePrep = null;
      try {
        // build up the SQL
        updatePrep = connection.prepareStatement("UPDATE PlayoffData SET Printed=true, AssignedTable=?"
            + " WHERE event_division=? AND Tournament=? AND PlayoffRound=? AND Team=?");
        // could do division here, too, but since getting it from Team object,
        // will defer to same place as other
        updatePrep.setInt(3, tournament);

        int j = 0;
        for (int i = 1; i <= numMatches; i++) {
          if (checkedMatches[i]) {
            final String round = request.getParameter("round"
                + i);
            final int playoffRound = Integer.parseInt(round);

            // Get teamA info
            final Team teamA = Team.getTeamFromDatabase(connection, Integer.parseInt(request.getParameter("teamA"
                + i)));
            m_name[j] = teamA.getTrimmedTeamName();
            m_number[j] = teamA.getTeamNumber();
            m_round[j] = "Round P"
                + round;
            m_table[j] = request.getParameter("tableA"
                + i);

            final int performanceRunA = Playoff.getRunNumber(connection, division, teamA.getTeamNumber(), playoffRound);
            m_divisionLabel[j] = HEAD_TO_HEAD_LABEL;
            m_division[j] = division;
            final int bracketA = Playoff.getBracketNumber(connection, tournament, teamA.getTeamNumber(),
                                                          performanceRunA);
            final String bracketALabel = String.format("Match %d", bracketA);
            m_time[j] = bracketALabel;

            updatePrep.setString(1, m_table[j]);
            updatePrep.setString(2, division);
            updatePrep.setInt(4, playoffRound);
            updatePrep.setInt(5, teamA.getTeamNumber());
            if (updatePrep.executeUpdate() < 1) {
              LOGGER.warn(String.format("Could not update playoff table and print flags for team: %s playoff round: %s playoff bracket: %s",
                                        teamA.getTeamNumber(), playoffRound, division));
            } else {
              // update the brackets with the table name
              H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, teamA, performanceRunA);
            }
            j++;

            // Get teamB info
            final Team teamB = Team.getTeamFromDatabase(connection, Integer.parseInt(request.getParameter("teamB"
                + i)));
            m_name[j] = teamB.getTrimmedTeamName();
            m_number[j] = teamB.getTeamNumber();
            m_round[j] = "Round P"
                + round;
            m_table[j] = request.getParameter("tableB"
                + i);

            final int performanceRunB = Playoff.getRunNumber(connection, division, teamB.getTeamNumber(), playoffRound);
            m_divisionLabel[j] = HEAD_TO_HEAD_LABEL;
            m_division[j] = division;
            final int bracketB = Playoff.getBracketNumber(connection, tournament, teamB.getTeamNumber(),
                                                          performanceRunB);
            final String bracketBLabel = String.format("Match %d", bracketB);
            m_time[j] = bracketBLabel;

            updatePrep.setString(1, m_table[j]);
            updatePrep.setString(2, division);
            updatePrep.setInt(4, playoffRound);
            updatePrep.setInt(5, teamB.getTeamNumber());
            if (updatePrep.executeUpdate() < 1) {
              LOGGER.warn(String.format("Could not update playoff table and print flags for team: %s playoff round: %s playoff bracket: %s",
                                        teamB.getTeamNumber(), playoffRound, division));
            } else {
              // update the brackets with the table name
              H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, teamB, performanceRunB);
            }
            j++;
          }
        }
      } finally {
        SQLFunctions.close(updatePrep);
      }
    }
    setChallengeInfo(description);
  }

  /**
   * Private support function to create new data arrays for the scoresheet
   * information. IMPORTANT!!! The value of m_numTeams must be set before the
   * call to this method is made.
   */
  private void initializeArrays() {
    m_table = new String[m_numSheets];
    m_name = new String[m_numSheets];
    m_round = new String[m_numSheets];
    m_number = new Integer[m_numSheets];
    m_divisionLabel = new String[m_numSheets];
    m_division = new String[m_numSheets];
    m_time = new String[m_numSheets];
  }

  private static final Font ARIAL_8PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL,
                                                                   new BaseColor(0, 0, 0));

  private static final Font ARIAL_10PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font COURIER_10PT_NORMAL = FontFactory.getFont(FontFactory.COURIER, 10, Font.NORMAL);

  private static final int POINTS_PER_INCH = 72;

  /**
   * Guess the orientation that the document should be.
   * 
   * @return true if it should be portrait
   * @throws DocumentException
   * @throws IOException
   */
  public static boolean guessOrientation(final ChallengeDescription description) throws DocumentException, IOException {
    final ScoresheetGenerator gen = new ScoresheetGenerator(1, description, "dummy");
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    gen.writeFile(out, false);
    final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    final PdfReader reader = new PdfReader(in);
    if (reader.getNumberOfPages() > 1) {
      // doesn't fit landscape
      return true;
    } else {
      return false;
    }
  }

  public void writeFile(final OutputStream out,
                        final boolean orientationIsPortrait)
      throws DocumentException {

    // This creates our new PDF document and declares its orientation
    Document pdfDoc;
    if (orientationIsPortrait) {
      pdfDoc = new Document(PageSize.LETTER); // portrait
    } else {
      pdfDoc = new Document(PageSize.LETTER.rotate()); // landscape
    }
    PdfWriter.getInstance(pdfDoc, out);

    // Measurements are always in points (72 per inch)
    // This sets up 1/2 inch margins side margins and 0.35in top and bottom
    // margins
    pdfDoc.setMargins(0.5f
        * POINTS_PER_INCH,
                      0.5f
                          * POINTS_PER_INCH,
                      0.35f
                          * POINTS_PER_INCH,
                      0.35f
                          * POINTS_PER_INCH);
    pdfDoc.open();

    // Header cell with challenge title to add to both scoresheets
    final Paragraph titleParagraph = new Paragraph();
    final Chunk titleChunk = new Chunk(m_pageTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL,
                                                                        BaseColor.WHITE));
    titleParagraph.setAlignment(Element.ALIGN_CENTER);
    titleParagraph.add(titleChunk);

    titleParagraph.add(Chunk.NEWLINE);
    final Chunk swVersionChunk = new Chunk("SW version: "
        + Version.getVersion(), FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, BaseColor.WHITE));
    titleParagraph.add(swVersionChunk);
    if (null != m_revision) {

      final Chunk revisionChunk = new Chunk(" Descriptor revision: "
          + m_revision, FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, BaseColor.WHITE));

      titleParagraph.add(revisionChunk);
    }

    final PdfPCell head = new PdfPCell();
    head.setColspan(2);
    head.setBorder(1);
    head.setPaddingTop(0);
    head.setPaddingBottom(3);
    head.setBackgroundColor(new BaseColor(64, 64, 64));
    head.setVerticalAlignment(Element.ALIGN_TOP);
    head.addElement(titleParagraph);

    // Cells for score field, and 2nd check initials
    final Phrase des = new Phrase("Data Entry Score _______", ARIAL_8PT_NORMAL);
    final PdfPCell desC = new PdfPCell(des);
    desC.setBorder(0);
    desC.setPaddingTop(9);
    desC.setPaddingRight(36);
    desC.setHorizontalAlignment(Element.ALIGN_RIGHT);
    final Phrase sci = new Phrase("2nd Check Initials _______", ARIAL_8PT_NORMAL);
    final PdfPCell sciC = new PdfPCell(sci);
    sciC.setBorder(0);
    sciC.setPaddingTop(9);
    sciC.setPaddingRight(36);
    sciC.setHorizontalAlignment(Element.ALIGN_RIGHT);

    // Create a table with a grid cell for each scoresheet on the page
    PdfPTable wholePage = getTableForPage(orientationIsPortrait);
    wholePage.setWidthPercentage(100);
    for (int i = 0; i < m_numSheets; i++) {
      if (i > 0
          && (orientationIsPortrait
              || (i
                  % 2) == 0)) {
        pdfDoc.newPage();
        wholePage = getTableForPage(orientationIsPortrait);
        wholePage.setWidthPercentage(100);
      }

      // This table is a single score sheet
      final PdfPTable scoreSheet = new PdfPTable(2);
      scoreSheet.getDefaultCell().setBorder(Rectangle.NO_BORDER);
      scoreSheet.getDefaultCell().setPaddingRight(1);
      scoreSheet.getDefaultCell().setPaddingLeft(0);

      scoreSheet.addCell(head);

      final PdfPTable teamInfo = new PdfPTable(7);
      teamInfo.setWidthPercentage(100);
      teamInfo.setWidths(new float[] { 1f, 1f, 1f, 1f, 1f, 1f, .9f });

      // Time label cell
      final Paragraph timeP = new Paragraph("Time:", ARIAL_10PT_NORMAL);
      timeP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell timeLc = new PdfPCell(scoreSheet.getDefaultCell());
      timeLc.addElement(timeP);
      teamInfo.addCell(timeLc);
      // Time value cell
      final Paragraph timeV = new Paragraph(null == m_time[i] ? SHORT_BLANK : m_time[i], COURIER_10PT_NORMAL);
      final PdfPCell timeVc = new PdfPCell(scoreSheet.getDefaultCell());
      timeVc.addElement(timeV);
      teamInfo.addCell(timeVc);

      // Table label cell
      final Paragraph tblP = new Paragraph("Table:", ARIAL_10PT_NORMAL);
      tblP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell tblLc = new PdfPCell(scoreSheet.getDefaultCell());
      tblLc.addElement(tblP);
      teamInfo.addCell(tblLc);
      // Table value cell
      final Paragraph tblV = new Paragraph(m_table[i], COURIER_10PT_NORMAL);
      final PdfPCell tblVc = new PdfPCell(scoreSheet.getDefaultCell());
      tblVc.addElement(tblV);
      teamInfo.addCell(tblVc);

      // Round number label cell
      final Paragraph rndP = new Paragraph("Round:", ARIAL_10PT_NORMAL);
      rndP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell rndlc = new PdfPCell(scoreSheet.getDefaultCell());
      rndlc.addElement(rndP);
      teamInfo.addCell(rndlc);
      // Round number value cell
      final Paragraph rndV = new Paragraph(m_round[i], COURIER_10PT_NORMAL);
      final PdfPCell rndVc = new PdfPCell(scoreSheet.getDefaultCell());
      // rndVc.setColspan(2);
      rndVc.addElement(rndV);
      teamInfo.addCell(rndVc);

      final PdfPCell temp1 = new PdfPCell(scoreSheet.getDefaultCell());
      // temp1.setColspan(2);
      temp1.addElement(new Paragraph("Judge ____", ARIAL_8PT_NORMAL));
      teamInfo.addCell(temp1);

      // Team number label cell
      final Paragraph nbrP = new Paragraph("Team #:", ARIAL_10PT_NORMAL);
      nbrP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell nbrlc = new PdfPCell(scoreSheet.getDefaultCell());
      nbrlc.addElement(nbrP);
      teamInfo.addCell(nbrlc);
      // Team number value cell
      final Paragraph nbrV = new Paragraph(null == m_number[i] ? SHORT_BLANK : String.valueOf(m_number[i]),
                                           COURIER_10PT_NORMAL);
      final PdfPCell nbrVc = new PdfPCell(scoreSheet.getDefaultCell());
      nbrVc.addElement(nbrV);
      teamInfo.addCell(nbrVc);

      // Team division label cell
      final Paragraph divP = new Paragraph(m_divisionLabel[i], ARIAL_10PT_NORMAL);
      divP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell divlc = new PdfPCell(scoreSheet.getDefaultCell());
      divlc.addElement(divP);
      divlc.setColspan(2);
      teamInfo.addCell(divlc);
      // Team division value cell
      final Paragraph divV = new Paragraph(m_division[i], COURIER_10PT_NORMAL);
      final PdfPCell divVc = new PdfPCell(scoreSheet.getDefaultCell());
      divVc.setColspan(2);
      divVc.addElement(divV);
      teamInfo.addCell(divVc);

      final PdfPCell temp2 = new PdfPCell(scoreSheet.getDefaultCell());
      // temp2.setColspan(2);
      temp2.addElement(new Paragraph("Team ____", ARIAL_8PT_NORMAL));
      teamInfo.addCell(temp2);

      // Team name label cell
      final Paragraph nameP = new Paragraph("Team Name:", ARIAL_10PT_NORMAL);
      nameP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell namelc = new PdfPCell(scoreSheet.getDefaultCell());
      namelc.setColspan(2);
      namelc.addElement(nameP);
      teamInfo.addCell(namelc);
      // Team name value cell
      final PdfPCell nameVc = new PdfPCell(scoreSheet.getDefaultCell());
      nameVc.setColspan(4);
      nameVc.setCellEvent(new PdfUtils.TruncateContent(m_name[i], COURIER_10PT_NORMAL));
      teamInfo.addCell(nameVc);

      // add tournament name
      final Paragraph tournamentNameV = new Paragraph(tournamentName, f6i);
      tournamentNameV.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell tournamentNameVc = new PdfPCell(scoreSheet.getDefaultCell());
      tournamentNameVc.addElement(tournamentNameV);
      teamInfo.addCell(tournamentNameVc);

      // add team info cell to the team table
      final PdfPCell teamInfoCell = new PdfPCell(scoreSheet.getDefaultCell());
      teamInfoCell.addElement(teamInfo);
      teamInfoCell.setColspan(2);

      scoreSheet.addCell(teamInfoCell);

      if (null != m_goalsTable) {
        final PdfPCell goalCell = new PdfPCell(m_goalsTable);
        goalCell.setBorder(0);
        goalCell.setPadding(0);
        goalCell.setColspan(2);
        scoreSheet.addCell(goalCell);
      }

      scoreSheet.addCell(desC);
      scoreSheet.addCell(sciC);

      if (null != m_copyright) {
        final Phrase copyright = new Phrase("\u00A9"
            + m_copyright, f6i);
        final PdfPCell copyrightC = new PdfPCell(scoreSheet.getDefaultCell());
        copyrightC.addElement(copyright);
        copyrightC.setBorder(0);
        copyrightC.setHorizontalAlignment(Element.ALIGN_CENTER);
        copyrightC.setColspan(2);

        scoreSheet.addCell(copyrightC);
      }

      // the cell in the whole page table that will contain the single score
      // sheet
      final PdfPCell scoresheetCell = new PdfPCell(scoreSheet);
      scoresheetCell.setBorder(0);
      scoresheetCell.setPadding(0);

      // Interior borders between scoresheets on a page
      if (!orientationIsPortrait) {
        if (i
            % 2 == 0) {
          scoresheetCell.setPaddingRight(0.1f
              * POINTS_PER_INCH);
        } else {
          scoresheetCell.setPaddingLeft(0.1f
              * POINTS_PER_INCH);
        }
      }

      // Add the current scoresheet to the page
      wholePage.addCell(scoresheetCell);

      // Add the current table of scoresheets to the document
      if (orientationIsPortrait
          || (i
              % 2 != 0)) {
        pdfDoc.add(wholePage);
      }
    }

    // Add a blank cells to complete the table of the last page
    if (!orientationIsPortrait
        && m_numSheets
            % 2 != 0) {
      final PdfPCell blank = new PdfPCell();
      blank.setBorder(0);
      wholePage.addCell(blank);
      pdfDoc.add(wholePage);
    }

    pdfDoc.close();
  }

  /**
   * Stores the goal cells that are inserted into the output after the team name
   * headers and before the scoring/initials blanks at the bottom of the
   * scoresheet.
   */
  private void setChallengeInfo(final ChallengeDescription description) {
    setPageTitle(description.getTitle());

    if (null != description.getRevision()) {
      setRevisionInfo(description.getRevision());
    }

    if (null != description.getCopyright()) {
      m_copyright = description.getCopyright();
    } else {
      m_copyright = null;
    }

    final PerformanceScoreCategory performanceElement = description.getPerformance();
    // use ArrayList as we will be doing indexed access in the loop
    final List<AbstractGoal> goals = new ArrayList<>(performanceElement.getGoals());

    final float[] relativeWidths = new float[3];
    relativeWidths[0] = 4;
    relativeWidths[1] = 48;
    relativeWidths[2] = 48;
    m_goalsTable = new PdfPTable(relativeWidths);

    String prevCategory = null;
    for (int goalIndex = 0; goalIndex < goals.size(); ++goalIndex) {
      final AbstractGoal goal = goals.get(goalIndex);
      if (!goal.isComputed()) {
        final String category = goal.getCategory();

        // add category cell if needed
        boolean firstRowInCategory = false;
        if (!StringUtils.equals(prevCategory, category)) {
          if (!StringUtils.isEmpty(category)) {

            // find out how many future goals have the same category
            int categoryRowSpan = 1;
            for (int otherIndex = goalIndex
                + 1; otherIndex < goals.size(); ++otherIndex) {
              final AbstractGoal otherGoal = goals.get(otherIndex);
              if (!otherGoal.isComputed()) {
                if (StringUtils.equals(category, otherGoal.getCategory())) {
                  ++categoryRowSpan;
                } else {
                  break;
                }
              }
            }

            final Paragraph catPara = new Paragraph(category, ARIAL_10PT_NORMAL);
            final PdfPCell categoryCell = new PdfPCell(catPara);
            categoryCell.setBorderWidthTop(1);
            categoryCell.setBorderWidthBottom(0);
            categoryCell.setBorderWidthLeft(0);
            categoryCell.setBorderWidthRight(0);
            categoryCell.setVerticalAlignment(Element.ALIGN_CENTER);
            categoryCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            categoryCell.setRotation(90);
            categoryCell.setRowspan(categoryRowSpan);
            m_goalsTable.addCell(categoryCell);
          }

          // first row in a new category, which may be empty
          firstRowInCategory = true;
        }

        // This is the text for the left hand "label" cell
        final String title = goal.getTitle();
        final Paragraph p = new Paragraph(title, ARIAL_10PT_NORMAL);
        final PdfPCell goalLabel = new PdfPCell(p);
        goalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        // align bottom, otherwise we don't line up with the goalValues. For some reason
        // the vertical alignment isn't used when adding elements to a PdfPCell, so the
        // goal values end up always aligned to the bottom of the cell.
        goalLabel.setVerticalAlignment(Element.ALIGN_BOTTOM);
        if (firstRowInCategory) {
          goalLabel.setBorderWidthTop(1);
          goalLabel.setBorderWidthBottom(0);
          goalLabel.setBorderWidthLeft(0);
          goalLabel.setBorderWidthRight(0);
        } else {
          goalLabel.setBorder(0);
        }
        goalLabel.setPaddingRight(9);
        if (StringUtils.isEmpty(category)) {
          // category column and goal label column
          goalLabel.setColspan(2);
        }

        m_goalsTable.addCell(goalLabel);

        // define the value cell
        final double min = goal.getMin();
        final String minStr = FP.equals(min, Math.round(min), 1E-6) ? String.valueOf((int) min) : String.valueOf(min);
        final double max = goal.getMax();
        final String maxStr = FP.equals(max, Math.round(max), 1E-6) ? String.valueOf((int) max) : String.valueOf(max);

        // If element has child nodes, then we have an enumerated list
        // of choices. Otherwise it is either yes/no or a numeric field.
        final PdfPCell goalValue = new PdfPCell();

        final Chunk choices = new Chunk("", COURIER_10PT_NORMAL);
        if (goal.isEnumerated()) {
          // replace spaces with "no-break" spaces
          boolean first = true;
          final List<EnumeratedValue> values = goal.getSortedValues();
          for (final EnumeratedValue value : values) {
            if (!first) {
              choices.append(" /"
                  + Utilities.NON_BREAKING_SPACE);
            } else {
              first = false;
            }
            choices.append(value.getTitle().toUpperCase().replace(' ', Utilities.NON_BREAKING_SPACE));
          }
          goalValue.addElement(choices);

        } else {
          if (goal.isYesNo()) {
            // order of yes/no needs to match ScoreEntry.generateYesNoButtons
            final Paragraph q = new Paragraph("NO / YES", COURIER_10PT_NORMAL);
            goalValue.addElement(q);

          } else {
            final String range = "("
                + minStr
                + " - "
                + maxStr
                + ")";
            final PdfPTable t = new PdfPTable(2);
            t.setHorizontalAlignment(Element.ALIGN_LEFT);
            t.setTotalWidth(1
                * POINTS_PER_INCH);
            t.setLockedWidth(true);
            final Phrase r = new Phrase("", ARIAL_8PT_NORMAL);
            t.addCell(new PdfPCell(r));
            final Phrase q = new Phrase(range, ARIAL_8PT_NORMAL);
            t.addCell(new PdfPCell(q));
            goalValue.setPaddingTop(9);
            goalValue.addElement(t);
          }
        }

        if (firstRowInCategory) {
          goalValue.setBorderWidthTop(1);
          goalValue.setBorderWidthBottom(0);
          goalValue.setBorderWidthLeft(0);
          goalValue.setBorderWidthRight(0);
        } else {
          goalValue.setBorder(0);
        }

        m_goalsTable.addCell(goalValue);

        // setup for next loop
        prevCategory = category;
      } // if not computed goal

    } // foreach goal

  }

  private int m_numSheets;

  private String m_revision;

  private String m_pageTitle;

  private String[] m_table;

  private String[] m_name;

  private String[] m_round;

  private Integer[] m_number;

  public static final String HEAD_TO_HEAD_LABEL = "Head to head Bracket:";

  public static final String AWARD_GROUP_LABEL = "Award Group:";

  private String[] m_divisionLabel;

  private String[] m_division;

  private String[] m_time;

  private PdfPTable m_goalsTable;

  public void setPageTitle(final String title) {
    m_pageTitle = title;
  }

  private void setRevisionInfo(final String revision) {
    m_revision = revision;
  }

  /**
   * Sets the table label for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this table
   *          label.
   * @param table A string with the table label for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setTable(final int i,
                       final String table)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_table[i] = table;
  }

  /**
   * Sets the division for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this table
   *          label.
   * @param divisionLabel the label to use for this division should be
   *          {@link #HEAD_TO_HEAD_LABEL} or {@link #AWARD_GROUP_LABEL}
   * @param division the string to display in the division section of the score
   *          sheet
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setDivision(final int i,
                          final String divisionLabel,
                          final String division)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_divisionLabel[i] = divisionLabel;
    m_division[i] = division;
  }

  /**
   * Sets the team name for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this team
   *          name.
   * @param name A string with the team name for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setName(final int i,
                      final String name)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_name[i] = name;
  }

  /**
   * Sets the team number for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this team
   *          number.
   * @param number A string with the team number for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setNumber(final int i,
                        final Integer number)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_number[i] = number;
  }

  /**
   * Sets the time for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this time.
   * @param time the time for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setTime(final int i,
                      final LocalTime time)
      throws IllegalArgumentException {
    setTime(i, TournamentSchedule.formatTime(time));
  }

  /**
   * Puts an arbitrary string in the time field.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this time.
   * @param time the time for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setTime(final int i,
                      final String time)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_time[i] = time;
  }

  /**
   * Sets the round number descriptor for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this round
   *          number.
   * @param round A string with the round number descriptor for the specified
   *          scoresheet. Should be a 1-based number.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setRound(final int i,
                       final String round)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_round[i] = round;
  }

  /**
   * Create table for page given number of sheets per page.
   * 
   * @param nup
   * @return
   */
  private static PdfPTable getTableForPage(final boolean orientationIsPortrait) {
    final PdfPTable wholePage;
    if (orientationIsPortrait) {
      wholePage = new PdfPTable(1); // 1 column
    } else {
      wholePage = new PdfPTable(2); // 2 columns
    }
    return wholePage;
  }
}
