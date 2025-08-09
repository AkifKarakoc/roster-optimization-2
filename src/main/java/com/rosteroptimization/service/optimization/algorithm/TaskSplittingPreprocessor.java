package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.Qualification;
import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent task splitting preprocessor that analyzes tasks and determines
 * optimal splitting strategies for better roster optimization
 */
@Component
@Slf4j
public class TaskSplittingPreprocessor {

    /**
     * Preprocess tasks with intelligent splitting decisions
     */
    public TaskSplittingResult preprocessTasks(List<Task> originalTasks, List<Shift> availableShifts) {
        log.info("Starting intelligent task preprocessing for {} tasks", originalTasks.size());

        if (availableShifts.isEmpty()) {
            log.warn("No shifts available for task splitting analysis");
            return TaskSplittingResult.noSplitting(originalTasks);
        }

        double minShiftHours = calculateMinimumShiftHours(availableShifts);
        double maxShiftHours = calculateMaximumShiftHours(availableShifts);

        log.debug("Shift capacity range: {:.1f}h - {:.1f}h", minShiftHours, maxShiftHours);

        List<Task> processedTasks = new ArrayList<>();
        Map<Long, TaskSplittingDecision> splittingDecisions = new HashMap<>();
        int totalSplitCount = 0;

        for (Task originalTask : originalTasks) {
            double taskHours = calculateTaskHours(originalTask);
            TaskSplittingDecision decision = analyzeTaskSplittingOptions(
                    originalTask, taskHours, minShiftHours, maxShiftHours, availableShifts);

            splittingDecisions.put(originalTask.getId(), decision);

            if (decision.isShouldSplit()) {
                List<Task> splitTasks = executeSplitting(originalTask, decision);
                processedTasks.addAll(splitTasks);
                totalSplitCount += splitTasks.size() - 1;

                log.debug("Split task {} ({:.1f}h) into {} parts using strategy: {}",
                        originalTask.getName(), taskHours, splitTasks.size(), decision.getStrategy());
            } else {
                processedTasks.add(originalTask);
            }
        }

        log.info("Task preprocessing completed: {} tasks processed, {} splits created",
                processedTasks.size(), totalSplitCount);

        return TaskSplittingResult.builder()
                .originalTasks(originalTasks)
                .processedTasks(processedTasks)
                .splittingDecisions(splittingDecisions)
                .splitTasksCount(totalSplitCount)
                .build();
    }

    /**
     * Analyze splitting options for a single task
     */
    private TaskSplittingDecision analyzeTaskSplittingOptions(Task task, double taskHours,
                                                              double minShiftHours, double maxShiftHours,
                                                              List<Shift> availableShifts) {

        // No splitting needed if task fits in smallest shift
        if (taskHours <= minShiftHours + 0.5) { // 30 min buffer
            return TaskSplittingDecision.noSplit("Task fits in minimum shift capacity");
        }

        // Cannot be handled if exceeds maximum possible capacity
        if (taskHours > maxShiftHours * 3) { // Max 3 consecutive shifts
            log.warn("Task {} ({:.1f}h) exceeds maximum possible capacity ({:.1f}h)",
                    task.getName(), taskHours, maxShiftHours * 3);
            return TaskSplittingDecision.noSplit("Task exceeds maximum capacity");
        }

        // Analyze different splitting strategies
        List<SplittingStrategy> strategies = analyzeSplittingStrategies(task, taskHours, availableShifts);

        // Select best strategy
        SplittingStrategy bestStrategy = selectBestStrategy(strategies, task);

        return TaskSplittingDecision.builder()
                .shouldSplit(true)
                .strategy(bestStrategy)
                .reason("Optimal splitting strategy selected")
                .build();
    }

