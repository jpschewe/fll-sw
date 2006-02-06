/**
 * 
 */
package fll.pdf.scoreEntry;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

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
public class ScoresheetGenerator
{
	private ScoresheetGenerator() { }
  private static final String longBlank = "_________________________";
  private static final String shortBlank = "___________";
  
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
	public ScoresheetGenerator(Map formParms, final org.w3c.dom.Document document)
	{
    try {
      m_numTeams = Integer.parseInt( ((String[])formParms.get(new String("numTeams")))[0] );
    } catch (RuntimeException e) {
      m_numTeams = 2;
    }
		initializeArrays();
		m_pageTitle = "";
		
		String parm;
		int j;
		for(int i = 1; i <= m_numTeams; i++)
		{
			j = i - 1; // convenience index

			parm = "TeamName" + i;
			try {
        m_name[j] = ((String[])formParms.get(parm))[0];
      } catch (RuntimeException e) {
        m_name[j] = longBlank;
      }
			parm = "TeamNumber" + i;
			try {
        m_number[j] = ((String[])formParms.get(parm))[0];
      } catch (RuntimeException e) {
        m_number[j] = shortBlank;
      }
			parm = "RoundNumber" + i;
			try {
        m_round[j] = ((String[])formParms.get(parm))[0];
      } catch (RuntimeException e) {
        m_round[j] = shortBlank;
      }
			parm = "Table" + i;
			try {
        m_table[j] = ((String[])formParms.get(parm))[0];
      } catch (RuntimeException e) {
        m_table[j] = shortBlank;
      }
		}
    
    setChallengeInfo(document);
	}

	public ScoresheetGenerator(int numTeams)
	{
		m_numTeams = numTeams;
		initializeArrays();

		m_pageTitle = "";
		for (int i = 0; i < m_numTeams; i++)
		{
			m_table[i] = shortBlank;
			m_name[i] = longBlank;
			m_round[i] = shortBlank;
			m_number[i] = shortBlank;
		}
	}
	
	/**
	 * Private support function to create new data arrays for the scoresheet
	 * information. IMPORTANT!!! The value of m_numTeams must be set before the
	 * call to this method is made.
	 * 
	 */
	private void initializeArrays()
	{
		m_table = new String[m_numTeams];
		m_name = new String[m_numTeams];
		m_round = new String[m_numTeams];
		m_number = new String[m_numTeams];
		m_goalLabel = new PdfPCell[0];
		m_goalValue = new PdfPCell[0];
	}

	private static final Font arial8ptNormal = FontFactory.getFont(
			FontFactory.HELVETICA, 8, Font.NORMAL, new Color(0, 0, 0));

	private static final Font arial10ptNormal = FontFactory.getFont(
			FontFactory.HELVETICA, 10, Font.NORMAL);

	private static final Font courier10ptNormal = FontFactory.getFont(
			FontFactory.COURIER, 10, Font.NORMAL);

