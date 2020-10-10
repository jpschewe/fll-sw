/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

/**
 * Panel to show validity information.
 */
final class ValidityPanel extends JPanel {

  private static final Color UNKNOWN = new Color(0, 0, 0, 0);

  private static final Color VALID = Color.GREEN;

  private static final Color INVALID = Color.RED;

  private static final int MIN_WIDTH = 10;

  private static final int MIN_HEIGHT = 10;

  /**
   * Default constructor sets state to unknown.
   */
  ValidityPanel() {
    setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
    setUnknown();
  }

  /**
   * State that the validity is unknown.
   */
  public void setUnknown() {
    setBackground(UNKNOWN);
    setToolTipText(null);
  }

  /**
   * State that the object is valid.
   */
  public void setValid() {
    setBackground(VALID);
    setToolTipText(null);
  }

  /**
   * State that the object is invalid.
   * 
   * @param message why is the object invalid, this may contain HTML markup
   */
  public void setInvalid(final String message) {
    setBackground(INVALID);
    setToolTipText(String.format("<html>%s</html>", message));
  }

}
