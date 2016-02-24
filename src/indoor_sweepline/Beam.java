package indoor_sweepline;

import java.util.List;
import java.util.Vector;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;


public class Beam
{
    public Beam(DataSet dataSet, LatLon center, double width, CorridorPart.ReachableSide defaultSide)
    {
	parts = new Vector<CorridorPart>();
	nodes = new Vector<Node>();
	this.dataSet = dataSet;
	defaultType = CorridorPart.Type.WALL;
	this.defaultSide = defaultSide;
	
	addNode(new Node(center), nodes);
	addCorridorPart(true, width);

	this.nodes = nodes;
    }
    
    
    public void setDefaultSide(CorridorPart.ReachableSide defaultSide)
    {
	this.defaultSide = defaultSide;
	adjustStripCache();
    }
    

    public void adjustNodes(double lon)
    {
	for (Node node : nodes)
	    node.setCoor(new LatLon(node.getCoor().lat(), lon));
    }
    
    
    public void adjustNodesInBeam()
    {
	double offset = 0;
	for (int i = 0; i < parts.size(); ++i)
	{
	    offset += parts.elementAt(i).width;
	    nodes.elementAt(i+1).setCoor(new LatLon(addMetersToLat(
		nodes.elementAt(0).getCoor(), offset), nodes.elementAt(0).getCoor().lon()));
	}
    }
    
    
    public List<CorridorPart> getBeamParts()
    {
	return parts;
    }
    
    
    public LatLon getFirstCoor()
    {
	return nodes.elementAt(0).getCoor();
    }

    
    public void addCorridorPart(boolean append, double width)
    {
	if (append)
	    parts.add(new CorridorPart(width, defaultType, defaultSide));
	else
	    parts.add(0, new CorridorPart(width, defaultType, defaultSide));;
	addNode(new Node(nodes.elementAt(0).getCoor()), nodes);
	adjustNodesInBeam();
	adjustStripCache();
    }

    
    public void setCorridorPartWidth(int partIndex, double value)
    {
	parts.elementAt(partIndex).width = value;
	adjustNodesInBeam();
	adjustStripCache();
    }

    
    public void setCorridorPartType(int partIndex, CorridorPart.Type type)
    {
	parts.elementAt(partIndex).setType(type, defaultSide);
	adjustStripCache();
    }

    
    public void setCorridorPartSide(int partIndex, CorridorPart.ReachableSide side)
    {
	parts.elementAt(partIndex).setSide(side, defaultSide);
	adjustStripCache();
    }
    
    
    private boolean isVoidAbove(int i)
    {
	return i == 0 || parts.elementAt(i-1).getType() == CorridorPart.Type.VOID;
    }
    
    private boolean isVoidBelow(int i)
    {
	return i == parts.size() || parts.elementAt(i).getType() == CorridorPart.Type.VOID;
    }
    
    private boolean isPassageAbove(int i)
    {
	return i > 0
	    && parts.elementAt(i-1).getType() == CorridorPart.Type.PASSAGE
	    && defaultSide == CorridorPart.ReachableSide.ALL;
    }
    
    private boolean isPassageBelow(int i)
    {
	return i < parts.size()
	    && parts.elementAt(i).getType() == CorridorPart.Type.PASSAGE
	    && defaultSide == CorridorPart.ReachableSide.ALL;
    }
    
    private boolean isReachableLeft(int i)
    {
	if (defaultSide == CorridorPart.ReachableSide.RIGHT)
	    return false;
	if (parts.elementAt(i).getSide() == CorridorPart.ReachableSide.LEFT)
	    return true;
	return defaultSide == CorridorPart.ReachableSide.LEFT;
    }
    
    private void adjustStripCache()
    {
	lhsStrips = new Vector<StripPosition>();
	rhsStrips = new Vector<StripPosition>();
	
	double offset = 0;
	
	for (int i = 0; i <= parts.size(); ++i)
	{
	    if (i == parts.size())
		System.out.println("I " + defaultSide + " " + i + " " + offset);
	    else
		System.out.println("I " + defaultSide + " " + i + " " + offset + " " + parts.elementAt(i).getType() + " " + parts.elementAt(i).getSide());
	    if (isVoidBelow(i))
	    {
		if (isPassageAbove(i))
		{
		    System.out.println("JA");
		    lhsStrips.add(new StripPosition(i, offset));
		    rhsStrips.add(new StripPosition(i, offset));
		}
		else if (!isVoidAbove(i))
		{
		    System.out.println("JB");
		    if (isReachableLeft(i-1))
			lhsStrips.add(new StripPosition(i, offset));
		    else
			rhsStrips.add(new StripPosition(i, offset));
		}
		else
		    System.out.println("JC");
	    }
	    else if (isPassageBelow(i))
	    {
		if (isVoidAbove(i))
		{
		    System.out.println("JD");
		    lhsStrips.add(new StripPosition(i, offset));
		    rhsStrips.add(new StripPosition(i, offset));
		}
		else if (!isPassageAbove(i))
		{
		    System.out.println("JE");
		    if (isReachableLeft(i-1))
			rhsStrips.add(new StripPosition(i, offset));
		    else
			lhsStrips.add(new StripPosition(i, offset));
		}
		else
		    System.out.println("JF");
	    }
	    else
	    {
		if (isVoidAbove(i))
		{
		    System.out.println("JG");
		    if (isReachableLeft(i))
			lhsStrips.add(new StripPosition(i, offset));
		    else
			rhsStrips.add(new StripPosition(i, offset));
		}
		else if (isPassageAbove(i))
		{
		    System.out.println("JH");
		    if (isReachableLeft(i))
			rhsStrips.add(new StripPosition(i, offset));
		    else
			lhsStrips.add(new StripPosition(i, offset));
		}
		else
		    System.out.println("JI");
	    }
	    
	    if (i < parts.size())
		offset += parts.elementAt(i).width;
	}
	System.out.println("B " + defaultSide + " " + lhsStrips.size() + " " + rhsStrips.size());
    }
    
    
    public Vector<Double> leftHandSideStrips()
    {
	Vector<Double> offsets = new Vector<Double>();
	for (StripPosition pos : lhsStrips)
	    offsets.add(pos.offset);
	    
	return offsets;
    }
    
    
    public Vector<Double> rightHandSideStrips()
    {
	Vector<Double> offsets = new Vector<Double>();
	for (StripPosition pos : rhsStrips)
	    offsets.add(pos.offset);
	    
	return offsets;
    }
    

