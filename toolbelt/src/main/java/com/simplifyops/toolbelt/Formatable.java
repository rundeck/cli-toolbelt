package com.simplifyops.toolbelt;

import java.util.List;
import java.util.Map;

/**
 * Created by greg on 11/17/16.
 */
public interface Formatable {
    default List<?> asList() {
        return null;
    }

    default Map<?, ?> asMap() {
        return null;
    }
}
