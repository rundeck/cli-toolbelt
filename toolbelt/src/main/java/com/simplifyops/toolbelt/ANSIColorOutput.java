package com.simplifyops.toolbelt;

import java.util.*;


/**
 * Created by greg on 6/13/16.
 */
public class ANSIColorOutput implements CommandOutput, OutputFormatter {
    private static final Object ESC = "\u001B";

    /**
     * Default color config
     */
    static final Config DEFAULT = new Config(Color.GREEN, null, Color.YELLOW, Color.RED);

    private OutputFormatter base;
    SystemOutput sink;
    private Config config = new Config(DEFAULT);

    public ANSIColorOutput(
            final SystemOutput sink,
            final OutputFormatter formatter,
            final Config config
    )
    {
        this(sink, formatter);
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * Configuration
     */
    public static class Config {
        /**
         * @return info color or null for none
         */
        Color info;

        /**
         * @return output color or null for none
         */
        Color output;

        /**
         * @return warning color or null for none
         */
        Color warning;

        /**
         * @return error color or null for none
         */
        Color error;

        public Config(final Color info, final Color output, final Color warning, final Color error) {
            this.info = info;
            this.output = output;
            this.warning = warning;
            this.error = error;
        }

        public Config(Config config) {
            this.info = config.info;
            this.output = config.output;
            this.warning = config.warning;
            this.error = config.error;
        }
    }

    public static class Builder {
        private ANSIColorOutput.Config config = new Config(DEFAULT);
        SystemOutput sink;
        OutputFormatter formatter;


        /**
         * @param color name of a Color
         *
         * @return
         */
        public Builder info(final String color) {
            return info(null != color ? ANSIColorOutput.Color.valueOf(color.toUpperCase()) : null);
        }

        public Builder info(final ANSIColorOutput.Color info) {
            config.info = info;
            return this;
        }

        public Builder output(final String color) {
            return output(null != color ? ANSIColorOutput.Color.valueOf(color.toUpperCase()) : null);
        }

        public Builder output(final ANSIColorOutput.Color output) {
            config.output = output;
            return this;
        }

        public Builder warning(final String color) {
            return warning(null != color ? ANSIColorOutput.Color.valueOf(color.toUpperCase()) : null);
        }

        public Builder warning(final ANSIColorOutput.Color warning) {
            config.warning = warning;
            return this;
        }

        public Builder error(final String color) {
            return warning(null != color ? ANSIColorOutput.Color.valueOf(color.toUpperCase()) : null);
        }

        public Builder error(final ANSIColorOutput.Color error) {
            config.error = error;
            return this;
        }

        public Builder config(final ANSIColorOutput.Config config) {
            this.config = config;
            return this;
        }

        public Builder sink(final SystemOutput sink) {
            this.sink = sink;
            return this;
        }

        public Builder formatter(final OutputFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public ANSIColorOutput build() {
            return new ANSIColorOutput(sink, formatter, new Config(config));
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    public ANSIColorOutput(final SystemOutput sink) {
        this.sink = sink;
    }

    public ANSIColorOutput(final SystemOutput sink, OutputFormatter base) {
        this.sink = sink;
        this.base = base;
    }

    @Override
    public OutputFormatter withBase(final OutputFormatter base) {
        return new ANSIColorOutput(sink, base);
    }

    @Override
    public String format(final Object o) {
        return toColors(o);
    }

    @Override
    public void info(final Object output) {
        if (output instanceof String && null != config.info) {
            sink.outPrint(config.info);
            sink.info(output);
            sink.outPrint(Color.RESET);
        } else {
            sink.info(toColors(output));
        }

    }

    @Override
    public void output(final Object object) {

        if (object instanceof String && null != config.output) {
            sink.outPrint(config.output);
            sink.info(object);
            sink.outPrint(Color.RESET);
        } else {
            sink.output(toColors(object));
        }
    }

    public static String toColors(final Object object) {
        return toColors(object, null);
    }

    public static String toColors(final Object object, OutputFormatter base) {
        if (null == object) {
            return null;
        }
        if (ColorString.class.isAssignableFrom(object.getClass())) {
            ColorString object1 = (ColorString) object;
            Set<ColorArea> colors = new TreeSet<>(object1.getColors());
            String string = null != base ? base.format(object1) : object1.toString();
            int cur = 0;
            int count = 0;
            StringBuilder sb = new StringBuilder();
            for (ColorArea area : colors) {
                if (count > 0) {
                    sb.append(Color.RESET.toString());
                    count--;
                }
                if (area.getStart() > cur) {
                    sb.append(string.substring(cur, area.getStart()));
                }
                cur = area.getStart();
                sb.append(area.getColor().toString());
                if (area.getLength() > 0) {
                    sb.append(string.substring(cur, cur + area.getLength()));
                    cur += area.getLength();
                    sb.append(Color.RESET.toString());
                } else {

                    count++;
                }
            }
            if (cur < string.length() - 1) {
                sb.append(string.substring(cur));
            }

            if (count > 0) {
                sb.append(Color.RESET.toString());
            }
            return sb.toString();
        } else {
            return null != base ? base.format(object) : object.toString();
        }
    }

    @Override
    public void error(final Object error) {
        if (null != config.error) {
            sink.errorPrint(config.error);
        }
        sink.error(error);
        if (null != config.error) {
            sink.errorPrint(Color.RESET);
        }
    }


    @Override
    public void warning(final Object error) {
        if (null != config.warning) {
            sink.errorPrint(config.warning);
        }
        sink.warning(error);
        if (null != config.warning) {
            sink.errorPrint(Color.RESET);
        }
    }

    public static enum Color {

        RESET("0"),
        RED("31"),
        ORANGE("38;5;208"),
        GREEN("32"),
        YELLOW("33"),
        BLUE("34"),
        INDIGO("38;5;90"),
        VIOLET("38;5;165"),
        MAGENTA("35"),
        WHITE("37");
        String code;

        Color(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return ESC + "[" + code + "m";
        }
    }

    public static interface ColorArea extends Comparable<ColorArea> {
        default int getStart() {
            return 0;
        }

        default int getLength() {
            return -1;
        }

        Color getColor();

        default int compareTo(ColorArea ca) {
            return getStart() < ca.getStart() ? -1 :
                   getStart() > ca.getStart() ? 1 :
                   0;
        }
    }

    /**
     * A String which defines colorized portions
     */
    public static interface ColorString {
        Set<ColorArea> getColors();
    }

    public static class Colorized implements ColorString {
        Set<ColorArea> colors;
        String value;

        public Colorized(final Set<ColorArea> colors, final String value) {
            this.colors = colors;
            this.value = value;
        }

        @Override
        public Set<ColorArea> getColors() {
            return colors;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static ColorString colorize(Color color, String string) {
        final Set<ColorArea> colors = new TreeSet<>();
        colors.add(() -> color);
        return new Colorized(colors, string);
    }

    public static ColorString colorize(String prefix, final Color color, String wrapped) {
        return colorize(prefix, color, wrapped, "");
    }

    public static ColorString colorize(final Color color, String wrapped, String suffix) {
        return colorize("", color, wrapped, suffix);
    }

    public static ColorString colorize(String prefix, final Color color, String wrapped, String suffix) {
        final Set<ColorArea> colors = new TreeSet<>();
        colors.add(new ColorArea() {
            @Override
            public Color getColor() {
                return color;
            }

            @Override
            public int getStart() {
                return prefix.length();
            }

            @Override
            public int getLength() {
                return wrapped.length();
            }
        });
        return new Colorized(colors, prefix + wrapped + suffix);
    }

    /**
     * Return a map with key/values replaced with colorized versions, if specified
     *
     * @param data  data
     * @param key   key color, or null
     * @param value value color, or null
     *
     * @return colorized keys/values
     */
    public static Map<?, ?> colorizeMap(
            Map<?, ?> data,
            ANSIColorOutput.Color key,
            ANSIColorOutput.Color value
    )
    {
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        data.keySet().forEach(k -> result.put(
                key != null ? ANSIColorOutput.colorize(key, k.toString()) : k,
                value != null ? ANSIColorOutput.colorize(value, data.get(k).toString()) : data.get(k)
        ));
        return result;
    }
}
