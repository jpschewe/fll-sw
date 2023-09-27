/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Modal dialog to get the groups of table colors.
 */
final class TableGroupDialog extends JDialog {

  /**
   * @param tableColors the table colors in the schedule
   */
  TableGroupDialog(final JFrame owner,
                   final Collection<String> tableColors) {
    super(owner, "Choose Table Groups", true);

    final List<String> tables = Collections.unmodifiableList(new ArrayList<>(tableColors));

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final JTextArea instructions = new JTextArea(0, 40);
    instructions.setText("Create a row for each group. Click the checkboxes to indicate which tables are to be grouped. The table optimizer will only move teams between tables in the same group. If all tables shoulod be used, don't create any groups.");
    instructions.setWrapStyleWord(true);
    instructions.setEditable(false);
    cpane.add(instructions, BorderLayout.NORTH);

    final JPanel groupPanel = new JPanel(new GridLayout(0, tables.size()
        + 2));
    cpane.add(groupPanel, BorderLayout.CENTER);

    groupPanel.add(new JLabel()); // group name
    for (final String table : tables) {
      groupPanel.add(new JLabel(table));
    }
    groupPanel.add(new JLabel()); // remove button

    final Box buttons = Box.createHorizontalBox();
    cpane.add(buttons, BorderLayout.SOUTH);

    final JButton addRow = new JButton("Add Group");
    buttons.add(addRow);
    addRow.addActionListener(ae -> {
      final List<String> group = new LinkedList<>();

      final JLabel groupName = new JLabel(String.format("Group %s", groupIndex
          + 1));
      groupPanel.add(groupName);

      final List<JCheckBox> checks = new LinkedList<>();
      for (int i = 0; i < tables.size(); ++i) {
        final String color = tables.get(i);
        final JCheckBox check = new JCheckBox();
        groupPanel.add(check);
        checks.add(check);

        check.addActionListener(e -> {
          if (check.isSelected()) {
            if (!group.contains(color)) {
              group.add(color);
            }
          } else {
            group.remove(color);
          }
        });
      }

      final JButton remove = new JButton("Remove");
      groupPanel.add(remove);

      remove.addActionListener(e -> {
        tableGroups.remove(group);
        groupPanel.remove(groupName);
        for (final JCheckBox check : checks) {
          groupPanel.remove(check);
        }
        groupPanel.remove(remove);
        --groupIndex;
        TableGroupDialog.this.pack();
      });

      tableGroups.add(group);

      ++groupIndex;
      TableGroupDialog.this.pack();
    });

    final JButton done = new JButton("Close");
    buttons.add(done);
    done.addActionListener(ae -> {
      TableGroupDialog.this.setVisible(false);
    });

    this.pack();
  }

  private int groupIndex = 0;

  private final List<List<String>> tableGroups = new ArrayList<>();

  List<List<String>> getTableGroups() {
    final List<List<String>> groups = new LinkedList<>();
    for (final List<String> g : tableGroups) {
      if (!g.isEmpty()) {
        groups.add(g);
      }
    }
    return groups;
  }
}
