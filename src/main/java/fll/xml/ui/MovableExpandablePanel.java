/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.EventObject;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

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

      final JButton moveDown = new JButton("Move Down");
      top.add(moveDown);

      new MovableActionListener(this, moveUp, moveDown);
    }

    if (deletable) {
      final JButton delete = new JButton("Delete");
      top.add(delete);

      new DeleteActionListener(this, delete);
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
  public interface MoveEventListener extends EventListener {
    /**
     * The component should be moved.
     *
     * @param e the event showing what is being moved
     */
    void requestedMove(MoveEvent e);

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
  public interface DeleteEventListener extends EventListener {
    /**
     * The component should be deleted.
     *
     * @param e the event specifying information about the delete request
     */
    void requestDelete(DeleteEvent e);

  }

  /**
   * @param l the listener to add
   */
  public void addDeleteEventListener(final DeleteEventListener l) {
    mListeners.add(DeleteEventListener.class, l);
  }

  /**
   * @param l the listener to remove
   */
  public void removeDeleteEventListener(final DeleteEventListener l) {
    mListeners.remove(DeleteEventListener.class, l);
  }

  /**
   * Send a notification about a delete event.
   */
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

  private static final class MovableActionListener implements ActionListener {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    private final @NotOnlyInitialized MovableExpandablePanel expandPanel;

    private final JButton moveUp;

    private final JButton moveDown;

    private MovableActionListener(final @UnknownInitialization(MovableExpandablePanel.class) MovableExpandablePanel expandPanel,
                                  final JButton moveUp,
                                  final JButton moveDown) {
      this.expandPanel = expandPanel;
      this.moveUp = moveUp;
      this.moveDown = moveDown;
      this.moveUp.addActionListener(this);
      this.moveDown.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final Object source = ae.getSource();
      if (moveUp.equals(source)) {
        expandPanel.fireMoveEventListener(MoveEvent.MoveDirection.UP);
      } else if (moveDown.equals(source)) {
        expandPanel.fireMoveEventListener(MoveEvent.MoveDirection.DOWN);
      } else {
        LOGGER.warn("Got unknown event source of {}, ignoring", source);
      }
    }
  } // MovableActionListener

  private static final class DeleteActionListener implements ActionListener {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    private final @NotOnlyInitialized MovableExpandablePanel expandPanel;

    private final JButton delete;

    private DeleteActionListener(final @UnknownInitialization(MovableExpandablePanel.class) MovableExpandablePanel expandPanel,
                                 final JButton delete) {
      this.expandPanel = expandPanel;
      this.delete = delete;
      this.delete.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
      final Object source = ae.getSource();
      if (delete.equals(source)) {
        expandPanel.fireDeleteEventListener();
      } else {
        LOGGER.warn("Got unknown event source of {}, ignoring", source);
      }
    }
  } // DeleteActionListener

}
