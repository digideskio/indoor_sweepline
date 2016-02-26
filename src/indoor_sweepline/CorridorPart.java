package indoor_sweepline;

import java.util.List;
import java.util.Vector;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;


public class CorridorPart
{
    public enum Type
    {
	VOID,
	PASSAGE,
	WALL,
	STAIRS
    }
    

    public enum ReachableSide
    {
	ALL,
	FRONT,
	BACK,
	LEFT,
	RIGHT
    }
    

    public CorridorPart(double width, Type type, ReachableSide side, DataSet dataSet)
    {
	this.width = width;
	this.type = type;
	this.side = side;
	
	this.dataSet = dataSet;
    }
    
    
    public boolean isObstacle(ReachableSide beamSide)
    {
	if (type == Type.VOID)
	    return false;
	if (type == Type.PASSAGE)
	    return (beamSide != ReachableSide.ALL);
	return true;
    }
    
    
    public Type getType()
    {
	return type;
    }
    
    public void setType(Type type, ReachableSide beamSide)
    {
	this.type = type;
	adjustSideType(beamSide);
    }
    
    
    public ReachableSide getSide()
    {
	return side;
    }
    
    public void setSide(ReachableSide side, ReachableSide beamSide)
    {
	this.side = side;
	adjustSideType(beamSide);
    }
    
    
    public void appendNodes(Node from, Node to, List<Node> nodes)
    {
	if (type == Type.STAIRS)
	{
	    LatLon middleCoor = new LatLon((from.getCoor().lat() + to.getCoor().lat())/2.,
		(from.getCoor().lon() + to.getCoor().lon())/2.);
	    if (middleNode == null)
	    {
		middleNode = new Node(middleCoor);
		dataSet.addPrimitive(middleNode);
	    }
	    else
		middleNode.setCoor(middleCoor);
	    nodes.add(middleNode);
	}
	else
	{
	    if (middleNode != null)
		middleNode.setDeleted(true);
	}
    }
    

    public double width;
    private Type type;
    private ReachableSide side;
    
    private DataSet dataSet;
    private Node middleNode;
    private Node detachedNode;
    
    
    private void adjustSideType(ReachableSide beamSide)
    {
	if ((type == Type.WALL || type == Type.STAIRS) && side == ReachableSide.ALL)
	{
	    if (beamSide == ReachableSide.RIGHT)
		side = ReachableSide.RIGHT;
	    else
		side = ReachableSide.LEFT;
	}
	else if (type == Type.PASSAGE)
	    side = ReachableSide.ALL;
    }
}
