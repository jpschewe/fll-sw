package fll.documents.elements;

import java.util.List;

import fll.xml.Goal;
import fll.xml.RubricRange;

/**
 * This represents a row pair in the pdf rubric table. The top row is colored
 * for the subjective category and is
 * labeled with the rows topic on the left side. On the right side of the same
 * colored row is the description
 * of the rows topic. The second row is an ND and then 4 descriptions describing
 * the depth of the teams ability
 * for the rows topic.
 * For example
 * <p>
 * Row 1 &lt;blue row color&gt;Discovery | Balanced emphasis on all three
 * aspects of FLL &lt;end blue row color&gt;<br>
 * Row 2 ND | emphasis on only one aspect | emphasis on two aspects | emphasis o
 * all three | balanced emphasis on all three
 * </p>
 */
public class RowElement {

  private final String rowDescription;

  private final Goal goal;

  /**
   * The rubric ranges sorted with the least first.
   *
   * @return unmodifiable list
   */
  public List<RubricRange> getSortedRubricRanges() {
    return goal.getRubric();
  }

  /**
   * @param goal the goal the row is for
   */
  public RowElement(final Goal goal) {
    this.goal = goal;
    this.rowDescription = null == goal.getDescription() ? "" : goal.getDescription().trim().replaceAll("\\s+", " ");
  }

  @Override
  public String toString() {
    final String newline = System.lineSeparator();
    final StringBuilder output = new StringBuilder();
    output.append("-RowElementStart----------------------------------");
    output.append(newline);
    output.append("Catagory: ");
    output.append(getCategory());
    output.append(newline);
    output.append("Row Title: ");
    output.append(getRowTitle());
    output.append(newline);
    output.append("Row Description: ");
    output.append(rowDescription);
    output.append(newline);
    return output.toString();
  }

  /**
   * @return the goal group, for example "Inspiration" (90 degrees on the left
   *         side of the table)
   */
  public String getCategory() {
    return goal.getCategory();
  }

  /**
   * @return the title on the left side of the colored row
   */
  public String getRowTitle() {
    return goal.getTitle();
  }

  /**
   * @return The goal that defines this RowElement.
   */
  public Goal getGoal() {
    return goal;
  }

  /**
   * @return the description of the goal
   */
  public String getDescription() {
    return rowDescription;
  }

}
