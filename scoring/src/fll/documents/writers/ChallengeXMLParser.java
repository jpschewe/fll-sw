package fll.documents.writers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;

import fll.Utilities;
import fll.documents.elements.RowElement;
import fll.documents.elements.SheetElement;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.Goal;
import fll.xml.RubricRange;
import fll.xml.ScoreCategory;

public class ChallengeXMLParser {
	ChallengeDescription challengeDescription = null;

	public static void main(String[] args) {
		ChallengeXMLParser cxp = new ChallengeXMLParser();
		cxp.parseXMLDocument("C:\\eclipse_4.4.2\\gitfll-sw\\scoring\\src\\fll\\resources\\challenge-descriptors\\fll-2014.xml");
		//System.out.println("====================================== PROGRAMMING_NAME =================================");
		//cxp.createSubjectiveSheetElement(SubjectiveConstants.PROGRAMMING_NAME);
		//System.out.println("====================================== PROJECT_NAME =================================");
		//cxp.createSubjectiveSheetElement(SubjectiveConstants.PROJECT_NAME);
		System.out.println("====================================== CORE_VALUES_NAME =================================");
		cxp.createSubjectiveSheetElement(SubjectiveConstants.CORE_VALUES_NAME);
		//System.out.println("====================================== ROBOT_DESIGN_NAME =================================");
		//cxp.createSubjectiveSheetElement(SubjectiveConstants.ROBOT_DESIGN_NAME);
	}
	
	public void parseXMLDocument(String xmlDoc) {
		if (null == xmlDoc) {
			System.exit(1);
		}
		
		final File challengeFile = new File(xmlDoc);
		Document challengeDocument = null;
		
		if (!challengeFile.exists()) {
			System.exit(1);
		}
		
		if (!challengeFile.canRead()) {
			System.exit(1);
		}
		
		if (!challengeFile.isFile()) {
			System.exit(1);
		}
		
		try {
			final Reader input = new InputStreamReader(new FileInputStream(challengeFile), Utilities.DEFAULT_CHARSET);
			challengeDocument= ChallengeParser.parse(input);
			if (null == challengeDocument) {
				System.exit(1);
			}

			challengeDescription = new ChallengeDescription(challengeDocument.getDocumentElement());
		} catch (final Exception e) {
			System.exit(1);
		}
	}
	
	public SheetElement createSubjectiveSheetElement(String subjectiveName) {
		//Get the info from the .xml sheet for the specific subjective category
		//An sc == a subjective category
		ScoreCategory sc = challengeDescription.getSubjectiveCategoryByName(subjectiveName);
		SheetElement sheet = new SheetElement(sc);
		System.out.println("Sheet Processing: " + sheet.getSheetName());
		sheet.processSheet();
		return sheet;
	}
}
