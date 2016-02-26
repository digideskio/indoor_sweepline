package indoor_sweepline;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.openstreetmap.josm.Main;


public class IndoorSweeplineWizardDialog extends JDialog
{
    public IndoorSweeplineWizardDialog(IndoorSweeplineController controller)
    {
	super(JOptionPane.getFrameForComponent(Main.parent), "Indoor Sweepline Wizard", false);
	
	this.controller = controller;
	beamIndex = 0;
	prev = new PrevAction();
	next = new NextAction();
	
	GridbagPanel panel = new GridbagPanel();
	
	panel.add(new JLabel(tr("Vertical layer:")), 0, 0, 3, 1);
	panel.add(new JTextField(4), 3, 0, 1, 1);
	
	panel.add(new JButton(prev), 0, 1, 1, 1);
	panel.add(structureBox(), 1, 1, 1, 1);	
	panel.add(new JButton(next), 2, 1, 2, 1);

	panel.add(new JLabel(tr("Strip width:")), 0, 2, 3, 1);
	panel.add(makeWidthField(), 3, 2, 1, 1);

	panel.add(makeStructureTable(), 0, 3, 4, 1);
	
	add(panel);
	pack();
	refresh();
    }
    

    public void setVisible(boolean visible)
    {	
	if (visible)
	    setLocationRelativeTo(JOptionPane.getFrameForComponent(Main.parent));
	super.setVisible(visible);
    }
    
    
    private void refresh()
    {
	inRefresh = true;
	
	leftRightCount = controller.leftRightCount();
	prev.setEnabled(beamIndex > 0);
	
	DefaultComboBoxModel<String> structureBoxModel = controller.structures();
	structureBoxModel.setSelectedItem(structureBoxModel.getElementAt(beamIndex));
	
	try
	{
	    stripWidth.setEditable(beamIndex % 2 == 1);
	    if (beamIndex % 2 == 0)
		stripWidth.setText("---");
	    else
		stripWidth.setText(Double.toString(controller.getStripWidth(beamIndex)));
	}
	catch (IllegalStateException ex)
	{
	}
	
	structureTableModel.setRowCount(0);
	if (beamIndex % 2 == 0)
	{
	    Vector<Object> row = new Vector<Object>();
	    row.addElement("");
	    row.addElement("");
	    row.addElement("");
	    structureTableModel.addRow(row);
	    
	    List<CorridorPart> parts = controller.getBeamParts(beamIndex);
	    for (CorridorPart part : parts)
	    {
		row = new Vector<Object>();
		row.addElement(Double.toString(part.width));
		row.addElement(corridorPartTypeToString(part.getType()));
		row.addElement(corridorPartSideToString(part.getSide()));
		structureTableModel.addRow(row);
	    }
	    row = new Vector<Object>();
	    row.addElement("");
	    row.addElement("");
	    row.addElement("");
	    structureTableModel.addRow(row);
	    structureTableModel.isBeam = true;
	}
	else
	{
	    Strip strip = controller.getStrip(beamIndex);
	    for (int i = 0; i < strip.lhs.size() || i < strip.rhs.size(); ++i)
	    {
		Vector<Object> row = new Vector<Object>();
		String position = i < strip.lhs.size() ? strip.lhs.elementAt(i).toString() : "X";
		position += " - " + (i < strip.rhs.size() ? strip.rhs.elementAt(i).toString() : "X");
		row.addElement(position);
		row.addElement(i < strip.parts.size() ?
		    corridorPartTypeToString(strip.parts.elementAt(i).getType()) : "wall");
		row.addElement(i % 2 == 0 ? "down" : "up");
		structureTableModel.addRow(row);
	    }
	    structureTableModel.isBeam = false;
	}
	
	inRefresh = false;
    }
    
    
    private String corridorPartTypeToString(CorridorPart.Type type)
    {
	if (type == CorridorPart.Type.VOID)
	    return "void";
	else if (type == CorridorPart.Type.PASSAGE)
	    return "passage";
	else if (type == CorridorPart.Type.WALL)
	    return "wall";
	else if (type == CorridorPart.Type.STAIRS)
	    return "stairs";
	else if (type == CorridorPart.Type.ESCALATOR)
	    return "escalator";
	else if (type == CorridorPart.Type.ELEVATOR)
	    return "elevator";
	return "";
    }
    
    
    private CorridorPart.Type parseCorridorPartType(String val)
    {
	if (val == "void")
	    return CorridorPart.Type.VOID;
	else if (val == "passage")
	    return CorridorPart.Type.PASSAGE;
	else if (val == "wall")
	    return CorridorPart.Type.WALL;
	else if (val == "stairs")
	    return CorridorPart.Type.STAIRS;
	else if (val == "escalator")
	    return CorridorPart.Type.ESCALATOR;
	else if (val == "elevator")
	    return CorridorPart.Type.ELEVATOR;
	return CorridorPart.Type.VOID;
    }
    
    
    private String corridorPartSideToString(CorridorPart.ReachableSide side)
    {
	if (side == CorridorPart.ReachableSide.ALL)
	    return "all";
	else if (side == CorridorPart.ReachableSide.FRONT)
	    return "front";
	else if (side == CorridorPart.ReachableSide.BACK)
	    return "back";
	else if (side == CorridorPart.ReachableSide.LEFT)
	    return "left";
	else if (side == CorridorPart.ReachableSide.RIGHT)
	    return "right";
	return "";
    }
    
    
    private CorridorPart.ReachableSide parseCorridorPartSide(String val)
    {
	if (val == "all")
	    return CorridorPart.ReachableSide.ALL;
	else if (val == "front")
	    return CorridorPart.ReachableSide.FRONT;
	else if (val == "back")
	    return CorridorPart.ReachableSide.BACK;
	else if (val == "left")
	    return CorridorPart.ReachableSide.LEFT;
	else if (val == "right")
	    return CorridorPart.ReachableSide.RIGHT;
	return CorridorPart.ReachableSide.ALL;
    }
    
    
    private JComboBox structureBox()
    {
	JComboBox structureBox = new JComboBox(controller.structures());
	structureBox.addActionListener(new StructureBoxListener());
	return structureBox;
    }
    
    
    private IndoorSweeplineController controller;
    
