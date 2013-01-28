/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.util.LogUtils;
import fll.web.admin.DownloadSubjectiveData;
import fll.xml.AbstractGoal;
import fll.xml.EnumeratedValue;
import fll.xml.ScoreCategory;
import fll.xml.ScoreType;
import fll.xml.XMLUtils;

/**
 * TableModel for entering subjective scores.
 */
public final class SubjectiveTableModel extends AbstractTableModel {

  private static final Logger LOG = LogUtils.getLogger();

  public static final int NUM_COLUMNS_LEFT_OF_SCORES = 5;

  /**
   * @param scoreDocument XML document that represents the teams that are being
   *          scored along with the judges and the current set of scores
   * @param subjectiveElement subjective category
   */
  public SubjectiveTableModel(final Document scoreDocument,
                              final ScoreCategory subjectiveCategory) {
    _scoreDocument = scoreDocument;
    _subjectiveCategory = subjectiveCategory;
    _goals = new LinkedList<AbstractGoal>(_subjectiveCategory.getGoals());
    final List<Element> scoreElements = getScoreElements(_scoreDocument, _subjectiveCategory.getName());
    _scoreElements = new Element[scoreElements.size()];
    for (int i = 0; i < scoreElements.size(); i++) {
      _scoreElements[i] = scoreElements.get(i);
    }
  }

  /**
   * Get the score elements for the specified category.
   */
  public static List<Element> getScoreElements(final Document scoreDocument,
                                               final String categoryName) {
    for (final Element subCatElement : new NodelistElementCollectionAdapter(
                                                                            scoreDocument.getDocumentElement()
                                                                                         .getElementsByTagName(DownloadSubjectiveData.SUBJECTIVE_CATEGORY_NODE_NAME))) {
      final String name = subCatElement.getAttribute("name");
      if (categoryName.equals(name)) {
        final List<Element> scoreElements = new NodelistElementCollectionAdapter(
                                                                                 subCatElement.getElementsByTagName(DownloadSubjectiveData.SCORE_NODE_NAME)).asList();
        return scoreElements;
      }
    }

    return Collections.emptyList();
  }

  @Override
  public String getColumnName(final int column) {
    switch (column) {
    case 0:
      return "TeamNumber";
    case 1:
      return "TeamName";
    case 2:
      return "Division";
    case 3:
      return "Judging Station";
    case 4:
      return "Judge";
    default:
      if (column == getNumGoals()
          + NUM_COLUMNS_LEFT_OF_SCORES) {
        return "No Show";
      } else if (column == getNumGoals()
          + NUM_COLUMNS_LEFT_OF_SCORES + 1) {
        return "Total Score";
      } else {
        return getGoalDescription(column
            - NUM_COLUMNS_LEFT_OF_SCORES).getTitle();
      }
    }
  }

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DB_DUPLICATE_SWITCH_CLAUSES", justification = "Duplicate switch clauses causes this method to be consistent with the other methods and adds to clarity")
  public Class<?> getColumnClass(final int column) {
    switch (column) {
    case 0:
      return Integer.class;
    case 1:
      return String.class;
    case 2:
      return String.class;
    case 3:
      return String.class;
    case 4:
      return String.class;
    default:
      if (column == getNumGoals()
          + NUM_COLUMNS_LEFT_OF_SCORES) {
        // No Show
        return Boolean.class;
      } else if (column == getNumGoals()
          + NUM_COLUMNS_LEFT_OF_SCORES + 1) {
        // Total Score
        return Double.class;
      } else {
        final AbstractGoal goal = getGoalDescription(column
            - NUM_COLUMNS_LEFT_OF_SCORES);
        if (goal.isEnumerated()) {
          return String.class;
        } else if (goal.isComputed()) {
          return Double.class;
        } else {
          return Integer.class;
        }
      }
    }
  }

  public int getRowCount() {
    return _scoreElements.length;
  }

  public int getColumnCount() {
    return NUM_COLUMNS_LEFT_OF_SCORES
        + getNumGoals() + 2;
  }

