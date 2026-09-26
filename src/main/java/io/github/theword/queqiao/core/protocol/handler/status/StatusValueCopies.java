package io.github.theword.queqiao.core.protocol.handler.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Copies the nested maps/lists used by cached status data. */
final class StatusValueCopies {

    private StatusValueCopies() {
    }

    static Map<String, Object> immutableMap(Map<String, Object> source) {
        return Collections.unmodifiableMap(copyMap(source, true));
    }

    static Map<String, Object> mutableMap(Map<String, Object> source) {
        return copyMap(source, false);
    }

    private static Map<String, Object> copyMap(Map<String, Object> source, boolean immutable) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), copyValue(entry.getValue(), immutable));
        }
        return result;
    }

    private static Object copyValue(Object value, boolean immutable) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> copy = copyMap(map, immutable);
            return immutable ? Collections.unmodifiableMap(copy) : copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>(((List<?>) value).size());
            for (Object element : (List<?>) value) {
                copy.add(copyValue(element, immutable));
            }
            return immutable ? Collections.unmodifiableList(copy) : copy;
        }
        return value;
    }
}
