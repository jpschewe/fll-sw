/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import fll.util.FLLInternalException;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;

/**
 * Map subjective headers to subjective categories.
 */
public class MapSubjectiveHeaders extends JDialog {

  private final ChallengeDescription description;

  private final TournamentSchedule schedule;

  private MappingTableModel tableModel;

  public MapSubjectiveHeaders(final Frame owner,
                              final ChallengeDescription description,
                              final TournamentSchedule schedule) {
    super(owner, true);
    this.description = description;
    this.schedule = schedule;

    initComponents();
  }

  public MapSubjectiveHeaders(final Dialog owner,
                              final ChallengeDescription description,
                              final TournamentSchedule schedule) {
    super(owner, true);
    this.description = description;
    this.schedule = schedule;

    initComponents();
  }

  private void initComponents() {
    getContentPane().setLayout(new BorderLayout());

    final JTextArea instructions = new JTextArea("Match the columns in the schedule with the subjective categories that they contain the schedule for. You cannot have the same subjective category mapped to 2 schedule columns.");
    instructions.setEditable(false);
    instructions.setWrapStyleWord(true);
    instructions.setLineWrap(true);
    getContentPane().add(instructions, BorderLayout.NORTH);

    tableModel = new MappingTableModel(description, schedule);

    final JTable table = new JTable(tableModel);

    final JScrollPane scroller = new JScrollPane(table);
    getContentPane().add(scroller, BorderLayout.CENTER);

    final JComponent buttonBox = Box.createHorizontalBox();
    getContentPane().add(buttonBox, BorderLayout.SOUTH);

    final JButton ok = new JButton("OK");
    buttonBox.add(ok);
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        if (isMappingValid()) {
          setVisible(false);
        } else {
          JOptionPane.showMessageDialog(MapSubjectiveHeaders.this,
                                        "Each subjective category must have exactly 1 schedule column", "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    final JButton cancel = new JButton("Cancel");
    buttonBox.add(cancel);
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        setVisible(false);
      }
    });

    pack();
  }

  /**
   * Find schedule column for category.
   * 
   * @param category what to find
   * @return null if not found
   */
  public String getSubjectiveHeaderForCategory(final ScoreCategory category) {
    return tableModel.getSubjectiveHeaderForCategory(category);
  }

  /**
   * @return true if all categories have 1 schedule column
   */
  public boolean isMappingValid() {
    return tableModel.isMappingValid();
  }

  private static final class MappingTableModel extends AbstractTableModel {

    private final Map<String, Map<ScoreCategory, Boolean>> mappings = new HashMap<>();

    private final List<String> subjectiveStations;

    private final List<ScoreCategory> categories;

    public MappingTableModel(final ChallengeDescription description,
                             final TournamentSchedule schedule) {
      subjectiveStations = new ArrayList<>(schedule.getSubjectiveStations());
      categories = description.getSubjectiveCategories();

      for (final String subjectiveStation : subjectiveStations) {
        final Map<ScoreCategory, Boolean> stationMappings = new HashMap<>();
        for (final ScoreCategory category : categories) {
          stationMappings.put(category, false);
        }
        mappings.put(subjectiveStation, stationMappings);
      }
    }

    /**
     * Find schedule column for category.
     * 
     * @param category what to find
     * @return null if not found
     */
    public String getSubjectiveHeaderForCategory(final ScoreCategory category) {
      for (final Map.Entry<String, Map<ScoreCategory, Boolean>> stationEntry : mappings.entrySet()) {
        if (stationEntry.getValue().get(category)) {
          return stationEntry.getKey();
        }
      }
      return null;
    }

    /**
     * @return true if each category has exactly 1 schedule column
     */
    public boolean isMappingValid() {
      for (final ScoreCategory category : categories) {
        int found = 0;

        for (final Map.Entry<String, Map<ScoreCategory, Boolean>> stationEntry : mappings.entrySet()) {
          if (stationEntry.getValue().get(category)) {
            ++found;
          }
        } // for each top mapping

        if (1 != found) {
          return false;
        }

      } // foreach category

      return true;
    }

    public int getRowCount() {
      return subjectiveStations.size();
    }

    public int getColumnCount() {
      return categories.size()
          + 1;
    }

    @Override
    public String getColumnName(final int column) {
      switch (column) {
      case 0:
        return "Schedule Column";
      default:
        return categories.get(column
            - 1).getTitle();
      }
    }

    public Object getValueAt(final int row,
                             final int column) {
      if (0 == column) {
        return subjectiveStations.get(row);
      } else {
        final String subjectiveStation = subjectiveStations.get(row);
        final ScoreCategory category = categories.get(column
            - 1);
        final Map<ScoreCategory, Boolean> rowMapping = mappings.get(subjectiveStation);
        return rowMapping.get(category);
      }
    }

    public void setValueAt(final Object value,
                           final int row,
                           final int column) {
      if (column > 0) {
        if (!(value instanceof Boolean)) {
          throw new FLLInternalException("Found class other than boolean in set: "
              + value.getClass());
        }

        final String subjectiveStation = subjectiveStations.get(row);
        final ScoreCategory category = categories.get(column
            - 1);
        final Map<ScoreCategory, Boolean> rowMapping = mappings.get(subjectiveStation);
        rowMapping.put(category, (Boolean) value);
      }

    }

    public Class<?> getColumnClass(final int columnIndex) {
      if (0 == columnIndex) {
        return String.class;
      } else {
        return Boolean.class;
      }
    }

    public boolean isCellEditable(final int rowIndex,
                                  final int columnIndex) {
      if (0 == columnIndex) {
        return false;
      } else {
        return true;
      }
    }

  }

}
