/**
 * 
 */
package fll.web.scoreEntry;

import java.awt.Color;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Node;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import fll.Team;
import fll.Utilities;
import fll.Version;
import fll.db.Queries;
import fll.xml.XMLUtils;

/**
 * @author Dan Churchill
 * 
 */
public class ScoresheetGenerator {
  private static final String LONG_BLANK = "_________________________";

  private static final String SHORT_BLANK = "___________";

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
   * placed in the generated form for that field.
   * 
   * This also updates the PlayoffData table in the assumption that the created
   * scoresheet will be printed, so table assignments are stored into the
   * database and the match is marked as printed. It is very questionable
   * whether this is where this should happen, but I don't feel like breaking it
   * out.
   * 
   * @param formParms
   *          java.util.Map object containing objects as described above.
   * @param connection
   *          java.sql.Connection object with open connection to the database.
   * @param document
   *          DOM document containing the challenge info used to generate the
   *          scoresheet.
   */
  public ScoresheetGenerator(final Map<?, ?> formParms, final Connection connection, final String tournament, final org.w3c.dom.Document document)
      throws SQLException {
    final int numMatches = Integer.parseInt(((String[])formParms.get("numMatches"))[0]);
    final boolean[] checkedMatches = new boolean[numMatches + 1]; // ignore slot
    // index 0
    int checkedMatchCount = 0;
    // Build array of out how many matches we are printing
    for(int i = 1; i <= numMatches; i++) {
      String checkX = "print" + i;
      checkedMatches[i] = null != formParms.get(checkX);
      if(checkedMatches[i]) {
        checkedMatchCount++;
      }
    }

    m_numTeams = checkedMatchCount * 2;

    initializeArrays();
    setPageTitle(m_pageTitle);

    // Loop through checked matches, populate data, and update database to track
    // printed status and remember assigned tables.
    PreparedStatement updatePrep = null;
    try {
      // build up the SQL
      updatePrep = connection.prepareStatement("UPDATE PlayoffData SET Printed=true, AssignedTable=?"
          + " WHERE event_division=? AND Tournament=? AND PlayoffRound=? AND Team=?");
      // could do division here, too, but since getting it from Team object,
      // will defer to same place as other
      updatePrep.setString(3, tournament);

      int j = 0;
      for(int i = 1; i <= numMatches; i++) {
        if(checkedMatches[i]) {
          final String round = ((String[])formParms.get("round" + i))[0];
          final int iRound = Integer.parseInt(round);
          // Get teamA info
          final Team teamA = Team.getTeamFromDatabase(connection, Integer.parseInt(((String[])formParms.get(new String("teamA" + i)))[0]));
          m_name[j] = teamA.getTeamName();
          m_number[j] = Integer.toString(teamA.getTeamNumber());
          m_round[j] = "Playoff Round " + round;
          m_table[j] = ((String[])formParms.get(new String("tableA" + i)))[0];
          updatePrep.setString(1, m_table[j]);
          updatePrep.setString(2, Queries.getEventDivision(connection, teamA.getTeamNumber()));
          updatePrep.setInt(4, iRound);
          updatePrep.setInt(5, teamA.getTeamNumber());
          updatePrep.executeUpdate();
          j++;
          // Get teamB info
          final Team teamB = Team.getTeamFromDatabase(connection, Integer.parseInt(((String[])formParms.get(new String("teamB" + i)))[0]));
          m_name[j] = teamB.getTeamName();
          m_number[j] = Integer.toString(teamB.getTeamNumber());
          m_round[j] = "Playoff Round " + round;
          m_table[j] = ((String[])formParms.get(new String("tableB" + i)))[0];
          updatePrep.setString(1, m_table[j]);
          updatePrep.setString(2, Queries.getEventDivision(connection, teamB.getTeamNumber()));
          updatePrep.setInt(4, iRound);
          updatePrep.setInt(5, teamB.getTeamNumber());
          updatePrep.executeUpdate();
          j++;
        }
      }
    } finally {
      SQLFunctions.closePreparedStatement(updatePrep);
    }

    setChallengeInfo(document);
  }

