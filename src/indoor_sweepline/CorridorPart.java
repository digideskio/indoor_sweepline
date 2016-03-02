package indoor_sweepline;

import java.util.List;
import java.util.Vector;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;


public class CorridorPart
{
    public enum Type
    {
	VOID,
	PASSAGE,
	WALL,
	STAIRS,
	ESCALATOR,
	ELEVATOR
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
    
    
    private void setExtraElements(Node from, Node to)
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
	    
	LatLon start = from.getCoor();
	if (side == ReachableSide.LEFT)
	{
	    if (start.lat() < middleCoor.lat())
		start = to.getCoor();
	}
	else if (side == ReachableSide.RIGHT)
	{
	    if (middleCoor.lat() < start.lat())
		start = to.getCoor();
	}
	else if (side == ReachableSide.FRONT)
	{
	    if (start.lon() < middleCoor.lon())
		start = to.getCoor();
	}
	else if (side == ReachableSide.BACK)
	{
	    if (middleCoor.lon() < start.lon())
		start = to.getCoor();
	}
	    
	double scale = Math.cos(middleCoor.lat() * (Math.PI/180.));
	LatLon detachedCoor = new LatLon(middleCoor.lat() + (start.lon() - middleCoor.lon()) * scale,
	    middleCoor.lon() + (start.lat() - middleCoor.lat()) / scale);
	if (detachedNode == null)
	{
	    detachedNode = new Node(detachedCoor);
	    dataSet.addPrimitive(detachedNode);
	}
	else
	    detachedNode.setCoor(detachedCoor);
	
	Vector<Node> extraWayNodes = new Vector<Node>();
	extraWayNodes.add(middleNode);
	extraWayNodes.add(detachedNode);
	if (extraWay == null)
	{
	    extraWay = new Way();
	    extraWay.setNodes(extraWayNodes);
	    dataSet.addPrimitive(extraWay);
	}
	else
	    extraWay.setNodes(extraWayNodes);
    }
    
    
    public void appendNodes(Node from, Node to, List<Node> nodes)
    {
	if (type == Type.STAIRS)
	{
	    setExtraElements(from, to);
	    nodes.add(middleNode);

	    extraWay.removeAll();
	    extraWay.put("highway", "steps");
	    extraWay.put("incline", "up;down");
	}
	else if (type == Type.ESCALATOR)
	{
	    setExtraElements(from, to);
	    nodes.add(middleNode);

	    extraWay.removeAll();
	    extraWay.put("highway", "steps");
	    extraWay.put("incline", "up;down");
	    extraWay.put("conveying", "forward;backward");
	}
	else if (type == Type.ELEVATOR)
	{
	    setExtraElements(from, to);
	    nodes.add(middleNode);

	    detachedNode.removeAll();
	    detachedNode.put("highway", "elevator");
	    
	    extraWay.removeAll();
	    extraWay.put("highway", "footway");
	}
	else
	{
	    if (middleNode != null)
	    {
		middleNode.setDeleted(true);
		middleNode = null;
	    }
	    if (detachedNode != null)
	    {
		detachedNode.setDeleted(true);
		detachedNode = null;
	    }
	    if (extraWay != null)
	    {
		extraWay.setDeleted(true);
		extraWay = null;
	    }
	}
    }
    

    public double width;
    private Type type;
    private ReachableSide side;
    
    private DataSet dataSet;
    private Node middleNode;
    private Node detachedNode;
    private Way extraWay;
    
    
    private void adjustSideType(ReachableSide beamSide)
    {
	if (type == Type.PASSAGE)
	    side = ReachableSide.ALL;
	else if (type != Type.VOID && side == ReachableSide.ALL)
	{
	    if (beamSide == ReachableSide.RIGHT)
		side = ReachableSide.RIGHT;
	    else
		side = ReachableSide.LEFT;
	}
	System.out.println("A " + type + " " + side);
    }
}
