package fll.documents.elements;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLRuntimeException;
import fll.xml.AbstractGoal;
import fll.xml.Goal;
import fll.xml.RubricRange;
import fll.xml.SubjectiveScoreCategory;

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
   * Category -> TableElement in the order that the goal categories are found.
   */
  private final Map<String, TableElement> tables = new LinkedHashMap<String, TableElement>();

  private final SubjectiveScoreCategory sheetData;

  private List<String> mMasterRubricRangeTitles;

  /**
   * @return The titles of all ranges in the rubric sorted from lowest range to
   *         highest range
   */
  public List<String> getRubricRangeTitles() {
    return mMasterRubricRangeTitles;
  }

  public SubjectiveScoreCategory getSheetData() {
    return this.sheetData;
  }

  /**
   * Used to ensure that {@code tables} does not have any null keys.
   */
  private static final String NULL_GOAL_CATEGORY = "";

  /**
   * @param sheetData the sheet data to process
   * @throws FLLRuntimeException if all goals in the ScoreCategory don't have
   *           the same rubric titles
   */
  public SheetElement(final SubjectiveScoreCategory sheetData) {
    // Sheet name will be Programming, Project, Robot Design or Core Values
    this.sheetName = sheetData.getName();
    this.sheetData = sheetData;

    final List<AbstractGoal> goalsList = sheetData.getGoals();

    // Go thru the sheet (ScoreCategory) and put all the rows (abstractGoal)
    // into the right tables (Category)
    // The assumption is made that the order in the xml is the proper order of
    // the abstractGoals
    for (final AbstractGoal abstractGoal : goalsList) {
      if (abstractGoal instanceof Goal) {
        final Goal goal = (Goal) abstractGoal;

        // getRubric returns a sorted list, so we can just add the titles in order
        final List<String> rubricRangeTitles = new LinkedList<>();
        for (final RubricRange range : goal.getRubric()) {
          rubricRangeTitles.add(range.getTitle());
        }

        if (null == mMasterRubricRangeTitles) {
          mMasterRubricRangeTitles = rubricRangeTitles;
        } else if (!mMasterRubricRangeTitles.equals(rubricRangeTitles)) {
          throw new FLLRuntimeException("Rubric range titles not consistent across all goals in score category: "
              + sheetData.getTitle());
        }

        String tableCategory = goal.getCategory();
        if (null == tableCategory) {
          tableCategory = NULL_GOAL_CATEGORY;
        }

        TableElement tableElement = tables.get(tableCategory);
        if (null == tableElement) {
          tableElement = new TableElement(tableCategory);
          tables.put(tableCategory, tableElement);
        }
        tableElement.addRowElement(new RowElement(goal));
      }
    }

    if (null == mMasterRubricRangeTitles) {
      mMasterRubricRangeTitles = new LinkedList<>();
    }
  }

  public String getSheetName() {
    return this.sheetName;
  }

  /**
   * @param visitor called with each {@link TableElement} in the order seen in the
   *          challenge description
   */
  public void forEachGoalCategory(final Consumer<TableElement> visitor) {
    tables.forEach((category,
                    table) -> {
      visitor.accept(table);
    });
  }

  public String toString() {
    final StringBuilder str = new StringBuilder();
    for (final Map.Entry<String, TableElement> entry : tables.entrySet()) {
      str.append("Table["
          + entry.getKey()
          + "]: "
          + entry.getValue());
      str.append(System.lineSeparator());
    }
    str.append("Subjective Category: "
        + this.sheetName);
    return str.toString();
  }
}
