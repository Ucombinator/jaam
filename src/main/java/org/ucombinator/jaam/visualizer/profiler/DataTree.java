package org.ucombinator.jaam.visualizer.profiler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataTree {

    private ArrayList<DataNode> topLevel;

    DataTree(String fileName) {
        try {

            File xmlFile = new File(fileName);

            DocumentBuilderFactory docBuildFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = docBuildFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(xmlFile);

            document.getDocumentElement().normalize();

            System.out.println("Root element name :- " + document.getDocumentElement().getNodeName());

            Element tree = (Element) document.getDocumentElement().getElementsByTagName("tree").item(0);

            Element allThreads = getAllThreads(tree);

            topLevel = new ArrayList<>();

            NodeList topLevelElements = allThreads.getChildNodes();

            for (int i = 0; i < topLevelElements.getLength(); ++i) {
                if (topLevelElements.item(i).getNodeType() != Node.ELEMENT_NODE
                        || topLevelElements.item(i).getNodeName().compareTo("node") != 0) continue;

                DataNode topLevelNode = new DataNode((Element)topLevelElements.item(i));

                topLevel.add(topLevelNode);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DataTree() {
        topLevel = new ArrayList<>();
    }

    public long getTotalTime() {
        return topLevel.stream().mapToLong(DataNode::getTotalTime).sum();
    }

    public DataTree prune(long threshold) {
        DataTree prunedTree = new DataTree();

        ArrayList<DataNode> toMerge = new ArrayList<>();

        for (DataNode n : topLevel) {
            if (n.getTotalTime() > threshold) {
                prunedTree.topLevel.add(prune(n, threshold));
            }
            else {
                toMerge.add(n);
            }
        }

        prunedTree.topLevel.add(merge("Merged", toMerge));

        return prunedTree;
    }

    public DataNode prune(DataNode node, long threshold ) {

        ArrayList<DataNode> survivingChildren = new ArrayList<>();
        ArrayList<DataNode> toMerge = new ArrayList<>();

        for (DataNode n : node.getChildren()) {
            if (n.getTotalTime() > threshold) {
                survivingChildren.add(prune(n, threshold));
            }
            else {
                toMerge.add(n);
            }
        }

        if (!toMerge.isEmpty()) {
            survivingChildren.add(merge("Merged", toMerge));
        }

        return new DataNode(node.getName(), node.getRelativeTime(), node.getTotalTime(), node.getInvocations(), survivingChildren);
    }

    public DataNode merge(String name, List<DataNode> nodes) {

        long sumTotal = 0;
        int sumInvocations = 0;
        double sumRelative = 0;

        for (DataNode n : nodes) {
            sumTotal       += n.getTotalTime();
            sumInvocations += n.getInvocations();
            sumRelative    += n.getRelativeTime();
        }

        int averageInvocations = sumInvocations / nodes.size();

        if (nodes.size() == 1) {
            name = nodes.get(0).getName();
        }

        return new DataNode(name, sumRelative, sumTotal, averageInvocations, new ArrayList<>());
    }

    public void print(int maxDepth) {

        for (DataNode r : topLevel) {
            System.out.println("--------------------------------------------------------------------------------");
            r.print(0, maxDepth);
            System.out.println("--------------------------------------------------------------------------------");
        }
    }

    private Element getAllThreads(Element tree) {

        for (int i = 0; i < tree.getChildNodes().getLength(); ++i) {
            if (tree.getChildNodes().item(i).getNodeName().compareTo("node") == 0)
                return (Element) tree.getChildNodes().item(i);
        }

        return null;
    }
}
