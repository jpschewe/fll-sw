/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

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
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Team;
import fll.Utilities;
import fll.Version;
import fll.db.Queries;
import fll.util.FP;
import fll.util.LogUtils;
import fll.xml.ChallengeParser;

/**
 * @author Dan Churchill
 */
public class ScoresheetGenerator {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String LONG_BLANK = "_________________________";

  private static final String SHORT_BLANK = "______";

  private static final char NON_BREAKING_SPACE = '\u00a0';

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
                             final org.w3c.dom.Document document) throws SQLException {
    final String numMatchesStr = request.getParameter("numMatches");
    if (null == numMatchesStr) {
      // must have been called asking for blank
      m_numTeams = Queries.getScoresheetLayoutNUp(connection);
      initializeArrays();

      setPageTitle("");
      for (int i = 0; i < m_numTeams; i++) {
        m_table[i] = SHORT_BLANK;
        m_name[i] = LONG_BLANK;
        m_round[i] = SHORT_BLANK;
        m_number[i] = null;
      }
    } else {
      // called with specific sheets to print
      final int numMatches = Integer.parseInt(numMatchesStr);
      final boolean[] checkedMatches = new boolean[numMatches + 1]; // ignore
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

      m_numTeams = checkedMatchCount * 2;

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
            final int iRound = Integer.parseInt(round);
            // Get teamA info
            final Team teamA = Team.getTeamFromDatabase(connection, Integer.parseInt(request.getParameter("teamA"
                + i)));
            m_name[j] = teamA.getTrimmedTeamName();
            m_number[j] = teamA.getTeamNumber();
            m_round[j] = "Round P"
                + round;
            m_table[j] = request.getParameter("tableA"
                + i);
            updatePrep.setString(1, m_table[j]);
            updatePrep.setString(2, Queries.getEventDivision(connection, teamA.getTeamNumber()));
            updatePrep.setInt(4, iRound);
            updatePrep.setInt(5, teamA.getTeamNumber());
            updatePrep.executeUpdate();
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
            updatePrep.setString(1, m_table[j]);
            updatePrep.setString(2, Queries.getEventDivision(connection, teamB.getTeamNumber()));
            updatePrep.setInt(4, iRound);
            updatePrep.setInt(5, teamB.getTeamNumber());
            updatePrep.executeUpdate();
            j++;
          }
        }
      } finally {
        SQLFunctions.close(updatePrep);
      }
    }
    setChallengeInfo(document);
  }

  /**
   * Private support function to create new data arrays for the scoresheet
   * information. IMPORTANT!!! The value of m_numTeams must be set before the
   * call to this method is made.
   */
  private void initializeArrays() {
    m_table = new String[m_numTeams];
    m_name = new String[m_numTeams];
    m_round = new String[m_numTeams];
    m_number = new Integer[m_numTeams];
    m_goalLabel = new PdfPCell[0];
    m_goalValue = new PdfPCell[0];
  }

  private static final Font ARIAL_8PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL,
                                                                   new BaseColor(0, 0, 0));

  private static final Font ARIAL_10PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font COURIER_10PT_NORMAL = FontFactory.getFont(FontFactory.COURIER, 10, Font.NORMAL);

  private static final int POINTS_PER_INCH = 72;

  public void writeFile(final Connection connection,
                        final OutputStream out) throws DocumentException, SQLException {

    final int nup = Queries.getScoresheetLayoutNUp(connection);
    final boolean orientationIsPortrait;
    if (nup == 1) {
      orientationIsPortrait = true;
    } else {
      orientationIsPortrait = false;
    }

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
    pdfDoc.setMargins(0.5f * POINTS_PER_INCH, 0.5f * POINTS_PER_INCH, 0.35f * POINTS_PER_INCH, 0.35f * POINTS_PER_INCH);
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

    final PdfPTable[] team = new PdfPTable[m_numTeams];
    final PdfPCell[] cell = new PdfPCell[m_numTeams];

    // Create a table with a grid cell for each scoresheet on the page
    PdfPTable wholePage = getTableForPage(nup);
    wholePage.setWidthPercentage(100);
    for (int i = 0; i < m_numTeams; i++) {
      if (i > 0
          && (i % nup) == 0) {
        pdfDoc.newPage();
        wholePage = getTableForPage(nup);
        wholePage.setWidthPercentage(100);
      }
      team[i] = new PdfPTable(2);
      team[i].getDefaultCell().setBorder(0);

      team[i].addCell(head);

      final PdfPTable teamInfo = new PdfPTable(5);
      teamInfo.setWidthPercentage(100);

      // Table label cell
      final Paragraph tblP = new Paragraph("Table:", ARIAL_10PT_NORMAL);
      tblP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell tblLc = new PdfPCell(team[i].getDefaultCell());
      tblLc.setPaddingRight(2);
      tblLc.addElement(tblP);
      teamInfo.addCell(tblLc);
      // Table value cell
      final Paragraph tblV = new Paragraph(m_table[i], COURIER_10PT_NORMAL);
      final PdfPCell tblVc = new PdfPCell(team[i].getDefaultCell());
      tblVc.addElement(tblV);
      teamInfo.addCell(tblVc);

      // Round number label cell
      final Paragraph rndP = new Paragraph("Round:", ARIAL_10PT_NORMAL);
      rndP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell rndlc = new PdfPCell(team[i].getDefaultCell());
      rndlc.setPaddingRight(2);
      rndlc.addElement(rndP);
      teamInfo.addCell(rndlc);
      // Round number value cell
      final Paragraph rndV = new Paragraph(m_round[i], COURIER_10PT_NORMAL);
      final PdfPCell rndVc = new PdfPCell(team[i].getDefaultCell());
      // rndVc.setColspan(2);
      rndVc.addElement(rndV);
      teamInfo.addCell(rndVc);

      final PdfPCell temp1 = new PdfPCell(team[i].getDefaultCell());
      // temp1.setColspan(2);
      temp1.addElement(new Paragraph("Judge ____", ARIAL_8PT_NORMAL));
      teamInfo.addCell(temp1);

      // Team number label cell
      final Paragraph nbrP = new Paragraph("Team #:", ARIAL_10PT_NORMAL);
      nbrP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell nbrlc = new PdfPCell(team[i].getDefaultCell());
      nbrlc.setPaddingRight(2);
      nbrlc.addElement(nbrP);
      teamInfo.addCell(nbrlc);
      // Team number value cell
      final Paragraph nbrV = new Paragraph(null == m_number[i] ? SHORT_BLANK : String.valueOf(m_number[i]),
                                           COURIER_10PT_NORMAL);
      final PdfPCell nbrVc = new PdfPCell(team[i].getDefaultCell());
      nbrVc.addElement(nbrV);
      teamInfo.addCell(nbrVc);

      // Team division label cell
      final Paragraph divP = new Paragraph("Division:", ARIAL_10PT_NORMAL);
      divP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell divlc = new PdfPCell(team[i].getDefaultCell());
      divlc.setPaddingRight(2);
      divlc.addElement(divP);
      teamInfo.addCell(divlc);
      // Team division value cell
      final String divStr;
      if (null != m_number[i]) {
        divStr = Queries.getEventDivision(connection, m_number[i]);
      } else {
        divStr = SHORT_BLANK;
      }
      final Paragraph divV = new Paragraph(divStr, COURIER_10PT_NORMAL);
      final PdfPCell divVc = new PdfPCell(team[i].getDefaultCell());
      // divVc.setColspan(2);
      divVc.addElement(divV);
      teamInfo.addCell(divVc);

      final PdfPCell temp2 = new PdfPCell(team[i].getDefaultCell());
      // temp2.setColspan(2);
      temp2.addElement(new Paragraph("Team ____", ARIAL_8PT_NORMAL));
      teamInfo.addCell(temp2);

      // Team name label cell
      final Paragraph nameP = new Paragraph("Team Name:", ARIAL_10PT_NORMAL);
      nameP.setAlignment(Element.ALIGN_RIGHT);
      final PdfPCell namelc = new PdfPCell(team[i].getDefaultCell());
      namelc.setPaddingRight(9);
      namelc.addElement(nameP);
      teamInfo.addCell(namelc);
      // Team name value cell
      final Paragraph nameV = new Paragraph(m_name[i], COURIER_10PT_NORMAL);
      final PdfPCell nameVc = new PdfPCell(team[i].getDefaultCell());
      nameVc.setColspan(4);
      nameVc.addElement(nameV);
      teamInfo.addCell(nameVc);

      // add team info cell to the team table
      final PdfPCell teamInfoCell = new PdfPCell(team[i].getDefaultCell());
      teamInfoCell.addElement(teamInfo);
      teamInfoCell.setColspan(2);

      team[i].addCell(teamInfoCell);

      for (int j = 0; j < m_goalLabel.length; j++) {
        team[i].addCell(m_goalLabel[j]);
        team[i].addCell(m_goalValue[j]);
      }

      team[i].addCell(desC);
      team[i].addCell(sciC);

      cell[i] = new PdfPCell(team[i]);
      cell[i].setBorder(0);
      cell[i].setPadding(0);

      // Interior borders between scoresheets on a page
      if (nup > 1) {
        if (i % 2 == 0) {
          cell[i].setPaddingRight(0.1f * POINTS_PER_INCH);
        } else {
          cell[i].setPaddingLeft(0.1f * POINTS_PER_INCH);
        }
      }

      // Add the current scoresheet to the page
      wholePage.addCell(cell[i]);

      // Add the current table of scoresheets to the document
      if ((i % nup) == (nup - 1)) {
        pdfDoc.add(wholePage);
      }
    }

    // Add a blank cells to complete the table of the last page
    final int numBlanks = (nup - (m_numTeams % nup))
        % nup;
    if (numBlanks > 0) {
      for (int j = 0; j < numBlanks; j++) {
        final PdfPCell blank = new PdfPCell();
        blank.setBorder(0);
        wholePage.addCell(blank);
      }
      pdfDoc.add(wholePage);
    }

    pdfDoc.close();
  }

  /**
   * Stores the goal cells that are inserted into the output after the team name
   * headers and before the scoring/initials blanks at the bottom of the
   * scoresheet.
   * 
   * @param document The document object containing the challenge descriptor
   *          info.
   */
  public void setChallengeInfo(final org.w3c.dom.Document document) {
    final org.w3c.dom.Element rootElement = document.getDocumentElement();
    setPageTitle(rootElement.getAttribute("title"));
    if (rootElement.hasAttribute("revision")) {
      setRevisionInfo(rootElement.getAttribute("revision"));
    }

    final org.w3c.dom.Element performanceElement = (org.w3c.dom.Element) rootElement.getElementsByTagName("Performance")
                                                                                    .item(0);
    final List<org.w3c.dom.Element> goals = new NodelistElementCollectionAdapter(
                                                                                 performanceElement.getElementsByTagName("goal")).asList();

    m_goalLabel = new PdfPCell[goals.size()];
    m_goalValue = new PdfPCell[goals.size()];
    int realI = 0;
    for (final org.w3c.dom.Element element : new NodelistElementCollectionAdapter(performanceElement.getChildNodes())) {
      if (element.getNodeType() == Node.ELEMENT_NODE) {
        if (element.getNodeName().equals("goal")) {
          final String name = element.getAttribute("name");

          // This is the text for the left hand "label" cell
          final String title = element.getAttribute("title");
          final Paragraph p = new Paragraph(title, ARIAL_10PT_NORMAL);
          p.setAlignment(Element.ALIGN_RIGHT);
          m_goalLabel[realI] = new PdfPCell();
          m_goalLabel[realI].setBorder(0);
          m_goalLabel[realI].setPaddingRight(9);
          m_goalLabel[realI].addElement(p);
          m_goalLabel[realI].setVerticalAlignment(Element.ALIGN_TOP);
          try {
            final double min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).doubleValue();
            final String minStr = FP.equals(min, Math.round(min), 1E-6) ? String.valueOf((int) min)
                : String.valueOf(min);
            final double max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).doubleValue();
            final String maxStr = FP.equals(max, Math.round(max), 1E-6) ? String.valueOf((int) max)
                : String.valueOf(max);

            // If element has child nodes, then we have an enumerated list
            // of choices. Otherwise it is either yes/no or a numeric field.
            m_goalValue[realI] = new PdfPCell();
            final Chunk choices = new Chunk("", COURIER_10PT_NORMAL);
            if (element.hasChildNodes()) {
              // replace spaces with "no-break" spaces
              boolean first = true;
              for (final org.w3c.dom.Element value : new NodelistElementCollectionAdapter(
                                                                                          element.getElementsByTagName("value"))) {
                if (!first) {
                  choices.append(" /"
                      + NON_BREAKING_SPACE);
                } else {
                  first = false;
                }
                choices.append(value.getAttribute("title").toUpperCase().replace(' ', NON_BREAKING_SPACE));
              }
              m_goalValue[realI].addElement(choices);

            } else {
              if (FP.equals(0, min, ChallengeParser.INITIAL_VALUE_TOLERANCE)
                  && FP.equals(1, max, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
                final Paragraph q = new Paragraph("YES / NO", COURIER_10PT_NORMAL);
                m_goalValue[realI].addElement(q);

              } else {
                final String range = "("
                    + minStr + " - " + maxStr + ")";
                final PdfPTable t = new PdfPTable(2);
                t.setHorizontalAlignment(Element.ALIGN_LEFT);
                t.setTotalWidth(1 * POINTS_PER_INCH);
                t.setLockedWidth(true);
                final Phrase r = new Phrase("", ARIAL_8PT_NORMAL);
                t.addCell(new PdfPCell(r));
                final Phrase q = new Phrase(range, ARIAL_8PT_NORMAL);
                t.addCell(new PdfPCell(q));
                m_goalValue[realI].setPaddingTop(9);
                m_goalValue[realI].addElement(t);
              }
            }
          } catch (final ParseException pe) {
            throw new RuntimeException("FATAL: min/max not parsable for goal: "
                + name);
          }

          m_goalValue[realI].setBorder(0);
          m_goalValue[realI].setVerticalAlignment(Element.ALIGN_MIDDLE);
          realI++;
        }
      }
    }
  }

  private int m_numTeams;

  private String m_revision;

  private String m_pageTitle;

  private String[] m_table;

  private String[] m_name;

  private String[] m_round;

  private Integer[] m_number;

  private PdfPCell[] m_goalLabel;

  private PdfPCell[] m_goalValue;

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
                       final String table) throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < "
          + m_numTeams);
    }
    m_table[i] = table;
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
                      final String name) throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < "
          + m_numTeams);
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
                        final Integer number) throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < "
          + m_numTeams);
    }
    m_number[i] = number;
  }

  /**
   * Sets the round number descriptor for scoresheet with index i.
   * 
   * @param i The 0-based index of the scoresheet to which to assign this round
   *          number.
   * @param round A string with the round number descriptor for the specified
   *          scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setRound(final int i,
                       final String round) throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < "
          + m_numTeams);
    }
    m_round[i] = round;
  }

  /**
   * Create table for page given number of sheets per page.
   * 
   * @param nup
   * @return
   */
  private static PdfPTable getTableForPage(final int nup) {
    final PdfPTable wholePage;
    switch (nup) {
    case 1: {
      wholePage = new PdfPTable(1); // 1 column
      break;
    }
    case 2: {
      wholePage = new PdfPTable(2); // 2 columns
      break;
    }
    default: {
      wholePage = new PdfPTable(2); // default to 2 columns - should never get
      // here
      LOGGER.error("Nup set to something later than 2: "
          + nup + " defaulting to 2.");
      break;
    }
    }
    return wholePage;
  }
}
