/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import org.apache.log4j.Logger;

import fll.util.LogUtils;
import fll.xml.AbstractGoal;
import fll.xml.ScoreCategory;

/*
 * TODO
 * - Determine where TransferHandler class needs to live based on what it needs to know (perhaps a stand alone class)
 * - How to reorder goals in the UI, needs to be used in TransferHandler.exportDone
 * - Support modifying the ChallengeDescription, should there be an immutable version?
 * - Think about how the model, view, controller should work with ChallengeDescription
 * 
 * 
 */

/**
 * Editor for {@link ScoreCategory} objects.
 */
public class ScoreCategoryEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  public ScoreCategoryEditor(final ScoreCategory category) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    for (final AbstractGoal goal : category.getGoals()) {
      final AbstractGoalEditor editor = new AbstractGoalEditor(goal);
      add(editor);
    }
  }

  private static final class SCETransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(final JComponent jc) {
      if (jc instanceof DragHandle) {
        return MOVE;
      } else {
        return 0;
      }
    }

    @Override
    public Transferable createTransferable(final JComponent c) {
      // needs to be set
      return new StringSelection("FIXME needs implementation");
    }

    @Override
    public void exportDone(final JComponent c,
                           final Transferable t,
                           final int action) {
      if (action == MOVE) {
        LOGGER.info("Doing move - NOT IMPLEMENTED");
      }
    }
  }

}
