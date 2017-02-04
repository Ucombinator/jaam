
// A column in our GUI is constructed from an array of panels
// We automatically add expandable split panes between each adjacent pair of panels
import java.util.ArrayList;
import javax.swing.JSplitPane;
import javax.swing.JComponent;

public class GUIPanelColumn
{
    ArrayList<JComponent> panels;
    ArrayList<JSplitPane> splitPanes;

    public GUIPanelColumn(ArrayList<JComponent> panelList, ArrayList<Double> weights)
    {
        assert(panelList.size() > 0);
        this.panels = panelList;
        splitPanes = new ArrayList<JSplitPane>();

        for(int i = 0; i < panels.size() - 1; i++)
        {
            JSplitPane newSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
            newSplit.setOneTouchExpandable(true);
            if(i == 0)
                newSplit.setTopComponent(panels.get(0));
            else
                newSplit.setTopComponent(splitPanes.get(splitPanes.size() - 1));

            newSplit.setBottomComponent(panels.get(i + 1));
            splitPanes.add(newSplit);
        }

        for(int i = 0; i < splitPanes.size(); i++)
        {
            splitPanes.get(i).setResizeWeight(weights.get(i));
            splitPanes.get(i).resetToPreferredSizes();
        }

        //System.out.println("Finished constructing column! Panels = " + Integer.toString(panels.size()) + ", split panes = " + Integer.toString(splitPanes.size()));
    }

    // Returns our JPanel if there is only one panel,
    // or the first JSplitPane if there are more panels.
    public JComponent getComponentLink()
    {
        if(panels.size() == 1)
            return panels.get(0);
        else
            return splitPanes.get(0);
    }
}
