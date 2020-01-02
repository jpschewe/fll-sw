/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utilities for working with Apache FOP.
 */
public final class FOPUtils {

  private FOPUtils() {
  }

  /**
   * Namespace for XSL-FO.
   */
  public static final String XSL_FO_NAMESPACE = "http://www.w3.org/1999/XSL/Format";

  /**
   * Prefix with colon.
   */
  public static final String XSL_FO_PREFIX = "fo:";

  /**
   * Create the root element for an XSL-FO document.
   * This ensures that the namespace is set properly.
   * 
   * @param document used to create the element
   * @return the root element
   */
  public static Element createRoot(final Document document) {
    final Element rootElement = createXslFoElement(document, "root");
    rootElement.setAttribute("xmlns:fo", XSL_FO_NAMESPACE);

    // get rid of warnings about not being able to find fonts
    rootElement.setAttribute("font-family", "Times");

    return rootElement;
  }

  /**
   * Helper to create elements with correct prefix.
   * 
   * @param document used to create the element
   * @param elementName the name of the element
   * @return the new element
   */
  public static Element createXslFoElement(final Document document,
                                           final String elementName) {
    final Element ele = document.createElementNS(XSL_FO_NAMESPACE, String.format("%s%s", XSL_FO_PREFIX, elementName));
    return ele;
  }

  /**
   * Create 8.5" x 11" page with half inch margins and 0.3 inch footer and no
   * header.
   * 
   * @param document used to create the elements
   * @param layoutMasterSet the master set element
   * @param name the name of the page master
   * @see #createSimplePageMaster(Document, Element, String, double, double,
   *      double, double, double, double, double, double)
   * @throws IllegalArgumentException see {#link
   *           {@link #createSimplePageMaster(Document, Element, String, double, double, double, double, double, double, double, double)}
   */
  public static void createSimplePageMaster(final Document document,
                                            final Element layoutMasterSet,
                                            final String name)
      throws IllegalArgumentException {
    createSimplePageMaster(document, layoutMasterSet, name, 8.5, 11, 0.5, 0.5, 0.5, 0.5, 0, 0.3);
  }

  /**
   * @param document used to create the elements
   * @param layoutMasterSet the master set element
   * @param name the name of the page master
   * @param pageWidth width in inches
   * @param pageHeight height in inches
   * @param leftMargin margin in inches
   * @param rightMargin margin in inches
   * @param topMargin margin in inches
   * @param bottomMargin margin in inches
   * @param headerHeight height in inches
   * @param footerHeight height in inches
   * @throws IllegalArgumentException if there is not enough room for the header
   *           or footer
   */
  public static void createSimplePageMaster(final Document document,
                                            final Element layoutMasterSet,
                                            final String name,
                                            final double pageWidth,
                                            final double pageHeight,
                                            final double leftMargin,
                                            final double rightMargin,
                                            final double topMargin,
                                            final double bottomMargin,
                                            final double headerHeight,
                                            final double footerHeight)
      throws IllegalArgumentException {
    if (headerHeight > topMargin) {
      throw new IllegalArgumentException(String.format("Header height (%f) cannot be greater than the top margin (%f)",
                                                       headerHeight, topMargin));
    }
    if (footerHeight > bottomMargin) {
      throw new IllegalArgumentException(String.format("Footer height (%f) cannot be greater than the bottom margin (%f)",
                                                       footerHeight, bottomMargin));
    }

    final Element simplePageMaster = createXslFoElement(document, "simple-page-master");
    simplePageMaster.setAttribute("master-name", name);
    simplePageMaster.setAttribute("page-height", String.format("%fin", pageHeight));
    simplePageMaster.setAttribute("page-width", String.format("%fin", pageWidth));

    simplePageMaster.setAttribute("margin-top", String.format("%fin", topMargin));
    simplePageMaster.setAttribute("margin-bottom", String.format("%fin", bottomMargin));
    simplePageMaster.setAttribute("margin-left", String.format("%fin", leftMargin));
    simplePageMaster.setAttribute("margin-right", String.format("%fin", rightMargin));

    final Element body = createXslFoElement(document, "region-body");
    body.setAttribute("margin-top", String.format("%fin", headerHeight));
    simplePageMaster.appendChild(body);

    final Element header = createXslFoElement(document, "region-before");
    header.setAttribute("extent", String.format("%sin", headerHeight));
    simplePageMaster.appendChild(header);

    final Element footer = createXslFoElement(document, "region-after");
    footer.setAttribute("extent", String.format("%sin", footerHeight));
    simplePageMaster.appendChild(footer);

    layoutMasterSet.appendChild(simplePageMaster);

  }

