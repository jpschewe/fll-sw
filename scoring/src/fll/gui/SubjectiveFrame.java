/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.gui;


import fll.model.SubjectiveTableModel;

import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import net.mtu.eggplant.util.BasicFileFilter;

import net.mtu.eggplant.util.gui.SortableTable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Application to enter subjective scores with
 *
 * @version $Revision$
 */
public final class SubjectiveFrame extends JFrame {

  public static void main(final String[] args) {
    try {
      final JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Please choose the subjective data file");
      //NOTE: Should get JonsInfra fixed for this
      fileChooser.setFileFilter(new BasicFileFilter("Zip files", "zip") {
        public boolean accept(final File f) {
          if(f.isDirectory()) {
            return true;
          } else {
            return super.accept(f);
          }
        }
      });
      final int state = fileChooser.showOpenDialog(null);
      if(JFileChooser.APPROVE_OPTION == state) {
        final File file = fileChooser.getSelectedFile();
        final SubjectiveFrame frame = new SubjectiveFrame(file);
        frame.pack();
        frame.show();
      } else {
        System.exit(0);
      }
    } catch(final IOException ioe) {
      JOptionPane.showMessageDialog(null,
                                    "Error reading data file: " + ioe.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  /**
   * Create a window to edit subjective scores.
   *
   * @param file where to read the data in from and where to save data to
   */
  public SubjectiveFrame(final File file) throws IOException {
    super("Subjective Score Entry");
    _file = file;
    
    getContentPane().setLayout(new BorderLayout());

    final ZipFile zipfile = new ZipFile(file);
    final InputStream challengeStream = zipfile.getInputStream(zipfile.getEntry("challenge.xml"));
    _challengeDocument = XMLUtils.parseXMLDocument(challengeStream);
    challengeStream.close();
    
    final InputStream scoreStream = zipfile.getInputStream(zipfile.getEntry("score.xml"));
    _scoreDocument = XMLUtils.parseXMLDocument(scoreStream);
    scoreStream.close();

    zipfile.close();
    
    final JPanel topPanel = new JPanel();
    getContentPane().add(topPanel, BorderLayout.NORTH);

    final JButton quitButton = new JButton("Quit");
    topPanel.add(quitButton);
    quitButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        quit();
      }
    });
    
    final JButton saveButton = new JButton("Save");
    topPanel.add(saveButton);
    saveButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        save();
      }
    });

    final JTabbedPane tabbedPane = new JTabbedPane();
    getContentPane().add(tabbedPane, BorderLayout.CENTER);
                         
    final NodeList subjectiveCategories = _challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
    for(int i=0; i<subjectiveCategories.getLength(); i++) {
      final Element subjectiveElement = (Element)subjectiveCategories.item(i);
      final SubjectiveTableModel tableModel = new SubjectiveTableModel(_scoreDocument, subjectiveElement);
      final JTable table = new SortableTable(tableModel);
      _tables.add(table);
      final JScrollPane tableScroller = new JScrollPane(table);
      tableScroller.setPreferredSize(new Dimension(640, 480));
      tabbedPane.addTab(subjectiveElement.getAttribute("title"), tableScroller);

      final NodeList goals = subjectiveElement.getElementsByTagName("goal");
      for(int g=0; g<goals.getLength(); g++) {
        final Element goalDescription = (Element)goals.item(g);
        if(goalDescription.hasChildNodes()) {
          //enumerated
          final Vector posValues = new Vector();
          posValues.add("");
          final NodeList posValuesList = goalDescription.getElementsByTagName("value");
          for(int v=0; v<posValuesList.getLength(); v++) {
            final Element posValue = (Element)posValuesList.item(v);
            posValues.add(posValue.getAttribute("title"));
          }
            
          final TableColumn column = table.getColumnModel().getColumn(g + 4);
          column.setCellEditor(new DefaultCellEditor(new JComboBox(posValues)));
        }
      }      
    }
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(final WindowEvent e) {
        quit();
      }
    });
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  }

  /**
   * Prompt the user with yes/no/cancel.  Yes exits and saves, no exits
   * without saving and cancel doesn't quit.
   */
  private void quit() {
    if(validateData()) {
    
      final int state = JOptionPane.showConfirmDialog(SubjectiveFrame.this,
                                                      "Save data?  Data will be saved in same file as it was read from.",
                                                      "Exit",
                                                      JOptionPane.YES_NO_CANCEL_OPTION);
      if(JOptionPane.YES_OPTION == state) {
        save();
        setVisible(false);
        dispose();
        System.exit(0);
      } else if(JOptionPane.NO_OPTION == state) {
        setVisible(false);
        dispose();
        System.exit(0);
      }
    }
  }

  /**
   * Make sure the data in the table is valid.  This checks to make sure that
   * for all rows all columns that contain numeric data are actually set, or
   * none of these columns are set in a row.  This avoids the case of partial
   * data.  This method is fail fast in that it will display a dialog box on
   * the first error it finds.
   *
   * @return true if everything is ok
   */
  private boolean validateData() {
    final NodeList subjectiveCategories = _challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
    for(int i=0; i<subjectiveCategories.getLength(); i++) {
      final Element subjectiveElement = (Element)subjectiveCategories.item(i);
      //final NodeList
      //FIX finish this
    }
    return true;
  }
  
  /**
   * Save out to the same file that things were read in.
   */
  private void save() {
    try {
      //stop editing of all tables
      final Iterator iter = _tables.iterator();
      while(iter.hasNext()) {
        final JTable table = (JTable)iter.next();
        final int editingColumn = table.getEditingColumn();
        final int editingRow = table.getEditingRow();
        if(editingColumn > -1) {
          final TableCellEditor cellEditor = table.getCellEditor(editingRow, editingColumn);
          if(null != cellEditor) {
            cellEditor.stopCellEditing();
          }
        }
      }
      
      final XMLWriter xmlwriter = new XMLWriter();
      
      final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(_file));
      xmlwriter.setOutput(zipOut, "UTF8");
      
      zipOut.putNextEntry(new ZipEntry("challenge.xml"));
      xmlwriter.write(_challengeDocument);
      zipOut.closeEntry();
      zipOut.putNextEntry(new ZipEntry("score.xml"));
      xmlwriter.write(_scoreDocument);
      zipOut.closeEntry();
      
      zipOut.close();
    } catch(final IOException ioe) {
      JOptionPane.showMessageDialog(null,
                                    "Error writing to data file: " + ioe.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  private final File _file;
  private final Document _challengeDocument;
  private final Document _scoreDocument;
  
  // -------- Inner classes only below here -----------
  /**
   * List model used for the comboboxes that is backed by an XML document.
   */
  private static final class SubjectiveListModel extends DefaultComboBoxModel {
    public SubjectiveListModel(final Document document) {
      _subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
    }

    public Object getElementAt(final int index) {
      return _subjectiveCategories.item(index);
    }

    public int getSize() {
      return _subjectiveCategories.getLength();
    }

    private final NodeList _subjectiveCategories;
    
  }

  private List _tables = new LinkedList();
}
