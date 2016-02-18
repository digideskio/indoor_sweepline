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
    
    
    public void addNorthernNodes(List<Node> nodes)
    {
	nodes.add(this.nodes.get(0));
    }
    

    public void addMedianNode(List<Node> nodes)
    {
	nodes.add(this.nodes.get(0));
    }
    

    public void addSouthernNodes(List<Node> nodes)
    {
	for (int i = 1; i < this.nodes.size(); ++i)
	    nodes.add(this.nodes.get(i));
    }
    
    
    public List<CorridorPart> getBeamParts()
    {
	return parts;
    }

    
    private CorridorPart.Type defaultType;
    private CorridorPart.ReachableSide defaultSide;
    
    public void addCorridorPart(double width)
    {
	parts.add(new CorridorPart(width, defaultType, defaultSide));
	addNode(new Node(new LatLon(addMetersToLat(
	    nodes.elementAt(nodes.size()-1).getCoor(), width), nodes.elementAt(nodes.size()-1).getCoor().lon())),
	    nodes);
    }

    
    public void setCorridorPartWidth(int partIndex, double value)
    {
	parts.elementAt(partIndex).width = value;
	adjustNodesInBeam();
    }

    
    public void setCorridorPartType(int partIndex, CorridorPart.Type type)
    {
	parts.elementAt(partIndex).type = type;
    }

    
    public void setCorridorPartSide(int partIndex, CorridorPart.ReachableSide side)
    {
	parts.elementAt(partIndex).side = side;
    }
    
    
    public Vector<Double> leftHandSideStrips()
    {
	Vector<Double> offsets = new Vector<Double>();
	double offset = 0;
	for (int i = 0; i < parts.size(); ++i)
	{
	    if (parts.elementAt(i).type == CorridorPart.Type.VOID)
	    {
		if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.WALL
			&& parts.elementAt(i-1).side == CorridorPart.ReachableSide.LEFT)
		    offsets.add(new Double(offset));
		else if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.PASSAGE)
		    offsets.add(new Double(offset));
	    }
	    else if (parts.elementAt(i).type == CorridorPart.Type.WALL)
	    {
		if (parts.elementAt(i).side == CorridorPart.ReachableSide.LEFT
			&& (i == 0 || parts.elementAt(i-1).type == CorridorPart.Type.VOID))
		    offsets.add(new Double(offset));
		else if (parts.elementAt(i).side == CorridorPart.ReachableSide.RIGHT
			&& i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.PASSAGE)
		    offsets.add(new Double(offset));
	    }
	    else /*if (parts.elementAt(i).type == CorridorPart.Type.PASSAGE)*/
	    {
		if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.WALL
			&& parts.elementAt(i).side == CorridorPart.ReachableSide.RIGHT)
		    offsets.add(new Double(offset));
		else if (i == 0 || parts.elementAt(i-1).type == CorridorPart.Type.VOID)
		    offsets.add(new Double(offset));
	    }
	    offset += parts.elementAt(i).width;
	}
	if (parts.size() > 0 && parts.elementAt(parts.size()-1).type == CorridorPart.Type.WALL
		&& parts.elementAt(parts.size()-1).side == CorridorPart.ReachableSide.LEFT)
	    offsets.add(new Double(offset));
	else if (parts.size() > 0 && parts.elementAt(parts.size()-1).type == CorridorPart.Type.PASSAGE)
	    offsets.add(new Double(offset));
	    
	return offsets;
    }
    
    
    public Vector<Double> rightHandSideStrips()
    {
	Vector<Double> offsets = new Vector<Double>();
	double offset = 0;
	for (int i = 0; i < parts.size(); ++i)
	{
	    if (parts.elementAt(i).type == CorridorPart.Type.VOID)
	    {
		if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.WALL
			&& parts.elementAt(i-1).side == CorridorPart.ReachableSide.RIGHT)
		    offsets.add(new Double(offset));
		else if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.PASSAGE)
		    offsets.add(new Double(offset));
	    }
	    else if (parts.elementAt(i).type == CorridorPart.Type.WALL)
	    {
		if (parts.elementAt(i).side == CorridorPart.ReachableSide.RIGHT
			&& (i == 0 || parts.elementAt(i-1).type == CorridorPart.Type.VOID))
		    offsets.add(new Double(offset));
		else if (parts.elementAt(i).side == CorridorPart.ReachableSide.LEFT
			&& i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.PASSAGE)
		    offsets.add(new Double(offset));
	    }
	    else /*if (parts.elementAt(i).type == CorridorPart.Type.PASSAGE)*/
	    {
		if (i > 0 && parts.elementAt(i-1).type == CorridorPart.Type.WALL
			&& parts.elementAt(i-1).side == CorridorPart.ReachableSide.LEFT)
		    offsets.add(new Double(offset));
		else if (i == 0 || parts.elementAt(i-1).type == CorridorPart.Type.VOID)
		    offsets.add(new Double(offset));
	    }
	    offset += parts.elementAt(i).width;
	}
	if (parts.size() > 0 && parts.elementAt(parts.size()-1).type == CorridorPart.Type.WALL
		&& parts.elementAt(parts.size()-1).side == CorridorPart.ReachableSide.RIGHT)
	    offsets.add(new Double(offset));
	else if (parts.size() > 0 && parts.elementAt(parts.size()-1).type == CorridorPart.Type.PASSAGE)
	    offsets.add(new Double(offset));
	    
	return offsets;
    }
    

    private Vector<CorridorPart> parts;
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
