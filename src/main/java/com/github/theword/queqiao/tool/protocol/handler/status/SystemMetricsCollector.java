package com.github.theword.queqiao.tool.protocol.handler.status;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

final class SystemMetricsCollector {
    private static final String FIELD_CPU_CORES = "cpu_cores";
    private static final String FIELD_LOAD_AVERAGE = "load_average";
    private static final String FIELD_SYSTEM_LOAD = "system_load";
    private static final String FIELD_PROCESS_LOAD = "process_load";
    private static final String FIELD_PHYSICAL_MEMORY = "physical_memory";
    private static final String FIELD_JVM_MEMORY = "jvm_memory";
    private static final String FIELD_TOTAL = "total";
    private static final String FIELD_FREE = "free";
    private static final String FIELD_USED = "used";
    private static final String FIELD_MAX = "max";
    private static final String FIELD_PERCENTAGE = "percentage";

    private final java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    Map<String, Object> collectCpuInformation() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put(FIELD_CPU_CORES, Runtime.getRuntime().availableProcessors());
        cpu.put(FIELD_LOAD_AVERAGE, round(osBean.getSystemLoadAverage()));
        cpu.put(FIELD_SYSTEM_LOAD, toPercent(invokeDoubleGetter("getSystemCpuLoad", "getCpuLoad")));
        cpu.put(FIELD_PROCESS_LOAD, toPercent(invokeDoubleGetter("getProcessCpuLoad")));
        return cpu;
    }

    Map<String, Object> collectMemoryInformation() {
        Map<String, Object> memoryInformation = new LinkedHashMap<>();

        long physicalTotal = invokeLongGetter("getTotalPhysicalMemorySize", "getTotalMemorySize");
        long physicalFree = invokeLongGetter("getFreePhysicalMemorySize", "getFreeMemorySize");
        long physicalUsed = (physicalTotal >= 0 && physicalFree >= 0) ? (physicalTotal - physicalFree) : -1L;

        Map<String, Object> physicalMemory = new LinkedHashMap<>();
        physicalMemory.put(FIELD_TOTAL, physicalTotal);
        physicalMemory.put(FIELD_FREE, physicalFree);
        physicalMemory.put(FIELD_USED, physicalUsed);
        physicalMemory.put(FIELD_PERCENTAGE, calculatePercentage(physicalUsed, physicalTotal));

        Runtime runtime = Runtime.getRuntime();
        long jvmTotal = runtime.totalMemory();
        long jvmFree = runtime.freeMemory();
        long jvmMax = runtime.maxMemory();
        long jvmUsed = jvmTotal - jvmFree;

        Map<String, Object> jvmMemory = new LinkedHashMap<>();
        jvmMemory.put(FIELD_TOTAL, jvmTotal);
        jvmMemory.put(FIELD_FREE, jvmFree);
        jvmMemory.put(FIELD_MAX, jvmMax);
        jvmMemory.put(FIELD_USED, jvmUsed);
        jvmMemory.put(FIELD_PERCENTAGE, calculatePercentage(jvmUsed, jvmMax));

        memoryInformation.put(FIELD_PHYSICAL_MEMORY, physicalMemory);
        memoryInformation.put(FIELD_JVM_MEMORY, jvmMemory);
        return memoryInformation;
    }

    private double invokeDoubleGetter(String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = osBean.getClass().getMethod(methodName);
                Object value = method.invoke(osBean);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } catch (Exception ignored) {
            }
        }
        return -1D;
    }

    private long invokeLongGetter(String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = osBean.getClass().getMethod(methodName);
                Object value = method.invoke(osBean);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } catch (Exception ignored) {
            }
        }
        return -1L;
    }

    private double toPercent(double value) {
        if (value < 0) {
            return -1D;
        }
        return round(value * 100);
    }

    private double calculatePercentage(long used, long total) {
        if (used < 0 || total <= 0) {
            return -1D;
        }
        return round((used * 100D) / total);
    }

    private double round(double value) {
        if (value < 0) {
            return -1D;
        }
        return Math.round(value * 100D) / 100D;
    }
}
