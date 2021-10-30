/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.GuiUtils;
import fll.xml.CaseStatement;
import fll.xml.GoalScope;
import fll.xml.SwitchStatement;
import fll.xml.VariableScope;
import fll.xml.ui.MovableExpandablePanel.DeleteEvent;
import fll.xml.ui.MovableExpandablePanel.DeleteEventListener;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Editor for {@link SwitchStatement}.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
/* package */ final class SwitchStatementEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final GoalScope goalScope;

  private final VariableScope variableScope;

  private final List<CaseStatementEditor> stmtEditors = new LinkedList<>();

  private final Box stmtContainer;

  private final CaseEventListener caseEventListener;

  private final SwitchStatement switchStmt;

  private final PolynomialEditor otherwiseEditor;

  /* package */ static final Color IF_COLOR = Color.ORANGE;

  /* package */ static final Color THEN_COLOR = Color.BLUE;

  /* package */ static final Color ELSE_COLOR = Color.CYAN;

  private static final int IF_THEN_LABEL_FONT_STYLE = Font.BOLD;

  private static final int IF_THEN_LABEL_FONT_SIZE = 18;

  private Font getIfThenFont(@UnknownInitialization(JPanel.class) SwitchStatementEditor this) {
    final Font labelFont = getFont();
    final String fontName;
    if (null == labelFont) {
      fontName = "Serif";
    } else {
      fontName = labelFont.getFontName();
    }
    final Font ifThenFont = new Font(fontName, IF_THEN_LABEL_FONT_STYLE, IF_THEN_LABEL_FONT_SIZE);
    return ifThenFont;
  }

  SwitchStatementEditor(final SwitchStatement switchStmt,
                        final GoalScope goalScope,
                        final VariableScope variableScope) {
    super(new BorderLayout());
    this.goalScope = goalScope;
    this.variableScope = variableScope;
    this.switchStmt = switchStmt;

    final Box buttonBox = Box.createHorizontalBox();
    add(buttonBox, BorderLayout.NORTH);

    final JButton addCase = new JButton("Add If/Then");
    buttonBox.add(addCase);
    buttonBox.add(Box.createHorizontalGlue());

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    stmtContainer = Box.createVerticalBox();
    container.add(stmtContainer);

    otherwiseEditor = new PolynomialEditor(switchStmt.getDefaultCase(), goalScope, variableScope);
    otherwiseEditor.setBorder(BorderFactory.createLineBorder(SwitchStatementEditor.ELSE_COLOR));
    final MovableExpandablePanel otherwisePanel = new MovableExpandablePanel("Otherwise value is", otherwiseEditor,
                                                                             false, false);
    container.add(otherwisePanel);
    otherwisePanel.setTitleFont(getIfThenFont());

    caseEventListener = new CaseEventListener();

    // object is initialized

    addCase.addActionListener(l -> addNewCaseStatement());

    // add the existing cases after everything is setup
    switchStmt.getCases().forEach(this::addCaseStatement);
  }

  private void addNewCaseStatement(@UnknownInitialization(SwitchStatementEditor.class) SwitchStatementEditor this) {
    final CaseStatement stmt = new CaseStatement();
    addCaseStatement(stmt);
    this.switchStmt.addCase(stmt);
  }

  private void addCaseStatement(@UnknownInitialization(SwitchStatementEditor.class) SwitchStatementEditor this,
                                final CaseStatement stmt) {
    final CaseStatementEditor editor = new CaseStatementEditor(stmt, goalScope, variableScope, getIfThenFont());
    stmtEditors.add(editor);

    final MovableExpandablePanel exPanel = new MovableExpandablePanel("If/then", editor, true, true);
    GuiUtils.addToContainer(stmtContainer, exPanel);

    exPanel.addMoveEventListener(caseEventListener);
    exPanel.addDeleteEventListener(caseEventListener);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    stmtEditors.forEach(CaseStatementEditor::commitChanges);
  }

  /**
   * @param messages add any errors to the list
   */
  @Override
  public boolean checkValidity(final Collection<String> messages) {
    boolean valid = true;

    if (switchStmt.getDefaultCase().getTerms().isEmpty()) {
      messages.add("There must be an otherwise case that has a value");
      valid = false;
    }

    final boolean otherwiseValid = otherwiseEditor.checkValidity(messages);
    valid &= otherwiseValid;

    for (final CaseStatementEditor editor : stmtEditors) {
      final boolean editorValid = editor.checkValidity(messages);
      valid &= editorValid;
    }

    return valid;
  }

  private final class CaseEventListener implements MoveEventListener, DeleteEventListener {

    @Override
    public void requestedMove(final MoveEvent e) {
      final int oldIndex = Utilities.getIndexOfComponent(stmtContainer, e.getComponent());
      if (oldIndex < 0) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to find source of move event in statement container");
        }
        return;
      }

      final int newIndex;
      if (e.getDirection() == MoveDirection.DOWN) {
        newIndex = oldIndex
            + 1;
      } else {
        newIndex = oldIndex
            - 1;
      }

      if (newIndex < 0
          || newIndex >= stmtContainer.getComponentCount()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Can't move component outside the container oldIndex: "
              + oldIndex
              + " newIndex: "
              + newIndex);
        }
        return;
      }

      // update editor list
      final CaseStatementEditor editor = stmtEditors.remove(oldIndex);
      stmtEditors.add(newIndex, editor);

      // update the UI
      stmtContainer.add(e.getComponent(), newIndex);
      stmtContainer.validate();

      // update the order in the challenge description
      final CaseStatement caseStmt = switchStmt.removeCase(oldIndex);
      switchStmt.addCase(newIndex, caseStmt);
    }

    @Override
    public void requestDelete(final DeleteEvent e) {
      final int confirm = JOptionPane.showConfirmDialog(SwitchStatementEditor.this,
                                                        "Are you sure that you want to delete the case statement?",
                                                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
      if (confirm != JOptionPane.YES_OPTION) {
        return;
      }

      final int index = Utilities.getIndexOfComponent(stmtContainer, e.getComponent());
      if (index < 0) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to find source of delete event in goal container");
        }
        return;
      }

      // update editor list
      stmtEditors.remove(index);

      // update the challenge description
      switchStmt.removeCase(index);

      // update the UI
      GuiUtils.removeFromContainer(stmtContainer, index);
    }

  } // CaseEventListener

}
