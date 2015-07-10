package fll.documents.elements;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fll.xml.AbstractGoal;
import fll.xml.Goal;
import fll.xml.ScoreCategory;

/**
 * This object represents the data element between the fll.xml and the subjective PDF file.
 * The structure of the SheetElement is
 * <blockquote>
 *  Hashtable&lt;String, TableElement&gt;
 *    The <code>String</code> is the category of the table on the sheet.  The category is what is printed
 *    vertically on the left side of the table and is a logical grouping of topics.<br>
 *    The <code>TableElement</code> is the data structure that holds the 2 to 4 rows of information
 * </blockquote>
 * 
 *
 */
public class SheetElement {

	String sheetName = null;
	Hashtable<String, TableElement> tables = new Hashtable<String, TableElement>();
	ScoreCategory sheetData = null;
	
	public SheetElement(ScoreCategory sheetData) {
		//Sheet name will be Programming, Project, Robot Design or Core Values
		this.sheetName = sheetData.getName();
		this.sheetData = sheetData;
	}
	
	/**
	 * An abstractGoal is a single row in a table.<br>
	 * It has the title, the description and the 4 levels of textual description of 
	 * what it takes to meet that level of success<br>
	 * <code>beginning, developing, accomplished, exemplary </code><br>
	 * There is no guarantee the abstractGoals are in order for the tables
	 */
	public void processSheet() {
		List<AbstractGoal> goalsList =  sheetData.getGoals();
		TableElement tableElement = null;
		String tableCategory = null;
		
		//Go thru the sheet (ScoreCategory) and put all the rows (abstractGoal) into the right tables (Category)
		//The assumption is made that the order in the xml is the proper order of the abstractGoals
		for (AbstractGoal abstractGoal : goalsList) {
			tableCategory = ((Goal)abstractGoal).getCategory();
			tableElement = tables.get(tableCategory);
			if (null == tableElement) {
				tableElement = new TableElement(((Goal)abstractGoal).getCategory());
				tables.put(tableCategory, tableElement);
			} 
			tableElement.addRowElement(new RowElement(abstractGoal));				
		}
	}
	
	public String getSheetName() {
		return this.sheetName;
	}
	
	public TableElement getTableElement(String table) {
		return tables.get(table);
	}
	
	public String toString() {
		Set<String> keys = tables.keySet();
        for(String key: keys){
            System.out.println("Table[" + key + "]: " + tables.get(key));
        }
		return "Subjective Category: " + this.sheetName;
	}
}
