/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import javax.swing.JLabel;

/**
 * Drag handle to be used with various classes.
 */
/* package */ class DragHandle extends JLabel {

  public DragHandle(final AbstractGoalEditor editor) {
    super("handle");
    mEditor = editor;
  }
  
  // FIXME will need a different type at some point to support being used with other classes
  private final AbstractGoalEditor mEditor;
  
  //FIXME will need some information about where can be dropped...
  
  

}