	public void writeFile(OutputStream out) throws DocumentException {
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
    Phrase ji = new Phrase("Judge's Initials _______", arial8ptNormal);
    PdfPCell ji_c = new PdfPCell(ji);
    ji_c.setBorder(0);
    ji_c.setPaddingTop(18);
    ji_c.setPaddingRight(36);
    ji_c.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase des = new Phrase("Data Entry Score _______", arial8ptNormal);
    PdfPCell des_c = new PdfPCell(des);
    des_c.setBorder(0);
    des_c.setPaddingTop(18);
    des_c.setPaddingRight(36);
    des_c.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase tci = new Phrase("Team Check Inititals _______", arial8ptNormal);
    PdfPCell tci_c = new PdfPCell(tci);
    tci_c.setBorder(0);
    tci_c.setPaddingTop(9);
    tci_c.setPaddingRight(36);
    tci_c.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Phrase sci = new Phrase("2nd Check Initials _______", arial8ptNormal);
    PdfPCell sci_c = new PdfPCell(sci);
    sci_c.setBorder(0);
    sci_c.setPaddingTop(9);
    sci_c.setPaddingRight(36);
    sci_c.setHorizontalAlignment(Element.ALIGN_RIGHT);

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
      Paragraph tbl_p = new Paragraph("Table:", arial10ptNormal);
      tbl_p.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell tbl_lc = new PdfPCell(team[i].getDefaultCell());
      tbl_lc.setPaddingRight(9);
      tbl_lc.addElement(tbl_p);
      team[i].addCell(tbl_lc);
      // Table value cell
      Paragraph tbl_v = new Paragraph(m_table[i], courier10ptNormal);
      PdfPCell tbl_vc = new PdfPCell(team[i].getDefaultCell());
      tbl_vc.addElement(tbl_v);
      team[i].addCell(tbl_vc);

      // Round number label cell
      Paragraph rnd_p = new Paragraph("Round Number:", arial10ptNormal);
      rnd_p.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell rnd_lc = new PdfPCell(team[i].getDefaultCell());
      rnd_lc.setPaddingRight(9);
      rnd_lc.addElement(rnd_p);
      team[i].addCell(rnd_lc);
      // Round number value cell
      Paragraph rnd_v = new Paragraph(m_round[i], courier10ptNormal);
      PdfPCell rnd_vc = new PdfPCell(team[i].getDefaultCell());
      rnd_vc.addElement(rnd_v);
      team[i].addCell(rnd_vc);

      // Team number label cell
      Paragraph nbr_p = new Paragraph("Team Number:", arial10ptNormal);
      nbr_p.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell nbr_lc = new PdfPCell(team[i].getDefaultCell());
      nbr_lc.setPaddingRight(9);
      nbr_lc.addElement(nbr_p);
      team[i].addCell(nbr_lc);
      // Team number value cell
      Paragraph nbr_v = new Paragraph(m_number[i], courier10ptNormal);
      PdfPCell nbr_vc = new PdfPCell(team[i].getDefaultCell());
      nbr_vc.addElement(nbr_v);
      team[i].addCell(nbr_vc);

      // Team name label cell
      Paragraph name_p = new Paragraph("Team Name:", arial10ptNormal);
      name_p.setAlignment(Element.ALIGN_RIGHT);
      PdfPCell name_lc = new PdfPCell(team[i].getDefaultCell());
      name_lc.setPaddingRight(9);
      name_lc.addElement(name_p);
      team[i].addCell(name_lc);
      // Team name value cell
      Paragraph name_v = new Paragraph(m_name[i], courier10ptNormal);
      PdfPCell name_vc = new PdfPCell(team[i].getDefaultCell());
      name_vc.addElement(name_v);
      team[i].addCell(name_vc);

      PdfPCell blankRow = new PdfPCell(new Phrase(""));
      blankRow.setColspan(2);
      blankRow.setBorder(0);
      blankRow.setMinimumHeight(18);
      team[i].addCell(blankRow);

      for (int j = 0; j < m_goalLabel.length; j++) {
        team[i].addCell(m_goalLabel[j]);
        team[i].addCell(m_goalValue[j]);
      }

      team[i].addCell(ji_c);
      team[i].addCell(des_c);
      team[i].addCell(tci_c);
      team[i].addCell(sci_c);

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
	public void setChallengeInfo(final org.w3c.dom.Document document)
	{
		final org.w3c.dom.Element rootElement = document.getDocumentElement();
		m_pageTitle = rootElement.getAttribute("title");
		final org.w3c.dom.Element performanceElement = (org.w3c.dom.Element) rootElement
				.getElementsByTagName("Performance").item(0);
		final NodeList goals = performanceElement.getElementsByTagName("goal");

		m_goalLabel = new PdfPCell[goals.getLength()];
		m_goalValue = new PdfPCell[goals.getLength()];

		for (int i = 0; i < goals.getLength(); i++)
		{
			final org.w3c.dom.Element element = (org.w3c.dom.Element) goals	.item(i);
			final String name = element.getAttribute("name");

			// This is the text for the left hand "label" cell
			final String title = element.getAttribute("title");
			Paragraph p = new Paragraph(title, arial10ptNormal);
			p.setAlignment(Element.ALIGN_RIGHT);
			m_goalLabel[i] = new PdfPCell();
			m_goalLabel[i].setBorder(0);
			m_goalLabel[i].setPaddingRight(9);
			m_goalLabel[i].addElement(p);
			m_goalLabel[i].setVerticalAlignment(Element.ALIGN_MIDDLE);
			try
			{
				final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(
						element.getAttribute("min")).intValue();
				final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(
						element.getAttribute("max")).intValue();

				// If element has child nodes, then we have an enumerated list
				// of choices. Otherwise it is either yes/no or a numeric field.
				if (element.hasChildNodes())
				{
					final NodeList posValues = element
							.getElementsByTagName("value");
					String choices = ((org.w3c.dom.Element) posValues.item(0))
							.getAttribute("title").replace(" ","\u00a0"); // replace spaces with "no-break" spaces
					for (int v = 1; v < posValues.getLength(); v++)
					{
						choices = choices
								+ " /\u00a0"
								+ ((org.w3c.dom.Element) posValues.item(v))
										.getAttribute("title").replace(" ","\u00a0");
					}
					Chunk c = new Chunk("", courier10ptNormal);
					c.append(choices.toUpperCase());
					m_goalValue[i] = new PdfPCell();
					m_goalValue[i].addElement(c);
				    
				}
				else
				{
					if (0 == min && 1 == max)
					{
						Paragraph q = new Paragraph("YES / NO", courier10ptNormal);
						m_goalValue[i] = new PdfPCell();
						m_goalValue[i].addElement(q);
						
					}
					else
					{
						final String range = "(" + min + " - " + max + ")";
						PdfPTable t = new PdfPTable(2);
						t.setHorizontalAlignment(Element.ALIGN_LEFT);
						t.setTotalWidth(72);
						t.setLockedWidth(true);
						Phrase r = new Phrase("", arial8ptNormal);
						t.addCell(new PdfPCell(r));
						Phrase q = new Phrase(range, arial8ptNormal);
						t.addCell(new PdfPCell(q));
						m_goalValue[i] = new PdfPCell();
						m_goalValue[i].setPaddingTop(9);
						m_goalValue[i].addElement(t);
					}
				}
				m_goalValue[i].setBorder(0);
				m_goalValue[i].setVerticalAlignment(Element.ALIGN_MIDDLE);
			}
			catch (final ParseException pe)
			{
				throw new RuntimeException(
						"FATAL: min/max not parsable for goal: " + name);
			}
		}
	}

	int m_numTeams;

	String m_pageTitle;

	String[] m_table;

	String[] m_name;

	String[] m_round;

	String[] m_number;

	PdfPCell[] m_goalLabel;

	PdfPCell[] m_goalValue;

	public void setPageTitle(String title) {
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
	public void setTable(int i, String table)
		throws IllegalArgumentException
	{
		if(i < 0)
			throw new IllegalArgumentException("Index must not be < 0");
		if(i >= m_numTeams)
			throw new IllegalArgumentException("Index must be < " + m_numTeams);
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
	public void setName(int i, String name)
		throws IllegalArgumentException
	{
		if(i < 0)
			throw new IllegalArgumentException("Index must not be < 0");
		if(i >= m_numTeams)
			throw new IllegalArgumentException("Index must be < " + m_numTeams);
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
	public void setNumber(int i, String number) throws IllegalArgumentException
	{
		if(i < 0)
			throw new IllegalArgumentException("Index must not be < 0");
		if(i >= m_numTeams)
			throw new IllegalArgumentException("Index must be < " + m_numTeams);
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
	public void setRound(int i, String round) throws IllegalArgumentException
	{
		if(i < 0)
			throw new IllegalArgumentException("Index must not be < 0");
		if(i >= m_numTeams)
			throw new IllegalArgumentException("Index must be < " + m_numTeams);
		m_round[i] = round;
	}
}
