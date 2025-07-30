/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;
import fll.util.FLLRuntimeException;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Information about challenge descriptions.
 */
public final class DescriptionInfo implements Comparable<DescriptionInfo> {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Get some information about the known challenge descriptions.
   *
   * @return list sorted by name and then revision
   */
  public static List<DescriptionInfo> getAllKnownChallengeDescriptionInfo() {
    final List<DescriptionInfo> descriptions = new LinkedList<>();

    final Collection<URL> urls = getAllKnownChallengeDescriptorURLs();
    for (final URL url : urls) {
      try (InputStream stream = url.openStream()) {
        try (Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET)) {
          final ChallengeDescription description = ChallengeParser.parse(reader);

          descriptions.add(new DescriptionInfo(url, description));
        }
      } catch (final IOException e) {
        LOGGER.error("I/O Error reading description: "
            + url.toString(), e);
      } catch (final RuntimeException e) {
        LOGGER.error("Error reading description: "
            + url.toString(), e);
        throw e;
      }
    }

    Collections.sort(descriptions);

    return descriptions;
  }

  /**
   * @param url {@link #getURL()}
   * @param description {@link #getDescription()}
   */
  public DescriptionInfo(final URL url,
                         final ChallengeDescription description) {
    mUrl = url;
    mDescription = description;
  }

  private final URL mUrl;

  /**
   * @return where the challenge description was found
   */
  public URL getURL() {
    return mUrl;
  }

  private final ChallengeDescription mDescription;

  /**
   * @return the challenge description
   */
  public ChallengeDescription getDescription() {
    return mDescription;
  }

  /**
   * @return {@link ChallengeDescription#getTitle()}
   */
  public String getTitle() {
    return mDescription.getTitle();
  }

  /**
   * @return {@link ChallengeDescription#getRevision()}
   */
  public String getRevision() {
    return mDescription.getRevision();
  }

  /**
   * @return the string that should be displayed to the user.
   */
  public String getDisplay() {
    final String revision = getRevision();
    if (!StringUtils.isBlank(revision)) {
      return String.format("%s (%s)", getTitle(), revision);
    } else {
      return getTitle();
    }
  }

  @Override
  public int compareTo(final DescriptionInfo other) {
    if (null == other) {
      return 1;
    } else if (this == other) {
      return 0;
    } else {

      final String oneTitle = getTitle();
      final String twoTitle = other.getTitle();
      final String oneRevision = getRevision();
      final String twoRevision = other.getRevision();

      final int titleCompare = StringUtils.compare(oneTitle, twoTitle);
      if (0 == titleCompare) {
        return StringUtils.compare(oneRevision, twoRevision);
      } else {
        return titleCompare;
      }

    }
  }

  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (this == o) {
      return true;
    } else if (null == o) {
      return false;
    } else if (o.getClass().equals(DescriptionInfo.class)) {
      final DescriptionInfo other = (DescriptionInfo) o;
      return getTitle().equals(other.getTitle());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getTitle().hashCode();
  }

  /**
   * Given a url, find the corresponding known challenge description URL.
   * 
   * @param url a string representation of a known challenge description URL
   * @return the URL
   * @throws FLLRuntimeException if the URL doesn't match a known challenge
   *           description
   */
  public static URL getKnownChallengeUrl(final String url) {
    final Collection<URL> knownDescriptions = getAllKnownChallengeDescriptorURLs();
    final Optional<URL> found = knownDescriptions.stream().filter(u -> url.equals(u.toString())).findAny();
    if (!found.isPresent()) {
      throw new FLLRuntimeException(String.format("There is no known challenge description with the URL %s", url));
    }

    return found.get();
  }

  /**
   * Get all challenge descriptors built into the software.
   *
   * @return a collection of all URLs to the challenge descriptions in the
   *         software
   */
  public static Collection<URL> getAllKnownChallengeDescriptorURLs() {
    LOGGER.trace("Top of getAllKnownChallengeDescriptorURLs");

    final String baseDir = "fll/resources/challenge-descriptors/";

    final ClassLoader classLoader = Utilities.getClassLoader();
    final URL directory = classLoader.getResource(baseDir);
    if (null == directory) {
      LOGGER.warn("base dir for challenge descriptors not found");
      return Collections.emptyList();
    }

    LOGGER.trace("Found challenge descriptors directory as {}", directory);

    final Collection<URL> urls = new LinkedList<>();
    if ("file".equals(directory.getProtocol())) {
      LOGGER.trace("Using file protocol");

      try {
        final URI uri = directory.toURI();
        final File fileDir = new File(uri);
        final File[] files = fileDir.listFiles();
        if (null != files) {
          for (final File file : files) {
            if (file.getName().endsWith(".xml")) {
              try {
                final URL fileUrl = file.toURI().toURL();
                urls.add(fileUrl);
              } catch (final MalformedURLException e) {
                LOGGER.error("Unable to convert file to URL: "
                    + file.getAbsolutePath(), e);
              }
            }
          }
        }
      } catch (final URISyntaxException e) {
        LOGGER.error("Unable to convert URL to URI: "
            + e.getMessage(), e);
      }
    } else if (directory.getProtocol().equals("jar")) {
      LOGGER.trace("Using jar protocol");

      final CodeSource src = XMLUtils.class.getProtectionDomain().getCodeSource();
      if (null != src) {
        final URL jar = src.getLocation();
        LOGGER.trace("src location {}", jar);

        try (JarInputStream zip = new JarInputStream(jar.openStream())) {
          JarEntry ze = null;
          while ((ze = zip.getNextJarEntry()) != null) {
            final String entryName = ze.getName();
            if (entryName.startsWith(baseDir)
                && entryName.endsWith(".xml")) {
              // add 1 to baseDir to skip past the path separator
              final String challengeName = entryName.substring(baseDir.length());

              // check that the file really exists and turn it into a URL
              final URL challengeUrl = classLoader.getResource(baseDir
                  + challengeName);
              if (null != challengeUrl) {
                urls.add(challengeUrl);
              } else {
                // TODO could write the resource out to a temporary file if
                // needed
                // then mark the file as delete on exit
                LOGGER.warn("URL doesn't exist for "
                    + baseDir
                    + challengeName
                    + " entry: "
                    + entryName);
              }
            }
          }

        } catch (final IOException e) {
          LOGGER.error("Error reading jar file at: "
              + jar.toString(), e);
        }

      } else {
        LOGGER.warn("Null code source in protection domain, cannot get challenge descriptors");
      }
    } else {
      throw new UnsupportedOperationException("Cannot list files for URL "
          + directory);

    }

    return urls;

  }

}