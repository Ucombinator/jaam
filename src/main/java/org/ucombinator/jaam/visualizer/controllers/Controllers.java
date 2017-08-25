package org.ucombinator.jaam.visualizer.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maintains a mapping between JavaFX GUI objects and their controllers */
public class Controllers {
    private static final HashMap<Object, Object> controllers = new HashMap<>();

    /** Get the controller for `key`.  If `key` is a `Node`, also searches ancestors. */
    public static <T> T get(Object key) {
        T value = getImpl(key);
        if (value != null) { return value; }
        else { throw new IllegalArgumentException("No controller for " + key); }
    }
    private static <T> T getImpl(Object key) {
        if (key != null) {
            Object value = controllers.get(key);

            if (value != null) {
                try {
                    // Assign to a variable so we can use annotations
                    @SuppressWarnings("unchecked") T t = (T)value;
                    return t;
                } catch (ClassCastException e) {
                    /* Fall through to rest of method */
                }
            }

            if (key instanceof Node) {
                return Controllers.getImpl(((Node)key).getParent());
            }
        }

        return null; // Failed to find a value for our key
    }

    /** Set the controller for `key` to be `value`.  Use `loadFXML` instead to have this automatically done. */
    public static void put(Object key, Object value) { controllers.put(key, value); }

    /** Load an FXML `resource` and set its `controller` */
    public static <T> void loadFXML(String resource, T controller) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(controller.getClass().getResource(resource));
        fxmlLoader.setController(controller);
        try {
            initMemberTables(fxmlLoader, controller.getClass());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Controllers.put(fxmlLoader.load(), controller);
    }

    /** Populate `FXMLLoader.ControllerAccessor.getControllerFields()` and `FXMLLoader.ControllerAccessor.getControllerMethods()`.
     *
     * Our rules are different than the normal `FXMLLoader` and are as follows:
     *   (1) All fields or methods referenced by FXML must exist and be annotated with `@FXML`.
     *   (2) Fields or methods annotated with `@FXML` are accessible from FXML even if they are `final` or `static`.
     *
     * Note that this code abuses reflection to get access to things we should have access to, but the extra error
     * checking we get as a result is wroth it.
     */
    private static void initMemberTables(FXMLLoader fxmlLoader, Class<?> type) throws ReflectiveOperationException {
        // Get `fxmlLoader.controllerAccessor`
        Field controllerAccessorField = fxmlLoader.getClass().getDeclaredField("controllerAccessor");
        controllerAccessorField.setAccessible(true);
        Object controllerAccessor = controllerAccessorField.get(fxmlLoader);

        // Get `controllerAccessor.controllerMethods`.
        // The type of `controllerAccessor.controllerMethods` involves the private enum `FXMLLoader.SupportedType`
        // (which we cannot access), so to initialize it we call `fxmlLoader.controllerAccessor.getControllerMethods()`
        // and then set each key to an empty `MemberHashMap`.
        Method getControllerMethodsMethod = controllerAccessor.getClass().getDeclaredMethod("getControllerMethods");
        getControllerMethodsMethod.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Object, Map<String, Method>> controllerMethods =
            (Map<Object, Map<String, Method>>)getControllerMethodsMethod.invoke(controllerAccessor);
        for (Object key : controllerMethods.keySet()) {
            controllerMethods.put(key, new MemberHashMap<>());
        }

        // Get `controllerAccessor.controllerFields`
        Field controllerFieldsField = controllerAccessor.getClass().getDeclaredField("controllerFields");
        controllerFieldsField.setAccessible(true);
        Map<String, List<Field>> controllerFields = new MemberHashMap<>();
        controllerFieldsField.set(controllerAccessor, controllerFields);

        // Populate `controllerFields` and `controllerMethods`
        addMembers(controllerFields, controllerMethods, type);
    }

    /** Class used to raise errors when the `FXMLLoader` is looking for a member but does not find it */
    private static class MemberHashMap<K, V> extends HashMap<K, V> {
        @Override public V get(Object key) {
            V v = super.get(key);
            if (v != null) { return v; }
            // It is okay for these members to not be found as they are generic members that are looked up in all FXML
            else if (key == FXMLLoader.INITIALIZE_METHOD_NAME ||
                     key == FXMLLoader.LOCATION_KEY ||
                     key == FXMLLoader.RESOURCES_KEY) {
                return null;
            } else { throw new RuntimeException("Controller is missing field or method `" + key + "`, or `" + key + "` is missing an `@FXML` annotation."); }
        }
    }

    /** Walk up the class hierarchy and add fields and members that are annotated with `@FXML` to the appropriate table */
    private static void addMembers(Map<String, List<Field>> fields, Map<?, Map<String, Method>> methods, Class<?> type)
            throws InvocationTargetException, IllegalAccessException {
        if (type != Object.class) { // Stop at top of hierarchy
            addMembers(fields, methods, type.getSuperclass()); // Recur up hierarchy

            // Add fields that are annotated with `@FXML`
            for (Field field : type.getDeclaredFields()) {
                if (field.getAnnotation(FXML.class) != null) {
                    // TODO: raise warning if not final
                    field.setAccessible(true);
                    if (!fields.containsKey(field.getName())) { // Avoid `get()` as `MemberHashMap` might raise an exception
                        fields.put(field.getName(), new ArrayList<>(1));
                    }
                    fields.get(field.getName()).add(field);
                }
            }

            // Add methods that are annotated with `@FXML`
            for (Method method : type.getDeclaredMethods()) {
                if (method.getAnnotation(FXML.class) != null) {
                    method.setAccessible(true);
                    Object convertedType = toSupportedTypeMethod.invoke(
                        null/* `toSupportedTypeMethod` is static so no object */, method);
                    if (convertedType != null) {
                        methods.get(convertedType).put(method.getName(), method);
                    }
                }
            }
        }
    }

    /** Sneaky access to the private method `FXMLLoader.toSupportedType` */
    private static final Method toSupportedTypeMethod;
    static {
        try {
            toSupportedTypeMethod = FXMLLoader.class.getDeclaredMethod("toSupportedType", Method.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        toSupportedTypeMethod.setAccessible(true);
    }
}