    private int beamIndex;
    private int leftRightCount;
    private PrevAction prev;
    private NextAction next;
    boolean inRefresh;
    
    
    private class PrevAction extends AbstractAction
    {
	public PrevAction()
	{
	    super("Prev");
	}
    
	public void actionPerformed(ActionEvent e)
	{
	    if (inRefresh)
		return;
		
	    if (beamIndex > 0)
		--beamIndex;
	    refresh();
	}
    }
    
    
    private class NextAction extends AbstractAction
    {
	public NextAction()
	{
	    super("Next");
	}
    
	public void actionPerformed(ActionEvent e)
	{
	    if (inRefresh)
		return;
		
	    ++beamIndex;
	    if (beamIndex >= leftRightCount)
		controller.addRightStructure();
	    refresh();
	}
    }
    
    
    private class StructureBoxListener implements ActionListener
    {
	public void actionPerformed(ActionEvent e)
	{
	    if (inRefresh)
		return;
		
	    String entry = (String)((JComboBox)e.getSource()).getSelectedItem();
	    DefaultComboBoxModel<String> structureBoxModel = controller.structures();
	    for (int i = 0; i < structureBoxModel.getSize(); ++i)
	    {
		if (structureBoxModel.getElementAt(i).equals(entry))
		    beamIndex = i;
	    }
	    refresh();
	}
    }
    
    
    private JTextField stripWidth;
    
    private JTextField makeWidthField()
    {
	stripWidth = new JTextField(5);
	stripWidth.getDocument().addDocumentListener(new StripWidthListener());
	return stripWidth;
    }
    
    
    private class StripWidthListener implements DocumentListener
    {
	public void changedUpdate(DocumentEvent e)
	{
	    update(e);
	}
	
	public void insertUpdate(DocumentEvent e)
	{
	    update(e);
	}
	
