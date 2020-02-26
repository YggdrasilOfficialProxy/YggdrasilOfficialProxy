/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * Reserved.FileName: YamlConfiguration.java@author: karlatemp@vip.qq.com: 2020/1/25 下午9:28@version: 2.0
 */

package cn.mcres.karlatemp.yop.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class YamlConfiguration extends MemoryConfiguration {
    public static final Yaml y;

    static {
        DumperOptions opt = new DumperOptions();
        y = new Yaml(new Loader(), new YWriter(), opt);
        opt.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
    }

    public static class YWriter extends Representer {
        {
            setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
            representers.put(null, data -> {
                if (data instanceof ConfigurationSection) {
                    return YWriter.this.representData(((ConfigurationSection) data).values());
                }
                if (data instanceof Map<?, ?>) {
                    return YWriter.this.representMapping(Tag.MAP, (Map<?, ?>) data, DumperOptions.FlowStyle.BLOCK);
                }
                if (data instanceof Collection<?>) {
                    return YWriter.this.representSequence(Tag.SEQ, (Collection<?>) data, DumperOptions.FlowStyle.BLOCK);
                }
                throw new InternalError("Unsupported " + data + ", " + data.getClass());
            });
        }
    }

    public static class Loader extends SafeConstructor implements Construct {

        {
            yamlConstructors.put(Tag.MAP, this);
            yamlConstructors.put(Tag.OMAP, this);
        }

        private Object alloc(Node node) {
            if (node == null) return null;
            Tag tag = node.getTag();
            if (tag == null) return null;
            if (tag.equals(Tag.NULL)) return null;
            if (node instanceof MappingNode) {
                MemoryConfigurationSection mp = new MemoryConfigurationSection();
                mp.useSplitter(false);
                List<NodeTuple> val = ((MappingNode) node).getValue();
                for (NodeTuple kv : val) {
                    Node k = kv.getKeyNode();
                    if (!(k instanceof ScalarNode)) {
                        throw new RuntimeException("Map key only support Scalar Node");
                    }
                    mp.set(String.valueOf(alloc(k)), alloc(kv.getValueNode()));
                }
                return mp;
            } else {
                Tag tg = node.getTag();
                Construct c = yamlConstructors.get(tg);
                if (c != null) {
                    if (c == this) throw new InternalError();
                    return c.construct(node);
                }
                Construct x = yamlClassConstructors.get(node.getNodeId());
                if (x != null) return x.construct(node);
                System.out.println(node);
            }
            throw new InternalError();
        }

        @Override
        public Object getSingleData(Class<?> type) {
            try {
                return alloc(composer.getSingleNode());
            } catch (InternalError internal) {
                throw new RuntimeException(internal);
            }
        }

        @Override
        public Object construct(Node node) {
            return alloc(node);
        }

        @Override
        public void construct2ndStep(Node node, Object object) {
            throw new UnsupportedOperationException();
        }
    }

    public YamlConfiguration() {
    }

    protected YamlConfiguration(Void ud) {
        super(ud);
    }

    @Override
    protected MemoryConfiguration newInstance() {
        return new cn.mcres.karlatemp.yop.config.YamlConfiguration(null);
    }

    @Override
    public void load(Reader stream) {
        synchronized (y) {
            merge(y.load(stream));
        }
    }

    @Override
    public void store(Writer writer) {
        synchronized (y) {
            y.dump(this, writer);
        }
    }
}
