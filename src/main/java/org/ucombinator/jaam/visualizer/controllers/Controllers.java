package org.ucombinator.jaam.visualizer.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.util.HashMap;

/** Maintains a mapping between JavaFX GUI objects and their controllers */
public class Controllers {
    private static final HashMap<Object, Object> controllers = new HashMap<>();

    /** Get the controller for `key`.  If `key` is a `Node`, also searches ancestors. */
    public static <T> T get(Object key) {
        Object value = controllers.get(key);

        try {
            if (value != null) return (T)value;
        } catch (ClassCastException e) {
            /* Fall through to rest of method */
        }

        if (key instanceof Node) {
            Object parent = ((Node)key).getParent();
            if (parent != null) { return Controllers.get(parent); }
        }

        throw new IllegalArgumentException("No controller for " + key);
    }

    /** Set the controller for `key` to be `value`.  Use `loadFXML` instead to have this automatically done. */
    public static void put(Object key, Object value) { controllers.put(key, value); }

    /** Load an FXML `resource` and set its `controller` */
    public static <T> void loadFXML(String resource, T controller) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(controller.getClass().getResource(resource));
        fxmlLoader.setController(controller);
        Controllers.put(fxmlLoader.load(), controller);
    }
}
