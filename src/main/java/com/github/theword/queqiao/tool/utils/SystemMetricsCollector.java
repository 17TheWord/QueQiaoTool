package com.github.theword.queqiao.tool.utils;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

final class SystemMetricsCollector {
    private final java.lang.management.OperatingSystemMXBean osBean =
            ManagementFactory.getOperatingSystemMXBean();

    Map<String, Object> collectCpuInformation() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("cpu_cores", Runtime.getRuntime().availableProcessors());
        cpu.put("load_average", round(osBean.getSystemLoadAverage()));
        cpu.put("system_load", toPercent(invokeDoubleGetter("getSystemCpuLoad", "getCpuLoad")));
        cpu.put("process_load", toPercent(invokeDoubleGetter("getProcessCpuLoad")));
        return cpu;
    }

    Map<String, Object> collectMemoryInformation() {
        Map<String, Object> memoryInformation = new LinkedHashMap<>();

        long physicalTotal = invokeLongGetter("getTotalPhysicalMemorySize", "getTotalMemorySize");
        long physicalFree = invokeLongGetter("getFreePhysicalMemorySize", "getFreeMemorySize");
        long physicalUsed = (physicalTotal >= 0 && physicalFree >= 0) ? (physicalTotal - physicalFree) : -1L;

        Map<String, Object> physicalMemory = new LinkedHashMap<>();
        physicalMemory.put("total", physicalTotal);
        physicalMemory.put("free", physicalFree);
        physicalMemory.put("used", physicalUsed);
        physicalMemory.put("percentage", calculatePercentage(physicalUsed, physicalTotal));

        Runtime runtime = Runtime.getRuntime();
        long jvmTotal = runtime.totalMemory();
        long jvmFree = runtime.freeMemory();
        long jvmMax = runtime.maxMemory();
        long jvmUsed = jvmTotal - jvmFree;

        Map<String, Object> jvmMemory = new LinkedHashMap<>();
        jvmMemory.put("total", jvmTotal);
        jvmMemory.put("free", jvmFree);
        jvmMemory.put("max", jvmMax);
        jvmMemory.put("used", jvmUsed);
        jvmMemory.put("percentage", calculatePercentage(jvmUsed, jvmMax));

        memoryInformation.put("physical_memory", physicalMemory);
        memoryInformation.put("jvm_memory", jvmMemory);
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
