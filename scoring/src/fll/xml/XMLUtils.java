/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import net.mtu.eggplant.io.IOUtils;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fll.util.LogUtils;

/**
 * Generate some XML documents.
 */
public final class XMLUtils {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  private XMLUtils() {
  }

  /**
   * Find a subjective category by name.
   * 
   * @param challengeDocument the document to look in
   * @param name the name to look for
   * @return the element or null if one is not found
   */
  public static Element getSubjectiveCategoryByName(final Document challengeDocument,
                                                    final String name) {
    for (final Element categoryElement : new NodelistElementCollectionAdapter(
                                                                              challengeDocument.getDocumentElement()
                                                                                               .getElementsByTagName("subjectiveCategory"))) {
      final String categoryName = categoryElement.getAttribute("name");
      if (categoryName.equals(name)) {
        return categoryElement;
      }
    }
    return null;
  }

  /**
   * Check if an element describes an enumerated goal or not.
   * 
   * @param element the goal element
   * @return if the element represents an enumerated goal
   */
  public static boolean isEnumeratedGoal(final Element element) {
    if (!"goal".equals(element.getNodeName())) {
      // not a goal element
      return false;
    }

    final Iterator<Element> values = new NodelistElementCollectionAdapter(element.getElementsByTagName("value")).iterator();
    return values.hasNext();
  }

  /**
   * Check if an element describes a computed goal or not.
   * 
   * @param element the goal element
   * @return if the element represents a computed goal
   */
  public static boolean isComputedGoal(final Element element) {
    return "computedGoal".equals(element.getNodeName());
  }

  /**
   * Get the bracket sort type from the document. If the attribute doesn't
   * exist, then return {@link BracketSortType#SEEDING}.
   */
  public static BracketSortType getBracketSort(final Document challengeDocument) {
    final Element root = challengeDocument.getDocumentElement();
    if (root.hasAttribute("bracketSort")) {
      final String sortStr = root.getAttribute("bracketSort");
      if (null == sortStr) {
        return BracketSortType.SEEDING;
      } else {
        return Enum.valueOf(BracketSortType.class, sortStr);
      }
    } else {
      return BracketSortType.SEEDING;
    }
  }

  /**
   * Get the winner criteria for the tournament.
   */
  public static WinnerType getWinnerCriteria(final Document challengeDocument) {
    final Element root = challengeDocument.getDocumentElement();
    return getWinnerCriteria(root);
  }

  /**
   * Get the winner criteria for a particular element.
   */
  public static WinnerType getWinnerCriteria(final Element element) {
    if (element.hasAttribute("winner")) {
      final String str = element.getAttribute("winner");
      final String sortStr;
      if (null != str) {
        sortStr = str.toUpperCase();
      } else {
        sortStr = "HIGH";
      }
      return Enum.valueOf(WinnerType.class, sortStr);
    } else {
      return WinnerType.HIGH;
    }
  }

  /**
   * Get the score type for a particular element.
   */
  public static ScoreType getScoreType(final Element element) {
    if (element.hasAttribute("scoreType")) {
      final String str = element.getAttribute("scoreType");
      final String sortStr;
      if (null != str) {
        sortStr = str.toUpperCase();
      } else {
        sortStr = "INTEGER";
      }
      return Enum.valueOf(ScoreType.class, sortStr);
    } else {
      return ScoreType.INTEGER;
    }
  }

  /**
   * @see #getDoubleAttributeValue(Element, String)
   */
  public static String getStringAttributeValue(final Element element,
                                               final String attributeName) {
    if (null == element) {
      return null;
    }
    final String str = element.getAttribute(attributeName);
    return str;
  }

  /**
   * @see #getDoubleAttributeValue(Element, String)
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "NP_BOOLEAN_RETURN_NULL" }, justification = "Need to return Null so that we can determine when there is no score")
  public static Boolean getBooleanAttributeValue(final Element element,
                                                 final String attributeName) {
    if (null == element) {
      return null;
    }
    final String str = element.getAttribute(attributeName);
    if (null == str
        || "".equals(str)) {
      return null;
    } else {
      return Boolean.valueOf(str);
    }
  }

  /**
   * Get a double value from an attribute.
   * 
   * @param element the element to get the attribute from, may be null
   * @param attributeName the attribute name to get
   * @return the value, null if element is null or the attribute value is null
   *         or empty
   */
  public static Double getDoubleAttributeValue(final Element element,
                                               final String attributeName) {
    if (null == element) {
      return null;
    }
    final String str = element.getAttribute(attributeName);
    if (null == str
        || "".equals(str)) {
      return null;
    } else {
      return Double.valueOf(str);
    }
  }

  /**
   * Compare two documents and check if they are the same or not.
   * 
   * @param controlDoc
   * @param testDoc
   * @return true if the documents have the same elements and attributes,
   *         reguardless of order
   */
  public static boolean compareDocuments(final Document controlDoc,
                                         final Document testDoc) {
    final Diff xmldiff = new Diff(controlDoc, testDoc);
    return xmldiff.similar();
  }

  public static List<String> getSubjectiveCategoryNames(final Document challengeDocument) {
    final List<String> subjectiveCategories = new LinkedList<String>();
    for (final Element categoryElement : new NodelistElementCollectionAdapter(
                                                                              challengeDocument.getDocumentElement()
                                                                                               .getElementsByTagName("subjectiveCategory"))) {
      final String categoryName = categoryElement.getAttribute("name");
      subjectiveCategories.add(categoryName);
    }
    return subjectiveCategories;
  }

  public static boolean isValidCategoryName(final Document challengeDocument,
                                            final String name) {
    return getSubjectiveCategoryNames(challengeDocument).contains(name);
  }

  /**
   * Date format for time type.
   */
  public static final ThreadLocal<DateFormat> XML_TIME_FORMAT = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

  /**
   * Parse the document from the given stream. The document is validated with
   * the specified schema.. Does not close the stream after reading.
   * 
   * @param stream a stream containing document
   * @return the challengeDocument, null on an error
   */
  public static Document parse(final Reader stream, final Schema schema) {
    try {
      final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      builderFactory.setNamespaceAware(true);
      builderFactory.setSchema(schema);
      builderFactory.setIgnoringComments(true);
      builderFactory.setIgnoringElementContentWhitespace(true);
      final DocumentBuilder parser = builderFactory.newDocumentBuilder();

      parser.setErrorHandler(new ErrorHandler() {
        public void error(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void fatalError(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void warning(final SAXParseException spe) throws SAXParseException {
          LOGGER.error(spe.getMessage(), spe);
        }
      });

      // parse
      final String content = IOUtils.readIntoString(stream);
      final Document document = parser.parse(new InputSource(new StringReader(content)));


      return document;
    } catch (final SAXParseException spe) {
      throw new RuntimeException("Error parsing file line: "
          + spe.getLineNumber() + " column: " + spe.getColumnNumber() + " " + spe.getMessage());
    } catch (final SAXException se) {
      throw new RuntimeException(se);
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }
  }
}