  /**
   * Print blank scoresheets.
   * 
   * @param numTeams
   *          number of scoresheets to print
   * @param document
   *          the challenge document
   */
  public ScoresheetGenerator(final int numTeams, final org.w3c.dom.Document document) {
    m_numTeams = numTeams;
    initializeArrays();

    setPageTitle("");
    for(int i = 0; i < m_numTeams; i++) {
      m_table[i] = SHORT_BLANK;
      m_name[i] = LONG_BLANK;
      m_round[i] = SHORT_BLANK;
      m_number[i] = SHORT_BLANK;
    }
    setChallengeInfo(document);
  }

  /**
   * Print a single scoresheet for a team.
   * 
   * @param teamNumber
   *          the team to print the scoresheet for
   * @param document
   *          the challenge document
   * @param connection
   *          the database connection
   */
  public ScoresheetGenerator(final int teamNumber, final org.w3c.dom.Document document, final Connection connection) throws SQLException {
    m_numTeams = 1;
    initializeArrays();

    final Team teamA = Team.getTeamFromDatabase(connection, teamNumber);

    setPageTitle("");
    m_table[0] = SHORT_BLANK;
    m_name[0] = teamA.getTeamName();
    m_round[0] = SHORT_BLANK;
    m_number[0] = Integer.toString(teamA.getTeamNumber());
    setChallengeInfo(document);
  }

  /**
   * Private support function to create new data arrays for the scoresheet
   * information. IMPORTANT!!! The value of m_numTeams must be set before the
   * call to this method is made.
   * 
   */
  private void initializeArrays() {
    m_table = new String[m_numTeams];
    m_name = new String[m_numTeams];
    m_round = new String[m_numTeams];
    m_number = new String[m_numTeams];
    m_goalLabel = new PdfPCell[0];
    m_goalValue = new PdfPCell[0];
  }

