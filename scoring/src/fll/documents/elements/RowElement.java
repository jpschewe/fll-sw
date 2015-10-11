package fll.documents.elements;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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
  /** Inspiration for example (90 degrees on the left side of the table) */
  private final String catagory;

  /** Discovery for example (left side of the colored row) */
  private final String rowTitle;

  /**
   * Balanced emphasis on all three aspects (Robot, Project, Core Values) of
   * FLL; its not just about winning awards (right side of the colored row)
   */
  private final String rowDescription;

  private final List<RubricRange> sortedRubricRanges;

  /**
   * The rubric ranges sorted with the least first.
   * 
   * @return unmodifiable list
   */
  public List<RubricRange> getSortedRubricRanges() {
    return sortedRubricRanges;
  }

  public RowElement(final Goal goal) {
    this.catagory = goal.getCategory();
    this.rowTitle = goal.getTitle();
    this.rowDescription = goal.getDescription().replaceAll("\\s+", " ").replaceAll("\n", "");

    final List<RubricRange> rubricRangeElements = new LinkedList<>(goal.getRubric());
    // sort so that the lowest range is first
    Collections.sort(rubricRangeElements, LEAST_RUBRIC_RANGE);
    sortedRubricRanges = Collections.unmodifiableList(rubricRangeElements);
  }

  private static final Comparator<RubricRange> LEAST_RUBRIC_RANGE = new Comparator<RubricRange>() {
    public int compare(final RubricRange one,
                       final RubricRange two) {
      return Integer.compare(one.getMin(), two.getMin());
    }
  };

  public String toString() {
    final String NL = System.lineSeparator();
    final StringBuilder output = new StringBuilder();
    output.append("-RowElementStart----------------------------------");
    output.append(NL);
    output.append("Catagory: ");
    output.append(catagory);
    output.append(NL);
    output.append("Row Title: ");
    output.append(rowTitle);
    output.append(NL);
    output.append("Row Description: ");
    output.append(rowDescription);
    output.append(NL);
    return output.toString();
  }

  public String getCatagory() {
    return catagory;
  }

  public String getRowTitle() {
    return rowTitle;
  }

  public String getDescription() {
    return rowDescription;
  }

}
