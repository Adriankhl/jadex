package jadex.commons.jtable;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * This subclass of <code>JTableHeader</code> extends a <code>JTable</code>
 * with the ability to resize a column to fit all its content on double click
 * on the right end of the column's header cell. <p>
 * Note that you have to set the tableheader's columnmodel
 * to the columnmodel of the table. See example for usage:
 * <pre>
 * JTable table;
 * ResizeableTableHeader tableHeader;
 *
 * // create new table
 * table= new JTable();
 * // create new ResizableTableHeader with the table's ColumnModel
 * tableHeader =  new ResizeableTableHeader(table.getColumnModel());
 * // set the new header for the table
 * table.setTableHeader(tableHeader);
 * </pre>
 * Or for the one line fans
 * <pre>
 * // create new table
 * JTable table= new JTable();
 * table.setTableHeader(new ResizeableTableHeader(table.getColumnModel()));
 * </pre>
 *
 */
public class ResizeableTableHeader extends JTableHeader implements TableModelListener
{
    /**
     * If true, auto resizing of columns when
     * the TableModel invokes a tableChanged event
     * is enabled. The default is false.
     */
    protected boolean autoResizingEnabled;

    /**
     * If true, resizing of columns will
     * also take the width of header cells
     * into account. The default is false.
     */
    protected boolean includeHeaderWidth;

    /**
     *  Constructs a <code>ResizeableTableHeader</code> with a default
     *  <code>TableColumnModel</code>.
     *
     */
    public ResizeableTableHeader()
    {
        this(null);
    }

    /**
     *  Constructs a <code>ResizeableTableHeader</code> which is initialized with
     *  <code>cm</code> as the column model.  If <code>cm</code> is
     *  <code>null</code> this method will initialize the table header
     *  with a default <code>TableColumnModel</code>.
     *
     * @param cm	the column model for the table
     */
    public ResizeableTableHeader(TableColumnModel cm)
    {
        super(cm);
        autoResizingEnabled = false;
        includeHeaderWidth = false;
        addMouseListener(new ResizingMouseAdapter());
    }

    /**
     *  Sets the table associated with this header.
     *  Also adds a TableModelListener to the table's
     *  TableModel to allow resizing of columns if
     *  data of table changed.
     *
     *  @param  table   the new table
     */
    public void setTable(JTable table)
    {
        JTable old = this.table;
        if (table != old)
        {
            if (old != null && old.getModel() != null)
            {
                old.getModel().removeTableModelListener(this);
            }
            if (table != null && table.getModel() != null)
            {
                table.getModel().addTableModelListener(this);
            }
        }
        this.table = table;
        firePropertyChange("table", old, table);
    }

    /**
     *  Sets whether columns are resized on table model events.
     *
     * @param	autoResizingEnabled		true if columns are resized automatically
     * @see	#getAutoResizingEnabled
     */
    public void setAutoResizingEnabled(boolean autoResizingEnabled)
    {
        boolean old = this.autoResizingEnabled;
        this.autoResizingEnabled = autoResizingEnabled;
        firePropertyChange("autoResizingEnabled", old, autoResizingEnabled);
    }

    /**
     * Returns true if auto resizing is enabled.
     *
     * @return	the <code>autoResizingEnabled</code> property
     * @see	#setAutoResizingEnabled
     */
    public boolean getAutoResizingEnabled()
    {
        return autoResizingEnabled;
    }

    /**
     *  Sets whether the header's width are included on calculation
     *
     * @param	includeHeaderWidth		true if the headers are included
     * @see	#getIncludeHeaderWidth
     */
    public void setIncludeHeaderWidth(boolean includeHeaderWidth)
    {
        boolean old = this.includeHeaderWidth;
        this.includeHeaderWidth = includeHeaderWidth;
        firePropertyChange("includeHeaderWidth", old, autoResizingEnabled);
    }

    /**
     * Returns true, if the header's width are
     * @return the <code>setIncludeHeaderWidth</code> property
     * @see	#setIncludeHeaderWidth
     */
    public boolean getIncludeHeaderWidth()
    {
        return includeHeaderWidth;
    }

    /**
     * Resizes the given column to fit all its content.
     *
     * @param column    The <code> TableColumn</code> to resize.
     */
    public void resizeColumn(TableColumn column)
    {
        if (column != null)
        {
            adjustColumnWidth(column);
        }
    }

    /**
     * Resizes all columns to fit all their content.
     */
    public void resizeAllColumns()
    {
        for (int col = 0; col < table.getColumnCount(); col++)
        {
            TableColumn column = table.getColumnModel().getColumn(col);
            adjustColumnWidth(column);
        }
    }


    /**
     * Sets preferred width, the minimum and maximum width for a given column.
     * The minimum and maximum widths are the bounds for the preferred width.
     * If the preferred width is not in the region of minmum and maximum,
     * it will be adjusted.
     * Also the user cannot resize columns out of this bounds by moving the edge
     * or double clicking it.
     * If a width is set to -1, no change is made to this value.
     *
     * @param column    The <code>TableColumn</code> to change.
     * @param preferredWidth    The preferred width of the column.
     * @param minWidth  The minimum width of the column.
     * @param maxWidth  The maximum width of the column.
     *
     * @see #setAllColumnWidths(int preferredWidth, int minWidth, int maxWidth)
     */
    public void setColumnWidths(TableColumn column,int preferredWidth, int minWidth, int maxWidth) {
        if (column!=null) {
            if (preferredWidth!=-1) column.setPreferredWidth(preferredWidth);
            if (minWidth!=-1) column.setMinWidth(minWidth);
            if (maxWidth!=-1) column.setMaxWidth(maxWidth);
        }
    }

