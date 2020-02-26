/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * Reserved.FileName: MemoryConfigurationSection.java@author: karlatemp@vip.qq.com: 2020/1/25 下午7:42@version: 2.0
 */

package cn.mcres.karlatemp.yop.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MemoryConfigurationSection implements ConfigurationSection {
    protected Map<String, Object> values;
    protected char splitter = '.';
    protected boolean useSplitter;
    protected boolean currentSplitter;
    protected ConfigurationSection parent;

    public MemoryConfigurationSection() {
        this(new ConcurrentHashMap<>());
    }

    public MemoryConfigurationSection(@NotNull Map<String, Object> values) {
        this.values = values;
        useSplitter = true;
    }

    public String toString() {
        return ConfigurationSection.toStr(this);
    }

    @Override
    public ConfigurationSection clearSplitter() {
        currentSplitter = false;
        return this;
    }

    @Override
    public boolean hasCurrentSplitter() {
        if (parent == null) return true;
        return currentSplitter;
    }

    @Override
    public ConfigurationSection parent() {
        return parent;
    }

    @Override
    public ConfigurationSection parent(ConfigurationSection parent) {
        if (this.parent == null) {
            synchronized (this) {
                if (this.parent == null) {
                    this.parent = parent;
                }
            }
        }
        return this;
    }

    protected MemoryConfigurationSection(Void unused) {
    }

    @Override
    public ConfigurationSection merge(ConfigurationSection section) {
        if (section == null) return this;
        Map<String, Object> values = section.values();
        Map<String, Object> vv = this.values;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if (vv.containsKey(k)) {
                Object ov = vv.get(k);
                if (ov instanceof ConfigurationSection && v instanceof ConfigurationSection) {
                    ((ConfigurationSection) ov).merge((ConfigurationSection) v);
                    continue;
                }
            }
            if (v instanceof ConfigurationSection) {
                v = ((ConfigurationSection) v).clone().parent(this);
            }
            vv.put(k, v);
        }
        return this;
    }

    @Override
    public char pathSplitter() {
        if (hasCurrentSplitter()) {
            return splitter;
        } else {
            if (parent == null) return splitter;
            return parent.pathSplitter();
        }
    }

    @Override
    public ConfigurationSection pathSplitter(char splitter) {
        currentSplitter = true;
        this.splitter = splitter;
        return this;
    }

    @Override
    public boolean useSplitter() {
        return useSplitter;
    }

    @Override
    public ConfigurationSection useSplitter(boolean use) {
        this.useSplitter = use;
        return this;
    }

    public Deque<String> split(String path) {
        if (!useSplitter) return new ArrayDeque<>(Collections.singletonList(path));
        ArrayDeque<String> list = new ArrayDeque<>();
        int offset = 0;
        do {
            int next = path.indexOf(pathSplitter(), offset);
            if (next == -1) {
                list.add(path.substring(offset));
                return list;
            } else {
                list.add(path.substring(offset, next));
                offset = next + 1;
            }
        } while (true);
    }

    protected ConfigurationSection getSection(Deque<String> path) {
        if (useSplitter) {
            ConfigurationSection current = this;
            do {
                if (current == null) return null;
                String p = path.poll();
                if (p == null) break;
                current = current.sectionIfExist(p);
            } while (true);
            return current;
        }
        return this;
    }

    @Override
    public String string(String path, Supplier<String> def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                if (def == null) return null;
                return def.get();
            } else if (from != this) {
                return from.string(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            return String.valueOf(values.get(path));
        }
        if (def == null) return null;
        return def.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> list(String path, Supplier<List<Object>> def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                if (def == null) return null;
                return def.get();
            } else if (from != this) {
                return from.list(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            Object val = values.get(path);
            if (val instanceof List) return (List<Object>) val;
            if (val instanceof Collection) return new ArrayList<>((Collection<Object>) val);
            return new ArrayList<>(Collections.singletonList(val));
        }
        if (def == null) return new ArrayList<>();
        return def.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> stringList(String path, Supplier<List<String>> def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                if (def == null) return null;
                return def.get();
            } else if (from != this) {
                return from.stringList(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            Object val = values.get(path);
            if (val instanceof List) return (List<String>) val;
            if (val instanceof Collection) return new ArrayList<>((Collection<String>) val);
            return new ArrayList<>(Collections.singletonList(val == null ? null : val.toString()));
        }
        if (def == null) return new ArrayList<>();
        return def.get();
    }

    @Override
    public int intV(String path, int def) {
        return number(path, def).intValue();
    }

    @Override
    public boolean exists(String path) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) return false;
            if (from != this) return from.exists(target);
            path = target;
        }
        return values.containsKey(path);
    }

    @Override
    public Object value(String path, Supplier<Object> def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                if (def == null) return null;
                return def.get();
            } else if (from != this) {
                return from.value(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            return values.get(path);
        }
        if (def == null) return null;
        return def.get();
    }

    @Override
    public short shortV(String path, short def) {
        return number(path, def).shortValue();
    }

    @Override
    public byte byteV(String path, byte def) {
        return number(path, def).byteValue();
    }

    @Override
    public char charV(String path, char def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                return def;
            } else if (from != this) {
                return from.charV(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            Object v = values.get(path);
            if (v instanceof Character) return (Character) v;
            if (v instanceof Number) return (char) ((Number) v).intValue();
        }
        return def;
    }

    @Override
    public float floatV(String path, float def) {
        return number(path, def).floatValue();
    }

    @Override
    public double doubleV(String path, double def) {
        return number(path, def).doubleValue();
    }

    @Override
    public boolean booleanV(String path, boolean def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                return def;
            } else if (from != this) {
                return from.booleanV(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            Object val = values.get(path);
            if (val instanceof String) {
                if (((String) val).trim().isEmpty()) return false;
                switch ((String) val) {
                    case "false":
                    case "0":
                    case "no":
                    case "null":
                    case "undefine":
                        return false;
                    default:
                        return true;
                }
            }
            if (val instanceof Boolean) return (Boolean) val;
            if (val instanceof Number) {
                return ((Number) val).intValue() != 0;
            }
            if (val instanceof Collection) return !((Collection<?>) val).isEmpty();
            if (val == null) return false;
            if (val instanceof Map) return !((Map<?, ?>) val).isEmpty();
            return true;
        }
        return def;
    }

    @Override
    public long longV(String path, long def) {
        return number(path, def).longValue();
    }

    @Override
    public Number number(String path, Number def) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) {
                return def;
            } else if (from != this) {
                return from.number(target, def);
            }
            path = target;
        }
        if (values.containsKey(path)) {
            Object val = values.get(path);
            if (val instanceof Number) return (Number) val;
            if (val instanceof String) {
                if (def != null) {
                    String vs = val.toString();
                    try {
                        return Byte.parseByte(vs);
                    } catch (NumberFormatException ignore) {
                    }
                    try {
                        return Short.parseShort(vs);
                    } catch (NumberFormatException ignore) {
                    }
                    try {
                        return Integer.parseInt(vs);
                    } catch (NumberFormatException ignore) {
                    }
                    try {
                        return Long.parseLong(vs);
                    } catch (NumberFormatException ignore) {
                    }
                    try {
                        return Float.parseFloat(vs);
                    } catch (NumberFormatException ignore) {
                    }
                    try {
                        return Double.parseDouble(vs);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return def;
    }

    @Override
    public ConfigurationSection set(String path, Object value) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection section = this;
            for (String s : paths) {
                section = section.sectionOrCreate(s);
            }
            if (section != this) {
                section.set(target, value);
                return this;
            }
            path = target;
        }
        if (value instanceof ConfigurationSection) {
            ((ConfigurationSection) value).parent(this);
        }
        values.put(path, value);
        return this;
    }

    @Override
    public ConfigurationSection remove(String path) {
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) return this;
            if (from != this) {
                from.remove(target);
                return this;
            }
            path = target;
        }
        values.remove(path);
        return this;
    }

    @Override
    public @NotNull ConfigurationSection sectionOrCreate(String path) {
        if (path == null) return this;
        if (useSplitter) {
            Deque<String> paths = split(path);
            String target = paths.removeLast();
            ConfigurationSection from = getSection(paths);
            if (from == null) return this;
            if (from != this) return from.sectionOrCreate(target);
            path = target;
        }
        MemoryConfigurationSection mcs = new MemoryConfigurationSection();
        mcs.parent = this;
        values.put(path, mcs);
        return mcs;
    }

    @Override
    public @NotNull Set<String> keys() {
        return ImmutableSet.copyOf(values.keySet());
    }

    protected cn.mcres.karlatemp.yop.config.MemoryConfigurationSection newInstance() {// Must override
        if (getClass() != cn.mcres.karlatemp.yop.config.MemoryConfigurationSection.class)
            throw new IllegalArgumentException();
        return new cn.mcres.karlatemp.yop.config.MemoryConfigurationSection((Void) null);
    }

    @Override
    public @NotNull cn.mcres.karlatemp.yop.config.MemoryConfigurationSection newContext() {
        MemoryConfigurationSection msc = newInstance();
        // Update fields.
        msc.useSplitter = useSplitter;
        msc.splitter = splitter;
        msc.values = values;
        msc.parent = this;
        msc.currentSplitter = currentSplitter;
        return msc;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull ConfigurationSection clone() {
        MemoryConfigurationSection cc = newContext();
        cc.parent = null;
        Map<String, Object> to = cc.values = new ConcurrentHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof ConfigurationSection) v = ((ConfigurationSection) v).clone();
            to.put(entry.getKey(), v);
        }
        return newContext();
    }

    @Override
    public ConfigurationSection clear() {
        values.clear();
        return this;
    }

    @Override
    public Map<String, Object> values() {
        return ImmutableMap.copyOf(values);
    }
}
