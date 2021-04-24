/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.lang3.tuple.Pair;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.xml.PerformanceScoreCategory;
import fll.xml.TiebreakerTest;
import fll.xml.ui.MovableExpandablePanel.DeleteEvent;
import fll.xml.ui.MovableExpandablePanel.DeleteEventListener;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Edit the list of {@link TiebreakerTest} objects on the
 * {@link PerformanceScoreCategory}.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
/* package */ class TiebreakerEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JComponent editorContainer;

  private final PerformanceScoreCategory performance;

  private final List<Pair<TiebreakerTestEditor, MovableExpandablePanel>> editors = new LinkedList<>();

  private final MoveEventListener moveListener;

  private final DeleteEventListener deleteListener;

  TiebreakerEditor(@Nonnull final PerformanceScoreCategory performance) {
    super(new BorderLayout());
    this.performance = performance;

    editorContainer = Box.createVerticalBox();

    // add some space on the left side of the expanded panel
    final JPanel expansion = new JPanel(new BorderLayout());

    final Box buttonBox = Box.createHorizontalBox();
    expansion.add(buttonBox, BorderLayout.NORTH);

    final JButton add = new JButton("Add Tiebreaker");
    buttonBox.add(add);
    add.addActionListener(l -> addNewTest());

    buttonBox.add(Box.createHorizontalGlue());

    expansion.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    expansion.add(editorContainer, BorderLayout.CENTER);

    final MovableExpandablePanel exPanel = new MovableExpandablePanel("Tie breakers", expansion, false, false);
    add(exPanel, BorderLayout.CENTER);

    moveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        final int oldIndex = Utilities.getIndexOfComponent(editorContainer, e.getComponent());
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
            || newIndex >= editorContainer.getComponentCount()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: "
                + oldIndex
                + " newIndex: "
                + newIndex);
          }
          return;
        }

        // update editor list
        final Pair<TiebreakerTestEditor, MovableExpandablePanel> editorPair = editors.remove(oldIndex);
        editors.add(newIndex, editorPair);

        // update the UI
        editorContainer.add(e.getComponent(), newIndex);
        editorContainer.validate();
        updateTitles();
      }
    };

    deleteListener = new DeleteEventListener() {

      @Override
      public void requestDelete(final DeleteEvent e) {
        final int confirm = JOptionPane.showConfirmDialog(TiebreakerEditor.this,
                                                          "Are you sure that you want to delete the tie breaker?",
                                                          "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
          return;
        }

        final int index = Utilities.getIndexOfComponent(editorContainer, e.getComponent());
        if (index < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of delete event in tiebreaker container");
          }
          return;
        }

        // update editor list
        final Pair<TiebreakerTestEditor, MovableExpandablePanel> editorPair = editors.get(index);
        editors.remove(index);

        // update the UI
        GuiUtils.removeFromContainer(editorContainer, index);
        updateTitles();
      }
    };

    performance.getTiebreaker().forEach(this::addTest);
  }

  private static final String TITLE_FORMAT = "Tiebreaker %d";

  private void updateTitles() {
    int index = 0;
    for (final Pair<TiebreakerTestEditor, MovableExpandablePanel> editorPair : editors) {
      final TiebreakerTestEditor editor = editorPair.getLeft();
      final MovableExpandablePanel panel = editorPair.getRight();

      final String title = String.format(TITLE_FORMAT, index
          + 1);
      panel.setTitle(title);

      ++index;
    }
  }

  private void addNewTest() {
    final TiebreakerTest test = new TiebreakerTest();
    addTest(test);
  }

  private void addTest(final TiebreakerTest test) {
    final TiebreakerTestEditor editor = new TiebreakerTestEditor(test, performance);

    final MovableExpandablePanel panel = new MovableExpandablePanel(String.format(TITLE_FORMAT, editors.size()), editor,
                                                                    true, true);
    editors.add(Pair.of(editor, panel));

    panel.addDeleteEventListener(deleteListener);
    panel.addMoveEventListener(moveListener);

    panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
    GuiUtils.addToContainer(editorContainer, panel);
  }

  public void commitChanges() {
    final List<TiebreakerTest> newTiebreaker = new LinkedList<>();

    editors.forEach(editorPair -> {
      final TiebreakerTestEditor editor = editorPair.getLeft();
      final TiebreakerTest test = editor.getTest();
      newTiebreaker.add(test);
    });

    performance.setTiebreaker(newTiebreaker);
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    boolean valid = true;

    for (final Pair<TiebreakerTestEditor, MovableExpandablePanel> editorPair : editors) {
      final TiebreakerTestEditor editor = editorPair.getLeft();
      final boolean editorValid = editor.checkValidity(messagesToDisplay);
      valid &= editorValid;
    }

    return valid;
  }

}