    /**
     * Analyze different splitting strategies
     */
    private List<SplittingStrategy> analyzeSplittingStrategies(Task task, double taskHours,
                                                               List<Shift> availableShifts) {
        List<SplittingStrategy> strategies = new ArrayList<>();

        // Strategy 1: Equal time splits
        for (Shift shift : availableShifts) {
            double shiftHours = calculateShiftHours(shift);
            if (shiftHours >= taskHours / 4) { // Don't create too many tiny parts
                int numParts = (int) Math.ceil(taskHours / shiftHours);
                if (numParts <= 4) { // Max 4 parts
                    strategies.add(SplittingStrategy.builder()
                            .type(SplittingStrategy.Type.EQUAL_TIME)
                            .targetShift(shift)
                            .numParts(numParts)
                            .efficiency(calculateEfficiency(taskHours, shiftHours, numParts))
                            .flexibility(calculateFlexibility(numParts))
                            .build());
                }
            }
        }

        // Strategy 2: Optimal capacity utilization
        Shift bestShift = findBestCapacityShift(availableShifts, taskHours);
        if (bestShift != null) {
            double shiftHours = calculateShiftHours(bestShift);
            int optimalParts = (int) Math.ceil(taskHours / shiftHours);
            strategies.add(SplittingStrategy.builder()
                    .type(SplittingStrategy.Type.OPTIMAL_CAPACITY)
                    .targetShift(bestShift)
                    .numParts(optimalParts)
                    .efficiency(calculateEfficiency(taskHours, shiftHours, optimalParts))
                    .flexibility(calculateFlexibility(optimalParts))
                    .build());
        }

        // Strategy 3: Minimize parts (use longest shifts)
        Shift longestShift = availableShifts.stream()
                .max(Comparator.comparingDouble(this::calculateShiftHours))
                .orElse(null);

        if (longestShift != null) {
            double shiftHours = calculateShiftHours(longestShift);
            int minParts = (int) Math.ceil(taskHours / shiftHours);
            strategies.add(SplittingStrategy.builder()
                    .type(SplittingStrategy.Type.MINIMIZE_PARTS)
                    .targetShift(longestShift)
                    .numParts(minParts)
                    .efficiency(calculateEfficiency(taskHours, shiftHours, minParts))
                    .flexibility(calculateFlexibility(minParts))
                    .build());
        }

        return strategies;
    }

    /**
     * Select the best splitting strategy
     */
    private SplittingStrategy selectBestStrategy(List<SplittingStrategy> strategies, Task task) {
        if (strategies.isEmpty()) {
            return SplittingStrategy.builder()
                    .type(SplittingStrategy.Type.EQUAL_TIME)
                    .numParts(2)
                    .efficiency(0.5)
                    .flexibility(0.5)
                    .build();
        }

        // Scoring function: weight efficiency and flexibility
        double efficiencyWeight = 0.7;
        double flexibilityWeight = 0.3;

        return strategies.stream()
                .max(Comparator.comparingDouble(strategy ->
                        strategy.getEfficiency() * efficiencyWeight +
                                strategy.getFlexibility() * flexibilityWeight))
                .orElse(strategies.get(0));
    }

    /**
     * Execute the selected splitting strategy
     */
    private List<Task> executeSplitting(Task originalTask, TaskSplittingDecision decision) {
        SplittingStrategy strategy = decision.getStrategy();
        int numParts = strategy.getNumParts();

        if (numParts <= 1) {
            return List.of(originalTask);
        }

        List<Task> splitTasks = new ArrayList<>();
        double totalHours = calculateTaskHours(originalTask);
        double hoursPerPart = totalHours / numParts;

        LocalDateTime currentStart = originalTask.getStartTime();

        for (int i = 0; i < numParts; i++) {
            Task splitTask = createSplitTask(originalTask, i + 1, numParts, currentStart, hoursPerPart);
            splitTasks.add(splitTask);

            // Calculate next start time
            currentStart = currentStart.plusMinutes((long)(hoursPerPart * 60));
        }

        return splitTasks;
    }

