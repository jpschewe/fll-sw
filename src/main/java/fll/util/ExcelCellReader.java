/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;
import org.checkerframework.checker.nullness.qual.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Read Excel files.
 */
public class ExcelCellReader extends CellFileReader {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  // See https://github.com/typetools/checker-framework/issues/979
  @SuppressWarnings("nullness")
  private static final ThreadLocal<DateFormat> DATE_FORMAT_AM_PM_SS = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("hh:mm:ss a");
    }
  };

  private final DataFormatter formatter;

  private final FormulaEvaluator formulaEvaluator;

  private final Workbook workbook;

  private int lineNumber = -1;

  private final Sheet sheet;

  /**
   * If a number and it's rounded value are equal to this precision, then the
   * number is an integer.
   */
  private static final double INTEGER_FP_CHECK = 1E-10;

  /**
   * @param file see {@link #isExcelFile(Path)}
   * @throws IOException see {@link #isExcelFile(Path)}
   * @return see {@link #isExcelFile(Path)}
   */
  public static boolean isExcelFile(final File file) throws IOException {
    return isExcelFile(file.toPath());
  }

  /**
   * Check if we believe this file to be an Excel file.
   *
   * @param path the path to check
   * @throws IOException if there is an error detecting the file type
   * @return true if this is an excel file
   */
  public static boolean isExcelFile(final Path path) throws IOException {
    if (null == path) {
      throw new NullPointerException("Path cannot be null");
    }

    final String contentType = new Tika().detect(path);
    LOGGER.trace("Detected content type: {} for {}", contentType, path);

    final boolean isCsvFile;
    if (null == contentType) {
      // fall back to checking the extension
      final Path filename = path.getFileName();
      if (null == filename) {
        isCsvFile = true;
      } else {
        isCsvFile = !filename.toString().endsWith(".xls")
            && !filename.toString().endsWith(".xlsx");
      }
    } else if (contentType.startsWith("text/")) {
      isCsvFile = true;
    } else {
      isCsvFile = false;
    }

    final boolean isExcelFile = !isCsvFile;
    LOGGER.trace("Excel? {}", isExcelFile);
    return isExcelFile;
  }

  /**
   * @param file the file to read
   * @return see {@link #getAllSheetNames(InputStream)}
   * @throws IOException see {@link #getAllSheetNames(InputStream)}
   */
  public static List<String> getAllSheetNames(final File file) throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      final List<String> result = getAllSheetNames(fis);
      fis.close();
      return result;
    } finally {
      if (null != fis) {
        fis.close();
      }
    }
  }

  /**
   * Get the names of all sheets in the specified stream.
   *
   * @param stream the file to read
   * @throws IOException if there is an error reading the file
   * @return the names of the sheets found in the workbook
   */
  public static List<String> getAllSheetNames(final InputStream stream) throws IOException {
    final List<String> sheetNames = new LinkedList<>();

    final Workbook workbook = createWorkbook(stream);
    final int numSheets = workbook.getNumberOfSheets();
    for (int i = 0; i < numSheets; ++i) {
      sheetNames.add(workbook.getSheetName(i));
    }

    return sheetNames;
  }

  private static Workbook createWorkbook(final InputStream file) throws IOException {
    try (InputStream stream = new PushbackInputStream(file)) {
      final Workbook workbook = WorkbookFactory.create(stream);
      return workbook;
    }
  }

  /**
   * Read an excel file from the specified stream.
   *
   * @param file where to read the excel file from, this is read into memory and
   *          then can be closed after the constructor is finished
   * @param sheetName the sheet to read
   * @throws IOException if there is an error reading the file
   */
  public ExcelCellReader(final InputStream file,
                         final String sheetName)
      throws IOException {
    Objects.requireNonNull(file);
    Objects.requireNonNull(sheetName, "Sheet name cannot be null");

    workbook = createWorkbook(file);
    if (workbook instanceof HSSFWorkbook) {
      formulaEvaluator = new HSSFFormulaEvaluator((HSSFWorkbook) workbook);
    } else if (workbook instanceof XSSFWorkbook) {
      formulaEvaluator = new XSSFFormulaEvaluator((XSSFWorkbook) workbook);
    } else {
      throw new RuntimeException("Unknown wookbook class: "
          + workbook.getClass().getName());
    }

    sheet = workbook.getSheet(sheetName);

    formatter = new DataFormatter();
  }

  @Override
  public long getLineNumber() {
    return lineNumber;
  }

  @Override
  @SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Return null rather than zero length array so that we know when we hit EFO")
  public @Nullable String @Nullable [] readNext() throws IOException {
    if (lineNumber >= sheet.getLastRowNum()) {
      return null;
    }

    ++lineNumber;
    final Row row = sheet.getRow(lineNumber);
    if (null == row) {
      return new String[0];
    }

    final List<@Nullable String> data = new LinkedList<>();
    for (int cellIdx = 0; cellIdx < row.getLastCellNum(); ++cellIdx) {
      final Cell cell = row.getCell(cellIdx, MissingCellPolicy.RETURN_NULL_AND_BLANK);
      if (null == cell) {
        data.add(null);
      } else {
        final String str;
        if (CellType.NUMERIC.equals(cell.getCellType())) {
          final double d = cell.getNumericCellValue();
          // test if a date!
          if (DateUtil.isCellDateFormatted(cell)) {
            // make sure to format times like we expect them
            final Date date = DateUtil.getJavaDate(d);
            str = DATE_FORMAT_AM_PM_SS.get().format(date);
          } else {
            // check for integer
            if (FP.equals(d, Math.round(d), INTEGER_FP_CHECK)) {
              str = String.valueOf((int) d);
            } else {
              str = formatter.formatCellValue(cell, formulaEvaluator);
            }
          }
        } else {
          str = formatter.formatCellValue(cell, formulaEvaluator);
        }
        data.add(str);
      }
    }
    return data.toArray(new String[data.size()]);
  }

  /**
   * @see java.io.Closeable#close()
   */
  @Override
  public void close() throws IOException {
    // nop
  }

}
