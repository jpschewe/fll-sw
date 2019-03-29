/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;



import fll.Utilities;
import fll.util.FormatterUtils;

import fll.xml.AbstractConditionStatement;
import fll.xml.AbstractGoal;
import fll.xml.BasicPolynomial;
import fll.xml.CaseStatement;
import fll.xml.ComputedGoal;
import fll.xml.ConditionStatement;
import fll.xml.EnumConditionStatement;
import fll.xml.Goal;
import fll.xml.GoalRef;
import fll.xml.ScopeException;
import fll.xml.ScoreCategory;
import fll.xml.SwitchStatement;
import fll.xml.Term;
import fll.xml.Variable;
import fll.xml.ui.MovableExpandablePanel.DeleteEvent;
import fll.xml.ui.MovableExpandablePanel.DeleteEventListener;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Editor for {@link ScoreCategory} objects.
 */
public abstract class ScoreCategoryEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField mWeight;

  private final List<AbstractGoalEditor> mGoalEditors = new LinkedList<>();

  private final Box mGoalEditorContainer;

  private final ScoreCategory mCategory;

  private final MoveEventListener mGoalMoveListener;

  private final DeleteEventListener mGoalDeleteListener;

  private final ValidityPanel categoryValid;

  public ScoreCategoryEditor(final ScoreCategory scoreCategory) {
    mCategory = scoreCategory;
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    categoryValid = new ValidityPanel();
    add(categoryValid);

    final Box weightContainer = Box.createHorizontalBox();
    add(weightContainer);

    weightContainer.add(new JLabel("Weight: "));

    mWeight = FormatterUtils.createDoubleField();
    weightContainer.add(mWeight);
    weightContainer.add(Box.createHorizontalGlue());

    mWeight.addPropertyChangeListener("value", e -> {
      if (null != mCategory) {
        final Number value = (Number) mWeight.getValue();
        if (null != value) {
          final double newWeight = value.doubleValue();
          mCategory.setWeight(newWeight);
        }
      }
    });

    final Box buttonBox = Box.createHorizontalBox();
    add(buttonBox);

    final JButton addGoal = new JButton("Add Goal");
    buttonBox.add(addGoal);
    addGoal.addActionListener(l -> addNewGoal());

    final JButton addComputedGoal = new JButton("Add Computed Goal");
    buttonBox.add(addComputedGoal);
    addComputedGoal.addActionListener(l -> addNewComputedGoal());

    buttonBox.add(Box.createHorizontalGlue());

    mGoalEditorContainer = Box.createVerticalBox();
    add(mGoalEditorContainer);

    mGoalMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        final int oldIndex = Utilities.getIndexOfComponent(mGoalEditorContainer, e.getComponent());
        if (oldIndex < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of move event in goal container");
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
            || newIndex >= mGoalEditorContainer.getComponentCount()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: "
                + oldIndex
                + " newIndex: "
                + newIndex);
          }
          return;
        }

        // update editor list
        final AbstractGoalEditor editor = mGoalEditors.remove(oldIndex);
        mGoalEditors.add(newIndex, editor);

        // update the UI
        mGoalEditorContainer.add(e.getComponent(), newIndex);
        mGoalEditorContainer.validate();

        // update the order in the challenge description
        final AbstractGoal goal = mCategory.removeGoal(oldIndex);
        mCategory.addGoal(newIndex, goal);
      }
    };

    mGoalDeleteListener = new DeleteEventListener() {

      @Override
      public void requestDelete(final DeleteEvent e) {
        final int confirm = JOptionPane.showConfirmDialog(ScoreCategoryEditor.this,
                                                          "Are you sure that you want to delete the goal?",
                                                          "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
          return;
        }

        final int index = Utilities.getIndexOfComponent(mGoalEditorContainer, e.getComponent());
        if (index < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of delete event in goal container");
          }
          return;
        }

        // update editor list
        mGoalEditors.remove(index);

        // update the challenge description
        mCategory.removeGoal(index);

        // update the UI
        GuiUtils.removeFromContainer(mGoalEditorContainer, index);
      }
    };

    mWeight.setValue(mCategory.getWeight());

    mCategory.getGoals().forEach(this::addGoal);

  }

  /**
   * @return the category being edited, may be null if
   *         {@link #setCategory(ScoreCategory)} hasn't been called
   */
  public ScoreCategory getCategory() {
    return mCategory;
  }

  private void addNewGoal() {
    final String name = String.format("goal_%d", mCategory.getGoals().size());
    final String title = String.format("Goal %d", mCategory.getGoals().size());
    final Goal newGoal = new Goal(name);
    newGoal.setTitle(title);
    mCategory.addGoal(newGoal);
    addGoal(newGoal);
  }

  private void addNewComputedGoal() {
    final String name = String.format("goal_%d", mCategory.getGoals().size());
    final String title = String.format("Goal %d", mCategory.getGoals().size());
    final ComputedGoal newGoal = new ComputedGoal(name, mCategory);
    newGoal.setTitle(title);
    mCategory.addGoal(newGoal);
    addGoal(newGoal);
  }

  private void addGoal(final AbstractGoal goal) {

    final AbstractGoalEditor editor;
    if (goal instanceof Goal) {
      editor = new GoalEditor((Goal) goal);
    } else if (goal instanceof ComputedGoal) {
      editor = new ComputedGoalEditor((ComputedGoal) goal, mCategory);
    } else {
      throw new RuntimeException("Unexpected goal class: "
          + goal.getClass());
    }
    editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    final MovableExpandablePanel panel = new MovableExpandablePanel(goal.getTitle(), editor, true, true);
    editor.addPropertyChangeListener("title", e -> {
      final String newTitle = (String) e.getNewValue();
      panel.setTitle(newTitle);
    });
    panel.addMoveEventListener(mGoalMoveListener);
    panel.addDeleteEventListener(mGoalDeleteListener);

    mGoalEditors.add(editor);

    GuiUtils.addToContainer(mGoalEditorContainer, panel);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      mWeight.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes, assuming bad value and ignoring", e);
    }

    mGoalEditors.forEach(e -> e.commitChanges());
  }

  /**
   * Called by {@link #checkValidity()}. If the list is empty after the call, then
   * the goal is valid, otherwise the goal is invalid and the messages will be
   * displayed to the user.
   * Subclasses should override this to add extra checks. Make sure to call the
   * parent class method.
   * 
   * @param messages put invalid messages in the list.
   */
  protected void gatherValidityMessages(final Collection<String> messages) {

    final Set<String> goalNames = new HashSet<>();
    for (final AbstractGoalEditor editor : mGoalEditors) {
      final String name = editor.getGoal().getName();

      final boolean editorValid = editor.checkValidity(messages);
      if (!editorValid) {
        messages.add(String.format("Goal \"%s\" has invalid elements", name));
      }

      final boolean newName = goalNames.add(name);
      if (!newName) {
        messages.add(String.format("Goal names must be unique in a category, the name \"%s\" is used more than once",
                                   name));
      }
    }

    checkCircularReferences(messages);
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    final List<String> messages = new LinkedList<>();
    gatherValidityMessages(messages);

    if (!messages.isEmpty()) {
      categoryValid.setInvalid(String.join("<br/>", messages));
      return false;
    } else {
      categoryValid.setValid();
      return true;
    }
  }

  private static void getReferencedComputedGoals(final BasicPolynomial poly,
                                                 final Set<ComputedGoal> referenced) {
    if (null == poly) {
      return;
    }

    for (final Term t : poly.getTerms()) {
      for (final GoalRef gr : t.getGoals()) {
        getReferencedComputedGoals(gr, referenced);
      } // foreach goal
    } // foreach term
  }

  private static void getReferencedComputedGoals(final CaseStatement caseStatement,
                                                 final Set<ComputedGoal> referenced) {
    if (null == caseStatement) {
      return;
    }

    getReferencedComputedGoals(caseStatement.getCondition(), referenced);

    if (null != caseStatement.getResultPoly()) {
      getReferencedComputedGoals(caseStatement.getResultPoly(), referenced);
    }
    if (null != caseStatement.getResultSwitch()) {
      getReferencedComputedGoals(caseStatement.getResultSwitch(), referenced);
    }

  }

  private static void getReferencedComputedGoals(final GoalRef gr,
                                                 final Set<ComputedGoal> referenced) {
    try {
      final AbstractGoal goal = gr.getGoal();
      if (goal.isComputed()) {
        referenced.add((ComputedGoal) goal);
      }
    } catch (final ScopeException e) {
      // handled elsewhere
      LOGGER.debug("Referenced goal doesn't exist: "
          + gr.getGoalName());
    }
  }

  private static void getReferencedComputedGoals(final AbstractConditionStatement cond,
                                                 final Set<ComputedGoal> referenced) {
    if (null == cond) {
      return;
    }

    if (cond instanceof ConditionStatement) {
      final ConditionStatement c = (ConditionStatement) cond;
      getReferencedComputedGoals(c.getLeft(), referenced);
      getReferencedComputedGoals(c.getRight(), referenced);
    } else if (cond instanceof EnumConditionStatement) {
      final EnumConditionStatement c = (EnumConditionStatement) cond;
      getReferencedComputedGoals(c.getLeftGoalRef(), referenced);
      getReferencedComputedGoals(c.getRightGoalRef(), referenced);
    } else {
      throw new IllegalArgumentException("Unknown condition type: "
          + cond.getClass());
    }
  }

  private static void getReferencedComputedGoals(final SwitchStatement sw,
                                                 final Set<ComputedGoal> referenced) {
    if (null == sw) {
      return;
    }

    for (final CaseStatement cstmt : sw.getCases()) {
      getReferencedComputedGoals(cstmt, referenced);
    }

    getReferencedComputedGoals(sw.getDefaultCase(), referenced);
  }

  /**
   * Find all computed goals that are directly referenced by the source goal.
   * 
   * @param source the source goal
   * @return the referenced goals
   */
  private static Set<ComputedGoal> getReferencedComputedGoals(final ComputedGoal source) {
    final Set<ComputedGoal> referenced = new HashSet<>();

    getReferencedComputedGoals(source.getSwitch(), referenced);

    for (final Variable var : source.getVariables()) {
      getReferencedComputedGoals(var, referenced);
    }

    return referenced;
  }

  private static boolean findCircularReferences(final ComputedGoal next,
                                                final List<ComputedGoal> visited,
                                                final Map<ComputedGoal, Set<ComputedGoal>> referenceMap,
                                                final Collection<String> messages) {
    final List<ComputedGoal> newVisited = new LinkedList<>(visited);
    newVisited.add(next);

    final Set<ComputedGoal> refs = referenceMap.getOrDefault(next, Collections.emptySet());
    for (final ComputedGoal check : refs) {
      if (newVisited.contains(check)) {
        newVisited.add(check); // so that the name gets in the list for the message
        final List<String> newVisitedNames = newVisited.stream().map(ComputedGoal::getName)
                                                       .collect(Collectors.toList());
        final String chainMessage = String.join(" -> ", newVisitedNames);
        final String message = "Found circular references: "
            + chainMessage;
        messages.add(message);
        return true;
      } else {
        final boolean result = findCircularReferences(check, newVisited, referenceMap, messages);
        if (result) {
          return true;
        }
      }
    }

    // no issues
    return false;
  }

  private void checkCircularReferences(final Collection<String> messages) {

    // compute direct references
    final Map<ComputedGoal, Set<ComputedGoal>> referenceMap = new HashMap<>();
    for (final AbstractGoal ag : mCategory.getGoals()) {
      if (ag.isComputed()) {
        final ComputedGoal goal = (ComputedGoal) ag;
        final Set<ComputedGoal> refs = getReferencedComputedGoals(goal);
        referenceMap.put(goal, refs);
      }
    }

    // walk the references to find circular dependencies
    referenceMap.forEach((goal,
                          ignored) -> {
      findCircularReferences(goal, Collections.emptyList(), referenceMap, messages);
    });

  }

}
