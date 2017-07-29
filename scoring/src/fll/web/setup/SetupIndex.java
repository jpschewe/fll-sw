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

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;
import net.mtu.eggplant.util.ComparisonUtils;

/**
 * Utilities for /setup/index.jsp.
 */
public class SetupIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Populate the page context with information for the jsp.
   * pageContext:
   * descriptions - List<DescriptionInfo> (sorted by title)
   * dbinitialized - boolean if the database has been initialized
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {

    final List<DescriptionInfo> descriptions = DescriptionInfo.getAllKnownChallengeDescriptionInfo();

    pageContext.setAttribute("descriptions", descriptions);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (final Connection connection = datasource.getConnection()) {
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
      final List<DescriptionInfo> descriptions = new LinkedList<DescriptionInfo>();

      final Collection<URL> urls = XMLUtils.getAllKnownChallengeDescriptorURLs();
      for (final URL url : urls) {
        try (final InputStream stream = url.openStream()) {
          try (final Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET)) {
            final Document document = ChallengeParser.parse(reader);

            final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

            descriptions.add(new DescriptionInfo(url, description));
          }
        } catch (final IOException e) {
          LOGGER.error("Error reading description: "
              + url.toString(), e);
        }
      }

      Collections.sort(descriptions);

      return descriptions;
    }

    public DescriptionInfo(final URL url,
                           final ChallengeDescription description) {
      mUrl = url;
      mDescription = description;
    }

    private final URL mUrl;

    public URL getURL() {
      return mUrl;
    }

    private final ChallengeDescription mDescription;

    public ChallengeDescription getDescription() {
      return mDescription;
    }

    public String getTitle() {
      return mDescription.getTitle();
    }

    public String getRevision() {
      return mDescription.getRevision();
    }

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

        final int titleCompare = ComparisonUtils.compareStrings(oneTitle, twoTitle);
        if (0 == titleCompare) {
          return ComparisonUtils.compareStrings(oneRevision, twoRevision);
        } else {
          return titleCompare;
        }

      }
    }

    @Override
    public boolean equals(final Object o) {
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
