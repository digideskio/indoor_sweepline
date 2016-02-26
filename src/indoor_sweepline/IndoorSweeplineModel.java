package indoor_sweepline;

import java.util.List;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;


public class IndoorSweeplineModel
{
    public IndoorSweeplineModel(OsmDataLayer activeLayer, LatLon center)
    {
	dataSet = activeLayer.data;
	this.center = center;
	wayPool = new Vector<Way>();
	nodePool = new Vector<Node>();
	
	beams = new Vector<Beam>();
	strips = new Vector<Strip>();
	addBeam();
	addStrip();
	addBeam();
	
	structureBox = new DefaultComboBoxModel<String>();
    }
    
    
    public void addBeam()
    {
	CorridorPart.ReachableSide side = CorridorPart.ReachableSide.LEFT;
	if (beams.size() == 0)
	    side = CorridorPart.ReachableSide.RIGHT;
	    
	double width = 10.;
	if (beams.size() > 0)
	{
	    width = 0;
	    for (CorridorPart part : beams.elementAt(beams.size() - 1).getBeamParts())
		width += part.width;
	}
    
	double offset = 0;
	for (int i = 0; i < strips.size(); ++i)
	    offset += strips.elementAt(i).width;
	    
	beams.add(new Beam(dataSet, new LatLon(center.lat(), addMetersToLon(center, offset)),
	    width, side));
	
	if (strips.size() > 0)
	    strips.elementAt(beams.size()-2).rhs = beams.elementAt(beams.size()-1).leftHandSideStrips();
	updateOsmModel();
    }
    
    
    public void addStrip()
    {
	strips.add(new Strip(dataSet));
	if (beams.size() > 1)
	{
	    beams.elementAt(beams.size()-1).setDefaultSide(CorridorPart.ReachableSide.ALL);
	    strips.elementAt(strips.size()-2).lhs = beams.elementAt(strips.size()-1).leftHandSideStrips();
	}
	strips.elementAt(strips.size()-1).lhs = beams.elementAt(strips.size()-1).rightHandSideStrips();
	updateOsmModel();
    }

    
    public int leftRightCount()
    {
	return beams.size() + strips.size();
    }
    
    
    public DefaultComboBoxModel<String> structures()
    {
	structureBox.removeAllElements();
	double offset = 0;
	for (int i = 0; i < strips.size(); ++i)
	{
	    if (i < beams.size())
		structureBox.addElement(Double.toString(offset));
	    structureBox.addElement(Double.toString(offset) + " - "
		+ Double.toString(offset + strips.elementAt(i).width));
	    offset += strips.elementAt(i).width;
	}
	if (strips.size() < beams.size())
	    structureBox.addElement(Double.toString(offset));
	
	return structureBox;
    }
    
    
    public void updateOsmModel()
    {
	adjustNodePositions();
	distributeWays();
	Main.map.mapView.repaint();
    }

    
    public Strip getStrip(int index)
    {
	return strips.elementAt(index / 2);
    }
    
    
    public double getStripWidth(int index)
    {
	return strips.elementAt(index / 2).width;
    }
    
    
    public void setStripWidth(int index, double value)
    {
	strips.elementAt(index / 2).width = value;
	updateOsmModel();
    }
    
    
    public List<CorridorPart> getBeamParts(int index)
    {
	return beams.elementAt(index / 2).getBeamParts();
    }

    
    public void addCorridorPart(int beamIndex, boolean append, double value)
    {
	beams.elementAt(beamIndex / 2).addCorridorPart(append, value);
	if (beamIndex / 2 > 0)
	    strips.elementAt(beamIndex / 2 - 1).rhs = beams.elementAt(beamIndex / 2).leftHandSideStrips();
	if (beamIndex / 2 < strips.size())
	    strips.elementAt(beamIndex / 2).lhs = beams.elementAt(beamIndex / 2).rightHandSideStrips();
	updateOsmModel();
    }

    
    public void setCorridorPartWidth(int beamIndex, int partIndex, double value)
    {
	beams.elementAt(beamIndex / 2).setCorridorPartWidth(partIndex, value);
	if (beamIndex / 2 > 0)
	    strips.elementAt(beamIndex / 2 - 1).rhs = beams.elementAt(beamIndex / 2).leftHandSideStrips();
	if (beamIndex / 2 < strips.size())
	    strips.elementAt(beamIndex / 2).lhs = beams.elementAt(beamIndex / 2).rightHandSideStrips();
	updateOsmModel();
    }

    
    public void setCorridorPartType(int beamIndex, int partIndex, CorridorPart.Type type)
    {
	if (beamIndex % 2 == 0)
	{
	    beams.elementAt(beamIndex / 2).setCorridorPartType(partIndex, type);
	    if (beamIndex / 2 > 0)
		strips.elementAt(beamIndex / 2 - 1).rhs = beams.elementAt(beamIndex / 2).leftHandSideStrips();
	    if (beamIndex / 2 < strips.size())
		strips.elementAt(beamIndex / 2).lhs = beams.elementAt(beamIndex / 2).rightHandSideStrips();
	}
	else
	    strips.elementAt(beamIndex / 2).setCorridorPartType(partIndex, type);
	updateOsmModel();
    }

    
    public void setCorridorPartSide(int beamIndex, int partIndex, CorridorPart.ReachableSide side)
    {
	beams.elementAt(beamIndex / 2).setCorridorPartSide(partIndex, side);
	if (beamIndex / 2 > 0)
	    strips.elementAt(beamIndex / 2 - 1).rhs = beams.elementAt(beamIndex / 2).leftHandSideStrips();
	if (beamIndex / 2 < strips.size())
	    strips.elementAt(beamIndex / 2).lhs = beams.elementAt(beamIndex / 2).rightHandSideStrips();
	updateOsmModel();
    }
    
    
    private DataSet dataSet;
    private LatLon center;
    // AbstractDatasetChangedEvent
    private Vector<Way> wayPool;
    private Vector<Node> nodePool;
    