    /**
     * Create a virtual split task
     */
    private Task createSplitTask(Task originalTask, int partNumber, int totalParts,
                                 LocalDateTime startTime, double durationHours) {
        Task splitTask = new Task();

        // Generate virtual ID
        long virtualId = Long.parseLong(originalTask.getId() + String.format("%02d", partNumber));

        splitTask.setId(virtualId);
        splitTask.setName(originalTask.getName() + " (Part " + partNumber + "/" + totalParts + ")");
        splitTask.setDescription(originalTask.getDescription() + " - Split part " + partNumber);
        splitTask.setStartTime(startTime);
        splitTask.setEndTime(startTime.plusMinutes((long)(durationHours * 60)));
        splitTask.setPriority(originalTask.getPriority());
        splitTask.setActive(originalTask.getActive());

        // Copy relationships
        splitTask.setDepartment(originalTask.getDepartment());
        splitTask.setRequiredQualifications(new HashSet<>(originalTask.getRequiredQualifications()));

        return splitTask;
    }

    // === UTILITY METHODS ===

    private double calculateTaskHours(Task task) {
        long minutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
        return minutes / 60.0;
    }

    private double calculateShiftHours(Shift shift) {
        int startHour = shift.getStartTime().getHour();
        int startMinute = shift.getStartTime().getMinute();
        int endHour = shift.getEndTime().getHour();
        int endMinute = shift.getEndTime().getMinute();

        // Handle overnight shifts
        if (endHour < startHour) {
            endHour += 24;
        }

        double startDecimal = startHour + (startMinute / 60.0);
        double endDecimal = endHour + (endMinute / 60.0);

        return endDecimal - startDecimal;
    }

    private double calculateMinimumShiftHours(List<Shift> shifts) {
        return shifts.stream()
                .mapToDouble(this::calculateShiftHours)
                .min()
                .orElse(8.0);
    }

    private double calculateMaximumShiftHours(List<Shift> shifts) {
        return shifts.stream()
                .mapToDouble(this::calculateShiftHours)
                .max()
                .orElse(12.0);
    }

    private Shift findBestCapacityShift(List<Shift> shifts, double taskHours) {
        return shifts.stream()
                .filter(shift -> calculateShiftHours(shift) >= taskHours / 4)
                .min(Comparator.comparingDouble(shift ->
                        Math.abs(calculateShiftHours(shift) - (taskHours / Math.ceil(taskHours / calculateShiftHours(shift))))))
                .orElse(null);
    }

    private double calculateEfficiency(double taskHours, double shiftHours, int numParts) {
        double totalCapacity = shiftHours * numParts;
        return Math.min(1.0, taskHours / totalCapacity);
    }

    private double calculateFlexibility(int numParts) {
        // More parts = more flexibility for assignment, but diminishing returns
        return Math.min(1.0, numParts / 3.0);
    }

    // === RESULT CLASSES ===

    /**
     * Task splitting analysis result
     */
    @Data
    @Builder
    public static class TaskSplittingResult {
        private List<Task> originalTasks;
        private List<Task> processedTasks;
        private Map<Long, TaskSplittingDecision> splittingDecisions;
        private int splitTasksCount;

        public int getOriginalTasksCount() {
            return originalTasks != null ? originalTasks.size() : 0;
        }

        public boolean hasSplitTasks() {
            return splitTasksCount > 0;
        }

        public static TaskSplittingResult noSplitting(List<Task> tasks) {
            return TaskSplittingResult.builder()
                    .originalTasks(tasks)
                    .processedTasks(new ArrayList<>(tasks))
                    .splittingDecisions(new HashMap<>())
                    .splitTasksCount(0)
                    .build();
        }
    }

    /**
     * Decision about whether and how to split a task
     */
    @Data
    @Builder
    public static class TaskSplittingDecision {
        private boolean shouldSplit;
        private SplittingStrategy strategy;
        private String reason;

        public static TaskSplittingDecision noSplit(String reason) {
            return TaskSplittingDecision.builder()
                    .shouldSplit(false)
                    .reason(reason)
                    .build();
        }
    }

    /**
     * Splitting strategy details
     */
    @Data
    @Builder
    public static class SplittingStrategy {
        private Type type;
        private Shift targetShift;
        private int numParts;
        private double efficiency;
        private double flexibility;

        public enum Type {
            EQUAL_TIME,       // Split into equal time periods
            OPTIMAL_CAPACITY, // Split to optimize shift capacity utilization
            MINIMIZE_PARTS    // Use longest shifts to minimize number of parts
        }
    }
}