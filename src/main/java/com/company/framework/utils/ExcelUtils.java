package com.company.framework.utils;

import com.company.framework.constants.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reusable Apache POI wrapper for Excel-driven ({@code .xlsx}) test data.
 *
 * <p>All read methods open the workbook, extract exactly what was asked for,
 * and close the workbook again within the same call (try-with-resources) -
 * callers never manage a {@link Workbook} handle themselves, which rules out
 * an entire class of "file left open / locked on Windows CI agents" bugs.
 *
 * <p>Cell values are always returned as {@link String}, with explicit handling
 * per {@link CellType} (STRING, NUMERIC incl. dates, BOOLEAN, FORMULA via
 * {@link FormulaEvaluator}, BLANK) so callers get a predictable value
 * regardless of how a QA engineer formatted the spreadsheet cell.
 */
public final class ExcelUtils {

    private static final Logger log = LogManager.getLogger(ExcelUtils.class);

    private ExcelUtils() {
        throw new UnsupportedOperationException("ExcelUtils is a static utility class and cannot be instantiated");
    }

    // ===================== Public API =====================

    /** Row count of the given sheet, excluding the header row (row 0). */
    public static int getRowCount(String sheetName) {
        return getRowCount(FrameworkConstants.EXCEL_FILE_PATH, sheetName);
    }

    public static int getRowCount(String filePath, String sheetName) {
        return withSheet(filePath, sheetName, sheet -> sheet.getLastRowNum());
    }

    /** Column count of the given sheet, based on the header row. */
    public static int getColumnCount(String sheetName) {
        return getColumnCount(FrameworkConstants.EXCEL_FILE_PATH, sheetName);
    }

    public static int getColumnCount(String filePath, String sheetName) {
        return withSheet(filePath, sheetName, sheet -> {
            Row header = requireHeaderRow(sheet, sheetName);
            return (int) header.getLastCellNum();
        });
    }

    /** Reads a single cell by zero-based row/column index. */
    public static String getCellData(String sheetName, int rowIndex, int colIndex) {
        return getCellData(FrameworkConstants.EXCEL_FILE_PATH, sheetName, rowIndex, colIndex);
    }

    public static String getCellData(String filePath, String sheetName, int rowIndex, int colIndex) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = requireSheet(workbook, sheetName, filePath);
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                log.debug("Row [{}] is empty in sheet [{}]", rowIndex, sheetName);
                return "";
            }
            Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            return resolveCellValue(cell, workbook.getCreationHelper().createFormulaEvaluator());

        } catch (IOException e) {
            throw failToRead(filePath, e);
        }
    }

    /** Reads an entire row (excluding nothing - raw values as they appear) by zero-based row index. */
    public static List<String> getRowData(String sheetName, int rowIndex) {
        return getRowData(FrameworkConstants.EXCEL_FILE_PATH, sheetName, rowIndex);
    }

    public static List<String> getRowData(String filePath, String sheetName, int rowIndex) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = requireSheet(workbook, sheetName, filePath);
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                return List.of();
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<String> values = new ArrayList<>();
            for (int c = 0; c < row.getLastCellNum(); c++) {
                values.add(resolveCellValue(row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
            }
            return values;

        } catch (IOException e) {
            throw failToRead(filePath, e);
        }
    }

    /**
     * Reads the complete sheet as a list of header-to-value maps - one map per
     * data row (row 0 is treated as the header and is not included as data).
     * This is the most convenient form for readable assertions and logging,
     * e.g. {@code row.get("username")}.
     */
    public static List<Map<String, String>> getSheetData(String sheetName) {
        return getSheetData(FrameworkConstants.EXCEL_FILE_PATH, sheetName);
    }

    public static List<Map<String, String>> getSheetData(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = requireSheet(workbook, sheetName, filePath);
            Row headerRow = requireHeaderRow(sheet, sheetName);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                headers.add(resolveCellValue(headerRow.getCell(c), evaluator));
            }

            List<Map<String, String>> data = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    rowMap.put(headers.get(c),
                            resolveCellValue(row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL), evaluator));
                }
                data.add(rowMap);
            }
            log.info("Read [{}] data row(s) from sheet [{}] in [{}]", data.size(), sheetName, filePath);
            return data;

        } catch (IOException e) {
            throw failToRead(filePath, e);
        }
    }

    /**
     * TestNG {@code @DataProvider}-ready form: each spreadsheet data row becomes
     * one {@code Object[]} row, values in column order. Usage:
     * <pre>{@code
     * @DataProvider(name = "loginData")
     * public Object[][] loginData() {
     *     return ExcelUtils.getData("LoginData");
     * }
     * }</pre>
     */
    public static Object[][] getData(String sheetName) {
        return getData(FrameworkConstants.EXCEL_FILE_PATH, sheetName);
    }

    public static Object[][] getData(String filePath, String sheetName) {
        List<Map<String, String>> rows = getSheetData(filePath, sheetName);
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i).values().toArray();
        }
        return data;
    }

    // ===================== Internal helpers =====================

    @FunctionalInterface
    private interface SheetReader<T> {
        T read(Sheet sheet);
    }

    private static <T> T withSheet(String filePath, String sheetName, SheetReader<T> reader) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            return reader.read(requireSheet(workbook, sheetName, filePath));
        } catch (IOException e) {
            throw failToRead(filePath, e);
        }
    }

    private static Sheet requireSheet(Workbook workbook, String sheetName, String filePath) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            log.error("Sheet [{}] not found in workbook [{}]", sheetName, filePath);
            throw new IllegalArgumentException("Sheet [" + sheetName + "] does not exist in " + filePath);
        }
        return sheet;
    }

    private static Row requireHeaderRow(Sheet sheet, String sheetName) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalStateException("Sheet [" + sheetName + "] has no header row at index 0");
        }
        return header;
    }

    /** Resolves any {@link CellType} - including evaluated formulas - to a String. */
    private static String resolveCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numericValue = cell.getNumericCellValue();
                // Render whole numbers without a trailing ".0" (e.g. Excel's "101" not "101.0")
                if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return resolveEvaluatedFormula(evaluator.evaluate(cell));
            case BLANK:
            default:
                return "";
        }
    }

    private static String resolveEvaluatedFormula(CellValue evaluated) {
        switch (evaluated.getCellType()) {
            case STRING:
                return evaluated.getStringValue().trim();
            case NUMERIC:
                return String.valueOf(evaluated.getNumberValue());
            case BOOLEAN:
                return String.valueOf(evaluated.getBooleanValue());
            default:
                return "";
        }
    }

    private static UncheckedIOException failToRead(String filePath, IOException cause) {
        log.error("Failed to read Excel file [{}]: {}", filePath, cause.getMessage(), cause);
        return new UncheckedIOException("Unable to read Excel file: " + filePath, cause);
    }
}
