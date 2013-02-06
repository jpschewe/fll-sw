/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.web.admin.DownloadSubjectiveData;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Summary dialog for subjective scores.
 * 
 * @author jpschewe
 * @version $Revision$
 */
/* package */class SummaryDialog extends JDialog {

  public SummaryDialog(final SubjectiveFrame owner) {
    super(owner, true);

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final TableModel[] models = buildTableModels(owner.getChallengeDescription(), owner.getScoreDocument());
    final JTable teamTable = new JTable(models[0]);
    teamTable.setGridColor(Color.BLACK);
    teamTable.setAutoCreateRowSorter(true);

    teamTable.setDefaultRenderer(Integer.class, CustomCellRenderer.INSTANCE);
    final JPanel teamPanel = new JPanel(new BorderLayout());
    final JLabel teamLabel = new JLabel("# of scores for each team in each category");
    final JScrollPane teamTableScroller = new JScrollPane(teamTable);
    teamPanel.add(teamLabel, BorderLayout.NORTH);
    teamPanel.add(teamTableScroller, BorderLayout.CENTER);
    cpane.add(teamPanel, BorderLayout.CENTER);

    final JTable summaryTable = new JTable(models[1]);
    summaryTable.setGridColor(Color.BLACK);
    summaryTable.setAutoCreateRowSorter(true);

    final JLabel summaryLabel = new JLabel("# of teams in each category with the specified # of scores");
    final JPanel summaryPanel = new JPanel(new BorderLayout());
    summaryPanel.add(summaryLabel, BorderLayout.NORTH);
    final JScrollPane summaryTableScroller = new JScrollPane(summaryTable);
    final Dimension sumPrefSize = summaryTable.getPreferredSize();
    summaryTableScroller.setPreferredSize(new Dimension(sumPrefSize.width, sumPrefSize.height * 2));
    summaryPanel.add(summaryTableScroller, BorderLayout.CENTER);
    cpane.add(summaryPanel, BorderLayout.NORTH);

    final JButton closeButton = new JButton("Close");
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        setVisible(false);
        dispose();
      }
    });
    cpane.add(closeButton, BorderLayout.SOUTH);
  }

  /**
   * @return [0] is the team table model, [1] is the summary table model
   */
  private TableModel[] buildTableModels(final ChallengeDescription description,
                                        final Document scoreDocument) {
    final Map<Integer, Integer[]> data = new HashMap<Integer, Integer[]>();

    final Element scoresElement = scoreDocument.getDocumentElement();
    final List<String> columnNames = new LinkedList<String>();
    final Map<Integer, String> teamNumberToDivision = new HashMap<Integer, String>();
    final List<ScoreCategory> subjectiveCategories = description.getSubjectiveCategories();
    for (int catIdx = 0; catIdx < subjectiveCategories.size(); catIdx++) {
      final ScoreCategory subjectiveElement = subjectiveCategories.get(catIdx);
      final String category = subjectiveElement.getName();
      final String categoryTitle = subjectiveElement.getTitle();
      columnNames.add(categoryTitle);

      final List<AbstractGoal> goals = subjectiveElement.getGoals();
      final Element categoryElement = SubjectiveUtils.getCategoryNode(scoresElement, category);
      for (final Element scoreElement : new NodelistElementCollectionAdapter(
                                                                             categoryElement.getElementsByTagName(DownloadSubjectiveData.SCORE_NODE_NAME))) {
        int numValues = 0;
        for (final AbstractGoal goalElement : goals) {
          if (!goalElement.isComputed()) {
            final String goalName = goalElement.getName();
            final Element subscoreElement = SubjectiveUtils.getSubscoreElement(scoreElement, goalName);
            if (null != subscoreElement) {
              final String value = subscoreElement.getAttribute("value");
              if (null != value
                  && !"".equals(value)) {
                numValues++;
                break;
              }
            }
          }// !computed
        } // end foreach goal

        final int teamNumber = Integer.parseInt(scoreElement.getAttribute("teamNumber"));
        if (!data.containsKey(teamNumber)) {
          final Integer[] counts = new Integer[subjectiveCategories.size()];
          Arrays.fill(counts, 0);
          data.put(teamNumber, counts);
        }
        teamNumberToDivision.put(teamNumber, scoreElement.getAttribute("division"));

        if (numValues > 0
            || Boolean.parseBoolean(scoreElement.getAttribute("NoShow"))) {
          // if there is a score or a No Show, then increment counter for this
          // team/category combination
          data.get(teamNumber)[catIdx]++;
        } // end if score

      } // end foreach score row
    }// end foreach category

    // sort the data and map it into an easier form for table consumption
    final Map<String, List<SummaryData>> summaryData = new HashMap<String, List<SummaryData>>();

    final List<SummaryData> tableData = new LinkedList<SummaryData>();
    final List<Integer> teamNumbers = new ArrayList<Integer>(data.keySet());
    Collections.sort(teamNumbers);
    for (final Integer teamNumber : teamNumbers) {
      final String division = teamNumberToDivision.get(teamNumber);

      List<SummaryData> divisionSummaryData = summaryData.get(division);
      if (null == divisionSummaryData) {
        divisionSummaryData = new ArrayList<SummaryData>();
        summaryData.put(division, divisionSummaryData);
      }
      final Integer[] counts = data.get(teamNumber);
      for (int column = 0; column < counts.length; ++column) {
        // make sure that counts[column] rows exist in summaryData
        final int numScoresForTeam = counts[column];
        while (divisionSummaryData.size() < numScoresForTeam + 1) {
          final List<Integer> summaryRow = new ArrayList<Integer>();
          for (int i = 0; i < columnNames.size(); ++i) {
            summaryRow.add(0);
          }
          final int numScores = divisionSummaryData.size();
          divisionSummaryData.add(new SummaryData(numScores, division, summaryRow));
        }
        final List<Integer> summaryRow = divisionSummaryData.get(numScoresForTeam).getScoreData();
        final int prev = summaryRow.get(column);
        summaryRow.set(column, prev + 1);
      }
      final List<Integer> scoreData = new LinkedList<Integer>();
      for (final Integer value : counts) {
        scoreData.add(value);
      }
      final SummaryData teamSummaryData = new SummaryData(teamNumber, division, scoreData);
      tableData.add(teamSummaryData);
    }

    return new TableModel[] { new CountTableModel(tableData, columnNames),
                             new SummaryTableModel(summaryData.values(), columnNames) };
  }

  /**
   * Table model for the summary data
   */
  private static final class SummaryTableModel extends AbstractTableModel {
    public SummaryTableModel(final Collection<List<SummaryData>> summaryData,
                             final List<String> columnNames) {
      _summaryData = new ArrayList<SummaryData>();
      for (final List<SummaryData> list : summaryData) {
        _summaryData.addAll(list);
      }
      _columnNames = columnNames;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SE_BAD_FIELD" }, justification = "Not serializing these classes")
    private final List<SummaryData> _summaryData;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SE_BAD_FIELD" }, justification = "Not serializing these classes")
    private final List<String> _columnNames;

    @Override
    public Class<?> getColumnClass(final int column) {
      switch (column) {
      case 0:
        return String.class;
      default:
        return Integer.class;
      }
    }

    public int getRowCount() {
      return _summaryData.size();
    }

    public int getColumnCount() {
      return _columnNames.size() + 2;
    }

    @Override
    public String getColumnName(final int column) {
      switch (column) {
      case 0:
        return "Division";
      case 1:
        return "# of scores";
      default:
        return _columnNames.get(column - 2);
      }
    }

    public Object getValueAt(final int row,
                             final int column) {
      final SummaryData data = _summaryData.get(row);
      switch (column) {
      case 0:
        return data.getDivision();
      case 1:
        return data.getTeamNumber();
      default:
        final List<Integer> rowData = data.getScoreData();
        return rowData.get(column - 2);
      }
    }
  }

  /**
   * Table model for the counts of scores for each team.
   */
  private static final class CountTableModel extends AbstractTableModel {
    /**
     * @param data the summary data
     * @param categoryColumnNames names of category columns
     */
    public CountTableModel(final List<SummaryData> data,
                           final List<String> categoryColumnNames) {
      _data = data;
      _categoryColumnNames = categoryColumnNames;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SE_BAD_FIELD" }, justification = "Not serializing these classes")
    private final List<SummaryData> _data;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SE_BAD_FIELD" }, justification = "Not serializing these classes")
    private final List<String> _categoryColumnNames;

    @Override
    public Class<?> getColumnClass(final int column) {
      switch (column) {
      case 1:
        return String.class;
      default:
        return Integer.class;
      }
    }

    public int getRowCount() {
      return _data.size();
    }

    public int getColumnCount() {
      return _categoryColumnNames.size() + 2;
    }

    @Override
    public String getColumnName(final int column) {
      switch (column) {
      case 0:
        return "Team Number";
      case 1:
        return "Division";
      default:
        return _categoryColumnNames.get(column - 2);
      }
    }

    public Object getValueAt(final int row,
                             final int column) {
      final SummaryData rowData = _data.get(row);
      switch (column) {
      case 0:
        return rowData.getTeamNumber();
      case 1:
        return rowData.getDivision();
      default:
        return rowData.getScoreData().get(column - 2);
      }
    }
  }

  /**
   * Show all 0's in red.
   */
  private static final class CustomCellRenderer extends DefaultTableCellRenderer {
    public static final CustomCellRenderer INSTANCE = new CustomCellRenderer();

    @Override
    public Component getTableCellRendererComponent(final JTable table,
                                                   final Object value,
                                                   final boolean isSelected,
                                                   final boolean hasFocus,
                                                   final int row,
                                                   final int column) {
      final Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (value instanceof Number
          && ((Number) value).intValue() == 0) {
        comp.setForeground(Color.RED);
      } else {
        comp.setForeground(Color.BLACK);
      }
      return comp;
    }
  }

  /**
   * Contains summary information.
   */
  private static final class SummaryData {
    private final String division;

    public String getDivision() {
      return division;
    }

    private final List<Integer> scoreData;

    public List<Integer> getScoreData() {
      return scoreData;
    }

    private final int teamNumber;

    /**
     * @return Either the team number of the number of scores (overloaded use)
     */
    public int getTeamNumber() {
      return teamNumber;
    }

    public SummaryData(final int teamNumber,
                       final String division,
                       final List<Integer> scoreData) {
      this.division = division;
      this.scoreData = scoreData;
      this.teamNumber = teamNumber;
    }
  }
}