  public Object getValueAt(final int row,
                           final int column) {
    try {
      final Element scoreEle = getScoreElement(row);
      switch (column) {
      case 0:
        if (scoreEle.hasAttribute("teamNumber")) {
          return Utilities.NUMBER_FORMAT_INSTANCE.parse(scoreEle.getAttribute("teamNumber")).intValue();
        } else {
          return null;
        }
      case 1:
        if (scoreEle.hasAttribute("teamName")) {
          return scoreEle.getAttribute("teamName");
        } else {
          return null;
        }
      case 2:
        return scoreEle.getAttribute("division");
      case 3:
        return scoreEle.getAttribute("judging_station");
      case 4:
        return scoreEle.getAttribute("judge");
      default:
        if (column == getNumGoals()
            + NUM_COLUMNS_LEFT_OF_SCORES) {
          return Boolean.valueOf(scoreEle.getAttribute("NoShow"));
        } else if (column == getNumGoals()
            + NUM_COLUMNS_LEFT_OF_SCORES + 1) {
          if (Boolean.valueOf(scoreEle.getAttribute("NoShow"))) {
            return 0;
          } else {
            // compute total score
            final double newTotalScore = _subjectiveCategory.evaluate(getTeamScore(row));
            return newTotalScore;
          }
        } else {
          final AbstractGoal goalDescription = getGoalDescription(column
              - NUM_COLUMNS_LEFT_OF_SCORES);
          final String goalName = goalDescription.getName();
          // the order really matters here because a computed goal will never
          // have an entry in scoreEle

          if (goalDescription.isComputed()) {
            return getTeamScore(row).getComputedScore(goalName);
          } else if (null == SubjectiveUtils.getSubscoreElement(scoreEle, goalName)) {
            return null;
          } else if (goalDescription.isEnumerated()) {
            return getTeamScore(row).getEnumRawScore(goalName);
          } else {
            final Double score = getTeamScore(row).getRawScore(goalName);
            final ScoreType scoreType = XMLUtils.getScoreType(scoreEle);
            if (ScoreType.FLOAT == scoreType
                || null == score) {
              return score;
            } else {
              return score.intValue();
            }
          }
        }
      }
    } catch (final ParseException pe) {
      throw new RuntimeException("Error in challenge.xml!!! Unparsable number", pe);
    }
  }

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DB_DUPLICATE_SWITCH_CLAUSES", justification = "Duplicate switch clauses causes this method to be consistent with the other methods and adds to clarity")
  public boolean isCellEditable(final int row,
                                final int column) {
    switch (column) {
    case 0:
      // TeamNumber
      return false;
    case 1:
      // TeamName
      return false;
    case 2:
      // Division
      return false;
    case 3:
      // Judging Station
      return false;
    case 4:
      // Judge
      return false;
    default:
      if (column == getNumGoals()
          + NUM_COLUMNS_LEFT_OF_SCORES) {
        // No Show
        return true;
      } else if (column == getNumGoals()
          + NUM_COLUMNS_LEFT_OF_SCORES + 1) {
        // Total Score
        return false;
      } else {
        // if no show, then no scores can be entered
        final Element scoreEle = getScoreElement(row);
        if (Boolean.valueOf(scoreEle.getAttribute("NoShow"))) {
          return false;
        }

        final AbstractGoal goalDescription = getGoalDescription(column
            - NUM_COLUMNS_LEFT_OF_SCORES);
        if (goalDescription.isComputed()) {
          return false;
        } else {
          return true;
        }
      }
    }
  }

  @Override
  public void setValueAt(final Object value,
                         final int row,
                         final int column) {
    setValueAt(value, row, column, true);
  }

