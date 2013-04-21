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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

/**
 * Utilities for /setup/index.jsp.
 */
public class SetupIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Populate the page context with information for the jsp.
   * pageContext:
   * descriptions - List<DescriptionInfo> (sorted by title)
   */
  public static void populateContext(final PageContext pageContext) {

    final List<DescriptionInfo> descriptions = new LinkedList<DescriptionInfo>();

    final Collection<URL> urls = XMLUtils.getAllKnownChallengeDescriptorURLs();
    for (final URL url : urls) {
      try {
        final InputStream stream = url.openStream();
        final Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET);
        final Document document = ChallengeParser.parse(reader);
        reader.close();
        final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

        final String title = description.getTitle();
        descriptions.add(new DescriptionInfo(url, title));

      } catch (final IOException e) {
        LOGGER.error("Error reading description: "
            + url.toString(), e);
      }
    }

    Collections.sort(descriptions);    
    pageContext.setAttribute("descriptions", descriptions);
  }

  /**
   * Information to display about a challenge description.
   */
  public static final class DescriptionInfo implements Comparable<DescriptionInfo> {
    public DescriptionInfo(final URL url,
                           final String title) {
      mUrl = url;
      mTitle = title;
    }

    private final URL mUrl;

    public URL getURL() {
      return mUrl;
    }

    private final String mTitle;

    public String getTitle() {
      return mTitle;
    }

    public int compareTo(final DescriptionInfo other) {
      if (null == other) {
        return 1;
      } else if (this == other) {
        return 0;
      } else {
        return getTitle().compareTo(other.getTitle());
      }
    }

  }
}
