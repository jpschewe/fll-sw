package fll.documents.writers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fll.documents.elements.SheetElement;
import fll.scheduler.SubjectiveTime;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;

public class SubjectivePdfManager {
	public static SubjectivePdfWriter writer = SubjectivePdfWriter.getInstance();
		
	SheetElement coreValuesSheet = null;
	SheetElement projectSheet = null;
	SheetElement robotDesignSheet = null;
	SheetElement programmingSheet = null;

	public static void main(String[] args) throws FileNotFoundException {
		SubjectivePdfManager pdfManager = new SubjectivePdfManager();		
		String[] subjects = {SubjectiveConstants.PROJECT_NAME, SubjectiveConstants.CORE_VALUES_NAME, SubjectiveConstants.ROBOT_DESIGN_NAME, SubjectiveConstants.PROGRAMMING_NAME};
		
		//get the xml sucked in
		final Reader descriptorReader = new FileReader("C:\\eclipse_4.4.2\\gitfll-sw\\scoring\\src\\fll\\resources\\challenge-descriptors\\fll-2014.xml");
	  final org.w3c.dom.Document document = ChallengeParser.parse(descriptorReader);
    final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
		
		//setup the sheets from the sucked in xml
		for (String subject : subjects) {
		  final SheetElement sheetElement = TournamentSchedule.createSubjectiveSheetElement(subject, description);
			pdfManager.setSheetElement(sheetElement);
			
			//This document will be all of the subjective pdf sheets in a single file.
			Document pdf = SubjectivePdfManager.writer.createStandardDocument();
	
			try {
				PdfWriter.getInstance(pdf, new FileOutputStream("C:\\FLL\\writeZone\\" + subject + ".pdf"));
			} catch (FileNotFoundException | DocumentException e) {
				System.err.println("Unable to create document.");
				e.printStackTrace(System.err);
			}
	
			pdf.open();
			
			///////////////////////////////////
			//Mock up a TeamScheduleInfo object since that will be passed to the writeTeamSubjectiveFeedbackPdf method
			
			String title = null;
			
			switch (subject) {
				case SubjectiveConstants.PROJECT_NAME:
					title = SubjectiveConstants.PROJECT_TITLE;
					break;
				case SubjectiveConstants.CORE_VALUES_NAME:
					title = SubjectiveConstants.CORE_VALUES_TITLE;
					break;
				case SubjectiveConstants.ROBOT_DESIGN_NAME:
					title = SubjectiveConstants.ROBOT_DESIGN_TITLE;
					break;
				case SubjectiveConstants.PROGRAMMING_NAME:
					title = SubjectiveConstants.PROGRAMMING_TITLE;
					break;
			}
	
			TeamScheduleInfo teamInfo = new TeamScheduleInfo(3,  66666);
			teamInfo.setDivision("Woods");
			teamInfo.setJudgingStation(title);
			teamInfo.setOrganization("Willow Creek Middle School");
			teamInfo.setTeamName("Masters of Disasters 2.0");
			SubjectiveTime s = new SubjectiveTime(title, new Date());
			teamInfo.addSubjectiveTime(s);
			
			//to here
			/////////////////////////////////////////////////////////
			
			//call the method simulating a number of TeamScheduleInfo sheets
			for (int a = 0; a < 4; a++) {
				pdfManager.writeTeamSubjectivePdf(pdf, teamInfo, teamInfo.getJudgingStation());
			}
			
			//This closes the single pdf document with all of the subjective sheets in it.
			pdf.close();
		}
	}
	
	public void writeTeamSubjectivePdf(Document doc, TeamScheduleInfo teamInfo, String subjectiveStation) {
		switch (subjectiveStation) {
			case SubjectiveConstants.CORE_VALUES_NAME:
				writeCoreValuesSheet(doc, teamInfo);
				break;
			case SubjectiveConstants.PROJECT_NAME:
				writeProjectSheet(doc, teamInfo);
				break;
			case SubjectiveConstants.ROBOT_DESIGN_NAME:
				writeDesignSheet(doc, teamInfo);
				break;
			case SubjectiveConstants.PROGRAMMING_NAME:
				writeProgrammingSheet(doc, teamInfo);
				break;
		}
	}

