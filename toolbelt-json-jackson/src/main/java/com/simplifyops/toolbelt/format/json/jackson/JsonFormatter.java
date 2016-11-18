package com.simplifyops.toolbelt.format.json.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplifyops.toolbelt.Formatable;
import com.simplifyops.toolbelt.OutputFormatter;

import java.util.List;
import java.util.Map;

/**
 * Created by greg on 11/17/16.
 */
public class JsonFormatter implements OutputFormatter {
    ObjectMapper mapper;
    OutputFormatter base;

    public JsonFormatter() {
        this.mapper = new ObjectMapper();
    }

    public JsonFormatter(final OutputFormatter base) {
        this.base = base;
    }

    public JsonFormatter(final ObjectMapper mapper, final OutputFormatter base) {
        this.mapper = mapper;
        this.base = base;
    }

    @Override
    public String format(final Object o) {
        if (o instanceof Formatable) {
            Formatable o1 = (Formatable) o;
            List<?> objects = o1.asList();
            if (null != objects) {
                return format(objects);
            }
            Map<?, ?> map = o1.asMap();
            if (null != map) {
                return format(map);
            }
        }
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return base != null ? base.format(o) : o.toString();
    }

    @Override
    public OutputFormatter withBase(final OutputFormatter base) {
        return new JsonFormatter(mapper, base);
    }
}