    public boolean appendNodes(IndoorSweeplineModel.SweepPolygonCursor cursor, boolean fromRight, List<Node> nodes)
    {
	System.out.println("F " + cursor.stripIndex + " " + cursor.partIndex + " " + fromRight + " " + lhsStrips.size() + " " + rhsStrips.size());
	if (fromRight)
	{
	    if (rhsStrips.elementAt(cursor.partIndex).nodeIndex > 0 &&
		parts.elementAt(rhsStrips.elementAt(cursor.partIndex).nodeIndex - 1).isObstacle(defaultSide))
	    {
		int i = countDown(rhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, false, rhsStrips, lhsStrips);
	    }
	    else
	    {
		int i = countUp(rhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, true, rhsStrips, lhsStrips);
	    }
	}
	else
	{
	    if (lhsStrips.elementAt(cursor.partIndex).nodeIndex > 0 &&
		parts.elementAt(lhsStrips.elementAt(cursor.partIndex).nodeIndex - 1).isObstacle(defaultSide))
	    {
		int i = countDown(lhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, false, lhsStrips, rhsStrips);
	    }
	    else
	    {
		int i = countUp(lhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, true, lhsStrips, rhsStrips);
	    }
	}
    }
    
    
    private CorridorPart.Type defaultType;
    private CorridorPart.ReachableSide defaultSide;

    
    private int countUp(Vector<StripPosition> strips, int partIndex, List<Node> nodes)
    {
	int i = strips.elementAt(partIndex).nodeIndex;
	nodes.add(this.nodes.elementAt(i));
	while (i < parts.size() && parts.elementAt(i).isObstacle(defaultSide))
	{
	    ++i;
	    nodes.add(this.nodes.elementAt(i));
	}
	return i;
    }
    
    
    private int countDown(Vector<StripPosition> strips, int partIndex, List<Node> nodes)
    {
	int i = strips.elementAt(partIndex).nodeIndex;
	nodes.add(this.nodes.elementAt(i));
	while (i > 0 && parts.elementAt(i-1).isObstacle(defaultSide))
	{
	    --i;
	    nodes.add(this.nodes.elementAt(i));
	}
	return i;
    }
    
    
    private static boolean updateCursor(IndoorSweeplineModel.SweepPolygonCursor cursor, int i,
	boolean fromRight, boolean goingUp, Vector<StripPosition> sameStrips, Vector<StripPosition> oppositeStrips)
    {
	if (goingUp)
	{
	    if (cursor.partIndex+1 < sameStrips.size() && sameStrips.elementAt(cursor.partIndex+1).nodeIndex == i)
	    {
		System.out.println("GA");
		++cursor.partIndex;
		return !fromRight;
	    }
	}
	else
	{
	    if (cursor.partIndex > 0 && sameStrips.elementAt(cursor.partIndex-1).nodeIndex == i)
	    {
		System.out.println("GB");
		--cursor.partIndex;
		return !fromRight;
	    }
	}
	
	int j = 0;
	while (j < oppositeStrips.size() && oppositeStrips.elementAt(j).nodeIndex < i)
	    ++j;
	cursor.partIndex = j;
	System.out.println("G " + i + " " + goingUp + " " + j + " " + fromRight);
	if (fromRight)
	    --cursor.stripIndex;
	else
	    ++cursor.stripIndex;
	return fromRight;
    }
    
    
    private class StripPosition
    {
	StripPosition(int nodeIndex, double offset)
	{
	    this.nodeIndex = nodeIndex;
	    this.offset = offset;
	}
    
	int nodeIndex;
	double offset;
    }
    
    private Vector<CorridorPart> parts;
    private Vector<StripPosition> lhsStrips;
    private Vector<StripPosition> rhsStrips;
    private Vector<Node> nodes;
    private DataSet dataSet;

    
    private void addNode(Node node, List<Node> nodes)
    {
	dataSet.addPrimitive(node);
	nodes.add(node);
    }

    
    private static double addMetersToLat(LatLon latLon, double south)
    {
	return latLon.lat() - south *(360./4e7);
    }
}
