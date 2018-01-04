package org.ucombinator.jaam.visualizer.layout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashSet;

@RunWith(JUnit4.class)
public class LayoutAlgorithmTest {

    public LayoutAlgorithmTest() {}

    @Test
    public void layoutIsCorrect() {
        HierarchicalGraph<StateVertex> graph = new HierarchicalGraph<>();
        HashSet<StateVertex> vertices = new HashSet<>();
        for(int i = 1; i <= 5; i++) {
            vertices.add(new LayoutMethodVertex(i, "Method " + i, null));
        }

        graph.setVertices(vertices);
    }
}