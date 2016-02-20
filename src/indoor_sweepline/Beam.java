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
	addCorridorPart(width);

	this.nodes = nodes;
    }
    
    
    public void setDefaultSide(CorridorPart.ReachableSide defaultSide)
    {
	this.defaultSide = defaultSide;
	adjustStripCache(defaultSide);
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

    
    public void addCorridorPart(double width)
    {
	parts.add(new CorridorPart(width, defaultType, defaultSide));
	addNode(new Node(new LatLon(addMetersToLat(
	    nodes.elementAt(nodes.size()-1).getCoor(), width), nodes.elementAt(nodes.size()-1).getCoor().lon())),
	    nodes);
	adjustStripCache(defaultSide);
    }

    
    public void setCorridorPartWidth(int partIndex, double value)
    {
	parts.elementAt(partIndex).width = value;
	adjustNodesInBeam();
	adjustStripCache(defaultSide);
    }

    
    public void setCorridorPartType(int partIndex, CorridorPart.Type type)
    {
	parts.elementAt(partIndex).type = type;
	adjustStripCache(defaultSide);
    }

    
    public void setCorridorPartSide(int partIndex, CorridorPart.ReachableSide side)
    {
	parts.elementAt(partIndex).side = side;
	adjustStripCache(defaultSide);
    }
    
    
    private void adjustStripCache(CorridorPart.ReachableSide side)
    {
	lhsStrips = new Vector<StripPosition>();
	rhsStrips = new Vector<StripPosition>();
	
	double offset = 0;
	
	for (int i = 0; i <= parts.size(); ++i)
	{
	    if (i == parts.size() || (parts.elementAt(i).type == CorridorPart.Type.VOID && i > 0))
	    {
		if (parts.elementAt(i-1).type == CorridorPart.Type.PASSAGE
			&& side == CorridorPart.ReachableSide.ALL)
		{
		    lhsStrips.add(new StripPosition(i, offset));
		    rhsStrips.add(new StripPosition(i, offset));
		}
		else if (parts.elementAt(i-1).type != CorridorPart.Type.VOID)
		{
		    if (parts.elementAt(i-1).side == CorridorPart.ReachableSide.LEFT)
			lhsStrips.add(new StripPosition(i, offset));
		    else
			rhsStrips.add(new StripPosition(i, offset));
		}
	    }
	    else if (parts.elementAt(i).type == CorridorPart.Type.PASSAGE
			&& side == CorridorPart.ReachableSide.ALL)
	    {
		if (i == 0 || parts.elementAt(i-1).type == CorridorPart.Type.VOID)
		{
		    lhsStrips.add(new StripPosition(i, offset));
		    rhsStrips.add(new StripPosition(i, offset));
		}
		else if (i > 0 && (parts.elementAt(i-1).type != CorridorPart.Type.PASSAGE
			|| side != CorridorPart.ReachableSide.ALL))
		{
		    if (parts.elementAt(i-1).side == CorridorPart.ReachableSide.LEFT)
			rhsStrips.add(new StripPosition(i, offset));
		    else
			lhsStrips.add(new StripPosition(i, offset));
		}
	    }
	    else /*if (parts.elementAt(i).type == CorridorPart.Type.WALL)*/
	    {
		if (i == 0 || parts.elementAt(i-1).type == CorridorPart.Type.VOID)
		{
		    if (parts.elementAt(i).side == CorridorPart.ReachableSide.LEFT)
			lhsStrips.add(new StripPosition(i, offset));
		    else
			rhsStrips.add(new StripPosition(i, offset));
		}
		else if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.PASSAGE
			&& side == CorridorPart.ReachableSide.ALL)
		{
		    if (parts.elementAt(i).side == CorridorPart.ReachableSide.LEFT)
			rhsStrips.add(new StripPosition(i, offset));
		    else
			lhsStrips.add(new StripPosition(i, offset));
		}
	    }
	    
	    if (i < parts.size())
		offset += parts.elementAt(i).width;
	}
	System.out.println("B " + side + " " + lhsStrips.size() + " " + rhsStrips.size());
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
	System.out.println("F " + cursor + " " + fromRight);
	if (cursor.partIndex % 2 == 0)
	{
	    if (fromRight)
	    {
		int i = countUp(rhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, true, rhsStrips, lhsStrips);
	    }
	    else
	    {
		int i = countUp(lhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, true, lhsStrips, rhsStrips);
	    }
	}
	else
	{
	    if (fromRight)
	    {
		int i = countDown(rhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, false, rhsStrips, lhsStrips);
	    }
	    else
	    {
		int i = countDown(lhsStrips, cursor.partIndex, nodes);
		return updateCursor(cursor, i, fromRight, false, lhsStrips, rhsStrips);
	    }
	}
    }
    
    
    private CorridorPart.Type defaultType;
    private CorridorPart.ReachableSide defaultSide;

    
    private int countUp(Vector<StripPosition> strips, int partIndex, List<Node> nodes)
    {
	int i = strips.elementAt(partIndex).nodeIndex;
	nodes.add(this.nodes.elementAt(i));
	while (i < parts.size() && parts.elementAt(i).type != CorridorPart.Type.VOID
		&& (parts.elementAt(i).type != CorridorPart.Type.PASSAGE
		    || defaultSide != CorridorPart.ReachableSide.ALL))
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
	while (i > 0 && parts.elementAt(i-1).type != CorridorPart.Type.VOID
		&& (parts.elementAt(i-1).type != CorridorPart.Type.PASSAGE
		    || defaultSide != CorridorPart.ReachableSide.ALL))
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
