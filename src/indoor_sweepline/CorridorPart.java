package indoor_sweepline;

import java.util.List;
import java.util.Vector;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;


public class CorridorPart
{
    public enum Type
    {
	VOID,
	PASSAGE,
	WALL
    }
    

    public enum ReachableSide
    {
	ALL,
	FRONT,
	BACK,
	LEFT,
	RIGHT
    }
    

    public CorridorPart(double width, Type type, ReachableSide side)
    {
	this.width = width;
	this.type = type;
	this.side = side;
    }
    

    public double width;
    public Type type;
    public ReachableSide side;
}
