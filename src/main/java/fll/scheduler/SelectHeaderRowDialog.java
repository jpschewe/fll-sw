/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.CellFileReader;
import fll.web.SelectHeaderRow;
import net.mtu.eggplant.util.gui.TableUtils;

/**
 * Dialog for the user to select the header row.
 */
class SelectHeaderRowDialog extends JDialog {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final int NUM_ROWS_TO_LOAD = 10;

  private final DefaultTableModel model;

  private final JTable table;

  private int numRowsToLoad = NUM_ROWS_TO_LOAD;

  private final File file;

  private final @Nullable String sheetName;

  private int headerRowIndex = -1;

  private boolean canceled = false;

  /**
   * @param file the file to read
   * @param owner the owner for the dialog
   * @param sheetName the name of the sheet to load in file if it is a spreadsheet
   */
  SelectHeaderRowDialog(final JFrame owner,
                        final File file,
                        final @Nullable String sheetName) {
    super(owner, "Choose Header Row", true);

    this.file = file;
    this.sheetName = sheetName;

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final JLabel instructions = new JLabel("Select the row containing the headers and press OK");
    cpane.add(instructions, BorderLayout.NORTH);

    model = new Model();
    table = new JTable(model);
    final JScrollPane scroller = new JScrollPane(table);
    cpane.add(scroller, BorderLayout.CENTER);
    table.setRowSelectionAllowed(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    final Box buttonBox = Box.createHorizontalBox();
    cpane.add(buttonBox, BorderLayout.SOUTH);

    final JButton loadMoreData = new JButton("Load More Data");
    buttonBox.add(loadMoreData);

    buttonBox.add(Box.createHorizontalGlue());

    final JButton okButton = new JButton("OK");
    buttonBox.add(okButton);

    final JButton cancelButton = new JButton("Cancel");
    buttonBox.add(cancelButton);

    okButton.addActionListener(e -> {
      headerRowIndex = table.getSelectedRow();
      canceled = false;
      if (headerRowIndex < 0) {
        JOptionPane.showMessageDialog(this, "You need to select a row or cancel", "Error", JOptionPane.ERROR_MESSAGE);
      } else {
        SelectHeaderRowDialog.this.setVisible(false);
      }
    });

    cancelButton.addActionListener(e -> {
      headerRowIndex = -1;
      canceled = true;
      SelectHeaderRowDialog.this.setVisible(false);
    });

    loadMoreData.addActionListener(e -> {
      numRowsToLoad += NUM_ROWS_TO_LOAD;
      loadData();
    });
    loadData();

    pack();
  }

  private void loadData(@UnknownInitialization(SelectHeaderRowDialog.class) SelectHeaderRowDialog this) {
    try {
      final CellFileReader reader = CellFileReader.createCellReader(file, sheetName);
      final String[][] data = SelectHeaderRow.loadData(reader, numRowsToLoad, "");

      final int numColumns = data.length > 0 ? data[0].length : 0;
      final String[] columnIdentifiers = new String[numColumns];
      for (int i = 0; i < numColumns; ++i) {
        columnIdentifiers[i] = String.format("Column %d", i);
      }

      model.setDataVector(data, columnIdentifiers);

      TableUtils.setColumnMinWidths(table);
    } catch (InvalidFormatException | IOException e) {
      final String msg = String.format("Error reading file %s: %s", file.getAbsolutePath(), e.getMessage());
      LOGGER.error(msg, e);
      JOptionPane.showMessageDialog(this, msg, "Error reading file", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * @return the selected header row, -1 if nothing was selected
   */
  public int getHeaderRowIndex() {
    return headerRowIndex;
  }

  /**
   * @return if the dialog was canceled
   */
  public boolean isCanceled() {
    return canceled;
  }

  private static final class Model extends DefaultTableModel {

    @Override
    public boolean isCellEditable(final int row,
                                  final int column) {
      return false;
    }
  }
}
