package org.ucombinator.jaam.visualizer.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Graph
{
    private ArrayList<AbstractVertex<AbstractVertex>> vertices;
    private ArrayList<String> tagsList;
    private ArrayList<Boolean> highlightedTags;
    private HashMap<String, Method> methods;
    private ArrayList<OurClass> ourClasses;

    private double maxHeight; // required for collapse method
    private int maxIndex;
    
    public Graph()
    {
        this.vertices = new ArrayList<AbstractVertex<AbstractVertex>>();
        this.methods = new LinkedHashMap<String, Method>();
        this.ourClasses = new ArrayList<OurClass>();
        this.maxIndex = -1;

        this.tagsList = new ArrayList<String>();
        this.highlightedTags = new ArrayList<Boolean>();
    }

    public ArrayList<AbstractVertex<AbstractVertex>> getVertices() {
        return this.vertices;
    }

    public ArrayList<OurClass> getOurClasses() {
        return this.ourClasses;
    }

//    public Method getMethod(String methodName) {
//        if(this.methods.containsKey(methodName))
//            return this.methods.get(methodName);
//        else
//            return null;
//    }

    public void setMaxHeight(double height)
    {
        this.maxHeight = height;
    }

    public double getMaxHeight()
    {
        return maxHeight;
    }

//    public void addErrorState(int id)
//    {
//        Instruction instruction = new Instruction("ErrorState", "", -1, false);
//        this.addVertex(id, instruction, false);
//    }

    public void addVertex(AbstractVertex vertex){
    	this.vertices.add(vertex);
    }
    
//    public void addVertex(int vIndex, Instruction instruction, boolean drawEdges)
//    {
//        //System.out.println("Adding vertex " + vIndex + ": " + instruction.getText());
//        AbstractVertex<AbstractVertex> ver = this.containsInputVertex(vIndex);
//
//        if(ver == null)
//        {
//            ver = new Vertex(instruction, vIndex, true);
//            this.vertices.add(ver);
//        }
//        else {
//            ver.setRealInstruction(instruction);
//            //System.out.println("Setting method for dummy vertex: " + ver.getMethodName());
//        }
//
//        this.matchVertexToMethod(ver, instruction.getMethodName());
//        ver.setDrawEdges(drawEdges);
//
//        if(instruction.getJimpleIndex() > this.maxIndex)
//            this.maxIndex = instruction.getJimpleIndex();
//    }

    public void matchVertexToMethod(AbstractVertex<AbstractVertex> v, String methodName)
    {
        Method currMethod;
        if(this.methods.containsKey(methodName))
        {
            currMethod = this.methods.get(methodName);
        }
        else
        {
            currMethod = new Method(this, methodName);
            this.methods.put(methodName, currMethod);
        }

        currMethod.addVertex(v);
    }

    public void addEdge(int src, int dest)
    {
        //System.out.println("Adding input edge: " + src + ", " + dest);
        AbstractVertex<AbstractVertex> vSrc, vDest;

        vSrc = this.containsInputVertex(src);
        vDest = this.containsInputVertex(dest);
        
        vSrc.addOutgoingNeighbor(vDest);
        vDest.addIncomingNeighbor(vSrc);
    }

//    public void addClass(String className, String classCode) {
//        System.out.println("Adding class: " + className);
//        OurClass c = new OurClass(className, classCode);
//        this.ourClasses.add(c);
//    }

    public void matchMethodsToClasses() {
        if(ourClasses.size() > 0) {
            System.out.println("Matching methods to ourClasses...");
            for(OurClass c : this.ourClasses)
                System.out.println(c.getCode());

            for (Method m : this.methods.values()) {
                System.out.println("Looking for match for method: " + m.getFullName());
                String fullClassName = m.parseClassName();
                int lastDotIndex = fullClassName.lastIndexOf(".");
                int firstDollarIndex = fullClassName.indexOf("$");

                String packageName = fullClassName.substring(0, lastDotIndex);
                String className;
                if (firstDollarIndex >= 0)
                    className = fullClassName.substring(lastDotIndex + 1, firstDollarIndex);
                else
                    className = fullClassName.substring(lastDotIndex + 1);

                System.out.println("packageName: " + packageName);
                System.out.println("className: " + className);

                for (OurClass c : this.ourClasses) {
                    if (c.getCode().contains("package " + packageName) && c.getCode().contains("class " + className)) {
                        System.out.println("Method: " + m.getFullName());
                        System.out.println("Found matching class: " + fullClassName);
                        c.setName(fullClassName);

                        c.addMethod(m);
                        m.setClass(c);
                    }
                }
            }
        }
    }

//    public void addTag(int nodeId, String tag)
//    {
//        AbstractVertex<AbstractVertex> ver = this.containsInputVertex(nodeId);
//        if(ver==null)
//            return;
//        
//        int t = this.tagPresent(tag);
//        
//        if(t < 0)
//        {
//            t = this.tagsList.size();
//            this.tagsList.add(tag);
//            this.highlightedTags.add(new Boolean(true));
//        }
//
//        //ver.addTag(Integer.toString(t));
//    }

//    public int tagPresent(String tag)
//    {
//        int i = 0;
//        for(String t : this.tagsList)
//        {
//            if(t.equalsIgnoreCase(tag))
//                return i;
//            i++;
//        }
//        return -1;
//    }
    
    public AbstractVertex<AbstractVertex> containsInputVertex(int id)
    {
        for(AbstractVertex<AbstractVertex> v : this.vertices)
        {
            if(v.getId() == id)
                return v;
        }

        return null;
    }

    /*public void save(String filename) {
        try {
            PacketOutput output = new PacketOutput(new FileOutputStream(filename));
            for (Vertex v : this.vertices)
                v.save(output);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }*/
}
