package testJson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testJson.multithreading.RunTestLoop;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Main {

    @NotNull private static final Type configType = new TypeToken<ArrayList<Config>>() {}.getType();
    @NotNull private static final Reporter reporter = Reporter.getInstance();

    // Add the following arguments to VM options: `-Xmx8G -Xms16M -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20`

    private static @NotNull List<Config> parseAndValidateConfigFile(@NotNull final String pathToConfigFile) {
        @Nullable List<Config> configs = null;
        try {
            @NotNull final String rawJson = new String(Files.readAllBytes(Paths.get(pathToConfigFile)));
            configs = new Gson().fromJson(rawJson, configType);
        } catch (IOException exception) {
            System.err.println("Failed to parse the config file; exception: " + exception);
            System.exit(1);
        }

        return configs;
    }

    /*
     * Example for running this project:
     * rm -rf ~/.m2
     * mvn clean compile
     * mvn exec:java -Dexec.args="-s /PATH/TO/json_benchmark/benchmarks/output/report_Java_config_2s.xlsx /PATH/TO/json_benchmark/benchmarks/input/config_2.json 2 16"
     */

    public static void main(String[] args) throws IOException {
        //#region Test input
        // First argument "-s"
        final int testCounter = Math.max(Integer.parseInt(args[3]), 1);
        @NotNull final String configPath = args[2];
        @NotNull final List<Config> configs = parseAndValidateConfigFile(configPath);
        @NotNull final String saveFilePath = args[1];
        final int threadCount = Integer.parseInt(args[4]);
        final boolean isDebug = false;
        //#endregion

        //#region Getting ready for testing
        @NotNull final RunTestLoop testRunner = new RunTestLoop(threadCount, testCounter, 2_000_000L);
        @NotNull final List<String> testNames = new LinkedList<>();
        for (@NotNull final Config testCase : configs) {
            try {
                testCase.setRaw(new String(Files.readAllBytes(Paths.get(testCase.getPath()))));
            } catch (IOException exception) {
                System.err.println("Failed to read the file: " + testCase.getPath() + " ; Exception: " + exception);
                System.exit(1);
            }
            testNames.add(testCase.getName());
        }
        @NotNull final Measurement wholeTestMeasurement = new Measurement();
        //#endregion

        //#region Testing
        wholeTestMeasurement.setStartTime();
        @NotNull final List<CompletableFuture<Void>> futures = new LinkedList<>();
        for (@NotNull final Config testCase : configs) {
            assert testCase.getRaw() != null;
            futures.add(testRunner.runTest(
                    testCase.getName(),
                    testCase.getNumberOfLetters(),
                    testCase.getDepth(),
                    testCase.getNumberOfChildren(),
                    testCase.getRaw()
            ));
        }
        testRunner.signalFinishedSubmittingTests();
        futures.forEach(CompletableFuture::join);
        wholeTestMeasurement.setFinishTime();

        Double wholeTestDuration = wholeTestMeasurement.getDuration();
        assert wholeTestDuration != null;

        //noinspection ConstantConditions
        if (isDebug) {
            System.out.println(new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(reporter.getMeasurementDuration()));
            System.out.println("Whole test: " + wholeTestDuration);
        }

        try (@NotNull final ExcelGenerator excelGenerator = new ExcelGenerator(
                saveFilePath,
                testNames,
                wholeTestDuration,
                configs
        )) {
            Map<String, Map<String, Map<Reporter.MeasurementType, Double>>> database = reporter.getMeasurementDuration();
            for (int counter = 1; counter <= testCounter; ++counter) {
                @NotNull final String testName = "Test " + counter;
                excelGenerator.appendWorksheet(testName, database.get(testName));
            }
        }

        System.out.println("Done!");
        //#endregion
    }

}
