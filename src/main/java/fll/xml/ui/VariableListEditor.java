/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.checkerframework.checker.initialization.qual.UnderInitialization;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.xml.ComputedGoal;
import fll.xml.GoalScope;
import fll.xml.Variable;
import fll.xml.ui.MovableExpandablePanel.DeleteEvent;
import fll.xml.ui.MovableExpandablePanel.DeleteEventListener;

/**
 * Edit a list of variables from a {@link ComputedGoal}.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
public class VariableListEditor extends JPanel {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final ComputedGoal goal;

  private final JComponent editorContainer;

  private final DeleteEventListener deleteListener;

  private final List<VariableEditor> editors = new LinkedList<>();

  private final GoalScope goalScope;

  /**
   * @param goal the goal to edit variables for
   * @param goalScope where to find goals
   */
  public VariableListEditor(final ComputedGoal goal,
                            final GoalScope goalScope) {
    super(new BorderLayout());

    this.goal = goal;
    this.goalScope = goalScope;

    editorContainer = Box.createVerticalBox();

    deleteListener = new DeleteEventListener() {

      @Override
      public void requestDelete(final DeleteEvent e) {
        final int confirm = JOptionPane.showConfirmDialog(VariableListEditor.this,
                                                          "Are you sure that you want to delete the variable?",
                                                          "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
          return;
        }

        final int index = Utilities.getIndexOfComponent(editorContainer, e.getComponent());
        if (index < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of delete event in variable container");
          }
          return;
        }

        // update editor list
        final VariableEditor editor = editors.get(index);
        editors.remove(index);

        // update the challenge description
        goal.removeVariable(editor.getVariable());

        // update the UI
        GuiUtils.removeFromContainer(editorContainer, index);
      }
    };

    // add some space on the left side of the expanded panel
    final JPanel expansion = new JPanel(new BorderLayout());

    final Box buttonBox = Box.createHorizontalBox();
    expansion.add(buttonBox, BorderLayout.NORTH);

    final JButton add = new JButton("Add Variable");
    buttonBox.add(add);
    add.addActionListener(l -> addNewVariable());

    buttonBox.add(Box.createHorizontalGlue());

    expansion.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    expansion.add(editorContainer, BorderLayout.CENTER);

    final MovableExpandablePanel exPanel = new MovableExpandablePanel("Variables", expansion, false, false);
    add(exPanel, BorderLayout.CENTER);

    goal.getAllVariables().forEach(v -> addVariable(v));
  }

  private void addNewVariable(@UnderInitialization(VariableListEditor.class) VariableListEditor this) {
    final String name = String.format("Variable %d", editorContainer.getComponentCount());
    final Variable var = new Variable(name);
    this.goal.addVariable(var);

    addVariable(var);
  }

  private void addVariable(@UnderInitialization(VariableListEditor.class) VariableListEditor this,
                           final Variable var) {
    final VariableEditor variableEditor = new VariableEditor(var, goalScope);
    final MovableExpandablePanel exPanel = new MovableExpandablePanel(var.getName(), variableEditor, false, true);
    GuiUtils.addToContainer(editorContainer, exPanel);

    variableEditor.addPropertyChangeListener("name", e -> {
      final String newName = (String) e.getNewValue();
      exPanel.setTitle(newName);
    });

    exPanel.addDeleteEventListener(deleteListener);

  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    editors.forEach(e -> e.commitChanges());
  }

  /**
   * Check that there aren't duplicate variable names.
   * 
   * @param messages add any errors to the list
   */
  /* package */ void gatherValidityMessages(final Collection<String> messages) {
    final Set<String> variableNames = new HashSet<>();

    editors.forEach(e -> {
      final String name = e.getVariable().getName();
      final boolean newElement = variableNames.add(name);
      if (!newElement) {
        final String message = String.format("The variable name '%s' is used twice in the goal", name);
        messages.add(message);
      }
    });
  }

}
