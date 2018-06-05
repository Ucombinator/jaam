package org.ucombinator.jaam.visualizer.profiler;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class DataNode {

    private String name;

    private double relativeTime;
    private long totalTime;
    private long myTime;
    private long descendentTime;
    private int invocations;

    private ArrayList<DataNode> children;

    DataNode(Element xmlElement) {

        NodeList innerValues = xmlElement.getChildNodes();

        setMembers(innerValues);
    }

    public DataNode(String name, double relativeTime, long totalTime, int invocations, List<DataNode> children) {
        this.name = name;
        this.relativeTime = relativeTime;
        this.totalTime = totalTime;
        this.invocations = invocations;
        this.children = new ArrayList<>();

        this.descendentTime = 0;
        for (DataNode c : children) {
            this.children.add(c);
            this.descendentTime += c.getTotalTime();
        }

        normalizeTime();
    }

    private void setMembers(NodeList innerValues) {

        children = new ArrayList<>();
        descendentTime = 0;

        for (int i = 0; i < innerValues.getLength(); ++i) {
            if (innerValues.item(i).getNodeType() != Node.ELEMENT_NODE) continue;

            Element e = (Element) innerValues.item(i);

            String eName = e.getTagName();

            if (eName.compareTo("Name") == 0) {
                this.name = e.getTextContent();
            }
            else if (eName.compareTo("Time_Relative") == 0) {
                String formattedText = e.getTextContent();
                formattedText = formattedText.substring(0, e.getTextContent().indexOf("%"));
                formattedText = formattedText.replace(",","");
                this.relativeTime = Double.parseDouble(formattedText);
            }
            else if (eName.compareTo("Time") == 0) {
                this.totalTime = Long.parseLong(e.getTextContent());
            }
            else if (eName.compareTo("Invocations") == 0) {
                this.invocations = Integer.parseInt(e.getTextContent());
            }
            else if (eName.compareTo("node") == 0) {

                DataNode child = new DataNode(e);
                this.children.add(child);
                this.descendentTime += child.totalTime;
            }
        }

        normalizeTime();
    }

    private void normalizeTime() {
        this.myTime = this.totalTime - this.descendentTime;
        if (this.myTime < 0) { // Sometimes the profiler calculation seems to be off...
            this.totalTime = this.descendentTime;
            this.myTime = 0;
        }
    }

    public void print() {
        print(0, -1);
    }

    public void print(int depth, int maxDepth) {
        for (int i = 0; i < depth; ++i) System.out.print("\t");
        System.out.println(this);

        if (depth == maxDepth) return;

        for (DataNode c : children) c.print(depth+1, maxDepth);
    }
    public String getName() {
        return name;
    }

    public double getRelativeTime() {
        return relativeTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getMyTime() {
        return myTime;
    }

    public long getDescendentTime() {
        return descendentTime;
    }

    public int getInvocations() {
        return invocations;
    }

    public ArrayList<DataNode> getChildren() {
        return children;
    }


    @Override
    public String toString() {
        return "Name : " + name + " -- TotalTime : " + totalTime +
                " -- MyTime : " + myTime + " -- DescendentTime " + descendentTime +
                " -- Invocations : " + invocations + " -- NumChildren : " + children.size() +
                " -- RelativeTime : " + relativeTime;
    }
}
