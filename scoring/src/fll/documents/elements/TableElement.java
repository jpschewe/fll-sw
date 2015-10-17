package fll.documents.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a single table of the many tables on a subjective score
 * sheet
 * The single table consists of n-rows, each one being a RowElement
 * 
 * @see fll.documents.elements.RowElement
 */
public class TableElement {

  // Example, MN Programming, or Strategy and Innovations
  private final String tableCategory;

  private final List<RowElement> rows = new ArrayList<RowElement>();

  public TableElement(final String tableCategory) {
    this.tableCategory = tableCategory;
  }

  /**
   * Used to get the subjective category (Core Values, Programming, Project,
   * Robot Design) this table belongs to.
   * 
   * @return String The set subjective category
   */
  public String getSubjectiveCatetory() {
    return this.tableCategory;
  }

  public void addRowElement(RowElement element) {
    this.rows.add(element);
  }

  public List<RowElement> getRowElements() {
    return this.rows;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    for (RowElement rowElement : rows) {
      result.append(rowElement);
      result.append(System.lineSeparator());
    }

    result.append("Table Catagory: ");
    result.append(this.tableCategory);
    result.append("\n");
    return result.toString();
  }
}
