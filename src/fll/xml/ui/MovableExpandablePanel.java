/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.util.EventListener;
import java.util.EventObject;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

/**
 * Panel that has move up and move down buttons with a title and allows the main
 * component to be hidden.
 */
public class MovableExpandablePanel extends JPanel {

  private boolean mExpanded = false;

  private final JLabel mTitleLabel;

  /**
   * @param title the title to display
   * @param view the component to display
   * @param movable if the move buttons should be visible. See
   *          {@link #addMoveEventListener(MoveEventListener)} to get notified of
   *          the move request
   * @param deletable if the delete button should be visible. See
   *          {@link #addDeleteEventListener(DeleteEventListener)} to get notified
   *          of the delete request
   */
  public MovableExpandablePanel(final String title,
                                final JComponent view,
                                final boolean movable,
                                final boolean deletable) {
    super(new BorderLayout());

    final JComponent top = Box.createHorizontalBox();
    add(top, BorderLayout.NORTH);

    mTitleLabel = new JLabel(title);
    top.add(mTitleLabel);

    final JButton expand = new JButton("+");
    top.add(expand);
    expand.addActionListener(e -> {
      mExpanded = !mExpanded;
      view.setVisible(mExpanded);
      MovableExpandablePanel.this.validate();

      if (mExpanded) {
        expand.setText("-");
      } else {
        expand.setText("+");
      }
    });

    if (movable) {
      final JButton moveUp = new JButton("Move Up");
      top.add(moveUp);
      moveUp.addActionListener(e -> {
        fireMoveEventListener(MoveEvent.MoveDirection.UP);
      });

      final JButton moveDown = new JButton("Move Down");
      top.add(moveDown);
      moveDown.addActionListener(e -> {
        fireMoveEventListener(MoveEvent.MoveDirection.DOWN);
      });
    }

    if (deletable) {
      final JButton delete = new JButton("Delete");
      top.add(delete);
      delete.addActionListener(e -> {
        fireDeleteEventListener();
      });
    }

    add(view, BorderLayout.CENTER);
    view.setVisible(mExpanded);
  }

  /**
   * Used to notify one that the widget should be moved.
   */
  public static class MoveEvent extends EventObject {

    /**
     * The direction to move the widget.
     */
    public enum MoveDirection {
    /**
     * Move the widget up.
     */
    UP,
    /**
     * Move the widget down.
     */
    DOWN
    }

    private final MoveDirection mDirection;

    /**
     * Create a new event.
     * 
     * @param source see {@link #getComponent()}
     * @param direction see {@link #getDirection()}
     */
    public MoveEvent(final JComponent source,
                     final MoveDirection direction) {
      super(source);
      mDirection = direction;
    }

    /**
     * @return the source as a {@link JComponent}
     * @see #getSource()
     */
    public JComponent getComponent() {
      return (JComponent) getSource();
    }

    /**
     * @return the direction to move the widget
     */
    public MoveDirection getDirection() {
      return mDirection;
    }
  }

  /**
   * Listen for move events.
   */
  public static interface MoveEventListener extends EventListener {
    /**
     * The component should be moved.
     */
    public void requestedMove(final MoveEvent e);

  }

  private final EventListenerList mListeners = new EventListenerList();

  /**
   * @param l the listener to add
   */
  public void addMoveEventListener(final MoveEventListener l) {
    mListeners.add(MoveEventListener.class, l);
  }

  /**
   * @param l the listener to remove
   */
  public void removeMoveEventListener(final MoveEventListener l) {
    mListeners.remove(MoveEventListener.class, l);
  }

  /**
   * Notify listeners of a requested move.
   * 
   * @param direction which direction the component should move
   */
  protected void fireMoveEventListener(final MoveEvent.MoveDirection direction) {
    final MoveEvent event = new MoveEvent(this, direction);

    for (final MoveEventListener l : mListeners.getListeners(MoveEventListener.class)) {
      l.requestedMove(event);
    }
  }

  /**
   * Used to notify one that the widget should be deleted.
   */
  public static class DeleteEvent extends EventObject {

    /**
     * Create a new event.
     * 
     * @param source see {@link #getComponent()}
     */
    public DeleteEvent(final JComponent source) {
      super(source);
    }

    /**
     * @return the source as a {@link JComponent}
     * @see #getSource()
     */
    public JComponent getComponent() {
      return (JComponent) getSource();
    }
  }

  /**
   * Listen for delete events.
   */
  public static interface DeleteEventListener extends EventListener {
    /**
     * The component should be deleted.
     */
    public void requestDelete(final DeleteEvent e);

  }

  public void addDeleteEventListener(final DeleteEventListener l) {
    mListeners.add(DeleteEventListener.class, l);
  }

  public void removeDeleteEventListener(final DeleteEventListener l) {
    mListeners.remove(DeleteEventListener.class, l);
  }

  protected void fireDeleteEventListener() {
    final DeleteEvent event = new DeleteEvent(this);

    for (final DeleteEventListener l : mListeners.getListeners(DeleteEventListener.class)) {
      l.requestDelete(event);
    }
  }

  /**
   * @param text the new title text
   */
  public void setTitle(final String text) {
    mTitleLabel.setText(text);
  }

}