  /**
   * Used for counting pages.
   */
  public static final String PAGE_SEQUENCE_NAME = "seq1";

  /**
   * Creates a page sequence with the id {@link #PAGE_SEQUENCE_NAME} for putting
   * page numbers in the footer.
   * 
   * @param document used to create elements
   * @param pageMasterName the name of the page master from
   *          {@link #createSimplePageMaster(Document, Element, String, double, double, double, double, double, double, double, double)}.
   * @return the page sequence element to be added to the root element
   */
  public static Element createPageSequence(final Document document,
                                           final String pageMasterName) {
    final Element ele = createXslFoElement(document, "page-sequence");
    ele.setAttribute("master-reference", pageMasterName);
    ele.setAttribute("id", PAGE_SEQUENCE_NAME);
    return ele;
  }

  /**
   * Simple footer with page numbers. Font size is set to 10pt.
   * 
   * @param document used to create elements
   * @return the element to be added to the page sequence
   * @see #createPageSequence(Document, String)
   * @see #PAGE_SEQUENCE_NAME
   */
  public static Element createSimpleFooter(final Document document) {
    final Element staticContent = createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-after");
    staticContent.setAttribute("font-size", "10pt");

    final Element block = createXslFoElement(document, "block");
    block.setAttribute("text-align", "end");
    block.appendChild(document.createTextNode("Page "));
    block.appendChild(createXslFoElement(document, "page-number"));
    block.appendChild(document.createTextNode(" of "));
    final Element lastPage = createXslFoElement(document, "page-number-citation-last");
    lastPage.setAttribute("ref-id", PAGE_SEQUENCE_NAME);
    block.appendChild(lastPage);

    staticContent.appendChild(block);

    return staticContent;
  }

  /**
   * @param document used to create elements
   * @return the body element to be added to the page sequence
   */
  public static Element createBody(final Document document) {
    final Element body = createXslFoElement(document, "flow");
    body.setAttribute("flow-name", "xsl-region-body");
    return body;
  }

  /**
   * Create a basic table that takes 100% of the width.
   * 
   * @param document used to create elements
   * @return the table element
   */
  public static Element createBasicTable(final Document document) {
    final Element table = createXslFoElement(document, "table");
    table.setAttribute("table-layout", "fixed");
    table.setAttribute("width", "100%");
    return table;
  }

  /**
   * Define a table column.
   * 
   * @param document used to create elements
   * @param proportionalWidth the relative width
   * @return the column element
   */
  public static Element createTableColumn(final Document document,
                                          final int proportionalWidth) {
    final Element ele = createXslFoElement(document, "table-column");
    ele.setAttribute("column-width", String.format("proportional-column-width(%d)", proportionalWidth));
    return ele;
  }

  /**
   * Create the table header element with bold text.
   * 
   * @param document used to create elements
   * @return the table header element
   */
  public static Element createTableHeader(final Document document) {
    final Element ele = createXslFoElement(document, "table-header");
    ele.setAttribute("font-weight", "bold");
    return ele;
  }

  /**
   * Center text alignment.
   */
  public static final String TEXT_ALIGN_CENTER = "center";

