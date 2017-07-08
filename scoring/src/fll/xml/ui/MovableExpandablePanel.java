/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.EventListener;
import java.util.EventObject;

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
   * Creates a panel that is movable.
   * 
   * @see #MovableExpandablePanel(String, JComponent, boolean)
   */
  public MovableExpandablePanel(final String title,
                                final JComponent view) {
    this(title, view, true);
  }

  /**
   * @param title the title to display
   * @param view the component to display
   * @param movable if the move buttons should be visible
   */
  public MovableExpandablePanel(final String title,
                                final JComponent view,
                                final boolean movable) {
    super(new BorderLayout());

    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
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

    add(view, BorderLayout.CENTER);
    view.setVisible(mExpanded);
  }

  public static class MoveEvent extends EventObject {
    public enum MoveDirection {
      UP, DOWN
    }

    private final MoveDirection mDirection;
    
    public MoveEvent(final JComponent source,
                     final MoveDirection direction) {
      super(source);
      mDirection = direction;
    }

    public JComponent getComponent() {
      return (JComponent) getSource();
    }

    public MoveDirection getDirection() {
      return mDirection;
    }
  }

  public static interface MoveEventListener extends EventListener {
    /**
     * The component should be moved.
     */
    public void requestedMove(final MoveEvent e);

  }

  private final EventListenerList mListeners = new EventListenerList();

  public void addMoveEventListener(final MoveEventListener l) {
    mListeners.add(MoveEventListener.class, l);
  }

  public void removeMoveEventListener(final MoveEventListener l) {
    mListeners.remove(MoveEventListener.class, l);
  }

  protected void fireMoveEventListener(final MoveEvent.MoveDirection direction) {
    final MoveEvent event = new MoveEvent(this, direction);
    
    for (final MoveEventListener l : mListeners.getListeners(MoveEventListener.class)) {
      l.requestedMove(event);
    }
  }
  
  /**
   * 
   * @param text the new title text
   */
  public void setTitle(final String text) {
    mTitleLabel.setText(text);
  }

}
