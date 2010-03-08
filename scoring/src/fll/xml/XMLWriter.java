/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Basic XML writer. Based on the sample in xerces-2.0.2 dom.Writer.
 */
public class XMLWriter {

  private static final Logger LOG = Logger.getLogger(XMLWriter.class);

  public XMLWriter() {
  }

  /**
   * Set the output writer, if encoding is null, use UTF8.
   */
  public void setOutput(final OutputStream stream, final String encoding) throws UnsupportedEncodingException {
    final Writer writer;
    if (encoding == null) {
      writer = new OutputStreamWriter(stream, "UTF8");
    } else {
      writer = new OutputStreamWriter(stream, encoding);
    }
    _output = new PrintWriter(writer);
  }

  /**
   * Set the output writer.
   */
  public void setOutput(final Writer writer) {
    _output = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(writer);
  }

  private String _stylesheet = null;

  public String getStyleSheet() {
    return _stylesheet;
  }

  public void setStyleSheet(final String stylesheet) {
    _stylesheet = stylesheet;
  }

  /**
   * Recursively write out node.
   */
  public void write(final Node node) {
    if (null == node) {
      return;
    }

    final short type = node.getNodeType();
    switch (type) {
    case Node.DOCUMENT_NODE: {
      final Document document = (Document) node;
      indent();
      _output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

      if (null != getStyleSheet()) {
        _output.println("<?xml-stylesheet type='text/css' href='"
            + getStyleSheet() + "'?>");
      }
      _output.flush();
      write(((document.getDoctype())));
      write(((document.getDocumentElement())));
      break;
    }
    case Node.DOCUMENT_TYPE_NODE: {
      final DocumentType doctype = (DocumentType) node;
      indent();
      _output.print("<!DOCTYPE ");
      _output.print(doctype.getName());
      final String publicId = doctype.getPublicId();
      final String systemId = doctype.getSystemId();
      if (publicId != null) {
        _output.print(" PUBLIC '");
        _output.print(publicId);
        _output.print("' '");
        _output.print(systemId);
        _output.print('\'');
      } else {
        _output.print(" SYSTEM '");
        _output.print(systemId);
        _output.print('\'');
      }
      final String internalSubset = doctype.getInternalSubset();
      if (internalSubset != null) {
        _output.println(" [");
        _output.print(internalSubset);
        _output.print(']');
      }
      _output.println('>');
      break;
    }
    case Node.ELEMENT_NODE: {
      indent();
      _output.print('<');
      _output.print(node.getNodeName());
      final Attr[] attrs = sortAttributes(node.getAttributes());
      for (final Attr attr : attrs) {
        _output.print(' ');
        _output.print(attr.getNodeName());
        _output.print("=\"");
        normalizeAndPrint(attr.getNodeValue());
        _output.print('"');
      }

      if (getNeedsIndent()) {
        _output.println('>');
      } else {
        _output.print('>');
      }

      _output.flush();

      _indent += INDENT_OFFSET;

      Node child = node.getFirstChild();
      while (child != null) {
        write(child);
        child = child.getNextSibling();
      }

      break;
    }
    case Node.ENTITY_REFERENCE_NODE: {
      _output.print('&');
      _output.print(node.getNodeName());
      _output.print(';');
      _output.flush();
      break;
    }
    case Node.CDATA_SECTION_NODE: {
      indent();
      _output.print("<![CDATA[");
      _output.print(node.getNodeValue());
      _output.print("]]>");
      _output.flush();
      break;
    }
    case Node.TEXT_NODE: {
      normalizeAndPrint(node.getNodeValue());
      _output.flush();
      break;
    }
    case Node.PROCESSING_INSTRUCTION_NODE: {
      indent();
      _output.print("<?");
      _output.print(node.getNodeName());
      final String data = node.getNodeValue();
      if (data != null
          && data.length() > 0) {
        _output.print(' ');
        _output.print(data);
      }
      _output.println("?>");
      _output.flush();
      break;
    }
    default:
      LOG.debug("Skipping node type: "
          + type);
    }
    if (type == Node.ELEMENT_NODE) {
      _indent -= INDENT_OFFSET;
      indent();
      _output.print("</");
      _output.print(node.getNodeName());
      if (getNeedsIndent()) {
        _output.println('>');
      } else {
        _output.print('>');
      }
      _output.flush();
    }
  }

  private Attr[] sortAttributes(final NamedNodeMap attrs) {
    final int len = attrs == null ? 0 : attrs.getLength();
    final Attr[] array = new Attr[len];
    for (int i = 0; i < len; i++) {
      array[i] = (Attr) attrs.item(i);
    }

    for (int i = 0; i < len - 1; i++) {
      String name = array[i].getNodeName();
      int index = i;
      for (int j = i + 1; j < len; j++) {
        final String curName = array[j].getNodeName();
        if (curName.compareTo(name) < 0) {
          name = curName;
          index = j;
        }
      }

      if (index != i) {
        final Attr temp = array[i];
        array[i] = array[index];
        array[index] = temp;
      }
    }

    return array;
  }

  private void normalizeAndPrint(final String s) {
    final int len = s == null ? 0 : s.length();
    for (int i = 0; i < len; i++) {
      normalizeAndPrint(s.charAt(i));
    }

  }

  private void normalizeAndPrint(final char c) {
    switch (c) {
    case '<': {
      _output.print("&lt;");
      break;
    }
    case '>': {
      _output.print("&gt;");
      break;
    }
    case '&': {
      _output.print("&amp;");
      break;
    }
    case '"': {
      _output.print("&quot;");
      break;
    }
    default:
      _output.print(c);
      break;
    }
  }

  /**
   * Print out some number of spaces depending on the value of _indent and
   * INDENT_OFFSET.
   */
  private void indent() {
    if (getNeedsIndent()) {
      for (int i = 0; i < _indent; i++) {
        _output.print(' ');
      }
    }
  }

  private boolean _needsIndent = false;

  /**
   * Get the needs indent property. If true then carriage returns and
   * indentation will be added to the XML file when written out. This defaults
   * to false because if you are writing out an XML file that already has nice
   * formatting, this will just add extra carriage returns and indentation.
   */
  public boolean getNeedsIndent() {
    return _needsIndent;
  }

  /**
   * @see #getNeedsIndent()
   */
  public void setNeedsIndent(final boolean v) {
    _needsIndent = v;
  }

  private PrintWriter _output;

  private int _indent = 0;

  private static final int INDENT_OFFSET = 2;
}
