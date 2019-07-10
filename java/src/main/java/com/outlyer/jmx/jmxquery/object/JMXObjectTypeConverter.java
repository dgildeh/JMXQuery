package com.outlyer.jmx.jmxquery.object;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to have general logic for type serialization and conversion 
 * @author Kiss Tibor
 *
 */
public class JMXObjectTypeConverter {

    static final Map<String, String> typeMap = new HashMap<String, String>();
    static {
        typeMap.put("String", String.class.getName());
        typeMap.put("Double", Double.class.getName());
        typeMap.put("Float", Float.class.getName());
        typeMap.put("Short", Short.class.getName());
        typeMap.put("Integer", Integer.class.getName());
        typeMap.put("Long", Long.class.getName());
        typeMap.put("Boolean", Boolean.class.getName());

        typeMap.put("char", char.class.getName());
        typeMap.put("short", short.class.getName());
        typeMap.put("int", int.class.getName());
        typeMap.put("long", long.class.getName());
        typeMap.put("double", double.class.getName());
        typeMap.put("float", float.class.getName());
        typeMap.put("boolean", boolean.class.getName());
        typeMap.put("double", double.class.getName());

    }

    /***
     * This methods convert a string to the specified type
     * @param typeName the expected type name
     * @param value the value as a text
     * @return the object
     */
    public final static Object toObject(final String typeName, final String value) {
        if (Boolean.class.getName().endsWith(typeName))
            return Boolean.parseBoolean(value);
        if (Byte.class.getName().endsWith(typeName))
            return Byte.parseByte(value);
        if (Short.class.getName().endsWith(typeName))
            return Short.parseShort(value);
        if (Integer.class.getName().endsWith(typeName))
            return Integer.parseInt(value);
        if (Long.class.getName().endsWith(typeName))
            return Long.parseLong(value);
        if (Float.class.getName().endsWith(typeName))
            return Float.parseFloat(value);
        if (Double.class.getName().endsWith(typeName))
            return Double.parseDouble(value);

        return value;
    }

}
