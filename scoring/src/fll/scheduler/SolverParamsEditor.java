/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Editor for {@link SolverParams}.
 */
public class SolverParamsEditor extends JPanel {

  private final JFormattedTextField startTimeEditor;

  public SolverParamsEditor() {
    super(new GridBagLayout());

    GridBagConstraints gbc;

    final JLabel startTimeLabel = new JLabel("Start Time:");
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.EAST;
    gbc.weighty = 0;
    add(startTimeLabel, gbc);
    
    startTimeEditor = new JFormattedTextField(new TimeFormat());
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0;
    add(startTimeEditor, gbc);

    // end of line spacer
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    add(new JPanel(), gbc);


    // end of form spacer
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weighty = 1.0;
    add(new JPanel(), gbc);
  }

  private SolverParams params;

  public void setParams(final SolverParams params) {
    this.params = params;
    
    startTimeEditor.setValue(params.getStartTime());
    
  }

  public SolverParams getParams() {
    return this.params;
  }

}
