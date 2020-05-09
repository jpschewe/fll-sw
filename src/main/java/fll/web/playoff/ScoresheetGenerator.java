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
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.Version;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FP;
import fll.util.PdfUtils;
import fll.web.report.FinalComputedScores;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.EnumeratedValue;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * @author Dan Churchill
 */
public class ScoresheetGenerator {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String LONG_BLANK = "_________________________";

  private static final String SHORT_BLANK = "______";

  private final com.itextpdf.text.Font f6i = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 6,
                                                                        com.itextpdf.text.Font.ITALIC);

  private final String tournamentName;

  private final ChallengeDescription description;

  /**
   * Create com.itextpdf.text.Document with the specified number of sheets.
   * Initially all sheets
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
    this.description = Objects.requireNonNull(description);
    initializeArrays();

    for (int i = 0; i < m_numSheets; i++) {
      m_table[i] = SHORT_BLANK;
      m_name[i] = LONG_BLANK;
      m_round[i] = SHORT_BLANK;
      m_number[i] = null;
      m_time[i] = null;
      m_divisionLabel[i] = AWARD_GROUP_LABEL;
      m_division[i] = SHORT_BLANK;
    }
  }

  /**
   * Generate blank score sheets. One is regular and one is practice.
   * 
   * @param description the challenge description
   */
  public ScoresheetGenerator(final ChallengeDescription description) {
    this.tournamentName = "Example";
    this.description = Objects.requireNonNull(description);

    // must have been called asking for blank
    m_numSheets = 2;
    initializeArrays();

    for (int i = 0; i < m_numSheets; i++) {
      m_table[i] = SHORT_BLANK;
      m_name[i] = LONG_BLANK;
      m_round[i] = Utilities.isOdd(i) ? "Practice" : SHORT_BLANK;
      m_divisionLabel[i] = AWARD_GROUP_LABEL;
      m_division[i] = SHORT_BLANK;
      m_number[i] = null;
      m_time[i] = null;
      m_isPractice[i] = Utilities.isOdd(i);
    }
  }

  /**
   * Create a new ScoresheetGenerator object populated with form header data
   * provided in the given servlet request. The request should contain the
   * attributes listed below (this matches the
   * expected format of the request returned):
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
   * 
   * @param request used to get information about award group and number of
   *          matches
   * @param connection where to find database information
   * @param tournament the tournament ID
   * @param description description of the challenge
   */
  public ScoresheetGenerator(final HttpServletRequest request,
                             final Connection connection,
                             final int tournament,
                             final ChallengeDescription description)
      throws SQLException {
    this.description = Objects.requireNonNull(description);
    final Tournament tournamentObj = Tournament.findTournamentByID(connection, tournament);
    this.tournamentName = tournamentObj.getName();
    final ScoreType performanceScoreType = description.getPerformance().getScoreType();

    final String numMatchesStr = request.getParameter("numMatches");

    final String division = request.getParameter("division");

    // called with specific sheets to print
    final int numMatches = Integer.parseInt(numMatchesStr);

    // ignore slot index 0 since the parameters are 1-based
    final boolean[] checkedMatches = new boolean[numMatches
        + 1];
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

    // Loop through checked matches, populate data, and update database to
    // track
    // printed status and remember assigned tables.
    try (
        PreparedStatement updatePrep = connection.prepareStatement("UPDATE PlayoffData SET Printed=true, AssignedTable=?"
            + " WHERE event_division=? AND Tournament=? AND PlayoffRound=? AND Team=?")) {
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
          final int bracketA = Playoff.getBracketNumber(connection, tournament, teamA.getTeamNumber(), performanceRunA);
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
          final int bracketB = Playoff.getBracketNumber(connection, tournament, teamB.getTeamNumber(), performanceRunB);
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
    }
  }

  /**
   * Private support function to create new data arrays for the scoresheet
   * information. IMPORTANT!!! The value of {@link #m_numSheets} must be set
   * before the
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
    m_isPractice = new boolean[m_numSheets];
  }

  private static final com.itextpdf.text.Font ARIAL_8PT_NORMAL = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA,
                                                                                                       8,
                                                                                                       com.itextpdf.text.Font.NORMAL,
                                                                                                       new com.itextpdf.text.BaseColor(0,
                                                                                                                                       0,
                                                                                                                                       0));

  private static final com.itextpdf.text.Font ARIAL_10PT_NORMAL = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA,
                                                                                                        10,
                                                                                                        com.itextpdf.text.Font.NORMAL);

  private static final com.itextpdf.text.Font COURIER_10PT_NORMAL = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.COURIER,
                                                                                                          10,
                                                                                                          com.itextpdf.text.Font.NORMAL);

  private static final int POINTS_PER_INCH = 72;

  /**
   * Guess the orientation that the com.itextpdf.text.Document should be.
   *
   * @return true if it should be portrait, number of pages per score sheet (0.5,
   *         1 or greater)
   * @param description description of the challenge
   * @throws com.itextpdf.text.DocumentException
   * @throws IOException
   */
  public static Pair<Boolean, Float> guessOrientation(final ChallengeDescription description)
      throws com.itextpdf.text.DocumentException, IOException {
    final ScoresheetGenerator gen = new ScoresheetGenerator(1, description, "dummy");
    final ByteArrayOutputStream outLandscape = new ByteArrayOutputStream();
    // Using landscape, so set pages per sheet to 0.5
    gen.writeFile(outLandscape, false, 0.5f);
    final ByteArrayInputStream inLandscape = new ByteArrayInputStream(outLandscape.toByteArray());
    final PdfReader readerLandscape = new PdfReader(inLandscape);
    final int numPagesLandscape = readerLandscape.getNumberOfPages();
    readerLandscape.close();

    if (numPagesLandscape > 1) {
      // doesn't fit landscape

      // need to run again to compute pages per score sheet
      final ByteArrayOutputStream outPortrait = new ByteArrayOutputStream();
      // Using portrait, so set pages per sheet to 1
      gen.writeFile(outPortrait, true, 1f);
      final ByteArrayInputStream inPortrait = new ByteArrayInputStream(outPortrait.toByteArray());
      final PdfReader readerPortrait = new PdfReader(inPortrait);
      final int numPagesPortrait = readerPortrait.getNumberOfPages();
      readerPortrait.close();

      return Pair.of(true, (float) numPagesPortrait);
    } else {
      return Pair.of(false, 0.5f);
    }
  }

  /**
   * @param out where to write the PDF
   * @param orientationIsPortrait true if the com.itextpdf.text.Document is in
   *          portrait mode, false
   *          if landscape (2 score sheets per page)
   * @param pagesPerScoreSheet number of pages each score sheet takes, used to get
   *          the practice watermark in the right places
   * @throws IOException if there is a problem writing to the output
   */
  public void writeFile(final OutputStream out,
                        final boolean orientationIsPortrait,
                        final float pagesPerScoreSheet)
      throws IOException {

    try {
      final Document performanceDoc = createDocument(orientationIsPortrait, pagesPerScoreSheet);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, out);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private Element createCopyrightBlock(final Document document) {
    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.setAttribute("font-size", "6pt");
    block.setAttribute("font-style", "italic");
    block.setAttribute("text-align", "center");

    block.appendChild(document.createTextNode("\u00A9"
        + this.description.getCopyright()));

    return block;
  }

  private Element createCopyrightFooter(final Document document) {
    final Element staticContent = FOPUtils.createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-after");

    final Element block = createCopyrightBlock(document);
    staticContent.appendChild(block);

    return staticContent;
  }

  private Document createDocument(final boolean orientationIsPortrait,
                                  final float pagesPerScoreSheet) {
    // FIXME need to rotate page when landscape - use reference-orientation
    // https://stackoverflow.com/questions/15523020/what-does-reference-orientation-really-do

    // TODO decide if we still want to support rotating the score sheet - 5/9/2020
    // sent email to Jeannie for opinion

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);
    rootElement.setAttribute("font-family", "Helvetica");

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    if (null != description.getCopyright()) {
      final Element footer = createCopyrightFooter(document);
      pageSequence.appendChild(footer);
    }

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element titleHeader = createTitleBlock(document);
    final Element goalsTable = createGoalsTable(document);
    final Element checkBlock = createCheckBlock(document);

    for (int sheetIndex = 0; sheetIndex < m_numSheets; sheetIndex++) {
      final Element sheet = createScoreSheet(document, titleHeader, goalsTable, checkBlock, sheetIndex);
      documentBody.appendChild(sheet);
    }

    return document;
  }

  private Element createScoreSheet(final Document document,
                                   final Element titleHeader,
                                   final Element goalsTable,
                                   final Element checkBlock,
                                   final int sheetIndex) {
    final Element sheet = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    // TODO may need to change this page break based on odd/even and rotation
    sheet.setAttribute("page-break-after", "always");

    sheet.appendChild(titleHeader.cloneNode(true));

    final Element teamInfoContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    sheet.appendChild(teamInfoContainer);
    teamInfoContainer.setAttribute("padding-bottom", "10pt");
    teamInfoContainer.setAttribute("padding-top", "10pt");
    FOPUtils.addBottomBorder(teamInfoContainer, 1.0);

    final Element teamInfo = createTeamInfoBlock(document, sheetIndex);
    teamInfoContainer.appendChild(teamInfo);

    final Element goalsTableContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    sheet.appendChild(goalsTableContainer);
    goalsTableContainer.setAttribute("padding-bottom", "10pt");

    goalsTableContainer.appendChild(goalsTable.cloneNode(true));

    sheet.appendChild(checkBlock.cloneNode(true));

    return sheet;
  }

  private Element createTeamInfoBlock(final Document document,
                                      final int sheetIndex) {
    final Element table = FOPUtils.createBasicTable(document);
    table.setAttribute("font-size", "10pt");

    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 9));

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
    table.appendChild(tableBody);

    final Element row1 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row1);

    final Element timeLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Time:");
    row1.appendChild(timeLabel);
    timeLabel.setAttribute("padding-top", "2pt");
    timeLabel.setAttribute("padding-bottom", "2pt");


    final Element timeValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                       null == m_time[sheetIndex] ? SHORT_BLANK : m_time[sheetIndex]);
    row1.appendChild(timeValue);
    timeValue.setAttribute("font-family", "Courier");
    timeValue.setAttribute("padding-top", "2pt");
    timeValue.setAttribute("padding-bottom", "2pt");

    final Element tableLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Table:");
    row1.appendChild(tableLabel);
    tableLabel.setAttribute("padding-top", "2pt");
    tableLabel.setAttribute("padding-bottom", "2pt");

    final Element tableValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                        null == m_table[sheetIndex] ? SHORT_BLANK
                                                            : m_table[sheetIndex]);
    tableValue.setAttribute("font-family", "Courier");
    tableValue.setAttribute("padding-top", "2pt");
    tableValue.setAttribute("padding-bottom", "2pt");
    row1.appendChild(tableValue);

    final Element roundLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Round:");
    row1.appendChild(roundLabel);
    roundLabel.setAttribute("padding-top", "2pt");
    roundLabel.setAttribute("padding-bottom", "2pt");

    final Element roundValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                        null == m_round[sheetIndex] ? SHORT_BLANK
                                                            : m_round[sheetIndex]);
    roundValue.setAttribute("font-family", "Courier");
    roundValue.setAttribute("padding-top", "2pt");
    roundValue.setAttribute("padding-bottom", "2pt");
 row1.appendChild(roundValue);

    final Element refSig = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Ref ____");
    row1.appendChild(refSig);
    refSig.setAttribute("font-size", "8pt");
    refSig.setAttribute("padding-top", "2pt");
    refSig.setAttribute("padding-bottom", "2pt");

    final Element row2 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row2);

    final Element teamNumberLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Team #:");
    row2.appendChild(teamNumberLabel);
    teamNumberLabel.setAttribute("padding-top", "2pt");
    teamNumberLabel.setAttribute("padding-bottom", "2pt");

    final Element teamNumberValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                             null == m_number[sheetIndex] ? SHORT_BLANK
                                                                 : String.valueOf(m_number[sheetIndex]));
    teamNumberValue.setAttribute("font-family", "Courier");
    row2.appendChild(teamNumberValue);
    teamNumberValue.setAttribute("padding-top", "2pt");
    teamNumberValue.setAttribute("padding-bottom", "2pt");

    final Element awardGroupLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                             m_divisionLabel[sheetIndex]);
    awardGroupLabel.setAttribute("number-columns-spanned", "2");
    awardGroupLabel.setAttribute("padding-top", "2pt");
    awardGroupLabel.setAttribute("padding-bottom", "2pt");
    row2.appendChild(awardGroupLabel);

    final Element awardGroupValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                             null == m_division[sheetIndex] ? SHORT_BLANK
                                                                 : m_division[sheetIndex]);
    awardGroupValue.setAttribute("font-family", "Courier");
    awardGroupValue.setAttribute("number-columns-spanned", "2");
    awardGroupValue.setAttribute("padding-top", "2pt");
    awardGroupValue.setAttribute("padding-bottom", "2pt");
    row2.appendChild(awardGroupValue);

    final Element teamSig = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Team ____");
    row2.appendChild(teamSig);
    teamSig.setAttribute("font-size", "8pt");
    teamSig.setAttribute("padding-top", "2pt");
    teamSig.setAttribute("padding-bottom", "2pt");

    final Element row3 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row3);

    final Element teamNameLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Team Name:");
    row3.appendChild(teamNameLabel);
    teamNameLabel.setAttribute("number-columns-spanned", "2");
    teamNameLabel.setAttribute("padding-top", "2pt");
    teamNameLabel.setAttribute("padding-bottom", "2pt");

    final Element teamNameValue = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                 null == m_name[sheetIndex] ? LONG_BLANK
                                                                     : m_name[sheetIndex]);
    row3.appendChild(teamNameValue);
    teamNameValue.setAttribute("number-columns-spanned", "4");
    teamNameValue.setAttribute("font-family", "Courier");
    teamNameValue.setAttribute("padding-top", "2pt");
    teamNameValue.setAttribute("padding-bottom", "2pt");

    final Element tournament = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                              null == tournamentName ? "___" : tournamentName);
    row3.appendChild(tournament);
    tournament.setAttribute("font-size", "6pt");
    tournament.setAttribute("font-style", "italic");
    tournament.setAttribute("padding-top", "2pt");
    tournament.setAttribute("padding-bottom", "2pt");

    return table;
  }

  private Element createTitleBlock(final Document document) {
    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.setAttribute("background-color", "black");

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.appendChild(title);
    title.setAttribute("font-size", "14pt");
    title.setAttribute("font-weight", "bold");
    title.setAttribute("text-align", "center");
    title.setAttribute("color", "white");
    title.appendChild(document.createTextNode(description.getTitle()));

    final Element version = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.appendChild(version);
    version.setAttribute("font-size", "8pt");
    version.setAttribute("text-align", "center");
    version.setAttribute("color", "white");

    final StringBuilder versionText = new StringBuilder();
    versionText.append("SW version: ");
    versionText.append(Version.getVersion());
    if (null != description.getRevision()) {
      versionText.append(" Descriptor revision: ");
      versionText.append(description.getRevision());
    }
    version.appendChild(document.createTextNode(versionText.toString()));

    return block;
  }

  private Element createCheckBlock(final Document document) {
    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.setAttribute("font-size", "8pt");
    block.setAttribute("text-align", "center");
    // ensure that the text takes up the whole line
    block.setAttribute("text-align-last", "justify");

    block.appendChild(document.createTextNode("Data Entry Score _______"));

    // add leader to take up the space between the 2 blocks of text
    final Element space = FOPUtils.createXslFoElement(document, "leader");
    space.setAttribute("leader-pattern", "space");
    block.appendChild(space);

    block.appendChild(document.createTextNode("2nd Check Initials _______"));

    return block;
  }

  @Deprecated
  private void oldCode(final boolean orientationIsPortrait,
                       final float pagesPerScoreSheet)
      throws com.itextpdf.text.DocumentException {

    // FIXME old code below here
    com.itextpdf.text.Document pdfDoc;
    if (orientationIsPortrait) {
      pdfDoc = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.LETTER); // portrait
    } else {
      pdfDoc = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.LETTER.rotate()); // landscape
    }
    final OutputStream out = null;
    final PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
    writer.setPageEvent(new WatermarkHandler(this, pagesPerScoreSheet));

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
    final com.itextpdf.text.Paragraph titleParagraph = new com.itextpdf.text.Paragraph();
    final com.itextpdf.text.Chunk titleChunk = new com.itextpdf.text.Chunk(description.getTitle(),
                                                                           com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD,
                                                                                                                 14,
                                                                                                                 com.itextpdf.text.Font.NORMAL,
                                                                                                                 com.itextpdf.text.BaseColor.WHITE));
    titleParagraph.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
    titleParagraph.add(titleChunk);

    titleParagraph.add(com.itextpdf.text.Chunk.NEWLINE);
    final com.itextpdf.text.Chunk swVersionChunk = new com.itextpdf.text.Chunk("SW version: "
        + Version.getVersion(), com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 8, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.WHITE));
    titleParagraph.add(swVersionChunk);
    // if (null != m_revision) {
    //
    // final com.itextpdf.text.Chunk revisionChunk = new com.itextpdf.text.Chunk("
    // Descriptor revision: "
    // + m_revision,
    // com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA,
    // 8, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.WHITE));
    //
    // titleParagraph.add(revisionChunk);
    // }

    final PdfPCell head = new PdfPCell();
    head.setColspan(2);
    head.setBorder(1);
    head.setPaddingTop(0);
    head.setPaddingBottom(3);
    head.setBackgroundColor(new com.itextpdf.text.BaseColor(64, 64, 64));
    head.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_TOP);
    head.addElement(titleParagraph);

    // Cells for score field, and 2nd check initials
    final com.itextpdf.text.Phrase des = new com.itextpdf.text.Phrase("Data Entry Score _______", ARIAL_8PT_NORMAL);
    final PdfPCell desC = new PdfPCell(des);
    desC.setBorder(0);
    desC.setPaddingTop(9);
    desC.setPaddingRight(36);
    desC.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
    final com.itextpdf.text.Phrase sci = new com.itextpdf.text.Phrase("2nd Check Initials _______", ARIAL_8PT_NORMAL);
    final PdfPCell sciC = new PdfPCell(sci);
    sciC.setBorder(0);
    sciC.setPaddingTop(9);
    sciC.setPaddingRight(36);
    sciC.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

    // Create a table with a grid cell for each score sheet on the page
    PdfPTable wholePage = getTableForPage(orientationIsPortrait);
    wholePage.setWidthPercentage(100);
    for (int sheetIndex = 0; sheetIndex < m_numSheets; sheetIndex++) {
      if (sheetIndex > 0
          && (orientationIsPortrait
              || Utilities.isEven(sheetIndex))) {
        pdfDoc.newPage();
        wholePage = getTableForPage(orientationIsPortrait);
        wholePage.setWidthPercentage(100);
      }

      // This table is a single score sheet
      final PdfPTable scoreSheet = new PdfPTable(2);
      scoreSheet.getDefaultCell().setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
      scoreSheet.getDefaultCell().setPaddingRight(1);
      scoreSheet.getDefaultCell().setPaddingLeft(0);

      scoreSheet.addCell(head);

      final PdfPTable teamInfo = new PdfPTable(7);
      teamInfo.setWidthPercentage(100);
      teamInfo.setWidths(new float[] { 1f, 1f, 1f, 1f, 1f, 1f, .9f });

      // Time label cell
      final com.itextpdf.text.Paragraph timeP = new com.itextpdf.text.Paragraph("Time:", ARIAL_10PT_NORMAL);
      timeP.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell timeLc = new PdfPCell(scoreSheet.getDefaultCell());
      timeLc.addElement(timeP);
      teamInfo.addCell(timeLc);
      // Time value cell
      final com.itextpdf.text.Paragraph timeV = new com.itextpdf.text.Paragraph(null == m_time[sheetIndex] ? SHORT_BLANK
          : m_time[sheetIndex], COURIER_10PT_NORMAL);
      final PdfPCell timeVc = new PdfPCell(scoreSheet.getDefaultCell());
      timeVc.addElement(timeV);
      teamInfo.addCell(timeVc);

      // Table label cell
      final com.itextpdf.text.Paragraph tblP = new com.itextpdf.text.Paragraph("Table:", ARIAL_10PT_NORMAL);
      tblP.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell tblLc = new PdfPCell(scoreSheet.getDefaultCell());
      tblLc.addElement(tblP);
      teamInfo.addCell(tblLc);
      // Table value cell
      final com.itextpdf.text.Paragraph tblV = new com.itextpdf.text.Paragraph(m_table[sheetIndex],
                                                                               COURIER_10PT_NORMAL);
      final PdfPCell tblVc = new PdfPCell(scoreSheet.getDefaultCell());
      tblVc.addElement(tblV);
      teamInfo.addCell(tblVc);

      // Round number label cell
      final com.itextpdf.text.Paragraph rndP = new com.itextpdf.text.Paragraph("Round:", ARIAL_10PT_NORMAL);
      rndP.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell rndlc = new PdfPCell(scoreSheet.getDefaultCell());
      rndlc.addElement(rndP);
      teamInfo.addCell(rndlc);
      // Round number value cell
      final com.itextpdf.text.Paragraph rndV = new com.itextpdf.text.Paragraph(m_round[sheetIndex],
                                                                               COURIER_10PT_NORMAL);
      final PdfPCell rndVc = new PdfPCell(scoreSheet.getDefaultCell());
      // rndVc.setColspan(2);
      rndVc.addElement(rndV);
      teamInfo.addCell(rndVc);

      final PdfPCell temp1 = new PdfPCell(scoreSheet.getDefaultCell());
      // temp1.setColspan(2);
      temp1.addElement(new com.itextpdf.text.Paragraph("Ref ____", ARIAL_8PT_NORMAL));
      teamInfo.addCell(temp1);

      // Team number label cell
      final com.itextpdf.text.Paragraph nbrP = new com.itextpdf.text.Paragraph("Team #:", ARIAL_10PT_NORMAL);
      nbrP.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell nbrlc = new PdfPCell(scoreSheet.getDefaultCell());
      nbrlc.addElement(nbrP);
      teamInfo.addCell(nbrlc);
      // Team number value cell
      final com.itextpdf.text.Paragraph nbrV = new com.itextpdf.text.Paragraph(null == m_number[sheetIndex]
          ? SHORT_BLANK
          : String.valueOf(m_number[sheetIndex]), COURIER_10PT_NORMAL);
      final PdfPCell nbrVc = new PdfPCell(scoreSheet.getDefaultCell());
      nbrVc.addElement(nbrV);
      teamInfo.addCell(nbrVc);

      // Team division label cell
      final com.itextpdf.text.Paragraph divP = new com.itextpdf.text.Paragraph(m_divisionLabel[sheetIndex],
                                                                               ARIAL_10PT_NORMAL);
      divP.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell divlc = new PdfPCell(scoreSheet.getDefaultCell());
      divlc.addElement(divP);
      divlc.setColspan(2);
      teamInfo.addCell(divlc);
      // Team division value cell
      final com.itextpdf.text.Paragraph divV = new com.itextpdf.text.Paragraph(m_division[sheetIndex],
                                                                               COURIER_10PT_NORMAL);
      final PdfPCell divVc = new PdfPCell(scoreSheet.getDefaultCell());
      divVc.setColspan(2);
      divVc.addElement(divV);
      teamInfo.addCell(divVc);

      final PdfPCell temp2 = new PdfPCell(scoreSheet.getDefaultCell());
      // temp2.setColspan(2);
      temp2.addElement(new com.itextpdf.text.Paragraph("Team ____", ARIAL_8PT_NORMAL));
      teamInfo.addCell(temp2);

      // Team name label cell
      final com.itextpdf.text.Paragraph nameP = new com.itextpdf.text.Paragraph("Team Name:", ARIAL_10PT_NORMAL);
      nameP.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell namelc = new PdfPCell(scoreSheet.getDefaultCell());
      namelc.setColspan(2);
      namelc.addElement(nameP);
      teamInfo.addCell(namelc);
      // Team name value cell
      final PdfPCell nameVc = new PdfPCell(scoreSheet.getDefaultCell());
      nameVc.setColspan(4);
      nameVc.setCellEvent(new PdfUtils.TruncateContent(m_name[sheetIndex], COURIER_10PT_NORMAL));
      teamInfo.addCell(nameVc);

      // add tournament name
      final com.itextpdf.text.Paragraph tournamentNameV = new com.itextpdf.text.Paragraph(tournamentName, f6i);
      tournamentNameV.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      final PdfPCell tournamentNameVc = new PdfPCell(scoreSheet.getDefaultCell());
      tournamentNameVc.addElement(tournamentNameV);
      teamInfo.addCell(tournamentNameVc);

      // add team info cell to the team table
      final PdfPCell teamInfoCell = new PdfPCell(scoreSheet.getDefaultCell());
      teamInfoCell.addElement(teamInfo);
      teamInfoCell.setColspan(2);

      scoreSheet.addCell(teamInfoCell);

      // space and horizontal line
      final PdfPCell spacerCell = new PdfPCell(scoreSheet.getDefaultCell());
      spacerCell.setMinimumHeight(10);
      spacerCell.setBorderWidthTop(0);
      spacerCell.setBorderWidthBottom(1);
      spacerCell.setBorderWidthLeft(0);
      spacerCell.setBorderWidthRight(0);
      spacerCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      spacerCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      spacerCell.setColspan(2);
      scoreSheet.addCell(spacerCell);

      // if (null != m_goalsTable) {
      // final PdfPCell goalCell = new PdfPCell(/* m_goalsTable */);
      // goalCell.setBorder(0);
      // goalCell.setPadding(0);
      // goalCell.setColspan(2);
      // scoreSheet.addCell(goalCell);
      // }

      scoreSheet.addCell(desC);
      scoreSheet.addCell(sciC);

      // if (null != copyright) {
      // final com.itextpdf.text.Phrase copyright = new
      // com.itextpdf.text.Phrase("\u00A9"
      // + this.copyright, f6i);
      // final PdfPCell copyrightC = new PdfPCell(scoreSheet.getDefaultCell());
      // copyrightC.addElement(copyright);
      // copyrightC.setBorder(0);
      // copyrightC.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      // copyrightC.setColspan(2);
      //
      // scoreSheet.addCell(copyrightC);
      // }

      // the cell in the wholePage table that will contain the single score
      // sheet
      final PdfPCell scoresheetCell = new PdfPCell(scoreSheet);
      scoresheetCell.setBorder(0);
      scoresheetCell.setPadding(0);

      // Interior borders between scoresheets on a page
      if (!orientationIsPortrait) {
        if (Utilities.isEven(sheetIndex)) {
          scoresheetCell.setPaddingRight(0.1f
              * POINTS_PER_INCH);
        } else {
          scoresheetCell.setPaddingLeft(0.1f
              * POINTS_PER_INCH);
        }
      }

      // Add the current scoresheet to the page
      wholePage.addCell(scoresheetCell);

      // Add the current table of scoresheets to the com.itextpdf.text.Document
      if (orientationIsPortrait
          || (Utilities.isOdd(sheetIndex))) {
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

  private Element createGoalsTable(final Document document) {

    final PerformanceScoreCategory performanceElement = description.getPerformance();
    // use ArrayList as we will be doing indexed access in the loop
    final List<AbstractGoal> goals = new ArrayList<>(performanceElement.getGoals());

    final Element goalsTable = FOPUtils.createBasicTable(document);
    goalsTable.setAttribute("font-size", "10pt");
    goalsTable.setAttribute("font-family", "Helvetica");
    goalsTable.appendChild(FOPUtils.createTableColumn(document, 4));
    goalsTable.appendChild(FOPUtils.createTableColumn(document, 48));
    goalsTable.appendChild(FOPUtils.createTableColumn(document, 48));

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
    goalsTable.appendChild(tableBody);

    String prevCategory = null;
    for (int goalIndex = 0; goalIndex < goals.size(); ++goalIndex) {
      final AbstractGoal goal = goals.get(goalIndex);
      if (!goal.isComputed()) {
        final Element row = FOPUtils.createXslFoElement(document, "table-row");
        tableBody.appendChild(row);

        final String category = goal.getCategory();

        // add category cell if needed
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

            final Element categoryCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, category,
                                                                        90);
            row.appendChild(categoryCell);
            categoryCell.setAttribute("number-rows-spanned", String.valueOf(categoryRowSpan));
          }

          // first row in a new category, which may be empty
          FOPUtils.addTopBorder(row, 1);
        }

        // This is the text for the left hand "label" cell
        final Element goalLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, goal.getTitle());
        row.appendChild(goalLabel);
        goalLabel.setAttribute("padding-top", "2pt");
        goalLabel.setAttribute("padding-bottom", "2pt");
        goalLabel.setAttribute("padding-right", "9pt");

        if (StringUtils.isEmpty(category)) {
          // category column and goal label column
          goalLabel.setAttribute("number-columns-spanned", "2");
        }

        // define the value cell
        final double min = goal.getMin();
        final String minStr = FP.equals(min, Math.round(min), FinalComputedScores.TIE_TOLERANCE)
            ? String.valueOf((int) min)
            : String.valueOf(min);
        final double max = goal.getMax();
        final String maxStr = FP.equals(max, Math.round(max), FinalComputedScores.TIE_TOLERANCE)
            ? String.valueOf((int) max)
            : String.valueOf(max);

        // If element has child nodes, then we have an enumerated list
        // of choices. Otherwise it is either yes/no or a numeric field.
        final Element goalValue;
        if (goal.isEnumerated()) {
          final StringBuilder choices = new StringBuilder();

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
          goalValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, choices.toString());
        } else {
          if (goal.isYesNo()) {
            // order of yes/no needs to match ScoreEntry.generateYesNoButtons
            goalValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, "NO / YES");
          } else {
            goalValue = FOPUtils.createXslFoElement(document, "table-cell");

            final String range = "("
                + minStr
                + " - "
                + maxStr
                + ")";
            final Element t = FOPUtils.createBasicTable(document);
            goalValue.appendChild(t);
            t.setAttribute("width", "100pt");
            t.setAttribute("font-size", "8pt");
            t.appendChild(FOPUtils.createTableColumn(document, 1));
            t.appendChild(FOPUtils.createTableColumn(document, 1));

            final Element tBody = FOPUtils.createXslFoElement(document, "table-body");
            t.appendChild(tBody);

            final Element tRow = FOPUtils.createXslFoElement(document, "table-row");
            tBody.appendChild(tRow);

            final Element r = FOPUtils.createTableCell(document, null, "");
            tRow.appendChild(r);
            FOPUtils.addBorders(r, 1, 1, 1, 1);

            final Element q = FOPUtils.createTableCell(document, null, range);
            tRow.appendChild(q);
            FOPUtils.addBorders(q, 1, 1, 1, 1);
          }
        }
        goalValue.setAttribute("font-family", "Courier");
        goalValue.setAttribute("padding-top", "2pt");
        goalValue.setAttribute("padding-bottom", "2pt");
        row.appendChild(goalValue);

        // setup for next loop
        prevCategory = category;
      } // if not computed goal

    } // foreach goal

    return goalsTable;
  }

  private final int m_numSheets;

  private String[] m_table;

  private String[] m_name;

  private String[] m_round;

  private Integer[] m_number;

  public static final String HEAD_TO_HEAD_LABEL = "Head to head Bracket:";

  public static final String AWARD_GROUP_LABEL = "Award Group:";

  private String[] m_divisionLabel;

  private String[] m_division;

  private String[] m_time;

  private boolean[] m_isPractice;

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
  private void setTime(final int i,
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
   * Sets if the score sheet for the specified index is a practice round.
   *
   * @param i The 0-based index of the score sheet
   * @param isPractice true if this is a practice round
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setPractice(final int i,
                          final boolean isPractice)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + m_numSheets);
    }
    m_isPractice[i] = isPractice;
  }

  /**
   * Used by {@link WatermarkHandler}.
   *
   * @return If index is out of bounds, return false.
   */
  /* package */ boolean isPractice(final int index) {
    if (index >= m_isPractice.length) {
      return false;
    } else {
      return m_isPractice[index];
    }
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

  private static class WatermarkHandler extends PdfPageEventHelper {

    private final PdfGState gstate;

    private final com.itextpdf.text.Font font;

    private final com.itextpdf.text.BaseColor color;

    private final float pagesPerScoreSheet;

    private static final float WATERMARK_OPACITY = 0.2f;

    private static final double PAGES_PER_SHEET_TOLERANCE = 1E-6;

    private final ScoresheetGenerator generator;

    /**
     * @param pagesPerScoreSheet number of pages per score sheet, 0.5 and values
     *          greater than or equal to 1 are supported
     */
    public WatermarkHandler(final ScoresheetGenerator generator,
                            final float pagesPerScoreSheet) {
      if (!FP.equals(pagesPerScoreSheet, 0.5, PAGES_PER_SHEET_TOLERANCE)
          && !FP.greaterThanOrEqual(pagesPerScoreSheet, 1, PAGES_PER_SHEET_TOLERANCE)) {
        throw new IllegalArgumentException("Allowed values for pages per score sheet are 0.5 and 1 or greater. Value is: "
            + pagesPerScoreSheet);
      }
      this.pagesPerScoreSheet = pagesPerScoreSheet;
      this.generator = generator;

      font = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 52,
                                                   com.itextpdf.text.Font.NORMAL);
      color = com.itextpdf.text.BaseColor.BLACK;

      gstate = new PdfGState();
      gstate.setFillOpacity(WATERMARK_OPACITY);
      gstate.setStrokeOpacity(WATERMARK_OPACITY);

    }

    @Override
    public void onEndPage(final PdfWriter writer,
                          final com.itextpdf.text.Document document) {
      // Need to determine which score sheet we are on based on page number and pages
      // per sheet.

      final com.itextpdf.text.Rectangle pageSize = document.getPageSize();
      final float y = pageSize.getHeight()
          / 2;

      final int currentPageNumber = writer.getCurrentPageNumber();
      final int indexOfFirstScoreSheetOnPage;
      if (pagesPerScoreSheet < 1) {
        indexOfFirstScoreSheetOnPage = (int) Math.floor(currentPageNumber
            / pagesPerScoreSheet)
            - 2;

        if (generator.isPractice(indexOfFirstScoreSheetOnPage)) {
          final float x = pageSize.getWidth()
              / 4;
          addWatermark(writer, x, y);
        }

        // check the second sheet on the page
        if (generator.isPractice(indexOfFirstScoreSheetOnPage
            + 1)) {
          final float x = pageSize.getWidth()
              * 3
              / 4;
          addWatermark(writer, x, y);
        }

      } else {
        indexOfFirstScoreSheetOnPage = (int) Math.ceil(currentPageNumber
            / pagesPerScoreSheet)
            - 1;
        if (generator.isPractice(indexOfFirstScoreSheetOnPage)) {
          final float x = pageSize.getWidth()
              / 2;
          addWatermark(writer, x, y);
        }
      }

    }

    private void addWatermark(final PdfWriter writer,
                              final float x,
                              final float y) {
      final PdfContentByte contentunder = writer.getDirectContentUnder();
      contentunder.saveState();
      contentunder.setGState(gstate);
      contentunder.beginText();
      contentunder.setFontAndSize(font.getBaseFont(), font.getSize());
      contentunder.setColorFill(color);
      contentunder.showTextAligned(com.itextpdf.text.Element.ALIGN_CENTER, "PRACTICE", x, y, 45);
      contentunder.endText();
      contentunder.restoreState();
    }

  }

}