	private void writeDesignSheet(Document doc, TeamScheduleInfo teamInfo) {
		PdfPTable table = writer.createStandardRubricTable();
		writer.setSheetProperties(SubjectiveConstants.ROBOT_DESIGN_TITLE);
		writer.writeHeader(doc, teamInfo);
		writer.writeRubricTable(table, robotDesignSheet.getTableElement(SubjectiveConstants.MD));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, robotDesignSheet.getTableElement(SubjectiveConstants.MND));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, robotDesignSheet.getTableElement(SubjectiveConstants.SI));
		writer.writeCommentsSection(table);

		try {
			doc.add(table);
		} catch (DocumentException e) {
			System.err.println("unable to write to the document for the bottom section.");
			e.printStackTrace(System.err);
		}

		writer.writeEndOfPageRow(doc);
	}

	private void writeProgrammingSheet(Document doc, TeamScheduleInfo teamInfo) {
		PdfPTable table = writer.createStandardRubricTable();
		writer.setSheetProperties(SubjectiveConstants.PROGRAMMING_TITLE);
		writer.writeHeader(doc, teamInfo);
		writer.writeRubricTable(table, programmingSheet.getTableElement(SubjectiveConstants.MNP));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, programmingSheet.getTableElement(SubjectiveConstants.PROG));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, programmingSheet.getTableElement(SubjectiveConstants.SI));
		writer.writeCommentsSection(table);

		try {
			doc.add(table);
		} catch (DocumentException e) {
			System.err.println("unable to write to the document for the bottom section.");
			e.printStackTrace(System.err);
		}

		writer.writeEndOfPageRow(doc);
	}

	private void writeProjectSheet(Document doc, TeamScheduleInfo teamInfo) {
		PdfPTable table = writer.createStandardRubricTable();
		writer.setSheetProperties(SubjectiveConstants.PROJECT_TITLE);
		writer.writeHeader(doc, teamInfo);
		writer.writeRubricTable(table, projectSheet.getTableElement(SubjectiveConstants.RS));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, projectSheet.getTableElement(SubjectiveConstants.IS));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, projectSheet.getTableElement(SubjectiveConstants.PS));
		writer.writeCommentsSection(table);

		try {
			doc.add(table);
		} catch (DocumentException e) {
			System.err.println("unable to write to the document for the bottom section.");
			e.printStackTrace(System.err);
		}

		writer.writeEndOfPageRow(doc);
	}

	private void writeCoreValuesSheet(Document doc, TeamScheduleInfo teamInfo) {
		PdfPTable table = writer.createStandardRubricTable();
		writer.setSheetProperties(SubjectiveConstants.CORE_VALUES_TITLE);
		writer.writeHeader(doc, teamInfo);
		writer.writeRubricTable(table, coreValuesSheet.getTableElement(SubjectiveConstants.ISP));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, coreValuesSheet.getTableElement(SubjectiveConstants.TW));
		writer.writeCommentsSection(table);
		writer.writeRubricTable(table, coreValuesSheet.getTableElement(SubjectiveConstants.GP));
		writer.writeCommentsSection(table);

		try {
			doc.add(table);
		} catch (DocumentException e) {
			System.err.println("unable to write to the document for the bottom section.");
			e.printStackTrace(System.err);
		}

		writer.writeEndOfPageRow(doc);
	}
	
	public void setSheetElement(SheetElement sheet) {
		switch (sheet.getSheetName()) {
			case SubjectiveConstants.CORE_VALUES_NAME:
				this.coreValuesSheet = sheet;
				break;
			case SubjectiveConstants.PROGRAMMING_NAME:
				this.programmingSheet = sheet;
				break;
			case SubjectiveConstants.PROJECT_NAME:
				this.projectSheet = sheet;
				break;
			case SubjectiveConstants.ROBOT_DESIGN_NAME:
				this.robotDesignSheet = sheet;
				break;
		}
	}
}
