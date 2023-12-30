/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Access to various images used by the software.
 */
public final class UserImages {

  private UserImages() {
    // TODO Auto-generated constructor stub
  }

  private static final String DIRECTORY_NAME = "user-images";

  /**
   * @return path to the images directory
   */
  public static Path getImagesPath() {
    return Paths.get(DIRECTORY_NAME);
  }

}