    private Vector<Beam> beams;
    private Vector<Strip> strips;
    
    DefaultComboBoxModel<String> structureBox;

    
    private void addNode(Node node, List<Node> nodes)
    {
	dataSet.addPrimitive(node);
	nodes.add(node);
    }
    
    
    private static LatLon addMeterOffset(LatLon latLon, double south, double east)
    {
	double scale = Math.cos(latLon.lat() * (Math.PI/180.));
	return new LatLon(latLon.lat() - south *(360./4e7), latLon.lon() + east / scale *(360./4e7));
    }
    
    private static double addMetersToLon(LatLon latLon, double east)
    {
	double scale = Math.cos(latLon.lat() * (Math.PI/180.));
	return latLon.lon() + east / scale *(360./4e7);
    }

    
    private void adjustNodePositions()
    {
	double offset = 0;
	for (int i = 0; i < strips.size(); ++i)
	{
	    offset += strips.elementAt(i).width;
	    if (i + 1 < beams.size())
		beams.elementAt(i + 1).adjustNodes(addMetersToLon(center, offset));
	}
    }
    
    
    private void assignCoor(int poolCount, LatLon latLon)
    {
	if (poolCount < nodePool.size())
	    nodePool.elementAt(poolCount).setCoor(latLon);
	else
	{
	    Node node = new Node(latLon);
	    dataSet.addPrimitive(node);
	    nodePool.add(node);
	}
    }
    
    
    private void truncateNodePool(int poolCount)
    {
	for (int i = poolCount; i < nodePool.size(); ++i)
	    nodePool.elementAt(i).setDeleted(true);
	nodePool.setSize(poolCount);
    }
    
    
    private void assignNds(int poolCount, List<Node> nodes)
    {
	if (poolCount < wayPool.size())
	    wayPool.elementAt(poolCount).setNodes(nodes);
	else
	{
	    Way way = new Way();
	    way.setNodes(nodes);
	    dataSet.addPrimitive(way);
	    wayPool.add(way);
	}
    }
    
    
    private void truncateWayPool(int poolCount)
    {
	for (int i = poolCount; i < wayPool.size(); ++i)
	    wayPool.elementAt(i).setDeleted(true);
	wayPool.setSize(poolCount);
    }
    
    
    public class SweepPolygonCursor
    {
	public SweepPolygonCursor(int stripIndex, int partIndex)
	{
	    this.stripIndex = stripIndex;
	    this.partIndex = partIndex;
	}
	