  /**
   * Set the value of a cell and only set it's modified flag if setModified is
   * true. This allows us to use setValueAt to reset incorrect values.
   */
  private void setValueAt(final Object value,
                          final int row,
                          final int column,
                          final boolean setModified) {
    boolean error = false;
    final Element element = getScoreElement(row);

    if (column == getNumGoals()
        + NUM_COLUMNS_LEFT_OF_SCORES) {
      // No Show
      if (value instanceof Boolean) {
        element.setAttributeNS(null, "NoShow", value.toString());
        if (setModified) {
          element.setAttributeNS(null, "modified", Boolean.TRUE.toString());
        }

        final Boolean b = (Boolean) value;
        if (b) {
          // delete all scores for that team
          for (int i = 0; i < getNumGoals(); i++) {
            setValueAt(null, row, i
                + NUM_COLUMNS_LEFT_OF_SCORES);
          }
        }
      } else {
        error = true;
      }
    } else if (value != null
        && !"".equals(value) && Boolean.parseBoolean(element.getAttribute("NoShow"))) {
      // don't allow changes to rows with NoShow set to true, but allow the
      // scores to be set to null
      error = true;
    } else {
      final AbstractGoal goalDescription = getGoalDescription(column
          - NUM_COLUMNS_LEFT_OF_SCORES);
      final String goalName = goalDescription.getName();
      // support deleting a value
      if (null == value
          || "".equals(value)) {
        // remove value
        final Element subscoreElement = SubjectiveUtils.getSubscoreElement(element, goalName);
        if (null != subscoreElement) {
          element.removeChild(subscoreElement);
        }
        if (setModified) {
          element.setAttributeNS(null, "modified", Boolean.TRUE.toString());
        }

      } else {
        Element subscoreElement = SubjectiveUtils.getSubscoreElement(element, goalName);
        if (null == subscoreElement) {
          subscoreElement = _scoreDocument.createElementNS(null, DownloadSubjectiveData.SUBSCORE_NODE_NAME);
          subscoreElement.setAttributeNS(null, "name", goalName);
          element.appendChild(subscoreElement);
        }

        if (goalDescription.isEnumerated()) {
          // enumerated, convert from title to value
          boolean found = false;
          for (final EnumeratedValue posValue : goalDescription.getValues()) {
            if (posValue.getTitle().equalsIgnoreCase((String) value)) {
              // found it
              subscoreElement.setAttributeNS(null, "value", posValue.getValue());
              if (setModified) {
                element.setAttributeNS(null, "modified", Boolean.TRUE.toString());
              }
              found = true;
            }
          }
          if (!found) {
            error = true;
          }
        } else {
          // numeric

          double min = goalDescription.getMin();
          double max = goalDescription.getMax();

          final ScoreType scoreType = goalDescription.getScoreType();
          try {
            final Number parsedValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(value.toString());
            if (parsedValue.doubleValue() > max
                || parsedValue.doubleValue() < min) {
              error = true;
            } else {
              if (ScoreType.FLOAT == scoreType) {
                subscoreElement.setAttributeNS(null, "value", String.valueOf(parsedValue.doubleValue()));
              } else {
                subscoreElement.setAttributeNS(null, "value", String.valueOf(parsedValue.intValue()));
              }
              if (setModified) {
                element.setAttributeNS(null, "modified", Boolean.TRUE.toString());
              }
            }
          } catch (final ParseException pe) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(pe, pe);
            }
            error = true;
          }
        }
      }
    }

    if (error) {
      // reset
      setValueAt(getValueAt(row, column), row, column, false);
    } else {
      fireTableCellUpdated(row, column);
      forceComputedGoalUpdates(row);
      fireTableCellUpdated(row, getColumnCount() - 1); // update the total
      // score
    }
  }

  /**
   * Force the computed goals in the specified row to be updated.
   */
  private void forceComputedGoalUpdates(final int row) {
    for (int i = 0; i < getNumGoals(); ++i) {
      final AbstractGoal goal = getGoalDescription(i);
      if (goal.isComputed()) {
        fireTableCellUpdated(row, i
            + NUM_COLUMNS_LEFT_OF_SCORES);
      }
    }
  }

  /**
   * The rows in the table.
   */
  private final Element[] _scoreElements;

  /**
   * Get the score element at index
   */
  private Element getScoreElement(final int index) {
    return _scoreElements[index];
  }

  /**
   * Get the row index for a team number and judge
   * 
   * @param teamNumber
   * @param judge
   * @return the row index, -1 if one cannot be found
   */
  public int getRowForTeamAndJudge(final int teamNumber,
                                   final String judge) {
    try {
      for (int index = 0; index < _scoreElements.length; ++index) {
        final Element scoreEle = _scoreElements[index];
        final int num = Utilities.NUMBER_FORMAT_INSTANCE.parse(scoreEle.getAttribute("teamNumber")).intValue();
        final String j = scoreEle.getAttribute("judge");
        if (teamNumber == num
            && judge.equals(j)) {
          return index;
        }
      }
      return -1;
    } catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the score element at index.
   */
  private SubjectiveTeamScore getTeamScore(final int index) {
    try {
      return new SubjectiveTeamScore(getScoreElement(index));
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    }
  }

  private final ScoreCategory _subjectiveCategory;

  /**
   * Get the description element for goal at index
   */
  private AbstractGoal getGoalDescription(final int index) {
    return _goals.get(index);
  }

  /**
   * Find out how many goals there are.
   */
  public int getNumGoals() {
    return _goals.size();
  }

  private final List<AbstractGoal> _goals;

  /**
   * The backing for the model
   */
  private final Document _scoreDocument;

  /**
   * Find column for subcategory title.
   * 
   * @param subcategory
   * @return the column, -1 if it cannot be found
   */
  public int getColForSubcategory(final String subcategory) {
    for (int col = 0; col < getColumnCount(); ++col) {
      if (subcategory.equals(getColumnName(col))) {
        return col;
      }
    }
    return -1;
  }

}