  private static final Font ARIAL_8PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(0, 0, 0));

  private static final Font ARIAL_10PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font COURIER_10PT_NORMAL = FontFactory.getFont(FontFactory.COURIER, 10, Font.NORMAL);

  public void writeFile(final Connection connection, final OutputStream out) throws DocumentException, SQLException {

    final int nup = Queries.getScoresheetLayoutNUp(connection);
    boolean orientationIsPortrait;

    if(nup == 1) {
      orientationIsPortrait = true;
    } else if(nup == 2) {
      orientationIsPortrait = false;
    } else {
      orientationIsPortrait = false;
    }

    // This creates our new PDF document and declares its orientation
    Document pdfDoc;
    if(orientationIsPortrait) {
      pdfDoc = new Document(PageSize.LETTER); // portrait
    } else {
      pdfDoc = new Document(PageSize.LETTER.rotate()); // landscape
    }
    PdfWriter.getInstance(pdfDoc, out);

    // Measurements are always in points (72 per inch)
    // This sets up 1/2 inch margins
    pdfDoc.setMargins(0.5f * 72, 0.5f * 72, 0.35f * 72, 0.35f * 72);
    pdfDoc.open();

    // Header cell with challenge title to add to both scoresheets
    final Paragraph titleParagraph = new Paragraph();
    final Chunk titleChunk = new Chunk(m_pageTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL, Color.WHITE));
    titleParagraph.setAlignment(Element.ALIGN_CENTER);
    titleParagraph.add(titleChunk);

    titleParagraph.add(Chunk.NEWLINE);
    final Chunk swVersionChunk = new Chunk("SW version: " + Version.getVersion(), FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.WHITE));
    titleParagraph.add(swVersionChunk);
    if(null != m_revision) {

      final Chunk revisionChunk = new Chunk(" Descriptor revision: " + m_revision, FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.WHITE));

      titleParagraph.add(revisionChunk);
    }

    final PdfPCell head = new PdfPCell();
    head.setColspan(2);
    head.setBorder(1);
    head.setPaddingTop(0);
    head.setPaddingBottom(3);
    head.setBackgroundColor(new Color(64, 64, 64));
    head.setVerticalAlignment(Element.ALIGN_TOP);
    head.addElement(titleParagraph);

    // Cells for judge initials, team initials, score field, and 2nd
    // check initials
    Phrase ji = new Phrase("Judge's Initials _______", ARIAL_8PT_NORMAL);
    PdfPCell jiC = new PdfPCell(ji);
    jiC.setBorder(0);
    jiC.setPaddingTop(9);
    jiC.setPaddingRight(36);
    jiC.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase des = new Phrase("Data Entry Score _______", ARIAL_8PT_NORMAL);
    PdfPCell desC = new PdfPCell(des);
    desC.setBorder(0);
    desC.setPaddingTop(9);
    desC.setPaddingRight(36);
    desC.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase tci = new Phrase("Team Check Inititals _______", ARIAL_8PT_NORMAL);
    PdfPCell tciC = new PdfPCell(tci);
    tciC.setBorder(0);
    tciC.setPaddingTop(9);
    tciC.setPaddingRight(36);
    tciC.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase sci = new Phrase("2nd Check Initials _______", ARIAL_8PT_NORMAL);
    PdfPCell sciC = new PdfPCell(sci);
    sciC.setBorder(0);
    sciC.setPaddingTop(9);
    sciC.setPaddingRight(36);
    sciC.setHorizontalAlignment(Element.ALIGN_RIGHT);

    Phrase cpr = new Phrase("All Challenge Pieces Returned _______", ARIAL_8PT_NORMAL);
    PdfPCell cprC = new PdfPCell(cpr);
    cprC.setBorder(0);
    cprC.setPaddingTop(9);
    cprC.setPaddingRight(36);
    cprC.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase blank1 = new Phrase("", ARIAL_8PT_NORMAL);
    PdfPCell blankC = new PdfPCell(blank1);
    blankC.setBorder(0);
    blankC.setPaddingTop(9);
    blankC.setPaddingRight(36);
    blankC.setHorizontalAlignment(Element.ALIGN_RIGHT);

    PdfPTable[] team = new PdfPTable[m_numTeams];
    PdfPCell[] cell = new PdfPCell[m_numTeams];

    // Create a table with a grid cell for each scoresheet on the page
    PdfPTable wholePage;
    // TODO - break this if statement out into a private function
    if(nup == 1) {
      wholePage = new PdfPTable(1); // 1 column
    } else if(nup == 2) {
      wholePage = new PdfPTable(2); // 2 columns
    } else {
      wholePage = new PdfPTable(2); // default to 2 columns - should never get
      // here
    }
    wholePage.setWidthPercentage(100);
    for(int i = 0; i < m_numTeams; i++) {
      if(i > 0 && (i % nup) == 0) {
        pdfDoc.newPage();
        if(nup == 1) {
          wholePage = new PdfPTable(1); // 1 column
        } else if(nup == 2) {
          wholePage = new PdfPTable(2); // 2 columns
        } else {
          wholePage = new PdfPTable(2); // default to 2 columns - should never
          // get here
        }
        wholePage.setWidthPercentage(100);
      }
      team[i] = new PdfPTable(2);
      team[i].getDefaultCell().setBorder(0);

      team[i].addCell(head);

      // Table label cell
      Paragraph tblP = new Paragraph("Table:", ARIAL_10PT_NORMAL);
      tblP.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell tblLc = new PdfPCell(team[i].getDefaultCell());
      tblLc.setPaddingRight(9);
      tblLc.addElement(tblP);
      team[i].addCell(tblLc);
      // Table value cell
      Paragraph tblV = new Paragraph(m_table[i], COURIER_10PT_NORMAL);
      PdfPCell tblVc = new PdfPCell(team[i].getDefaultCell());
      tblVc.addElement(tblV);
      team[i].addCell(tblVc);

      // Round number label cell
      Paragraph rndP = new Paragraph("Round Number:", ARIAL_10PT_NORMAL);
      rndP.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell rndlc = new PdfPCell(team[i].getDefaultCell());
      rndlc.setPaddingRight(9);
      rndlc.addElement(rndP);
      team[i].addCell(rndlc);
      // Round number value cell
      Paragraph rndV = new Paragraph(m_round[i], COURIER_10PT_NORMAL);
      PdfPCell rndVc = new PdfPCell(team[i].getDefaultCell());
      rndVc.addElement(rndV);
      team[i].addCell(rndVc);

      // Team number label cell
      Paragraph nbrP = new Paragraph("Team Number:", ARIAL_10PT_NORMAL);
      nbrP.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell nbrlc = new PdfPCell(team[i].getDefaultCell());
      nbrlc.setPaddingRight(9);
      nbrlc.addElement(nbrP);
      team[i].addCell(nbrlc);
      // Team number value cell
      Paragraph nbrV = new Paragraph(m_number[i], COURIER_10PT_NORMAL);
      PdfPCell nbrVc = new PdfPCell(team[i].getDefaultCell());
      nbrVc.addElement(nbrV);
      team[i].addCell(nbrVc);

      // Team name label cell
      Paragraph nameP = new Paragraph("Team Name:", ARIAL_10PT_NORMAL);
      nameP.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell namelc = new PdfPCell(team[i].getDefaultCell());
      namelc.setPaddingRight(9);
      namelc.addElement(nameP);
      team[i].addCell(namelc);
      // Team name value cell
      Paragraph nameV = new Paragraph(m_name[i], COURIER_10PT_NORMAL);
      PdfPCell nameVc = new PdfPCell(team[i].getDefaultCell());
      nameVc.addElement(nameV);
      team[i].addCell(nameVc);

      PdfPCell blankRow = new PdfPCell(new Phrase(""));
      blankRow.setColspan(2);
      blankRow.setBorder(0);
      blankRow.setMinimumHeight(9);
      team[i].addCell(blankRow);

      for(int j = 0; j < m_goalLabel.length; j++) {
        team[i].addCell(m_goalLabel[j]);
        team[i].addCell(m_goalValue[j]);
      }

      team[i].addCell(jiC);
      team[i].addCell(desC);
      team[i].addCell(tciC);
      team[i].addCell(sciC);

      // second check
      team[i].addCell(cprC);
      team[i].addCell(blankC);

      cell[i] = new PdfPCell(team[i]);
      cell[i].setBorder(0);
      cell[i].setPadding(0);

      // Interior borders between scoresheets on a page
      if(nup > 1) {
        if(i % 2 == 0) {
          cell[i].setPaddingRight(36);
        } else {
          cell[i].setPaddingLeft(36);
        }
      }

      // Add the current scoresheet to the page
      wholePage.addCell(cell[i]);

      // Add the current table of scoresheets to the document
      if((i % nup) == (nup - 1)) {
        pdfDoc.add(wholePage);
      }
    }

    // Add a blank cells to complete the table of the last page
    int numBlanks = (nup - (m_numTeams % nup)) % nup;
    if(numBlanks > 0) {
      for(int j = 0; j < numBlanks; j++) {
        PdfPCell blank = new PdfPCell();
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
   * @param document
   *          The document object containing the challenge descriptor info.
   */
  public void setChallengeInfo(final org.w3c.dom.Document document) {
    final org.w3c.dom.Element rootElement = document.getDocumentElement();
    setPageTitle(rootElement.getAttribute("title"));
    if(rootElement.hasAttribute("revision")) {
      setRevisionInfo(rootElement.getAttribute("revision"));
    }

    final org.w3c.dom.Element performanceElement = (org.w3c.dom.Element)rootElement.getElementsByTagName("Performance").item(0);
    final List<org.w3c.dom.Element> goals = XMLUtils.filterToElements(performanceElement.getElementsByTagName("goal"));
    final List<org.w3c.dom.Element> computedGoals = XMLUtils.filterToElements(performanceElement.getElementsByTagName("computedGoal"));
    final List<org.w3c.dom.Element> children = XMLUtils.filterToElements(performanceElement.getChildNodes());

    m_goalLabel = new PdfPCell[goals.size() + computedGoals.size()];
    m_goalValue = new PdfPCell[goals.size() + computedGoals.size()];
    int realI = 0;
    for(final org.w3c.dom.Element element : children) {
      if(element.getNodeType() == Node.ELEMENT_NODE) {
        if(element.getNodeName().equals("goal") || element.getNodeName().equals("computedGoal")) {
          final String name = element.getAttribute("name");

          // This is the text for the left hand "label" cell
          final String title = element.getAttribute("title");
          Paragraph p = new Paragraph(title, ARIAL_10PT_NORMAL);
          p.setAlignment(Element.ALIGN_RIGHT);
          m_goalLabel[realI] = new PdfPCell();
          m_goalLabel[realI].setBorder(0);
          m_goalLabel[realI].setPaddingRight(9);
          m_goalLabel[realI].addElement(p);
          m_goalLabel[realI].setVerticalAlignment(Element.ALIGN_TOP);
          // If element is a computed goal, just put a blank on the right.
          if(element.getNodeName().equals("computedGoal")) {
            Paragraph q = new Paragraph(SHORT_BLANK, COURIER_10PT_NORMAL);
            m_goalValue[realI] = new PdfPCell();
            m_goalValue[realI].addElement(q);
          } else {
            try {
              final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).intValue();
              final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).intValue();

              // If element has child nodes, then we have an enumerated list
              // of choices. Otherwise it is either yes/no or a numeric field.
              if(element.hasChildNodes()) {
                final List<org.w3c.dom.Element> posValues = XMLUtils.filterToElements(element.getElementsByTagName("value"));
                String choices = posValues.get(0).getAttribute("title").replace(" ", "\u00a0"); // replace
                // spaces
                // with
                // "no-break"
                // spaces
                for(int v = 1; v < posValues.size(); v++) {
                  choices = choices + " /\u00a0" + posValues.get(v).getAttribute("title").replace(" ", "\u00a0");
                }
                Chunk c = new Chunk("", COURIER_10PT_NORMAL);
                c.append(choices.toUpperCase());
                m_goalValue[realI] = new PdfPCell();
                m_goalValue[realI].addElement(c);

              } else {
                if(0 == min && 1 == max) {
                  Paragraph q = new Paragraph("YES / NO", COURIER_10PT_NORMAL);
                  m_goalValue[realI] = new PdfPCell();
                  m_goalValue[realI].addElement(q);

                } else {
                  final String range = "(" + min + " - " + max + ")";
                  PdfPTable t = new PdfPTable(2);
                  t.setHorizontalAlignment(Element.ALIGN_LEFT);
                  t.setTotalWidth(72);
                  t.setLockedWidth(true);
                  Phrase r = new Phrase("", ARIAL_8PT_NORMAL);
                  t.addCell(new PdfPCell(r));
                  Phrase q = new Phrase(range, ARIAL_8PT_NORMAL);
                  t.addCell(new PdfPCell(q));
                  m_goalValue[realI] = new PdfPCell();
                  m_goalValue[realI].setPaddingTop(9);
                  m_goalValue[realI].addElement(t);
                }
              }
            } catch(final ParseException pe) {
              throw new RuntimeException("FATAL: min/max not parsable for goal: " + name);
            }
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

  private String[] m_number;

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
   * @param i
   *          The 0-based index of the scoresheet to which to assign this table
   *          label.
   * @param table
   *          A string with the table label for the specified scoresheet.
   * @throws IllegalArgumentException
   *           Thrown if the index is out of valid range.
   */
  public void setTable(final int i, final String table) throws IllegalArgumentException {
    if(i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if(i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < " + m_numTeams);
    }
    m_table[i] = table;
  }

  /**
   * Sets the team name for scoresheet with index i.
   * 
   * @param i
   *          The 0-based index of the scoresheet to which to assign this team
   *          name.
   * @param name
   *          A string with the team name for the specified scoresheet.
   * @throws IllegalArgumentException
   *           Thrown if the index is out of valid range.
   */
  public void setName(final int i, final String name) throws IllegalArgumentException {
    if(i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if(i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < " + m_numTeams);
    }
    m_name[i] = name;
  }

  /**
   * Sets the team number for scoresheet with index i.
   * 
   * @param i
   *          The 0-based index of the scoresheet to which to assign this team
   *          number.
   * @param number
   *          A string with the team number for the specified scoresheet.
   * @throws IllegalArgumentException
   *           Thrown if the index is out of valid range.
   */
  public void setNumber(final int i, final String number) throws IllegalArgumentException {
    if(i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if(i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < " + m_numTeams);
    }
    m_number[i] = number;
  }

  /**
   * Sets the round number descriptor for scoresheet with index i.
   * 
   * @param i
   *          The 0-based index of the scoresheet to which to assign this round
   *          number.
   * @param round
   *          A string with the round number descriptor for the specified
   *          scoresheet.
   * @throws IllegalArgumentException
   *           Thrown if the index is out of valid range.
   */
  public void setRound(final int i, final String round) throws IllegalArgumentException {
    if(i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if(i >= m_numTeams) {
      throw new IllegalArgumentException("Index must be < " + m_numTeams);
    }
    m_round[i] = round;
  }
}
