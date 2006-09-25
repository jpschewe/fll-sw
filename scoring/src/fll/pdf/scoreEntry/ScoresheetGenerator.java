/**
 * 
 */
package fll.pdf.scoreEntry;

import java.awt.Color;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Map;

import org.w3c.dom.NodeList;

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

import fll.Utilities;

/**
 * @author Dan Churchill
 *
 */
public class ScoresheetGenerator {
  private ScoresheetGenerator() { }
  private static final String LONG_BLANK = "_________________________";
  private static final String SHORT_BLANK = "___________";
  
  /**
   * Create a new ScoresheetGenerator object populated with form header data
   * provided in the given Map. The map should contain String[] objects, each of
   * length 1, keyed by these String objects (this matches the expected format
   * of the Map returned by javax.servlet.ServletRequest.getParameterMap):
   * <ul>
   * <li><b>"numTeams"</b> - The number of scoresheets that will be created
   * (defaults to 2 if not specified).
   * <li><b>"TeamNameX"</b> - For X = 1, 2, ... numTeams. Team name for
   * scoresheet X.
   * <li><b>"TeamNumberX"</b> - For X = 1, 2, ... numTeams. Team number for
   * scoresheet X.
   * <li><b>"RoundNumberX"</b> - For X = 1, 2, ... numTeams. Round number for
   * scoresheet X.
   * <li><b>"TableX"</b> - For X = 1, 2, ... numTeams. Table assignment for
   * scoresheet X.
   * </ul>
   * If any of the above objects don't exist in the Map, blank lines will be
   * placed in the generated form for that field.
   * 
   * @param formParms
   *          java.util.Map object containing objects as described above.
   * @param document
   *          DOM document containing the challenge info used to generate the
   *          scoresheet.
   */
  public ScoresheetGenerator(final Map formParms, final org.w3c.dom.Document document) {
    try {
      m_numTeams = Integer.parseInt(((String[])formParms.get(new String("numTeams")))[0] );
    } catch (RuntimeException e) {
      m_numTeams = 2;
    }
    initializeArrays();
    m_pageTitle = "";
                
    String parm;
    int j;
    for(int i = 1; i <= m_numTeams; i++) {
        j = i - 1; // convenience index

        parm = "TeamName" + i;
        try {
          m_name[j] = ((String[])formParms.get(parm))[0];
        } catch (RuntimeException e) {
          m_name[j] = LONG_BLANK;
        }
        parm = "TeamNumber" + i;
        try {
          m_number[j] = ((String[])formParms.get(parm))[0];
        } catch (RuntimeException e) {
          m_number[j] = SHORT_BLANK;
        }
        parm = "RoundNumber" + i;
        try {
          m_round[j] = ((String[])formParms.get(parm))[0];
        } catch (RuntimeException e) {
          m_round[j] = SHORT_BLANK;
        }
        parm = "Table" + i;
        try {
          m_table[j] = ((String[])formParms.get(parm))[0];
        } catch (RuntimeException e) {
          m_table[j] = SHORT_BLANK;
        }
      }

    setChallengeInfo(document);
  }