	public boolean equals(SweepPolygonCursor rhs)
	{
	    return rhs != null
		&& stripIndex == rhs.stripIndex && partIndex == rhs.partIndex;
	}
    
	public int stripIndex;
	public int partIndex;
    }
    
    private void distributeWays()
    {
	Vector<Vector<Boolean>> stripRefs = new Vector<Vector<Boolean>>();
	for (Strip strip : strips)
	{
	    Vector<Boolean> refs = new Vector<Boolean>();
	    if (strip.lhs.size() < strip.rhs.size())
		refs.setSize(strip.rhs.size());
	    else
		refs.setSize(strip.lhs.size());
	    stripRefs.add(refs);
	}
	
	int wayPoolCount = 0;
	nodePoolCount = 0;
	Boolean truePtr = new Boolean(true);
	for (int i = 0; i < stripRefs.size(); ++i)
	{
	    Vector<Boolean> refs = stripRefs.elementAt(i);
	    for (int j = 0; j < refs.size(); ++j)
	    {
		if (refs.elementAt(j) == null)
		{
		    SweepPolygonCursor cursor = new SweepPolygonCursor(i, j);
		    Vector<Node> nodes = new Vector<Node>();
		    
		    boolean toTheLeft = true;
		    while (stripRefs.elementAt(cursor.stripIndex).elementAt(cursor.partIndex) == null)
		    {
			stripRefs.elementAt(cursor.stripIndex).setElementAt(truePtr, cursor.partIndex);
			if (toTheLeft && cursor.partIndex < strips.elementAt(cursor.stripIndex).lhs.size())
			    toTheLeft = beams.elementAt(cursor.stripIndex).appendNodes(
				cursor, toTheLeft, nodes, strips);
			else if (!toTheLeft && cursor.partIndex < strips.elementAt(cursor.stripIndex).rhs.size())
			    toTheLeft = beams.elementAt(cursor.stripIndex + 1).appendNodes(
				cursor, toTheLeft, nodes, strips);
			else
			    toTheLeft = appendUturn(cursor, toTheLeft, nodes);
		    }
		    
		    if (nodes.size() > 0)
		    {
			strips.elementAt(cursor.stripIndex).partAt(cursor.partIndex).
			    appendNodes(nodes.elementAt(nodes.size()-1), nodes.elementAt(0), nodes);
			nodes.add(nodes.elementAt(0));
		    }
		    assignNds(wayPoolCount++, nodes);
		}
	    }
	}
	
	truncateWayPool(wayPoolCount);
	truncateNodePool(nodePoolCount);
    }
    
    
    private int nodePoolCount;
    
    private boolean appendUturn(SweepPolygonCursor cursor, boolean toTheLeft, Vector<Node> nodes)
    {
	Strip strip = strips.elementAt(cursor.stripIndex);
	if (strip.rhs.size() < strip.lhs.size())
	    assignCoor(nodePoolCount, addMeterOffset(beams.elementAt(cursor.stripIndex).getFirstCoor(),
		strip.lhs.elementAt(cursor.partIndex / 2 * 2), strip.width / 2.));
	else
	    assignCoor(nodePoolCount, addMeterOffset(beams.elementAt(cursor.stripIndex).getFirstCoor(),
		strip.rhs.elementAt(cursor.partIndex / 2 * 2), strip.width / 2.));
	if (nodes.size() > 0)
	    strip.partAt(cursor.partIndex).
		appendNodes(nodes.elementAt(nodes.size()-1), nodePool.elementAt(nodePoolCount), nodes);
	nodes.add(nodePool.elementAt(nodePoolCount));
	++nodePoolCount;
	
	if (cursor.partIndex % 2 == 0)
	    ++cursor.partIndex;
	else
	    --cursor.partIndex;
	return !toTheLeft;
    }
}
