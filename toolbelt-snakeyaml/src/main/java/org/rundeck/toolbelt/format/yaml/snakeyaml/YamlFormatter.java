package org.rundeck.toolbelt.format.yaml.snakeyaml;

import org.rundeck.toolbelt.Formatable;
import org.rundeck.toolbelt.OutputFormatter;
import org.rundeck.toolbelt.ToStringFormatter;
import org.rundeck.toolbelt.OutputFormatter;
import org.rundeck.toolbelt.ToStringFormatter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

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
     */
    public YamlFormatter() {
        this.yaml = new Yaml();
        this.base = new ToStringFormatter();
    }

    /**
     */
    public YamlFormatter(Representer representer, DumperOptions options) {
        this.yaml = new Yaml(representer, options);
        this.base = new ToStringFormatter();
    }

    /**
     */
    public YamlFormatter(DumperOptions options) {
        this.yaml = new Yaml(options);
        this.base = new ToStringFormatter();
    }

    /**
     * @param base base formatter
     */
    public YamlFormatter(final OutputFormatter base) {
        this();
        this.base = base;
    }

    /**
     * @param base base formatter
     */
    private YamlFormatter(Yaml yaml, final OutputFormatter base) {
        this.yaml = yaml;
        this.base = base;
    }

    @Override
    public OutputFormatter withBase(final OutputFormatter base) {
        return new YamlFormatter(this.yaml, base);
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
