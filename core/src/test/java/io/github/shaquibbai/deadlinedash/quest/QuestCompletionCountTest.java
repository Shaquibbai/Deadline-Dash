package io.github.shaquibbai.deadlinedash.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class QuestCompletionCountTest {

    private QuestManager questManager;

    @BeforeEach
    void setUp() {
        questManager = new QuestManager();
    }

    @Test
    @DisplayName("Initial completed task count is 0")
    void testInitialCompletedTaskCount() {
        assertEquals(0, questManager.getCompletedTaskCount());
    }

    @Test
    @DisplayName("recordTaskCompleted increments count and is idempotent against duplicates")
    void testRecordTaskCompletedIdempotence() {
        AtomicInteger listenerCallCount = new AtomicInteger(0);
        AtomicReference<String> lastCompletedQuest = new AtomicReference<>(null);
        AtomicInteger lastTotalCount = new AtomicInteger(0);

        questManager.setQuestCompletionListener((questId, total) -> {
            listenerCallCount.incrementAndGet();
            lastCompletedQuest.set(questId);
            lastTotalCount.set(total);
        });

        // 1. Record Quest 1
        questManager.recordTaskCompleted("Quest1");
        assertEquals(1, questManager.getCompletedTaskCount());
        assertEquals(1, listenerCallCount.get());
        assertEquals("Quest1", lastCompletedQuest.get());
        assertEquals(1, lastTotalCount.get());

        // 2. Duplicate Quest 1 call should be ignored
        questManager.recordTaskCompleted("Quest1");
        assertEquals(1, questManager.getCompletedTaskCount(), "Duplicate completion must not increase count");
        assertEquals(1, listenerCallCount.get(), "Duplicate completion must not trigger listener again");

        // 3. Record Quest 2
        questManager.recordTaskCompleted("Quest2");
        assertEquals(2, questManager.getCompletedTaskCount());
        assertEquals(2, listenerCallCount.get());
        assertEquals("Quest2", lastCompletedQuest.get());
        assertEquals(2, lastTotalCount.get());

        // 4. Duplicate Quest 2 call should be ignored
        questManager.recordTaskCompleted("Quest2");
        assertEquals(2, questManager.getCompletedTaskCount());
        assertEquals(2, listenerCallCount.get());
    }

    @Test
    @DisplayName("resetState clears completed task count and allows re-recording")
    void testResetStateClearsCount() {
        questManager.recordTaskCompleted("Quest1");
        questManager.recordTaskCompleted("Quest2");
        assertEquals(2, questManager.getCompletedTaskCount());

        questManager.resetState();
        assertEquals(0, questManager.getCompletedTaskCount());

        questManager.recordTaskCompleted("Quest1");
        assertEquals(1, questManager.getCompletedTaskCount());
    }
}