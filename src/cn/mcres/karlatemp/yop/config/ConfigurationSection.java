/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * Reserved.FileName: ConfigurationSection.java@author: karlatemp@vip.qq.com: 2020/1/25 下午7:18@version: 2.0
 */

package cn.mcres.karlatemp.yop.config;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public interface ConfigurationSection {
    /**
     * Merge other section
     *
     * @param section The target.
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection merge(cn.mcres.karlatemp.yop.config.ConfigurationSection section);

    /**
     * The splitter using.
     *
     * @return The path splitter.
     */
    char pathSplitter();

    /**
     * Change using splitter. And make {@link #hasCurrentSplitter()} return true.
     * <p>
     * Has no effect on {@link #newContext()} and parent {@link cn.mcres.karlatemp.yop.config.ConfigurationSection}
     *
     * @param splitter The splitter change.
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection pathSplitter(char splitter);

    /**
     * Does current context using splitter.
     *
     * @return true if using splitter.
     */
    boolean useSplitter();

    /**
     * Check current context has splitter.
     *
     * @return true if parent is null or {@link #clearSplitter()} invoked.
     */
    boolean hasCurrentSplitter();

    /**
     * Make {@link #hasCurrentSplitter()} return false.
     * This method has no effect if parent is null.
     *
     * @return this
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection clearSplitter();

    /**
     * Set current context using splitter
     *
     * @param use Is using.
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection useSplitter(boolean use);

    String string(String path, Supplier<String> def);

    List<Object> list(String path, Supplier<List<Object>> def);

    default List<Object> list(String path) {
        return list(path, null);
    }

    default List<String> stringList(String path) {
        return stringList(path, null);
    }

    List<String> stringList(String path, Supplier<List<String>> def);

    default String string(String path, String def) {
        return string(path, () -> def);
    }

    default String string(String path) {
        return string(path, (Supplier<String>) null);
    }

    int intV(String path, int def);

    default int intV(String path) {
        return intV(path, 0);
    }

    /**
     * Check this path has ot no vale.
     *
     * @param path The path checking.
     * @return true if path exists.
     */
    boolean exists(String path);

    default Object value(String path, Object def) {
        return value(path, () -> def);
    }

    /**
     * Get the value of path.
     * <p>
     * If you want to get value with Target Path Contains splitter. Use this:
     * <pre>{@code
     *      Object value = section.newContext().useSplitter(false).value(PATH, null);
     * }</pre>
     *
     * @param path The path.
     * @param def  Default value supplier
     * @return The result value.
     */
    Object value(String path, Supplier<Object> def);

    default Object value(String path) {
        return value(path, null);
    }

    short shortV(String path, short def);

    default short shortV(String path) {
        return shortV(path, (short) 0);
    }

    byte byteV(String path, byte def);

    default byte byteV(String path) {
        return byteV(path, (byte) 0);
    }

    default char charV(String path) {
        return charV(path, (char) 0);
    }

    char charV(String path, char def);

    float floatV(String path, float def);

    default float floatV(String path) {
        return floatV(path, 0F);
    }

    double doubleV(String path, double def);

    default double doubleV(String path) {
        return doubleV(path, 0D);
    }

    boolean booleanV(String path, boolean def);

    default boolean booleanV(String path) {
        return booleanV(path, false);
    }

    long longV(String path, long def);

    default long longV(String path) {
        return longV(path, 0L);
    }

    Number number(String path, Number def);

    default Number number(String path) {
        return number(path, 0);
    }

    /**
     * Set the value to ConfigurationSection. This will also affect the values in {@link #newContext()}
     * <p>
     * WARNING: If root (Configuration) unsupported this value. Then will throw a Exception in
     * {@link Configuration#store(OutputStream)}
     * {@link Configuration#store(java.io.Writer)}
     * {@link Configuration#store(File)}
     * {@link Configuration#store(Path)}
     *
     * @param path  The path set.
     * @param value The value.
     * @return this
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection set(String path, Object value);

    /**
     * Remove a value.
     *
     * @param path The path.
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection remove(String path);

    /**
     * Get sub section.
     *
     * @param path The section path.
     * @return Sub Section.(if exist)
     */
    default cn.mcres.karlatemp.yop.config.ConfigurationSection sectionIfExist(String path) {
        Object val = value(path);
        if (val instanceof cn.mcres.karlatemp.yop.config.ConfigurationSection)
            return (cn.mcres.karlatemp.yop.config.ConfigurationSection) val;
        return null;
    }

    @NotNull
    cn.mcres.karlatemp.yop.config.ConfigurationSection sectionOrCreate(String path);

    /**
     * Same as {@link #sectionIfExist(String)}
     *
     * @param path The path
     * @return Sub Section
     */
    default cn.mcres.karlatemp.yop.config.ConfigurationSection section(String path) {
        return sectionIfExist(path);
    }

    /**
     * Get the keys of current section.
     *
     * @return keys.
     */
    @NotNull
    Set<String> keys();

    /**
     * Create a new context / Create a Pointer.
     *
     * @return The pointer created.
     */
    @NotNull
    cn.mcres.karlatemp.yop.config.ConfigurationSection newContext();

    /**
     * Clone current section.
     * Result no parent.
     *
     * @return Cloned Section.
     */
    default @NotNull cn.mcres.karlatemp.yop.config.ConfigurationSection copy() {
        return clone();
    }

    @NotNull cn.mcres.karlatemp.yop.config.ConfigurationSection clone();

    /**
     * Clear section values.
     *
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection clear();

    /**
     * Get un-editable values.
     *
     * @return The values of this section.
     */
    Map<String, Object> values();

    static String toStr(cn.mcres.karlatemp.yop.config.ConfigurationSection cs) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> it = cs.values().entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            sb.append(", ").append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.append("}").toString();
    }

    /**
     * Get parent.
     *
     * @return The parent
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection parent();

    default cn.mcres.karlatemp.yop.config.ConfigurationSection root() {
        ConfigurationSection c = this;
        do {
            ConfigurationSection x = c.parent();
            if (x == null) return c;
            c = x;
        } while (true);
    }

    /**
     * Set parent if absent.
     *
     * @param parent The target.
     * @return this.
     */
    cn.mcres.karlatemp.yop.config.ConfigurationSection parent(cn.mcres.karlatemp.yop.config.ConfigurationSection parent);
}