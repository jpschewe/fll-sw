/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import org.apache.commons.io.IOUtils;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.tomcat.TomcatLauncher;
import fll.util.FLLInternalException;
import fll.web.Welcome;

/**
 * Access to various images used by the software.
 */
public final class UserImages {

  private UserImages() {
  }

  private static final String DIRECTORY_NAME = "user-images";

  /**
   * @return path to the images directory
   */
  public static Path getImagesPath() {
    return Paths.get(DIRECTORY_NAME);
  }

  /**
   * Use the default partner logo replacing whatever logo the user has provided.
   */
  public static void useDefaultPartnerLogo() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      throw new FLLInternalException("Unable to find web server root directory");
    }

    final Path partnerLogo = UserImages.getImagesPath().resolve(Welcome.PARTNER_LOGO_FILENAME);
    final Path partnerDefaultLogo = webroot.resolve("images").resolve("htk_logo.jpg");
    if (!Files.exists(partnerDefaultLogo)) {
      throw new FLLInternalException("Unable to find default partner logo: "
          + partnerDefaultLogo.toAbsolutePath().toString());
    }
    try {
      Files.copy(partnerDefaultLogo, partnerLogo, StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException e) {
      throw new FLLInternalException("Error copying default partner logo", e);
    }
  }

  /**
   * Get the partner logo as a base64 encoded string to be used in PDF documents.
   * 
   * @return base64 encoded version of the image
   */
  public static String getPartnerLogoAsBase64() {
    final Base64.Encoder encoder = Base64.getEncoder();

    final Path partnerLogo = UserImages.getImagesPath().resolve(Welcome.PARTNER_LOGO_FILENAME);
    try (InputStream input = Files.newInputStream(partnerLogo)) {
      final String encoded = encoder.encodeToString(IOUtils.toByteArray(input));
      return encoded;
    } catch (final IOException e) {
      throw new FLLInternalException("Unable to read partner logo", e);
    }
  }

  /**
   * Use the default FLL logo replacing whatever logo the user has provided.
   */
  public static void useDefaultFllLogo() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      throw new FLLInternalException("Unable to find web server root directory");
    }

    final Path fllLogo = UserImages.getImagesPath().resolve(Welcome.FLL_LOGO_FILENAME);
    final Path fllDefaultLogo = webroot.resolve("images").resolve("fll_logo.jpg");
    if (!Files.exists(fllDefaultLogo)) {
      throw new FLLInternalException("Unable to find default FLL logo: "
          + fllDefaultLogo.toAbsolutePath().toString());
    }
    try {
      Files.copy(fllDefaultLogo, fllLogo, StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException e) {
      throw new FLLInternalException("Error copying default FLL logo", e);
    }
  }

  /**
   * Name of file in user images that contains the fll logo used for the
   * subjective score sheets.
   */
  public static final String FLL_SUBJECTIVE_LOGO_FILENAME = "fll_logo_subjective.jpg";

  /**
   * Replace any user provided FLL subjective logo with the default one in the
   * software.
   */
  public static void useDefaultFllSubjectiveLogo() {
    final Path fllSubjectiveLogo = UserImages.getImagesPath().resolve(FLL_SUBJECTIVE_LOGO_FILENAME);

    final ClassLoader loader = castNonNull(UserImages.class.getClassLoader());
    try (InputStream input = loader.getResourceAsStream("fll/resources/documents/FLLHeader.jpg")) {
      if (null == input) {
        throw new FLLInternalException("Unable to find default FLL subjective logo");
      }
      Files.copy(input, fllSubjectiveLogo, StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException e) {
      throw new FLLInternalException("Error copying default FLL logo", e);
    }
  }

  /**
   * Name of file in user images that contains the challenge logo.
   */
  public static final String CHALLENGE_LOGO_FILENAME = "challenge_logo.jpg";

  /**
   * Replace any user provided challenge logo with the default one in the
   * software.
   */
  public static void useDefaultChallengeLogo() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      throw new FLLInternalException("Unable to find web server root directory");
    }

    final Path challengeLogo = UserImages.getImagesPath().resolve(CHALLENGE_LOGO_FILENAME);
    final Path challengeDefaultLogo = webroot.resolve("images").resolve("default_challenge_logo.jpg");
    if (!Files.exists(challengeDefaultLogo)) {
      throw new FLLInternalException("Unable to find default challenge logo: "
          + challengeDefaultLogo.toAbsolutePath().toString());
    }
    try {
      Files.copy(challengeDefaultLogo, challengeLogo, StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException e) {
      throw new FLLInternalException("Error copying default challenge logo", e);
    }
  }

}
