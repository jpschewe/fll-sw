/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.Container;

import javax.swing.JComponent;

/**
 * Some functions for working with the user interface components.
 */
public final class GuiUtils {

  private GuiUtils() {
  }

  /**
   * Add element to container and ensure that the UI is updated so that the
   * component appears.
   * 
   * @param container the container to add to
   * @param element the element to add to the container
   */
  public static void addToContainer(final JComponent container,
                                    final JComponent element) {
    container.add(element);
    final Container topAncestor = container.getTopLevelAncestor();
    if (null != topAncestor) {
      topAncestor.validate();
    }
  }

  /**
   * Remove element from container and ensure that the UI is updated so that the
   * component appears.
   * 
   * @param container the container to remove from
   * @param element the element to remove from the container
   */
  public static void removeFromContainer(final JComponent container,
                                         final JComponent element) {
    container.remove(element);
    final Container topAncestor = container.getTopLevelAncestor();
    if (null != topAncestor) {
      topAncestor.validate();
    }
  }

  /**
   * Remove element by index from container and ensure that the UI is updated so
   * that the
   * component appears.
   * 
   * @param container the container to remove from
   * @param index the index of the element to remove
   */
  public static void removeFromContainer(final JComponent container,
                                         final int index) {
    container.remove(index);
    final Container topAncestor = container.getTopLevelAncestor();
    if (null != topAncestor) {
      topAncestor.validate();
    }
  }

}
