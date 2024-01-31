package testJson.multithreading;

import jsonGenerator.Generator;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import searchTree.BreadthFirstSearch;
import searchTree.DepthFirstSearch;
import testJson.Reporter;
import utils.GsonClass;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunTestLoop {

    @NotNull private static final String charactersPoll = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz!@#$%&";
    @NotNull private static final Reporter reporter = Reporter.getInstance();

    private final ExecutorService threadPool;
    private final int testCounter;
    private final long valueToSearch;
    private boolean hasAllTestBeenSubmitted;
    private int numberOfFinishedTests;
    private int numberOfTests;

    public RunTestLoop(int workerCount, int testCounter, long valueToSearch) {
        this.threadPool = Executors.newFixedThreadPool(workerCount);
        this.testCounter = testCounter;
        this.valueToSearch = valueToSearch;
        this.hasAllTestBeenSubmitted = false;
        this.numberOfFinishedTests = 0;
        this.numberOfTests = 0;
    }

    public @NotNull CompletableFuture<Void> runTest(@NotNull String jsonName, int numberOfLetters, int depth, int numberOfChildren, @NotNull String rawJson) {
        ++this.numberOfTests;
        @NotNull final String testName = "Test 1";
        @NotNull CompletableFuture<String> testFuture = this.runSingleTest(testName, jsonName, numberOfLetters, depth, numberOfChildren, rawJson);

        for (int counter = 1; counter < this.testCounter; ++counter) {
            @NotNull final String innerTestName = "Test " + (counter + 1);
            testFuture = testFuture.thenCompose( (_ignored) ->
                    this.runSingleTest(innerTestName, jsonName, numberOfLetters, depth, numberOfChildren, rawJson));
        }

        return testFuture.thenRun(() -> {
            ++this.numberOfFinishedTests;
            this.tryTerminate();
        });
    }

    public void signalFinishedSubmittingTests() {
        this.hasAllTestBeenSubmitted = true;
        this.tryTerminate();
    }

    private @NotNull CompletableFuture<String> runSingleTest(@NotNull String testCount, @NotNull String jsonName, int numberOfLetters, int depth, int numberOfChildren, @NotNull String rawJson) {
        reporter.startMeasuring(testCount, jsonName, Reporter.MeasurementType.TOTAL_INCLUDE_CONTEXT_SWITCH);
        return CompletableFuture.runAsync(() -> reporter.measure(testCount, jsonName, Reporter.MeasurementType.GENERATE_JSON, () -> {
            try {
                Generator.generateJson(charactersPoll, numberOfLetters, depth, numberOfChildren);
            } catch (JSONException exception) {
                throw new RuntimeException("Failed to generate a JSON: " + exception);
            }
        }), this.threadPool)
                .thenApplyAsync((_void) -> {
                    try {
                        return reporter.measureEx(testCount, jsonName, Reporter.MeasurementType.DESERIALIZE_JSON, () -> GsonClass.fromJson(rawJson));
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                }, this.threadPool)
                .thenApplyAsync((json) -> {
                    reporter.measure(testCount, jsonName, Reporter.MeasurementType.ITERATE_ITERATIVELY, () -> {
                        if (BreadthFirstSearch.run(json, this.valueToSearch))
                            throw new RuntimeException("BFS the tree found value that shouldn't be in it: " + this.valueToSearch);
                    });
                    return json;
                }, this.threadPool)
                .thenApplyAsync((json) -> {
                    reporter.measure(testCount, jsonName, Reporter.MeasurementType.ITERATE_RECURSIVELY, () -> {
                        if (DepthFirstSearch.run(json, this.valueToSearch))
                            throw new RuntimeException("DFS the tree found value that shouldn't be in it: " + this.valueToSearch);
                    });
                    return json;
                }, this.threadPool)
                .thenApplyAsync((json) -> {
                    try {
                        return reporter.measureEx(testCount, jsonName, Reporter.MeasurementType.SERIALIZE_JSON, () -> GsonClass.toJson(json));
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    } finally {
                        reporter.finishMeasuring(testCount, jsonName, Reporter.MeasurementType.TOTAL_INCLUDE_CONTEXT_SWITCH);
                    }
                }, this.threadPool);
    }

    private synchronized void tryTerminate() {
        if (this.hasAllTestBeenSubmitted && this.numberOfTests == this.numberOfFinishedTests)
            this.threadPool.shutdown();
    }
}
