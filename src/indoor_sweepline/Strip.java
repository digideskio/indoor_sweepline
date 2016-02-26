package indoor_sweepline;


import java.util.Vector;
import org.openstreetmap.josm.data.osm.DataSet;


public class Strip
{
    public Strip(DataSet dataSet)
    {
	width = 10.;
	parts = new Vector<CorridorPart>();
	lhs = new Vector<Double>();
	rhs = new Vector<Double>();
	
	this.dataSet = dataSet;
    }

    
    public void setCorridorPartType(int partIndex, CorridorPart.Type type)
    {
	while (parts.size() <= partIndex)
	    parts.add(new CorridorPart(0., CorridorPart.Type.WALL,
		partIndex % 2 == 0 ? CorridorPart.ReachableSide.FRONT :
		CorridorPart.ReachableSide.BACK, dataSet));
	parts.elementAt(partIndex).setType(type, CorridorPart.ReachableSide.ALL);
    }
    
    
    public CorridorPart partAt(int i)
    {
	while (parts.size() <= i)
	    parts.add(new CorridorPart(0., CorridorPart.Type.WALL,
		i % 2 == 0 ? CorridorPart.ReachableSide.FRONT : CorridorPart.ReachableSide.BACK, dataSet));
	return parts.elementAt(i);
    }
    
    
    public double width;
    public Vector<CorridorPart> parts;
    public Vector<Double> lhs;
    public Vector<Double> rhs;
    
    private DataSet dataSet;
}