  public ScoresheetGenerator(final int numTeams, final org.w3c.dom.Document document) {
    m_numTeams = numTeams;
    initializeArrays();

    m_pageTitle = "";
    for (int i = 0; i < m_numTeams; i++) {
        m_table[i] = SHORT_BLANK;
        m_name[i] = LONG_BLANK;
        m_round[i] = SHORT_BLANK;
        m_number[i] = SHORT_BLANK;
      }
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

  private static final Font ARIAL_8PT_NORMAL = FontFactory.getFont(
                                                                 FontFactory.HELVETICA, 8, Font.NORMAL, new Color(0, 0, 0));

  private static final Font ARIAL_10PT_NORMAL = FontFactory.getFont(
                                                                  FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font COURIER_10PT_NORMAL = FontFactory.getFont(
                                                                    FontFactory.COURIER, 10, Font.NORMAL);

  public void writeFile(final OutputStream out) throws DocumentException {
    // This creates our new PDF document and declares it to be in landscape
    // orientation
    Document pdfDoc = new Document(PageSize.LETTER.rotate());
    PdfWriter.getInstance(pdfDoc, out);

    // Measurements are always in points (72 per inch) - This sets up 1/2 inch
    // margins
    pdfDoc.setMargins(0.5f * 72, 0.5f * 72, 0.5f * 72, 0.5f * 72);
    pdfDoc.open();

    // Header cell with challenge title to add to both scoresheets
    Paragraph p = new Paragraph();
    Chunk c = new Chunk(m_pageTitle, FontFactory.getFont(
                                                         FontFactory.HELVETICA_BOLD, 14, Font.NORMAL, new Color(255, 255, 255)));
    p.setAlignment(Element.ALIGN_CENTER);
    p.add(c);
    PdfPCell head = new PdfPCell();
    head.setColspan(2);
    head.setBorder(1);
    head.setPaddingTop(0);
    head.setPaddingBottom(3);
    head.setBackgroundColor(new Color(64, 64, 64));
    head.setVerticalAlignment(Element.ALIGN_TOP);
    head.addElement(p);

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

    PdfPTable[] team = new PdfPTable[m_numTeams];
    PdfPCell[] cell = new PdfPCell[m_numTeams];

    PdfPTable wholePage = new PdfPTable(2);
    wholePage.setWidthPercentage(100);
    for (int i = 0; i < m_numTeams; i++) {
      if (i > 1 && (i % 2) == 0) {
        pdfDoc.newPage();
        wholePage = new PdfPTable(2);
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
      blankRow.setMinimumHeight(0);
      team[i].addCell(blankRow);

      for (int j = 0; j < m_goalLabel.length; j++) {
        team[i].addCell(m_goalLabel[j]);
        team[i].addCell(m_goalValue[j]);
      }

      team[i].addCell(jiC);
      team[i].addCell(desC);
      team[i].addCell(tciC);
      team[i].addCell(sciC);

      cell[i] = new PdfPCell(team[i]);
      cell[i].setBorder(0);
      cell[i].setPadding(0);
      if (i % 2 == 0) {
        cell[i].setPaddingRight(36);
      } else {
        cell[i].setPaddingLeft(36);
      }
      wholePage.addCell(cell[i]);
      if (i % 2 == 1) {
        pdfDoc.add(wholePage);
      }
    }
    // When an odd number of teams, add a blank cell to complete the
    // table of the last page
    if (m_numTeams % 2 == 1) {
      PdfPCell blank = new PdfPCell();
      blank.setBorder(0);
      wholePage.addCell(blank);
      pdfDoc.add(wholePage);
    }

    pdfDoc.close();
  }

  /**
   * Stores the goal cells that are inserted into the output after the team
   * name headers and before the scoring/initials blanks at the bottom of the
   * scoresheet.
   * 
   * @param document
   *            The document object containing the challenge descriptor info.
   */
  public void setChallengeInfo(final org.w3c.dom.Document document) {
    final org.w3c.dom.Element rootElement = document.getDocumentElement();
    m_pageTitle = rootElement.getAttribute("title");
    final org.w3c.dom.Element performanceElement = (org.w3c.dom.Element) rootElement
      .getElementsByTagName("Performance").item(0);
    final NodeList goals = performanceElement.getElementsByTagName("goal");

    m_goalLabel = new PdfPCell[goals.getLength()];
    m_goalValue = new PdfPCell[goals.getLength()];

    for (int i = 0; i < goals.getLength(); i++) {
        final org.w3c.dom.Element element = (org.w3c.dom.Element) goals .item(i);
        final String name = element.getAttribute("name");

        // This is the text for the left hand "label" cell
        final String title = element.getAttribute("title");
        Paragraph p = new Paragraph(title, ARIAL_10PT_NORMAL);
        p.setAlignment(Element.ALIGN_RIGHT);
        m_goalLabel[i] = new PdfPCell();
        m_goalLabel[i].setBorder(0);
        m_goalLabel[i].setPaddingRight(9);
        m_goalLabel[i].addElement(p);
        m_goalLabel[i].setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(
                                                                   element.getAttribute("min")).intValue();
            final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(
                                                                   element.getAttribute("max")).intValue();

            // If element has child nodes, then we have an enumerated list
            // of choices. Otherwise it is either yes/no or a numeric field.
            if (element.hasChildNodes()) {
                final NodeList posValues = element
                  .getElementsByTagName("value");
                String choices = ((org.w3c.dom.Element) posValues.item(0))
                  .getAttribute("title").replace(" ", "\u00a0"); // replace spaces with "no-break" spaces
                for (int v = 1; v < posValues.getLength(); v++) {
                    choices = choices
                      + " /\u00a0"
                      + ((org.w3c.dom.Element) posValues.item(v))
                      .getAttribute("title").replace(" ", "\u00a0");
                  }
                Chunk c = new Chunk("", COURIER_10PT_NORMAL);
                c.append(choices.toUpperCase());
                m_goalValue[i] = new PdfPCell();
                m_goalValue[i].addElement(c);
                                    
              } else {
                if (0 == min && 1 == max) {
                    Paragraph q = new Paragraph("YES / NO", COURIER_10PT_NORMAL);
                    m_goalValue[i] = new PdfPCell();
                    m_goalValue[i].addElement(q);
                                                
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
                    m_goalValue[i] = new PdfPCell();
                    m_goalValue[i].setPaddingTop(9);
                    m_goalValue[i].addElement(t);
                  }
              }
            m_goalValue[i].setBorder(0);
            m_goalValue[i].setVerticalAlignment(Element.ALIGN_MIDDLE);
          } catch (final ParseException pe) {
            throw new RuntimeException(
                                       "FATAL: min/max not parsable for goal: " + name);
          }
      }
  }

  private int m_numTeams;

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

  /**
   * Sets the table label for scoresheet with index i.
   * 
   * @param i
   *            The 0-based index of the scoresheet to which to assign this
   *            table label.
   * @param table
   *            A string with the table label for the specified scoresheet.
   * @throws IllegalArgumentException
   *             Thrown if the index is out of valid range.
   */
  public void setTable(final int i, final String table)
    throws IllegalArgumentException {
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
   *            The 0-based index of the scoresheet to which to assign this
   *            team name.
   * @param name
   *            A string with the team name for the specified scoresheet.
   * @throws IllegalArgumentException
   *             Thrown if the index is out of valid range.
   */
  public void setName(final int i, final String name)
    throws IllegalArgumentException {
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
   *            The 0-based index of the scoresheet to which to assign this
   *            team number.
   * @param name
   *            A string with the team number for the specified scoresheet.
   * @throws IllegalArgumentException
   *             Thrown if the index is out of valid range.
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
   *            The 0-based index of the scoresheet to which to assign this
   *            round number.
   * @param name
   *            A string with the round number descriptor for the specified
   *            scoresheet.
   * @throws IllegalArgumentException
   *             Thrown if the index is out of valid range.
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