  /**
   * Create a basic table cell that hides text that is too long for the cell.
   * Borders are not set.
   * 
   * @param document used to create elements
   * @param text the text to put in the cell
   * @param textAlignment CSS text alignment attribute
   * @return the table cell
   */
  public static Element createTableCell(final Document document,
                                        final String textAlignment,
                                        final String text) {
    final Element cell = createXslFoElement(document, "table-cell");

    final Element blockContainer = createXslFoElement(document, "block-container");
    blockContainer.setAttribute("overflow", "hidden");
    cell.appendChild(blockContainer);

    final Element block = createXslFoElement(document, "block");
    blockContainer.appendChild(block);
    if (null != textAlignment) {
      block.setAttribute("text-align", textAlignment);
    }
    block.appendChild(document.createTextNode(text));

    return cell;
  }

  /**
   * Set a top solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addTopBorder(final Element element,
                                  final double width) {
    addBorder(element, width, "top");
  }

  /**
   * Set a bottom solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addBottomBorder(final Element element,
                                     final double width) {
    addBorder(element, width, "bottom");
  }

  /**
   * Set a left solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addLeftBorder(final Element element,
                                   final double width) {
    addBorder(element, width, "left");
  }

  /**
   * Set a right solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addRightBorder(final Element element,
                                    final double width) {
    addBorder(element, width, "right");
  }

  private static void addBorder(final Element element,
                                final double width,
                                final String side) {
    element.setAttribute(String.format("border-%s", side), String.format("%fpt solid black", width));
  }

  /**
   * Set all borders.
   * 
   * @param element element to add the borders to
   * @param topWidth {@link #addTopBorder(Element, double)}
   * @param bottomWidth {@link #addBottomBorder(Element, double)}
   * @param leftWidth {@link #addLeftBorder(Element, double)}
   * @param rightWidth {@link #addRightBorder(Element, double)}
   */
  public static void addBorders(final Element element,
                                final double topWidth,
                                final double bottomWidth,
                                final double leftWidth,
                                final double rightWidth) {
    addTopBorder(element, topWidth);
    addBottomBorder(element, bottomWidth);
    addLeftBorder(element, leftWidth);
    addRightBorder(element, rightWidth);
  }

  /**
   * Create a factory that looks in the current directory to resolve relative
   * URLs.
   * 
   * @return factory
   */
  public static FopFactory createSimpleFopFactory() {
    return FopFactory.newInstance(Paths.get(".").toUri());
  }

  /**
   * @param fopFactory the factory to use
   * @param xslfo the document to render
   * @param out where to render the document
   * @throws IOException if there is an error writing
   * @throws FOPException if there is an error configuring FOP
   * @throws TransformerException if there is an error in the input document
   */
  public static void renderPdf(final FopFactory fopFactory,
                               final Document xslfo,
                               final OutputStream out)
      throws IOException, FOPException, TransformerException {
    final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

    // Setup JAXP using identity transformer
    final TransformerFactory factory = TransformerFactory.newInstance();
    final Transformer transformer = factory.newTransformer(); // identity transformer

    // Setup input and output for XSLT transformation
    final Source src = new DOMSource(xslfo);

    // Resulting SAX events (the generated FO) must be piped through to FOP
    final Result res = new SAXResult(fop.getDefaultHandler());

    // Start XSLT transformation and FOP processing
    transformer.transform(src, res);
  }

  /**
   * @param document used to create elements
   * @return block containing a blank line
   */
  public static Element createBlankLine(final Document document) {
    final Element block = createXslFoElement(document, "block");
    final Element leader = createXslFoElement(document, "leader");
    block.appendChild(leader);
    return block;
  }

  /**
   * @param document used to create elements
   * @param thickness the thickness of the line in points
   * @return block containing the horizontal line
   */
  public static Element createHorizontalLine(final Document document,
                                             final int thickness) {
    final Element lineBlock = FOPUtils.createXslFoElement(document, "block");

    final Element line = FOPUtils.createXslFoElement(document, "leader");
    line.setAttribute("leader-pattern", "rule");
    line.setAttribute("leader-length", "100%");
    line.setAttribute("rule-style", "solid");
    line.setAttribute("rule-thickness", String.format("%dpt", thickness));
    lineBlock.appendChild(line);

    return lineBlock;
  }
}
