/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.text.ParseException;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.util.ScoreUtils;
import fll.xml.ScoreType;
import fll.xml.XMLUtils;

/**
 * TableModel for entering subjective scores.
 */
public final class SubjectiveTableModel extends AbstractTableModel {

  private static final Logger LOG = Logger.getLogger(SubjectiveTableModel.class);

  /**
   * @param scoreDocument XML document that represents the teams that are being
   *          scored along with the judges and the current set of scores
   * @param subjectiveElement subjective category
   */
  public SubjectiveTableModel(final Document scoreDocument, final Element subjectiveElement) {
    _scoreDocument = scoreDocument;
    _subjectiveElement = subjectiveElement;
    _goals = XMLUtils.filterToElements(subjectiveElement.getChildNodes());
    final Element categoryScoreElement = (Element) (_scoreDocument.getDocumentElement()).getElementsByTagName(subjectiveElement.getAttribute("name")).item(0);
    final List<Element> scoreElements = XMLUtils.filterToElements(categoryScoreElement.getElementsByTagName("score"));
    _scoreElements = new Element[scoreElements.size()];
    for (int i = 0; i < scoreElements.size(); i++) {
      _scoreElements[i] = scoreElements.get(i);
    }
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
      return "Judge";
    default:
      if (column == getNumGoals() + 4) {
        return "No Show";
      } else if (column == getNumGoals() + 5) {
        return "Total Score";
      } else {
        return getGoalDescription(column - 4).getAttribute("title");
      }
    }
  }

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="DB_DUPLICATE_SWITCH_CLAUSES", justification="Duplicate switch clauses causes this method to be consistent with the other methods and adds to clarity")
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
    default:
      if (column == getNumGoals() + 4) {
        // No Show
        return Boolean.class;
      } else if (column == getNumGoals() + 5) {
        // Total Score
        return Double.class;
      } else {
        final Element goalEle = getGoalDescription(column - 4);
        if (XMLUtils.isEnumeratedGoal(goalEle)) {
          return String.class;
        } else if (XMLUtils.isComputedGoal(goalEle)) {
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
    return 6 + getNumGoals();
  }

  public Object getValueAt(final int row, final int column) {
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
        return scoreEle.getAttribute("judge");
      default:
        if (column == getNumGoals() + 4) {
          return Boolean.valueOf(scoreEle.getAttribute("NoShow"));
        } else if (column == getNumGoals() + 5) {
          // compute total score
          final double newTotalScore = ScoreUtils.computeTotalScore(getTeamScore(row));
          return newTotalScore;
        } else {
          final Element goalDescription = getGoalDescription(column - 4);
          final String goalName = goalDescription.getAttribute("name");
          // the order really matters here because a computed goal will never
          // have an entry in scoreEle
          if (XMLUtils.isComputedGoal(goalDescription)) {
            return getTeamScore(row).getComputedScore(goalName);
          } else if (!scoreEle.hasAttribute(goalName)) {
            return null;
          } else if (XMLUtils.isEnumeratedGoal(goalDescription)) {
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="DB_DUPLICATE_SWITCH_CLAUSES", justification="Duplicate switch clauses causes this method to be consistent with the other methods and adds to clarity")
  public boolean isCellEditable(final int row, final int column) {
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
      // Judge
      return false;
    default:
      if (column == getNumGoals() + 4) {
        // No Show
        return true;
      } else if (column == getNumGoals() + 5) {
        // Total Score
        return false;
      } else {
        final Element goalDescription = getGoalDescription(column - 4);
        if (XMLUtils.isComputedGoal(goalDescription)) {
          return false;
        } else if ("goal".equals(goalDescription.getNodeName())) {
          return true;
        } else {
          throw new RuntimeException("Expected 'computedGoal' or 'goal', but found: "
              + goalDescription.getNodeName());
        }
      }
    }
  }

  @Override
  public void setValueAt(final Object value, final int row, final int column) {
    setValueAt(value, row, column, true);
  }

  /**
   * Set the value of a cell and only set it's modified flag if setModified is
   * true. This allows us to use setValueAt to reset incorrect values.
   */
  private void setValueAt(final Object value, final int row, final int column, final boolean setModified) {
    boolean error = false;
    final Element element = getScoreElement(row);

    if (column == getNumGoals() + 4) {
      // No Show
      if (value instanceof Boolean) {
        element.setAttribute("NoShow", value.toString());
        if (setModified) {
          element.setAttribute("modified", Boolean.TRUE.toString());
        }

        final Boolean b = (Boolean) value;
        if (b) {
          // delete all scores for that team
          for (int i = 0; i < getNumGoals(); i++) {
            setValueAt(null, row, i + 4);
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
      final Element goalDescription = getGoalDescription(column - 4);
      final String goalName = goalDescription.getAttribute("name");
      // support deleting a value
      if (null == value
          || "".equals(value)) {
        // remove value
        element.setAttribute(goalName, "");
        if (setModified) {
          element.setAttribute("modified", Boolean.TRUE.toString());
        }
      } else {
        final List<Element> posValues = XMLUtils.filterToElements(goalDescription.getElementsByTagName("value"));
        if (posValues.size() > 0) {
          // enumerated, convert from title to value
          boolean found = false;
          for (final Element posValue : posValues) {
            if (posValue.getAttribute("title").equalsIgnoreCase((String) value)) {
              // found it
              element.setAttribute(goalName, posValue.getAttribute("value"));
              if (setModified) {
                element.setAttribute("modified", Boolean.TRUE.toString());
              }
              found = true;
            }
          }
          if (!found) {
            error = true;
          }
        } else {
          // numeric

          double min = 0;
          double max = 1;
          try {
            min = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalDescription.getAttribute("min")).doubleValue();
            max = Utilities.NUMBER_FORMAT_INSTANCE.parse(goalDescription.getAttribute("max")).doubleValue();

          } catch (final ParseException pe) {
            throw new RuntimeException("Error in challenge.xml!!! min or max unparseable for goal: "
                + goalDescription.getAttribute("name"));
          }

          final ScoreType scoreType = XMLUtils.getScoreType(element);
          try {
            final Number parsedValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(value.toString());
            if (parsedValue.doubleValue() > max
                || parsedValue.doubleValue() < min) {
              error = true;
            } else {
              if (ScoreType.FLOAT == scoreType) {
                element.setAttribute(goalName, String.valueOf(parsedValue.doubleValue()));
              } else {
                element.setAttribute(goalName, String.valueOf(parsedValue.intValue()));
              }
              if (setModified) {
                element.setAttribute("modified", Boolean.TRUE.toString());
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
      final Element goalEle = getGoalDescription(i);
      if (XMLUtils.isComputedGoal(goalEle)) {
        fireTableCellUpdated(row, i + 4);
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
  public int getRowForTeamAndJudge(final int teamNumber, final String judge) {
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
      return new SubjectiveTeamScore(_subjectiveElement, getScoreElement(index));
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    }
  }

  private final Element _subjectiveElement;

  /**
   * Get the description element for goal at index
   */
  private Element getGoalDescription(final int index) {
    return _goals.get(index);
  }

  /**
   * Find out how many goals there are.
   */
  private int getNumGoals() {
    return _goals.size();
  }

  private final List<Element> _goals;

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
