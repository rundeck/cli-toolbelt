package com.simplifyops.toolbelt.output.yaml.snakeyaml;

import com.simplifyops.toolbelt.Formatable;
import com.simplifyops.toolbelt.OutputFormatter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Format objects as YAML, this will convert any Map/Collection into Yaml,
 * and any Object that implements {@link Formatable} and returns a non-null Map or List.
 * If the object does not correspond to one of those inputs, the base formatter will be used
 */
public class YamlFormatter implements OutputFormatter {
    private Yaml yaml;
    private OutputFormatter base;

    /**
     * @param base base formatter
     */
    public YamlFormatter(final OutputFormatter base) {
        this.base = base;
        this.yaml = new Yaml();
    }

    /**
     * @param base    base formatter
     * @param options yaml options
     */
    public YamlFormatter(final OutputFormatter base, DumperOptions options) {
        this.base = base;
        this.yaml = new Yaml(options);
    }

    @Override
    public String format(final Object o) {
        if (o instanceof Map) {
            return yaml.dump(o);
        } else if (o instanceof Collection) {
            return yaml.dump(o);
        } else if (o instanceof Formatable) {
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
        return base.format(o);
    }
}
