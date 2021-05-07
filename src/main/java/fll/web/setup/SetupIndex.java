/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.setup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;

/**
 * Utilities for /setup/index.jsp.
 */
public final class SetupIndex {

  private SetupIndex() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Populate the page context with information for the jsp.
   * Variables added to the pageContext:
   * <ul>
   * <li>descriptions - {@link List} of {@link DescriptionInfo} (sorted by
   * title)</li>
   * <li>dbinitialized - boolean if the database has been initialized</li>
   * </ul>
   * 
   * @param application application variable access
   * @param pageContext page variable access
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {

    final List<DescriptionInfo> descriptions = DescriptionInfo.getAllKnownChallengeDescriptionInfo();

    pageContext.setAttribute("descriptions", descriptions);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      pageContext.setAttribute("dbinitialized", Utilities.testDatabaseInitialized(connection));
    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }
  }

  /**
   * Information to display about a challenge description.
   */
  public static final class DescriptionInfo implements Comparable<DescriptionInfo> {

    /**
     * Get some information about the known challenge descriptions.
     *
     * @return list sorted by name and then revision
     */
    public static List<DescriptionInfo> getAllKnownChallengeDescriptionInfo() {
      final List<DescriptionInfo> descriptions = new LinkedList<>();

      final Collection<URL> urls = ChallengeParser.getAllKnownChallengeDescriptorURLs();
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
    @Nonnull
    public String getTitle() {
      return mDescription.getTitle();
    }

    /**
     * @return {@link ChallengeDescription#getRevision()}
     */
    @Nonnull
    public String getRevision() {
      return mDescription.getRevision();
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

  }

}
