package indoor_sweepline;


import java.util.Vector;


public class Strip
{
    public Strip()
    {
	width = 10.;
	parts = new Vector<CorridorPart.Type>();
	lhs = new Vector<Double>();
	rhs = new Vector<Double>();
    }

    
    public void setCorridorPartType(int partIndex, CorridorPart.Type type)
    {
	while (parts.size() <= partIndex)
	    parts.add(CorridorPart.Type.WALL);
	parts.setElementAt(type, partIndex);
    }
    
    
    public double width;
    public Vector<CorridorPart.Type> parts;
    public Vector<Double> lhs;
    public Vector<Double> rhs;
}
