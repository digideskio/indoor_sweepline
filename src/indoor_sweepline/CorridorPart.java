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
    

    public double width;
    private Type type;
    private ReachableSide side;
    
    
    private void adjustSideType(ReachableSide beamSide)
    {
	if (type == Type.WALL && side == ReachableSide.ALL)
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
