package testJson;

import org.jetbrains.annotations.Nullable;

public class Measurement {
    private @Nullable Long startTime;
    private @Nullable Double duration;

    public @Nullable Long getStartTime() {
        return this.startTime;
    }

    public void setStartTime() {
        this.startTime = System.nanoTime();
    }

    public @Nullable Double getDuration() {
        return this.duration;
    }

    public void setFinishTime() {
        Long startTime = this.getStartTime();
        if (startTime == null)
            throw new RuntimeException("Can't set duration of measurement that don't have start time");
        final long durationNano = System.nanoTime() - startTime;
        this.duration = durationNano / 1_000_000.0;
    }
}
