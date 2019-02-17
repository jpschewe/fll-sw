/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.Collection;

/**
 * Common interface for objects that can be validated.
 */
/* package */ interface Validatable {

  /**
   * Check if the object is valid. This method should not use short-circuit logic
   * when checking contained objects. This ensures that the objects that
   * display the validity state can be updated.
   * <p>
   * The messages parameter is to be populated with any messages that the caller
   * is to display. If the object being validated is displaying messages, it
   * should not add those messages to the messages parameter to keep from
   * displaying the same messages in multiple places.
   * </p>
   * 
   * @param messagesToDisplay the messages to be displayed by the caller
   * @return if the object is valid
   */
  boolean checkValidity(Collection<String> messagesToDisplay);

}
