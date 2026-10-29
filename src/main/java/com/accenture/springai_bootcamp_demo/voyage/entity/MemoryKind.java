package com.accenture.springai_bootcamp_demo.voyage.entity;

/**
 * What kind of experience a memory is. Importance (1-10) is fixed per kind
 * instead of asking the model to rate every memory, which would cost an extra
 * call each time.
 */
public enum MemoryKind {
    ROUTINE(2),
    ACTION(3),
    ALARM(4),
    CONVERSATION(5),
    ANNOUNCEMENT(6),
    REFLECTION(7),
    WITNESSED(8);

    private final int importance;

    MemoryKind(int importance) {
        this.importance = importance;
    }

    public int importance() {
        return importance;
    }
}