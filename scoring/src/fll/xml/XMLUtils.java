/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.Diff;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * Generate some XML documents.
 */
public final class XMLUtils {

  private XMLUtils() {
  }

  public static final DocumentBuilder DOCUMENT_BUILDER;

  // create basic document builder
  static {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DOCUMENT_BUILDER = factory.newDocumentBuilder();
      DOCUMENT_BUILDER.setErrorHandler(new ErrorHandler() {
        public void error(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void fatalError(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void warning(final SAXParseException spe) throws SAXParseException {
          System.err.println(spe.getMessage());
        }
      });
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce.getMessage());
    }
  }

  /**
   * Parse xmlDoc an XML document. Just does basic parsing, no validity checks.
   */
  public static Document parseXMLDocument(final InputStream xmlDocStream) {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(true);

      final DocumentBuilder parser = factory.newDocumentBuilder();
      parser.setErrorHandler(new ErrorHandler() {
        public void error(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void fatalError(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void warning(final SAXParseException spe) throws SAXParseException {
          System.err.println(spe.getMessage());
        }
      });

      final Document document = parser.parse(xmlDocStream);
      return document;
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce.getMessage());
    } catch (final SAXException se) {
      throw new RuntimeException(se.getMessage());
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe.getMessage());
    }
  }

  /**
   * Find a subjective category by name.
   * 
   * @param challengeDocument the document to look in
   * @param name the name to look for
   * @return the element or null if one is not found
   */
  public static Element getSubjectiveCategoryByName(final Document challengeDocument, final String name) {
    for (final Element categoryElement : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
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

    final List<Element> values = XMLUtils.filterToElements(element.getElementsByTagName("value"));
    return !values.isEmpty();
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
   * Filter the {@link NodeList} to only Elements.
   * 
   * @param nodelist cannot be null
   * @return the list of {@link Element}s
   */
  public static List<Element> filterToElements(final NodeList nodelist) {
    final List<Element> retval = new ArrayList<Element>(nodelist.getLength());
    for (int i = 0; i < nodelist.getLength(); ++i) {
      final Node node = nodelist.item(i);
      if (node instanceof Element) {
        retval.add((Element) node);
      }
    }
    return retval;
  }

  /**
   * Get the bracket sort type from the document. If the attribute doesn't
   * exist, then return {@link BracketSortType#SEEDING}.
   */
  public static BracketSortType getBracketSort(final Document challengeDocument) {
    final Element root = challengeDocument.getDocumentElement();
    if (root.hasAttribute("bracketSort")) {
      final String sortStr = root.getAttribute("bracketSort");
      if(null == sortStr) {
        return BracketSortType.SEEDING;
      } else{
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
  public static String getStringAttributeValue(final Element element, final String attributeName) {
    if (null == element) {
      return null;
    }
    final String str = element.getAttribute(attributeName);
    return str;
  }

  /**
   * @see #getDoubleAttributeValue(Element, String)
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { 
  "NP_BOOLEAN_RETURN_NULL" }, justification = "Need to return Null so that we can determine when there is no score")
  public static Boolean getBooleanAttributeValue(final Element element, final String attributeName) {
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
   * @return the value, null if element is null or the attribute value is null or empty
   */
  public static Double getDoubleAttributeValue(final Element element, final String attributeName) {
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
  public static boolean compareDocuments(final Document controlDoc, final Document testDoc) {
    final Diff xmldiff = new Diff(controlDoc, testDoc);
    return xmldiff.similar();
  }

  public static List<String> getSubjectiveCategoryNames(final Document challengeDocument) {
    final List<String> subjectiveCategories = new LinkedList<String>();
    for (final Element categoryElement : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
      final String categoryName = categoryElement.getAttribute("name");
      subjectiveCategories.add(categoryName);
    }
    return subjectiveCategories;
  }
  
  public static boolean isValidCategoryName(final Document challengeDocument, final String name) {
    return getSubjectiveCategoryNames(challengeDocument).contains(name);
  }
}
