package fll.documents.elements;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fll.xml.AbstractGoal;
import fll.xml.Goal;
import fll.xml.ScoreCategory;

/**
 * This object represents the data element between the fll.xml and the
 * subjective PDF file.
 * The structure of the SheetElement is
 * <blockquote>
 * Hashtable&lt;String, TableElement&gt;
 * The <code>String</code> is the category of the table on the sheet. The
 * category is what is printed
 * vertically on the left side of the table and is a logical grouping of topics.
 * <br>
 * The <code>TableElement</code> is the data structure that holds the 2 to 4
 * rows of information
 * </blockquote>
 */
public class SheetElement {

  private final String sheetName;

  /**
   * Category -> TableElement
   */
  private final Hashtable<String, TableElement> tables = new Hashtable<String, TableElement>();

  /**
   * The categories for this ScoreCategory in order found.
   */
  private final List<String> categories = new LinkedList<>();

  private final ScoreCategory sheetData;

  public ScoreCategory getSheetData() {
    return this.sheetData;
  }

  public SheetElement(final ScoreCategory sheetData) {
    // Sheet name will be Programming, Project, Robot Design or Core Values
    this.sheetName = sheetData.getName();
    this.sheetData = sheetData;
  }

  /**
   * An abstractGoal is a single row in a table.<br>
   * It has the title, the description and the 4 levels of textual description
   * of
   * what it takes to meet that level of success<br>
   * <code>beginning, developing, accomplished, exemplary </code><br>
   * There is no guarantee the abstractGoals are in order for the tables
   */
  public void processSheet() {
    final List<AbstractGoal> goalsList = sheetData.getGoals();

    // Go thru the sheet (ScoreCategory) and put all the rows (abstractGoal)
    // into the right tables (Category)
    // The assumption is made that the order in the xml is the proper order of
    // the abstractGoals
    for (final AbstractGoal abstractGoal : goalsList) {
      if (abstractGoal instanceof Goal) {
        final Goal goal = (Goal) abstractGoal;
        final String tableCategory = goal.getCategory();
        if (!this.categories.contains(tableCategory)) {
          categories.add(tableCategory);
        }

        TableElement tableElement = tables.get(tableCategory);
        if (null == tableElement) {
          tableElement = new TableElement(((Goal) abstractGoal).getCategory());
          tables.put(tableCategory, tableElement);
        }
        tableElement.addRowElement(new RowElement(goal));
      }
    }
  }

  public String getSheetName() {
    return this.sheetName;
  }

  public TableElement getTableElement(String table) {
    return tables.get(table);
  }

  /**
   * The categories in this ScoreCategory in the order they were found in the
   * challenge description.
   * 
   * @return unmodifiable list
   */
  public List<String> getCategories() {
    return Collections.unmodifiableList(this.categories);
  }

  public String toString() {
    final StringBuilder str = new StringBuilder();
    Set<String> keys = tables.keySet();
    for (String key : keys) {
      str.append("Table["
          + key + "]: " + tables.get(key));
      str.append(System.lineSeparator());
    }
    str.append("Subjective Category: "
        + this.sheetName);
    return str.toString();
  }
}
