/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/PropertiesMap.java
 */

package cn.mcres.karlatemp.yop.modesl;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonAdapter(PropertiesMap.Serializer.class)
public class PropertiesMap {
    protected final Map<String, Property> target = new LinkedHashMap<>();

    public Property getProperty(String key) {
        return target.get(key);
    }

    public void putProperty(Property property) {
        target.put(property.name, property);
    }

    public void removeProperty(String key) {
        target.remove(key);
    }

    public void clearProperty(String key) {
        target.clear();
    }

    public Map<String, Property> getAsMap() {
        return target;
    }

    public static class Property {
        public final String name;
        public String value;
        public String signature;

        public Property(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }
    }

    public static final class Serializer extends TypeAdapter<PropertiesMap> {
        @Override
        public void write(JsonWriter out, PropertiesMap value) throws IOException {
            out.beginArray();
            for (Property property : value.properties()) {
                out.beginObject();
                out.name("name").value(property.name)
                        .name("value").value(property.value);
                if (property.signature != null) {
                    out.name("signature").value(property.signature);
                }
                out.endObject();
            }
            out.endArray();
        }

        @Override
        public PropertiesMap read(JsonReader in) throws IOException {
            in.beginArray();
            PropertiesMap map = new PropertiesMap();
            while (in.hasNext()) {
                in.beginArray();
                String name = null, value = null, signature = null;
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "name": {
                            name = in.nextString();
                            break;
                        }
                        case "value": {
                            value = in.nextString();
                            break;
                        }
                        case "signature": {
                            signature = in.nextString();
                            break;
                        }
                        default:
                            in.skipValue();
                    }
                }
                map.putProperty(new Property(name, value, signature));
                in.endArray();
            }
            in.endArray();
            return map;
        }
    }

    public Collection<Property> properties() {
        return target.values();
    }

}