    /**
     * Sets preferred width, the minimum and maximum width for all columns.
     * See setColumnWidth() for further details.
     *
     * @param preferredWidth    The preferred width of the column.
     * @param minWidth  The minimum width of the column.
     * @param maxWidth  The maximum width of the column.
     *
     * @see #setColumnWidths(javax.swing.table.TableColumn column,int preferredWidth, int minWidth, int maxWidth)
     */
    public void setAllColumnWidths(int preferredWidth, int minWidth, int maxWidth) {
        for (int col = 0; col < table.getColumnCount(); col++)
        {
            TableColumn column = table.getColumnModel().getColumn(col);
            setColumnWidths(column,preferredWidth,minWidth,maxWidth);
        }
    }


    /**
     * Listen for table model events from tablemodel.
     * If new data is inserted, only determine the width
     * of new cell and adjust column width, if necessary.
     * If data is deleted or updated, call <code>resizeAllColumns()</code>.
     *
     * @param e The <code>TableModelEvent</code>
     */
    public void tableChanged(TableModelEvent e)
    {
    	if(getAutoResizingEnabled())
        {
            if(e.getType() == TableModelEvent.DELETE)
            {
                resizeAllColumns();
            }
            else
            {
                // for performance reason only adjust width, if
                // one of the new cell is greater than the column width.
                for(int col=0; col<table.getColumnCount(); col++)
                {
                    TableColumn column = table.getColumnModel().getColumn(col);
                    if(canResize(column))
                    {
                        int width = column.getPreferredWidth();
                        for(int row = e.getFirstRow(); row<=e.getLastRow() && row<table.getRowCount() && row!=-1; row++)
                        {
                            TableCellRenderer renderer = table.getCellRenderer(row, col);
                           	Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(row, col), false, false, row, col);
                            width = Math.max(width, comp.getPreferredSize().width + table.getColumnModel().getColumnMargin());
                        }
                        column.setPreferredWidth(width);
                    }
                }
            }

        }

    }


    /**
     *  Adjust the width of a column to fit all cells.
     *
     *  @param column	The column to adjust.
     */
    private void adjustColumnWidth(TableColumn column)
    {

        int width = 0;
        int col = table.convertColumnIndexToView(column.getModelIndex());

        // Determine width of header.
        if (includeHeaderWidth)
        {
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer == null)
            {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            Component headerComp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, col);
            width = Math.max(width, headerComp.getPreferredSize().width);
        }

        // Determine max width of cells.
        for (int row = 0; row < table.getRowCount(); row++)
        {
            TableCellRenderer renderer = table.getCellRenderer(row, col);
            Component comp = renderer.getTableCellRendererComponent(table, table.getValueAt(row, col), false, false, row, col);
            width = Math.max(width, comp.getPreferredSize().width);
        }
        // add columnmargins
        width += table.getColumnModel().getColumnMargin();

        column.setPreferredWidth(width);
    }

    /**
     * Returns true, if resizing for the tableheader is allowed and
     * if the column is resizable.
     * @param column    The column to check
     * @return  True, if the column is resizeable, otherwise false.
     */
    private boolean canResize(TableColumn column)
    {
        return (column!=null) && getResizingAllowed() && column.getResizable();
    }

    /**
     * Gets the the column that will be resized for a specific point.
     * This method only return a column, if the point is within
     * the last 3 pixels + 3 pixels of next column. Otherwise it returns null.
     *
     * @param p The point to check, if we are in the resizing area.
     *
     * @return The resizing column, if the point is in the resizing area,
     * otherwise null.
     *
     * @see #getResizingColumn(java.awt.Point p,int column)
     */
    private TableColumn getResizingColumn(Point p)
    {
        return getResizingColumn(p, columnAtPoint(p));
    }

    /**
     * @see #getResizingColumn(java.awt.Point p)
     */
    private TableColumn getResizingColumn(Point p, int column)
    {
        if (column == -1)
        {
            return null;
        }
        Rectangle r = getHeaderRect(column);
        r.grow(-3, 0);
        if (r.contains(p))
        {
            return null;
        }
        int midPoint = r.x + r.width / 2;
        int columnIndex;
        if (getComponentOrientation().isLeftToRight())
        {
            columnIndex = (p.x < midPoint) ? column - 1 : column;
        }
        else
        {
            columnIndex = (p.x < midPoint) ? column : column - 1;
        }
        if (columnIndex == -1)
        {
            return null;
        }
        return getColumnModel().getColumn(columnIndex);
    }

    /**
     * Listener that will resize a column, if a double click
     * in a resizing area is performed.
     */
    private class ResizingMouseAdapter extends MouseAdapter
    {
        public void mouseClicked(MouseEvent me)
        {
            if (me.getClickCount() == 2 && me.getButton() == MouseEvent.BUTTON1)
            {
                TableColumn resizingColumn = getResizingColumn(me.getPoint());
                if (canResize(resizingColumn))
                {
                    adjustColumnWidth(resizingColumn);
                }
            }
        }
    }


}
