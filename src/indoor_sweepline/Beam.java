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
	    parts.add(new CorridorPart(width, defaultType, defaultSide, dataSet));
	else
	    parts.add(0, new CorridorPart(width, defaultType, defaultSide, dataSet));
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
    
    
    public Node lhsNode(int i)
    {
	return nodes.elementAt(lhsStrips.elementAt(i).nodeIndex);
    }
    
    public Node rhsNode(int i)
    {
	return nodes.elementAt(rhsStrips.elementAt(i).nodeIndex);
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
	    if (isVoidBelow(i))
	    {
		if (isPassageAbove(i))
		{
		    lhsStrips.add(new StripPosition(i, offset));
		    rhsStrips.add(new StripPosition(i, offset));
		}
		else if (!isVoidAbove(i))
		{
		    if (isReachableLeft(i-1))
			lhsStrips.add(new StripPosition(i, offset));
		    else
			rhsStrips.add(new StripPosition(i, offset));
		}
	    }
	    else if (isPassageBelow(i))
	    {
		if (isVoidAbove(i))
		{
		    lhsStrips.add(new StripPosition(i, offset));
		    rhsStrips.add(new StripPosition(i, offset));
		}
		else if (!isPassageAbove(i))
		{
		    if (isReachableLeft(i-1))
			rhsStrips.add(new StripPosition(i, offset));
		    else
			lhsStrips.add(new StripPosition(i, offset));
		}
	    }
	    else
	    {
		if (isVoidAbove(i))
		{
		    if (isReachableLeft(i))
			lhsStrips.add(new StripPosition(i, offset));
		    else
			rhsStrips.add(new StripPosition(i, offset));
		}
		else if (isPassageAbove(i))
		{
		    if (isReachableLeft(i))
			rhsStrips.add(new StripPosition(i, offset));
		    else
			lhsStrips.add(new StripPosition(i, offset));
		}
	    }
	    
	    if (i < parts.size())
		offset += parts.elementAt(i).width;
	}
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
    

    public boolean appendNodes(IndoorSweeplineModel.SweepPolygonCursor cursor, boolean fromRight, Vector<Node> nodes,
	Vector<Strip> strips)
    {
	if (fromRight)
	{
	    if (nodes.size() > 0)
		strips.elementAt(cursor.stripIndex).partAt(cursor.partIndex).
		    appendNodes(nodes.elementAt(nodes.size()-1),
			this.nodes.elementAt(rhsStrips.elementAt(cursor.partIndex).nodeIndex), nodes);
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
	    if (nodes.size() > 0)
	    {
		System.out.println(lhsStrips.size() + " " + cursor.partIndex);
		strips.elementAt(cursor.stripIndex).partAt(cursor.partIndex).
		    appendNodes(nodes.elementAt(nodes.size()-1),
			this.nodes.elementAt(lhsStrips.elementAt(cursor.partIndex).nodeIndex), nodes);
	    }
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
	    parts.elementAt(i).appendNodes(this.nodes.elementAt(i), this.nodes.elementAt(i+1), nodes);
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
	    parts.elementAt(i-1).appendNodes(this.nodes.elementAt(i-1), this.nodes.elementAt(i), nodes);
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
		++cursor.partIndex;
		return !fromRight;
	    }
	}
	else
	{
	    if (cursor.partIndex > 0 && sameStrips.elementAt(cursor.partIndex-1).nodeIndex == i)
	    {
		--cursor.partIndex;
		return !fromRight;
	    }
	}
	
	int j = 0;
	while (j < oppositeStrips.size() && oppositeStrips.elementAt(j).nodeIndex < i)
	    ++j;
	cursor.partIndex = j;
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
