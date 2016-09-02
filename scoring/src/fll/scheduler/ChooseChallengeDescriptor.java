/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.apache.commons.lang3.StringUtils;

import fll.util.FLLInternalException;
import fll.web.setup.SetupIndex.DescriptionInfo;
import net.mtu.eggplant.util.BasicFileFilter;

/**
 * Prompt the user to choose a challenge descriptor.
 * This is a modal dialog.
 * 
 * @see #getSelectedDescription()
 */
public class ChooseChallengeDescriptor extends JDialog {
  private JComboBox<DescriptionInfo> mCombo;

  private JLabel mFileField;

  private URL mSelected = null;

  /**
   * @param owner parent component
   */
  public ChooseChallengeDescriptor(final Frame owner) {
    super(owner, true);
    initComponents();
  }

  /**
   * @param owner parent component
   */
  public ChooseChallengeDescriptor(final Dialog owner) {
    super(owner, true);
    initComponents();
  }

  private void initComponents() {
    getContentPane().setLayout(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.BOTH;
    final JTextArea instructions = new JTextArea("Choose a challenge description from the drop down list OR choose a file containing your custom challenge description.",
                                                 3, 40);
    instructions.setEditable(false);
    instructions.setWrapStyleWord(true);
    instructions.setLineWrap(true);
    getContentPane().add(instructions, gbc);

    gbc = new GridBagConstraints();
    mCombo = new JComboBox<DescriptionInfo>();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    getContentPane().add(mCombo, gbc);
    mCombo.setRenderer(new DescriptionInfoRenderer());
    mCombo.setEditable(false);
    final List<DescriptionInfo> descriptions = DescriptionInfo.getAllKnownChallengeDescriptionInfo();
    for (final DescriptionInfo info : descriptions) {
      mCombo.addItem(info);
    }

    mFileField = new JLabel();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    getContentPane().add(mFileField, gbc);

    final JButton chooseButton = new JButton("Choose File");
    gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    getContentPane().add(chooseButton, gbc);
    chooseButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent ae) {
        mFileField.setText(null);

        final JFileChooser fileChooser = new JFileChooser();
        final FileFilter filter = new BasicFileFilter("FLL Description (xml)", new String[] { "xml" });
        fileChooser.setFileFilter(filter);

        final int returnVal = fileChooser.showOpenDialog(ChooseChallengeDescriptor.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          final File selectedFile = fileChooser.getSelectedFile();
          mFileField.setText(selectedFile.getAbsolutePath());
        }
      }
    });

    final Box buttonPanel = new Box(BoxLayout.X_AXIS);
    gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    getContentPane().add(buttonPanel, gbc);

    buttonPanel.add(Box.createHorizontalGlue());
    final JButton ok = new JButton("Ok");
    buttonPanel.add(ok);
    ok.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent ae) {

        // use the selected description if nothing is entered in the file box
        final DescriptionInfo descriptionInfo = mCombo.getItemAt(mCombo.getSelectedIndex());
        if (null != descriptionInfo) {
          mSelected = descriptionInfo.getURL();
        }

        final String text = mFileField.getText();
        if (!StringUtils.isEmpty(text)) {
          final File file = new File(text);
          if (file.exists()) {
            try {
              mSelected = file.toURI().toURL();
            } catch (final MalformedURLException e) {
              throw new FLLInternalException("Can't turn file into URL?", e);
            }
          }
        }

        setVisible(false);
      }
    });

    final JButton cancel = new JButton("Cancel");
    buttonPanel.add(cancel);
    cancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent ae) {
        mSelected = null;
        setVisible(false);
      }
    });

    pack();
  }

  /**
   * @return the chosen challenge descriptor, null if the user canceled.
   */
  public URL getSelectedDescription() {
    return mSelected;
  }

  private static final class DescriptionInfoRenderer implements ListCellRenderer<DescriptionInfo> {
    public DescriptionInfoRenderer() {
      mDelegate = new BasicComboBoxRenderer();
    }

    private final BasicComboBoxRenderer mDelegate;

    @Override
    public Component getListCellRendererComponent(final JList<? extends DescriptionInfo> list,
                                                  final DescriptionInfo descriptionInfo,
                                                  final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
      if (null == descriptionInfo) {
        return mDelegate.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
      } else {
        final String title = descriptionInfo.getTitle();
        final String revision = descriptionInfo.getRevision();
        final StringBuilder value = new StringBuilder();
        value.append(title);
        if (null != revision) {
          value.append('(');
          value.append(revision);
          value.append(')');
        }

        return mDelegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }
    }
  }
}