	public void removeUpdate(DocumentEvent e)
	{
	    update(e);
	}
	
	
	private void update(DocumentEvent e)
	{
	    if (inRefresh)
		return;
	    
	    try
	    {
		if (beamIndex % 2 == 1)
		    controller.setStripWidth(beamIndex, Double.parseDouble(stripWidth.getText()));
	    }
	    catch (NumberFormatException ex)
	    {
	    }
	    
	    refresh();
	}
    }

    
    private class StructureTableModel extends DefaultTableModel
    {
	@Override
	public boolean isCellEditable(int row, int column)
	{
	    return isBeam || column == 1;
	}
	
	public boolean isBeam;
    }
    
    private StructureTableModel structureTableModel;
    
    private JScrollPane makeStructureTable()
    {
	structureTableModel = new StructureTableModel();	
	structureTableModel.addColumn("Width");
	structureTableModel.addColumn("Type");
	structureTableModel.addColumn("Reachable Side");
	structureTableModel.addTableModelListener(new StructureTableListener());
	
	JTable table = new JTable(structureTableModel);
	
	TableColumn column = table.getColumnModel().getColumn(1);
	JComboBox comboBox = new JComboBox();
	comboBox.addItem("void");
	comboBox.addItem("passage");
	comboBox.addItem("wall");
	comboBox.addItem("stairs");
	comboBox.addItem("escalator");
	comboBox.addItem("elevator");
	column.setCellEditor(new DefaultCellEditor(comboBox));

	column = table.getColumnModel().getColumn(2);
	comboBox = new JComboBox();
	comboBox.addItem("all");
	comboBox.addItem("left");
	comboBox.addItem("right");
	column.setCellEditor(new DefaultCellEditor(comboBox));

	return new JScrollPane(table);
    }
    
    private class StructureTableListener implements TableModelListener
    {
	public void tableChanged(TableModelEvent e)
	{
	    if (inRefresh)
		return;
	    
	    int column = e.getColumn();
	    int row = e.getFirstRow();
	    if (column == 0 && beamIndex % 2 == 0)
	    {
		try
		{
		    if (row == 0 || row == structureTableModel.getRowCount() - 1)
			controller.addCorridorPart(beamIndex, row != 0,
			    Double.parseDouble(((TableModel)e.getSource()).getValueAt(row, column).toString()));
		    else
			controller.setCorridorPartWidth(beamIndex, row - 1,
			    Double.parseDouble(((TableModel)e.getSource()).getValueAt(row, column).toString()));
		}
		catch (NumberFormatException ex)
		{
		}
	    }
	    else if (column == 1 && beamIndex % 2 == 0)
	    {
		if (row > 0 && row < structureTableModel.getRowCount() - 1)
		    controller.setCorridorPartType(beamIndex, row - 1,
			parseCorridorPartType(((TableModel)e.getSource()).getValueAt(row, column).toString()));
	    }
	    else if (column == 1 && beamIndex % 2 == 1)
	    {
		controller.setCorridorPartType(beamIndex, row,
		    parseCorridorPartType(((TableModel)e.getSource()).getValueAt(row, column).toString()));
	    }
	    else if (column == 2 && beamIndex % 2 == 0)
	    {
		if (row > 0 && row < structureTableModel.getRowCount() - 1)
		    controller.setCorridorPartSide(beamIndex, row - 1,
			parseCorridorPartSide(((TableModel)e.getSource()).getValueAt(row, column).toString()));
	    }
	    
	    refresh();
	}
    }
    
    
    private class GridbagPanel extends JPanel
    {
	public GridbagPanel()
	{
	    gridbag = new GridBagLayout();
	    layoutCons = new GridBagConstraints();
	    setLayout(gridbag);
	}
	
	public void add(Component comp, int gridx, int gridy, int gridwidth, int gridheight)
	{
	    layoutCons.gridx = gridx;
	    layoutCons.gridy = gridy;
	    layoutCons.gridwidth = gridwidth;
	    layoutCons.gridheight = gridheight;
	    layoutCons.weightx = 0.0;
	    layoutCons.weighty = 0.0;
	    layoutCons.fill = GridBagConstraints.BOTH;
	
	    gridbag.setConstraints(comp, layoutCons);
	    add(comp);
	}
	
	private GridBagLayout gridbag;
	private GridBagConstraints layoutCons;
    }
}
