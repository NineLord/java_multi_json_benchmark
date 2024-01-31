package testJson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Reporter {

    private static final Reporter instance = new Reporter();

    private final ReadWriteLock measurementLock;
    private final Map<String, Map<String, Map<MeasurementType, Measurement>>> measurement;

    //#region Constructor
    public static Reporter getInstance() {
        return instance;
    }

    private Reporter() {
        this.measurementLock = new ReentrantReadWriteLock();
        this.measurement = new HashMap<>();
    }
    //#endregion

    public @NotNull Map<String, Map<String, Map<MeasurementType, Double>>> getMeasurementDuration() {
        this.measurementLock.readLock().lock();
        try {
            return this.measurement.entrySet().stream().map(testEntry -> {
                @NotNull String testCount = testEntry.getKey();
                @NotNull Map<String, Map<MeasurementType, Measurement>> jsonMap = testEntry.getValue();
                @NotNull Map<String, Map<MeasurementType, Double>> newJsonMap = jsonMap.entrySet().stream().map(jsonEntry -> {
                    @NotNull String jsonName = jsonEntry.getKey();
                    @NotNull Map<MeasurementType, Measurement> measurementMap = jsonEntry.getValue();
                    @NotNull Map<MeasurementType, Double> newMeasurementMap = measurementMap.entrySet().stream().map(measurementEntry -> {
                        @NotNull MeasurementType measurementType = measurementEntry.getKey();
                        @NotNull Measurement measurement = measurementEntry.getValue();
                        return new SimpleImmutableEntry<>(measurementType, measurement.getDuration());
                    }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                    return new SimpleImmutableEntry<>(jsonName, newMeasurementMap);
                }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
               return new SimpleImmutableEntry<>(testCount, newJsonMap);
            }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        } finally {
            this.measurementLock.readLock().unlock();
        }
    }

    //#region Generic Measurement
    public void measure(@NotNull final String testCount, @NotNull final String jsonName, @NotNull final MeasurementType measurementType, Runnable callback) {
        this.startMeasuring(testCount, jsonName, measurementType);
        try {
            callback.run();
        } finally {
            this.finishMeasuring(testCount, jsonName, measurementType);
        }
    }

    public <V> V measureEx(@NotNull final String testCount, @NotNull final String jsonName, @NotNull final MeasurementType measurementType, Callable<V> callback) throws Exception {
        this.startMeasuring(testCount, jsonName, measurementType);
        try {
            return callback.call();
        } finally {
            this.finishMeasuring(testCount, jsonName, measurementType);
        }
    }

    public void startMeasuring(@NotNull final String testCount, @NotNull final String jsonName, @NotNull final MeasurementType measurementType) {
        this.measurementLock.writeLock().lock();
        try {
            @NotNull Map<String, Map<MeasurementType, Measurement>> testMap = this.measurement.computeIfAbsent(testCount, k -> new HashMap<>());
            @NotNull Map<MeasurementType, Measurement> jsonMap = testMap.computeIfAbsent(jsonName, k -> new HashMap<>());
            @NotNull Measurement measurement = jsonMap.computeIfAbsent(measurementType, k -> new Measurement());
            measurement.setStartTime();
        } finally {
            this.measurementLock.writeLock().unlock();
        }
    }

    public void finishMeasuring(@NotNull final String testCount, @NotNull final String jsonName, @NotNull final MeasurementType measurementType) {
        this.measurementLock.writeLock().lock();
        try {
            @Nullable Map<String, Map<MeasurementType, Measurement>> testMap = this.measurement.get(testCount);
            if (testMap == null)
                throw new RuntimeException(String.format("Tried to finish measuring before starting measuring, testCount=%s ; jsonName=%s ; measurementType=%s",
                        testCount, jsonName, measurementType.name()));
            @Nullable Map<MeasurementType, Measurement> jsonMap = testMap.get(jsonName);
            if (jsonMap == null)
                throw new RuntimeException(String.format("Tried to finish measuring before starting measuring, testCount=%s ; jsonName=%s ; measurementType=%s",
                        testCount, jsonName, measurementType.name()));
            @Nullable Measurement measurement = jsonMap.get(measurementType);
            if (measurement == null)
                throw new RuntimeException(String.format("Tried to finish measuring before starting measuring, testCount=%s ; jsonName=%s ; measurementType=%s",
                        testCount, jsonName, measurementType.name()));
            measurement.setFinishTime();
        } finally {
            this.measurementLock.writeLock().unlock();
        }
    }
    //#endregion

    public enum MeasurementType {
        GENERATE_JSON,
        DESERIALIZE_JSON,
        ITERATE_ITERATIVELY,
        ITERATE_RECURSIVELY,
        SERIALIZE_JSON,
        TOTAL,
        TOTAL_INCLUDE_CONTEXT_SWITCH
    }

}
