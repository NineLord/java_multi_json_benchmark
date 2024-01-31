package junk;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import testJson.Reporter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) throws Exception {
        testJsonArray();
    }

    public static void getPcUsage() {
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long physicalMemorySize = os.getTotalPhysicalMemorySize();
        long freePhysicalMemory = os.getFreePhysicalMemorySize();
        long freeSwapSize = os.getFreeSwapSpaceSize();
        long committedVirtualMemorySize = os.getCommittedVirtualMemorySize();
    }

    public static int getInt() {
        return 5;
    }

    public static void generateExcel() {
        @NotNull final String pathToFile = "/PATH/TO/report2.xlsx";
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet worksheet = workbook.createSheet("Test");
            Row row1 = worksheet.createRow(0);
            Cell cell11 = row1.createCell(0);
            cell11.setCellValue("B");
            workbook.write(Files.newOutputStream(Paths.get(pathToFile)));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void forEach() {
        IntStream.range(0, 3).forEachOrdered(index -> {
            System.out.println(index);
        });
    }

    public static void enumToString() {
        Map<String, Integer> map = new HashMap<>();
        map.put(Reporter.MeasurementType.DESERIALIZE_JSON.name(), 1);
        System.out.println(map);
    }

    public static void testAllWs() {
        testWsHttp("http://layer-sub:7000");
        testWsHttp("https://layer-sub:7000");
        testWsHttp("ws://layer-sub:7000");
        testWsHttp("wss://layer-sub:7000");
        testWsHttp("shimi://layer-sub:7000");
        testWsHttp("tavory-layer-sub:7000");
    }

    public static void testWsHttp(@NotNull final String input) {
        System.out.println("Input: '" + input + "'\t; Output: '" + changeHttpToWsProtocol(input) + "'");
    }

    public static @NotNull String changeHttpToWsProtocol(@NotNull final String input) {
        if (input.startsWith("http://"))
            return input.replaceFirst("http://", "ws://");
        else if (input.startsWith("https://"))
            return input.replaceFirst("https://", "wss://");
        else
            return input;
    }

    public static void testJsonArray() throws JSONException {
        JSONArray array = new JSONArray();
        array.put(5, "a");
        System.out.println(array);
    }
}
