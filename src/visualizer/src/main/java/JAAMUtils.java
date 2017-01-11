import java.util.Iterator;

public class JAAMUtils {

	private static AbstractVertex getVertexWithID(String id){
		Iterator<AbstractVertex> it = Parameters.stFrame.mainPanel.getPanelRoot().getInnerGraph().getVertices().values().iterator();
		while(it.hasNext()){
			AbstractVertex v = it.next();
			if(v.getStrID().equals(id)){
				return v;
			}
		}
		return null;
	}

}
