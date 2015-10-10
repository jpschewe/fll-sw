package fll.documents.writers;

import java.io.IOException;
import java.net.MalformedURLException;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfPTable;

import fll.documents.elements.SheetElement;
import fll.scheduler.TeamScheduleInfo;
import fll.util.FLLInternalException;

public class SubjectivePdfManager {
  public static final SubjectivePdfWriter writer = SubjectivePdfWriter.getInstance();

  private SheetElement coreValuesSheet = null;

  private SheetElement projectSheet = null;

  private SheetElement robotDesignSheet = null;

  private SheetElement programmingSheet = null;

  public void writeTeamSubjectivePdf(Document doc,
                                     TeamScheduleInfo teamInfo,
                                     String subjectiveStation)
                                         throws BadElementException, MalformedURLException, IOException {
    //FIXME make this generic
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
    default:
      throw new FLLInternalException("Unknown expected subjective station name: "
          + subjectiveStation);
    }
  }

  private void writeDesignSheet(Document doc,
                                TeamScheduleInfo teamInfo)
                                    throws BadElementException, MalformedURLException, IOException {
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

  private void writeProgrammingSheet(Document doc,
                                     TeamScheduleInfo teamInfo)
                                         throws BadElementException, MalformedURLException, IOException {
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

  private void writeProjectSheet(Document doc,
                                 TeamScheduleInfo teamInfo)
                                     throws BadElementException, MalformedURLException, IOException {
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

  private void writeCoreValuesSheet(Document doc,
                                    TeamScheduleInfo teamInfo)
                                        throws BadElementException, MalformedURLException, IOException {
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
    //FIXME make this generic
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
    default:
      throw new FLLInternalException("Unknown expected subjective station name: "
          + sheet.getSheetName());
    }
  }
}
