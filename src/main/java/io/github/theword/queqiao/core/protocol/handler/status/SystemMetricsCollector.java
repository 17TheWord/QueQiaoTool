package io.github.theword.queqiao.core.protocol.handler.status;

import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final String METRIC_SYSTEM_CPU = "system_cpu_load";
    private static final String METRIC_PROCESS_CPU = "process_cpu_load";
    private static final String METRIC_PHYSICAL_TOTAL = "physical_memory_total";
    private static final String METRIC_PHYSICAL_FREE = "physical_memory_free";

    private final java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final Logger logger;
    private final Map<String, List<Method>> methods = new LinkedHashMap<>();
    private final Set<String> loggedMetrics = ConcurrentHashMap.newKeySet();

    SystemMetricsCollector(Logger logger) {
        this.logger = logger;
        cacheMethods(METRIC_SYSTEM_CPU, "getSystemCpuLoad", "getCpuLoad");
        cacheMethods(METRIC_PROCESS_CPU, "getProcessCpuLoad");
        cacheMethods(METRIC_PHYSICAL_TOTAL, "getTotalPhysicalMemorySize", "getTotalMemorySize");
        cacheMethods(METRIC_PHYSICAL_FREE, "getFreePhysicalMemorySize", "getFreeMemorySize");
    }

    Map<String, Object> collectCpuInformation() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put(FIELD_CPU_CORES, Runtime.getRuntime().availableProcessors());
        cpu.put(FIELD_LOAD_AVERAGE, round(osBean.getSystemLoadAverage()));
        cpu.put(FIELD_SYSTEM_LOAD, toPercent(invokeDoubleGetter(METRIC_SYSTEM_CPU)));
        cpu.put(FIELD_PROCESS_LOAD, toPercent(invokeDoubleGetter(METRIC_PROCESS_CPU)));
        return cpu;
    }

    Map<String, Object> collectMemoryInformation() {
        Map<String, Object> memoryInformation = new LinkedHashMap<>();

        long physicalTotal = invokeLongGetter(METRIC_PHYSICAL_TOTAL);
        long physicalFree = invokeLongGetter(METRIC_PHYSICAL_FREE);
        long physicalUsed = (physicalTotal >= 0 && physicalFree >= 0 && physicalFree <= physicalTotal)
                ? physicalTotal - physicalFree : -1L;

        Map<String, Object> physicalMemory = new LinkedHashMap<>();
        physicalMemory.put(FIELD_TOTAL, physicalTotal);
        physicalMemory.put(FIELD_FREE, physicalFree);
        physicalMemory.put(FIELD_USED, physicalUsed);
        physicalMemory.put(FIELD_PERCENTAGE, calculatePercentage(physicalUsed, physicalTotal));

        Runtime runtime = Runtime.getRuntime();
        long jvmTotal = runtime.totalMemory();
        long jvmFree = runtime.freeMemory();
        long jvmMax = runtime.maxMemory();
        long jvmUsed = jvmFree <= jvmTotal ? jvmTotal - jvmFree : -1L;

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

    private void cacheMethods(String metric, String... methodNames) {
        List<Method> resolved = new ArrayList<>();
        for (String methodName : methodNames) {
            try {
                resolved.add(resolvePublicMethod(osBean.getClass(), methodName));
            } catch (NoSuchMethodException | SecurityException ignored) {
                // 不同 JDK/OS 暴露的 OperatingSystemMXBean 方法不同，缺少候选项属于正常情况。
            }
        }
        methods.put(metric, Collections.unmodifiableList(resolved));
    }

    private Method resolvePublicMethod(Class<?> type, String methodName) throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> interfaceType : current.getInterfaces()) {
                Method method = resolveInterfaceMethod(interfaceType, methodName);
                if (method != null) {
                    return method;
                }
            }
            try {
                Method method = current.getMethod(methodName);
                if (Modifier.isPublic(current.getModifiers())) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // Continue through the class hierarchy and public interfaces.
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private Method resolveInterfaceMethod(Class<?> interfaceType, String methodName) {
        if (Modifier.isPublic(interfaceType.getModifiers())) {
            try {
                return interfaceType.getMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                // Search parent interfaces below.
            }
        }
        for (Class<?> parent : interfaceType.getInterfaces()) {
            Method method = resolveInterfaceMethod(parent, methodName);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private double invokeDoubleGetter(String metric) {
        List<Method> candidates = methods.get(metric);
        if (candidates == null || candidates.isEmpty()) {
            logUnavailableOnce(metric);
            return -1D;
        }

        boolean invocationFailed = false;
        for (Method method : candidates) {
            try {
                Object value = method.invoke(osBean);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                invocationFailed = true;
            }
        }
        if (invocationFailed) {
            logInvocationFailureOnce(metric);
        } else {
            logUnavailableOnce(metric);
        }
        return -1D;
    }

    private long invokeLongGetter(String metric) {
        List<Method> candidates = methods.get(metric);
        if (candidates == null || candidates.isEmpty()) {
            logUnavailableOnce(metric);
            return -1L;
        }

        boolean invocationFailed = false;
        for (Method method : candidates) {
            try {
                Object value = method.invoke(osBean);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                invocationFailed = true;
            }
        }
        if (invocationFailed) {
            logInvocationFailureOnce(metric);
        } else {
            logUnavailableOnce(metric);
        }
        return -1L;
    }

    private void logUnavailableOnce(String metric) {
        if (logger != null && loggedMetrics.add(metric + ":unsupported")) {
            logger.debug("当前运行环境不支持系统指标 {}，该指标将返回 -1", metric);
        }
    }

    private void logInvocationFailureOnce(String metric) {
        if (logger != null && loggedMetrics.add(metric + ":failed")) {
            logger.warn("读取系统指标 {} 失败，该指标将返回 -1", metric);
        }
    }

    private double toPercent(double value) {
        if (!Double.isFinite(value) || value < 0D || value > 1D) {
            return -1D;
        }
        return round(value * 100D);
    }

    private double calculatePercentage(long used, long total) {
        if (used < 0 || total <= 0) {
            return -1D;
        }
        return round((used * 100D) / total);
    }

    private double round(double value) {
        if (!Double.isFinite(value) || value < 0D) {
            return -1D;
        }
        return Math.round(value * 100D) / 100D;
    }
}
