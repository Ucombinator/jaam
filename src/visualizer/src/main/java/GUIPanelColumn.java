/**
 * Created by timothyjohnson on 10/5/16.
 */
// A column in our GUI is constructed from an array of panels
// We automatically add expandable split panes between each adjacent pair of panels
import java.util.ArrayList;

import javafx.geometry.Orientation;
import javafx.scene.layout.Pane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;

public class GUIPanelColumn
{
    ArrayList<Pane> panes;
    ArrayList<SplitPane> splitPanes;

    public GUIPanelColumn(ArrayList<Pane> paneList, ArrayList<Double> weights)
    {
        assert(paneList.size() > 0);
        this.panes = paneList;
        splitPanes = new ArrayList<SplitPane>();

        for(int i = 0; i < panes.size() - 1; i++)
        {
            SplitPane newSplit = new SplitPane();
            newSplit.setOrientation(Orientation.VERTICAL);
            if(i == 0)
                newSplit.getItems().add(panes.get(0));
            else
                newSplit.getItems().add(splitPanes.get(splitPanes.size() - 1));

            newSplit.getItems().add(panes.get(i + 1));
            splitPanes.add(newSplit);
        }

        // TODO: Set initial sizes of panes
        /*for(int i = 0; i < splitPanes.size(); i++)
        {
            splitPanes.get(i).setResizeWeight(weights.get(i));
            splitPanes.get(i).resetToPreferredSizes();
        }*/
    }

    // Returns our Pane if there is only one pane,
    // or the first SplitPane if there are more panes.
    public Region getComponentLink()
    {
        if(panes.size() == 1)
            return panes.get(0);
        else
            return splitPanes.get(0);
    }
}
