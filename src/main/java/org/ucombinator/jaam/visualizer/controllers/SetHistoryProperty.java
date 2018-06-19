package org.ucombinator.jaam.visualizer.controllers;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class SetHistoryProperty<T> extends SetPropertyBase<T> {

    private ArrayList<ObservableSet<T>> bean;
    private int currentIndex;

    @Override
    public Set<T> getBean() {
        return this.getValue();
    }

    @Override
    public String getName() {
        return "set_history";
    }

    @Override
    public boolean add(T item) {
        System.out.println("Intercepting add event for SetHistoryProperty...");
        ObservableSet<T> newSet = this.copyCurrent();
        boolean modified = newSet.add(item);

        if (modified) {
            this.update(newSet);
        }

        return modified;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        System.out.println("Intercepting addAll event for SetHistoryProperty...");
        ObservableSet<T> newSet = this.copyCurrent();
        boolean modified = newSet.addAll(c);

        if (modified) {
            this.update(newSet);
        }

        return modified;
    }

    public boolean remove(Object obj) {
        System.out.println("Intercepting remove event for SetHistoryProperty...");
        ObservableSet<T> newSet = this.copyCurrent();
        boolean modified = newSet.remove(obj);

        if (modified) {
            this.update(newSet);
        }

        return modified;
    }

    public boolean removeAll(Collection<?> c) {
        System.out.println("Intercepting removeAll event for SetHistoryProperty...");
        ObservableSet<T> newSet = this.copyCurrent();
        boolean modified = newSet.removeAll(c);

        if (modified) {
            this.update(newSet);
        }

        return modified;
    }


    @Override
    public void clear() {
        System.out.println("Intercepting clear event for SetHistoryProperty...");
        this.trimAfterCurrent();
        ObservableSet<T> newSet = FXCollections.observableSet();
        bean.add(newSet);
        currentIndex++;
        this.fireValueChangedEvent();
    }

    public SetHistoryProperty(ObservableSet<T> initialValue) {
        super(initialValue);
        this.bean = new ArrayList<>();
        this.bean.add(initialValue);
        this.currentIndex = 0;
    }

    private ObservableSet<T> copyCurrent() {
        ObservableSet<T> set = FXCollections.observableSet();
        set.addAll(this.getBean());
        return set;
    }

    private void trimAfterCurrent() {
        while (this.bean.size() > this.currentIndex + 1) {
            this.bean.remove(this.bean.size() - 1);
        }
    }

    private void update(ObservableSet<T> newSet) {
        this.trimAfterCurrent();
        bean.add(newSet);
        currentIndex++;
        this.setValue(this.bean.get(currentIndex));
    }

    public void increment() {
        if (this.currentIndex < this.bean.size() - 1) {
            this.currentIndex++;
            this.setValue(this.bean.get(currentIndex));
        }
    }

    public void decrement() {
        if (this.currentIndex > 0) {
            this.currentIndex--;
            this.setValue(this.bean.get(currentIndex));
        }
    }
}
