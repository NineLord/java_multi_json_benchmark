package testJson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.MathDataCollector;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class ExcelGenerator implements Closeable {

    @NotNull private final String pathToSaveFile;
    @NotNull private final List<String> jsonNames;
    private final double lengthWholeTest;
    @NotNull private final List<Config> inputConfiguration;
    @NotNull private final Workbook workbook;

    @NotNull private final Map<String, Map<Reporter.MeasurementType, MathDataCollector>> averagesPerJson;
	@NotNull private final Map<Reporter.MeasurementType, MathDataCollector> averageAllJsons;

    @NotNull private final CellStyle styleBorder;
    @NotNull private final CellStyle styleBorderAndCenter;
    @NotNull private final CellStyle styleColorfulBorderAndCenter;

    public ExcelGenerator(@NotNull final String pathToSaveFile, @NotNull final List<String> jsonNames, double lengthWholeTest, @NotNull final List<Config> configs) {
        this.pathToSaveFile = pathToSaveFile;
        this.jsonNames = jsonNames;
        this.lengthWholeTest = lengthWholeTest;
        this.inputConfiguration = configs;
        this.workbook = new XSSFWorkbook();

        this.averagesPerJson = new HashMap<>();
        this.jsonNames.forEach(jsonName -> this.averagesPerJson.put(jsonName, ExcelGenerator.getDataCollectorsForEachTest()));
	    this.averageAllJsons = ExcelGenerator.getDataCollectorsForEachTest();

        this.styleBorder = this.workbook.createCellStyle();
        this.styleBorder.setBorderTop(BorderStyle.THIN);
        this.styleBorder.setBorderBottom(BorderStyle.THIN);
        this.styleBorder.setBorderLeft(BorderStyle.THIN);
        this.styleBorder.setBorderRight(BorderStyle.THIN);

        this.styleBorderAndCenter = this.workbook.createCellStyle();
        this.styleBorderAndCenter.setBorderTop(BorderStyle.THIN);
        this.styleBorderAndCenter.setBorderBottom(BorderStyle.THIN);
        this.styleBorderAndCenter.setBorderLeft(BorderStyle.THIN);
        this.styleBorderAndCenter.setBorderRight(BorderStyle.THIN);
        this.styleBorderAndCenter.setAlignment(HorizontalAlignment.CENTER);
        this.styleBorderAndCenter.setVerticalAlignment(VerticalAlignment.CENTER);

        this.styleColorfulBorderAndCenter = this.workbook.createCellStyle();
        this.styleColorfulBorderAndCenter.setBorderTop(BorderStyle.THIN);
        this.styleColorfulBorderAndCenter.setBorderBottom(BorderStyle.THIN);
        this.styleColorfulBorderAndCenter.setBorderLeft(BorderStyle.THIN);
        this.styleColorfulBorderAndCenter.setBorderRight(BorderStyle.THIN);
        this.styleColorfulBorderAndCenter.setAlignment(HorizontalAlignment.CENTER);
        this.styleColorfulBorderAndCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        this.styleColorfulBorderAndCenter.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        this.styleColorfulBorderAndCenter.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private static Map<Reporter.MeasurementType, MathDataCollector> getDataCollectorsForEachTest() {
        Map<Reporter.MeasurementType, MathDataCollector> dataCollectors = new HashMap<>();
        Arrays.stream(Reporter.MeasurementType.values()).forEach(measurementType ->
                dataCollectors.put(measurementType, new MathDataCollector())
        );
        return dataCollectors;
    }

    //#region Adding Data
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull ExcelGenerator appendWorksheet(@NotNull final String worksheetName, @NotNull final Map<String, Map<Reporter.MeasurementType, Double>> database) {
        @NotNull final Sheet worksheet = this.workbook.createSheet(worksheetName);

        this.addData(worksheet, database);
        Map<Integer, Integer> preDefinedColumns = new HashMap<>();
        preDefinedColumns.put(0, 10200);
        preDefinedColumns.put(1, 2300);
        preDefinedColumns.put(3, 11000);
        preDefinedColumns.put(4, 2300);
        resizeColumns(worksheet, preDefinedColumns);

        return this;
    }

    private void addData(@NotNull final Sheet worksheet, @NotNull final Map<String, Map<Reporter.MeasurementType, Double>> database) {
        @NotNull final Map<Reporter.MeasurementType, MathDataCollector> testDataCollectors = ExcelGenerator.getDataCollectorsForEachTest();

        int currentRow = 0;
        for (@NotNull final String jsonName : this.jsonNames) {
            @NotNull final Map<Reporter.MeasurementType, Double> testData = database.get(jsonName);
            this.setColorfulTitle(worksheet, currentRow++, 0, jsonName);

            @NotNull final MathDataCollector jsonDataCollector = new MathDataCollector();
            this.addTestData(worksheet, currentRow++, jsonName, testData, jsonDataCollector, testDataCollectors, Reporter.MeasurementType.GENERATE_JSON, "Generating JSON");
            this.addTestData(worksheet, currentRow++, jsonName, testData, jsonDataCollector, testDataCollectors, Reporter.MeasurementType.ITERATE_ITERATIVELY, "Iterating JSON Iteratively - BFS");
            this.addTestData(worksheet, currentRow++, jsonName, testData, jsonDataCollector, testDataCollectors, Reporter.MeasurementType.ITERATE_RECURSIVELY, "Iterating JSON Recursively - DFS");
            this.addTestData(worksheet, currentRow++, jsonName, testData, jsonDataCollector, testDataCollectors, Reporter.MeasurementType.DESERIALIZE_JSON, "Deserializing JSON");
            this.addTestData(worksheet, currentRow++, jsonName, testData, jsonDataCollector, testDataCollectors, Reporter.MeasurementType.SERIALIZE_JSON, "Serializing JSON");

            this.setTitle(worksheet, currentRow, 0, "Total");
            this.setValue(worksheet, currentRow++, 1, jsonDataCollector.getSum());
            this.averagesPerJson.get(jsonName).get(Reporter.MeasurementType.TOTAL).add(jsonDataCollector.getSum());
            this.averageAllJsons.get(Reporter.MeasurementType.TOTAL).add(jsonDataCollector.getSum());
            testDataCollectors.get(Reporter.MeasurementType.TOTAL).add(jsonDataCollector.getSum());

            this.addTestData(worksheet, currentRow++, jsonName, testData, jsonDataCollector, testDataCollectors, Reporter.MeasurementType.TOTAL_INCLUDE_CONTEXT_SWITCH, "Total Including Context Switch");

            ++currentRow;
        }

        currentRow = 0;
        this.setColorfulTitle(worksheet, currentRow++, 3, "Averages of this Test");

        this.setTitle(worksheet, currentRow, 3, "Average Generating JSONs");
        this.setValue(worksheet, currentRow++, 4, testDataCollectors.get(Reporter.MeasurementType.GENERATE_JSON).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Iterating JSONs Iteratively - BFS");
        this.setValue(worksheet, currentRow++, 4, testDataCollectors.get(Reporter.MeasurementType.ITERATE_ITERATIVELY).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Iterating JSONs Recursively - DFS");
        this.setValue(worksheet, currentRow++, 4, testDataCollectors.get(Reporter.MeasurementType.ITERATE_RECURSIVELY).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Deserializing JSONs");
        this.setValue(worksheet, currentRow++, 4, testDataCollectors.get(Reporter.MeasurementType.DESERIALIZE_JSON).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Serializing JSONs");
        this.setValue(worksheet, currentRow++, 4, testDataCollectors.get(Reporter.MeasurementType.SERIALIZE_JSON).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Totals");
        this.setValue(worksheet, currentRow++, 4, testDataCollectors.get(Reporter.MeasurementType.TOTAL).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Totals Including Context Switch");
        this.setValue(worksheet, currentRow, 4, testDataCollectors.get(Reporter.MeasurementType.TOTAL_INCLUDE_CONTEXT_SWITCH).getAverage());
    }

    private void addTestData(@NotNull final Sheet worksheet, int currentRow,
                             @NotNull final String jsonName, @NotNull final Map<Reporter.MeasurementType, Double> testData,
                             @NotNull final MathDataCollector jsonDataCollector, @NotNull final Map<Reporter.MeasurementType, MathDataCollector> testDataCollectors,
                             @NotNull final Reporter.MeasurementType testName, @NotNull final String title) {
        final double value = testData.get(testName);
        this.setTitle(worksheet, currentRow, 0, title);
        this.setValue(worksheet, currentRow, 1, value);
        jsonDataCollector.add(value);
        this.averagesPerJson.get(jsonName).get(testName).add(value);
        this.averageAllJsons.get(testName).add(value);
        testDataCollectors.get(testName).add(value);
    }
    //#endregion

    //#region Add summary worksheet
    private void addAverageWorksheet() {
        @NotNull final Sheet worksheet = this.workbook.createSheet("Average");
        this.addAvgData(worksheet);
        Map<Integer, Integer> preDefinedColumns = new HashMap<>();
        preDefinedColumns.put(0, 10200);
        preDefinedColumns.put(1, 2300);
        preDefinedColumns.put(3, 11000);
        preDefinedColumns.put(4, 2300);
        resizeColumns(worksheet, preDefinedColumns);
    }

    private void addAvgData(@NotNull final Sheet worksheet) {
        int currentRow = 0;
        for (@NotNull final String jsonName : this.jsonNames) {
            @NotNull final Map<Reporter.MeasurementType, MathDataCollector> testData = this.averagesPerJson.get(jsonName);

            this.setColorfulTitle(worksheet, currentRow++, 0, jsonName);

            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.GENERATE_JSON, "Average Generating JSONs");
            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.ITERATE_ITERATIVELY, "Average Iterating JSONs Iteratively - BFS");
            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.ITERATE_RECURSIVELY, "Average Iterating JSONs Recursively - DFS");
            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.DESERIALIZE_JSON, "Average Deserializing JSONs");
            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.SERIALIZE_JSON, "Average Serializing JSONs");
            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.TOTAL, "Average Totals");
            this.addAverageData(worksheet, currentRow++, testData, Reporter.MeasurementType.TOTAL_INCLUDE_CONTEXT_SWITCH, "Average Totals Including Context Switch");

            ++currentRow;
        }

        currentRow = 0;
        this.setColorfulTitle(worksheet, currentRow++, 3, "Averages of all Tests");

        this.setTitle(worksheet, currentRow, 3, "Average Generating all JSONs");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.GENERATE_JSON).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Iterating all JSONs Iteratively - BFS");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.ITERATE_ITERATIVELY).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Iterating all JSONs Recursively - DFS");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.ITERATE_RECURSIVELY).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Deserializing all JSONs");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.DESERIALIZE_JSON).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Serializing all JSONs");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.SERIALIZE_JSON).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Totals");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.TOTAL).getAverage());
        this.setTitle(worksheet, currentRow, 3, "Average Totals Including Context Switch");
        this.setValue(worksheet, currentRow++, 4, this.averageAllJsons.get(Reporter.MeasurementType.TOTAL_INCLUDE_CONTEXT_SWITCH).getAverage());

        ++currentRow;

        this.setTitle(worksheet, currentRow, 3, "Totals of all Tests Including Context Switch");
        this.setValue(worksheet, currentRow, 4, this.lengthWholeTest);
    }

    private void addAverageData(@NotNull final Sheet worksheet, int currentRow,
                                @NotNull final Map<Reporter.MeasurementType, MathDataCollector> testData,
                                @NotNull final Reporter.MeasurementType testName, @NotNull final String title) {
        final Double value = testData.get(testName).getAverage();
        this.setTitle(worksheet, currentRow, 0, title);
        if (value != null)
            this.setValue(worksheet, currentRow, 1, value);
    }
    //#endregion

    //#region Add about worksheet
    private void createAboutWorksheet() {
        @NotNull final Sheet worksheet = this.workbook.createSheet("About");

        int currentRow = 0;
        for (@NotNull final Config config : this.inputConfiguration) {
            this.setColorfulTitle(worksheet, currentRow++, 0, config.getName());

            this.setTitle(worksheet, currentRow, 0, "Size");
            this.setTitle(worksheet, currentRow++, 1, config.getSize());

            this.setTitle(worksheet, currentRow, 0, "Number Of Letters");
            this.setTitle(worksheet, currentRow++, 1, config.getNumberOfLetters());

            this.setTitle(worksheet, currentRow, 0, "Depth");
            this.setTitle(worksheet, currentRow++, 1, config.getDepth());

            this.setTitle(worksheet, currentRow, 0, "Number Of Children");
            this.setTitle(worksheet, currentRow++, 1, config.getNumberOfChildren());

            this.setTitle(worksheet, currentRow, 0, "Path");
            this.setTitle(worksheet, currentRow++, 1, config.getPath());

            ++currentRow;
        }

        resizeColumns(worksheet, null);
    }
    //#endregion

    //#region Excel Utils
    private void setColorfulTitle(@NotNull final Sheet worksheet, final int rowNumber, final int columnNumber, @NotNull final String title) {
        CellRangeAddress cells = new CellRangeAddress(rowNumber, rowNumber, columnNumber, columnNumber + 1);
        @NotNull final Cell titleCell = getOrCreateCell(worksheet, rowNumber, columnNumber);
        titleCell.setCellValue(title);
        forEachCell(worksheet, cells, cell -> cell.setCellStyle(this.styleColorfulBorderAndCenter));
        worksheet.addMergedRegion(cells);
    }

    private void setTitle(@NotNull final Sheet worksheet, final int rowNumber, final int columnNumber, @NotNull final String title) {
        @NotNull final Cell cell = getOrCreateCell(worksheet, rowNumber, columnNumber);
        cell.setCellValue(title);
        cell.setCellStyle(this.styleBorder);
    }

    private void setTitle(@NotNull final Sheet worksheet, final int rowNumber, final int columnNumber, final int title) {
        @NotNull final Cell cell = getOrCreateCell(worksheet, rowNumber, columnNumber);
        cell.setCellValue(title);
        cell.setCellStyle(this.styleBorder);
    }

    private void setValue(@NotNull final Sheet worksheet, final int rowNumber, final int columnNumber, @Nullable final Double value) {
        @NotNull final Cell cell = getOrCreateCell(worksheet, rowNumber, columnNumber);
        if (value != null)
            cell.setCellValue(Math.round(value * 1000) / 1000.0);
        cell.setCellStyle(this.styleBorderAndCenter);
    }

    private static @NotNull Cell getOrCreateCell(@NotNull final Sheet worksheet, final int rowNumber, final int columnNumber) {
        @NotNull final Row row = getOrCreateRow(worksheet, rowNumber);
        return getOrCreateCell(row, columnNumber);
    }

    private static @NotNull Cell getOrCreateCell(@NotNull final Row row, final int columnNumber) {
        Cell cell = row.getCell(columnNumber);
        if (cell != null)
            return cell;
        else
            return row.createCell(columnNumber);
    }

    private static @NotNull Row getOrCreateRow(@NotNull final Sheet worksheet, final int rowNumber) {
        Row row = worksheet.getRow(rowNumber);
        if (row != null)
            return row;
        else
            return worksheet.createRow(rowNumber);
    }

    private static void resizeColumns(@NotNull final Sheet worksheet, @Nullable final Map<Integer, Integer> preDefinedColumns) {
        if (worksheet.getPhysicalNumberOfRows() <= 0)
            return;

        worksheet
                .getRow(worksheet.getFirstRowNum())
                .forEach(cell ->
                        worksheet.autoSizeColumn(cell.getColumnIndex())
                );

        if (preDefinedColumns != null)
            preDefinedColumns.forEach(worksheet::setColumnWidth);
    }

    private static void forEachCell(@NotNull final Sheet worksheet, @NotNull final CellRangeAddress cells, Consumer<Cell> callback) {
        cells.forEach(cellAddress -> {
            @NotNull final Cell currentCell = getOrCreateCell(worksheet, cellAddress.getRow(), cellAddress.getColumn());
            callback.accept(currentCell);
        });
    }
    //#endregion

    @Override
    public void close() throws IOException {
        try {
            this.addAverageWorksheet();
            this.createAboutWorksheet();
        } catch (Exception exception) {
            System.err.println("Unexpected error at adding summary/about worksheet: " + exception);
        }
        try (@NotNull final OutputStream outputStream = Files.newOutputStream(Paths.get(this.pathToSaveFile))) {
            this.workbook.write(outputStream);
        } finally {
            this.workbook.close();
        }
    }
}
