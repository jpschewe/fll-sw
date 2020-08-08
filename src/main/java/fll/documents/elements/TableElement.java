package fll.documents.elements;

import java.util.ArrayList;
import java.util.List;

import fll.xml.Goal;

/**
 * This class represents a single table of the many tables on a subjective score
 * sheet.
 * The single table consists of n-rows, each one being a {@link Goal}.
 */
public class TableElement {

  // Example, MN Programming, or Strategy and Innovations
  private final String tableCategory;

  private final List<Goal> goals = new ArrayList<>();

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

  public void addGoal(final Goal element) {
    this.goals.add(element);
  }

  public List<Goal> getRowElements() {
    return this.goals;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    for (final Goal rowElement : goals) {
      result.append(rowElement);
      result.append(System.lineSeparator());
    }

    result.append("Table Catagory: ");
    result.append(this.tableCategory);
    result.append("\n");
    return result.toString();
  }
}
