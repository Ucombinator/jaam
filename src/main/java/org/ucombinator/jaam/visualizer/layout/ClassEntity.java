package org.ucombinator.jaam.visualizer.layout;

public interface ClassEntity {
    // Returns the name with all packages
    String getClassName();

    // Returns only the final name, without any packages
    String getShortClassName();
}
