/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem.list;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A message to tell the list module to display.
 */
public class Show extends BaseListMessage {
  /**
   * @param node {@link #getNode()}
   */
  public Show(final String node) {
    super(node, "show");
  }

  /**
   * @return null as there is no data for this message
   */
  @Override
  public @Nullable Object getData() {
    return null;
  }

}
