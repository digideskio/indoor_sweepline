package indoor_sweepline;

import java.util.List;
import java.util.Vector;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;


public class Beam
{
    public Beam(DataSet dataSet, LatLon center)
    {
	parts = new Vector<CorridorPart>();
	nodes = new Vector<Node>();
	this.dataSet = dataSet;
	
	addNode(new Node(center), nodes);
	addCorridorPart(10.);

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

    
    public void addCorridorPart(double width)
    {
	parts.add(new CorridorPart(width, CorridorPart.Type.PASSAGE, CorridorPart.ReachableSide.ALL));
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
