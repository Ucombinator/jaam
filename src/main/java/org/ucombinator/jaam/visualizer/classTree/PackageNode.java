package org.ucombinator.jaam.visualizer.classTree;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.controlsfx.glyphfont.FontAwesome;
import org.ucombinator.jaam.visualizer.layout.CodeEntity;
import org.ucombinator.jaam.visualizer.layout.StateVertex;
import org.ucombinator.jaam.visualizer.main.Main;

import java.util.HashSet;

public class PackageNode extends ClassTreeNode {

    public HashSet<PackageNode> subPackages;
    public HashSet<ClassNode>   classNodes; // Leaf nodes store their associated vertices

    public PackageNode(String name, String prefix) {
        super(name,prefix);
        this.subPackages = new HashSet<>();
        this.classNodes  = new HashSet<>();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public PackageNode addPackageIfAbsent(String name) {
        PackageNode subDir = null;
        for(PackageNode f : subPackages)
        {
            if(f.name.compareTo(name) == 0)
            {
                subDir = f;
                break;
            }
        }
        if(subDir == null)
        {
            subDir = new PackageNode(name, this.fullName);
            subPackages.add(subDir);
        }

        return subDir;
    }

    public void addClassIfAbsent(String name) {
        classNodes.add(new ClassNode(name, this.fullName));
    }

    public void compress()
    {
        while(subPackages.size() == 1 && classNodes.isEmpty())
        {
            PackageNode onlyElement = subPackages.iterator().next();
            name = name.concat("." + onlyElement.name);
            subPackages = onlyElement.subPackages;
            classNodes.addAll(onlyElement.classNodes);
        }

        for(PackageNode f : subPackages)
            f.compress();
    }

    public void build(TreeItem<ClassTreeNode> parent) {
        CheckBoxTreeItem<ClassTreeNode> item = buildTreeItem(parent);

        item.setGraphic(Main.getIconFont().create(FontAwesome.Glyph.FOLDER).color(Color.DARKBLUE));

        Main.getIconFont().create(FontAwesome.Glyph.FOLDER_ALT).color(Color.DARKBLUE);

        for (PackageNode p: subPackages) {
            p.build(item);
        }

        for (ClassNode c: classNodes) {
            c.build(item);
        }
    }

    @Override
    public HashSet<StateVertex> getChildVertices()
    {
        HashSet<StateVertex> all = new HashSet<>();
        for (PackageNode p : subPackages) {
            all.addAll(p.getChildVertices());
        }
        for (ClassNode c : classNodes) {
            all.addAll(c.getChildVertices());
        }

        return all;
    }

    public boolean addVertex(StateVertex vertex) {

        if (!(vertex instanceof CodeEntity)) return false;

        String vertexClassName = ((CodeEntity) vertex).getClassName();

        for (PackageNode p : subPackages) {
            if (vertexClassName.startsWith(p.fullName)) {
                return p.addVertex(vertex);
            }
        }
        for (ClassNode c : classNodes) {
            if (vertexClassName.startsWith(c.fullName)) {
                return c.addVertex(vertex);
            }
        }
        return false;
    }

}
