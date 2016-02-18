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
	outerWay = new Way();
	activeLayer.data.addPrimitive(outerWay);
	
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
	strips.add(new Strip());
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
	double offset = 0;
	for (int i = 0; i < strips.size(); ++i)
	{
	    offset += strips.elementAt(i).width;
	    if (i + 1 < beams.size())
		beams.elementAt(i + 1).adjustNodes(addMetersToLon(center, offset));
	}
    
	List<Node> nodes = new Vector<Node>();
	
	beams.get(0).addMedianNode(nodes);
	for (int i = 0; i < beams.size(); ++i)
	    beams.get(i).addSouthernNodes(nodes);
	for (int i = beams.size()-1; i >= 0; --i)
	    beams.get(i).addNorthernNodes(nodes);
	    
	outerWay.setNodes(nodes);
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

    
    public void addCorridorPart(int beamIndex, double value)
    {
	beams.elementAt(beamIndex / 2).addCorridorPart(value);
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
    private Way outerWay;
    
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
}
