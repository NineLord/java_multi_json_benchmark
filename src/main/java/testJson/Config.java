package testJson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Config {

    private @NotNull
    final String name;
    private @NotNull
    final String size;
    private @NotNull
    final String path;
    private final int numberOfLetters;
    private final int depth;
    private final int numberOfChildren;
    private @Nullable String raw;

    public Config(@NotNull String name, @NotNull String size, @NotNull String path, int numberOfLetters, int depth, int numberOfChildren) {
        this.name = name;
        this.size = size;
        this.path = path;
        this.numberOfLetters = numberOfLetters;
        this.depth = depth;
        this.numberOfChildren = numberOfChildren;
        this.raw = null;
    }

    public void setRaw(@NotNull String raw) {
        this.raw = raw;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getSize() {
        return size;
    }

    public @NotNull String getPath() {
        return path;
    }

    public int getNumberOfLetters() {
        return numberOfLetters;
    }

    public int getDepth() {
        return depth;
    }

    public int getNumberOfChildren() {
        return numberOfChildren;
    }

    public @Nullable String getRaw() {
        return raw;
    }
}
