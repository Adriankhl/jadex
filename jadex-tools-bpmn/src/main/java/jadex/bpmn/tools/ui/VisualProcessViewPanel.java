package jadex.bpmn.tools.ui;

import jadex.bpmn.editor.BpmnEditor;
import jadex.bpmn.editor.gui.BpmnEditorPanel;
import jadex.bpmn.editor.gui.BpmnGraph;
import jadex.bpmn.editor.gui.BpmnGraphComponent;
import jadex.bpmn.editor.gui.GuiConstants;
import jadex.bpmn.editor.gui.ModelContainer;
import jadex.bpmn.editor.gui.ZoomSlider;
import jadex.bpmn.editor.gui.controllers.SelectionController;
import jadex.bpmn.editor.model.visual.BpmnVisualModelReader;
import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.io.SBpmnModelReader;
import jadex.bpmn.runtime.BpmnInterpreter;
import jadex.bpmn.tools.ProcessThreadInfo;
import jadex.bridge.BulkMonitoringEvent;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.commons.IBreakpointPanel;
import jadex.commons.IFilter;
import jadex.commons.future.FutureTerminatedException;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.gui.JSplitPanel;
import jadex.commons.gui.future.SwingIntermediateResultListener;
import jadex.commons.gui.jtable.ResizeableTableHeader;
import jadex.commons.gui.jtable.TableSorter;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import com.mxgraph.util.mxEvent;

/**
 * 
 */
public class VisualProcessViewPanel extends JPanel
{
	//------- attributes --------
	
	/** The process. */
	protected IExternalAccess access;
	
	/** The displayed process threads. */
	protected Set<ProcessThreadInfo> threadinfos;
	
	/** The previous process thread steps. */
	protected List<ProcessThreadInfo> historyinfos;
		
	/** The list model for the activations. */
	protected ProcessThreadModel ptmodel;

	/** The list model for the history. */
	protected HistoryModel	hmodel;
	
	/** The list for the activations. */
	protected JTable threads;
	
	/** The list for the history. */
	protected JTable history;
	
	/** The breakpoint panel. */
	protected IBreakpointPanel	bpp;

	/** The change listener. */
	protected ISubscriptionIntermediateFuture<IMonitoringEvent> sub;
	
	//------- constructors --------
	
	/**
	 *  Create an agenda panel.
	 */
	public VisualProcessViewPanel(final IExternalAccess access, IBreakpointPanel bpp) 
	{
		try
		{
			this.access = access;
			this.bpp	= bpp;
			this.threadinfos	= new LinkedHashSet<ProcessThreadInfo>();
			this.historyinfos	= new ArrayList<ProcessThreadInfo>();
			this.ptmodel = new ProcessThreadModel();
			this.hmodel	= new HistoryModel();
			
			String filename = access.getModel().getFilename();
			final ModelContainer modelcontainer = new ModelContainer(null);
			BpmnGraph graph = new BpmnGraph(modelcontainer, BpmnEditor.STYLE_SHEETS[0].getSecondEntity());
			BpmnVisualModelReader vreader = new BpmnVisualModelReader(graph);
			graph.deactivate();
			graph.setEventsEnabled(false);
			graph.getModel().beginUpdate();
			MBpmnModel mmodel = SBpmnModelReader.readModel(new FileInputStream(filename), filename, vreader);
			graph.getModel().endUpdate();
			graph.setEventsEnabled(true);
			graph.activate();
			modelcontainer.setBpmnModel(mmodel);
			modelcontainer.setGraph(graph);
			modelcontainer.setFile(new File(filename));
			BpmnGraphComponent bpmncomp = new BpmnGraphComponent(graph);
			JPanel bpmnpan = new JPanel(new BorderLayout());
			bpmnpan.add(bpmncomp, BorderLayout.CENTER);
			
			ZoomSlider zs = new ZoomSlider(bpmncomp, modelcontainer);
//			zs.setMinimumSize(zs.getPreferredSize());
//			zs.setPreferredSize(zs.getPreferredSize());
			zs.setMaximumSize(zs.getPreferredSize());
			JPanel tp = new JPanel();
//			tp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			BoxLayout bl = new BoxLayout(tp, BoxLayout.LINE_AXIS);
			tp.setLayout(bl);
			tp.add(new JPanel(new GridLayout(1,1))); // necessary to show zoomslider correctly
			tp.add(zs);
			bpmnpan.add(tp, BorderLayout.SOUTH);
			graph.getView().setScale(GuiConstants.DEFAULT_ZOOM);
			
			bpmncomp.init(modelcontainer);
//			modelcontainer.getGraph().getSelectionModel().addListener(mxEvent.CHANGE, new SelectionController(modelcontainer));
			modelcontainer.getGraphComponent().refresh();
			
			TableSorter sorter = new TableSorter(ptmodel);
			sorter.setSortingStatus(0, TableSorter.ASCENDING);
			this.threads = new JTable(sorter);
			ResizeableTableHeader header = new ResizeableTableHeader(threads.getColumnModel());
			header.setIncludeHeaderWidth(true);
	//		threads.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			threads.setTableHeader(header);
			threads.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			sorter.setTableHeader(header);
			threads.getColumnModel().setColumnMargin(10);
	
			sorter = new TableSorter(hmodel);
			this.history = new JTable(sorter);
			header = new ResizeableTableHeader(history.getColumnModel());
			header.setIncludeHeaderWidth(true);
	//		history.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			history.setTableHeader(header);
			history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			sorter.setTableHeader(header);
			history.getColumnModel().setColumnMargin(10);
			
			final JCheckBox hon = new JCheckBox("Store History");
			hon.setSelected(true);
				
			// todo: initial/current state
			
			sub = access.subscribeToEvents(new IFilter<IMonitoringEvent>()
			{
				public boolean filter(IMonitoringEvent ev)
				{
					return true;	
				}
			}, true, PublishEventLevel.FINE);
			sub.addResultListener(new SwingIntermediateResultListener<IMonitoringEvent>(new IntermediateDefaultResultListener<IMonitoringEvent>()
			{
				public void intermediateResultAvailable(IMonitoringEvent event)
				{
					if(event==null)
						return;
					
					// todo: hide decomposing bulk events
					if(event instanceof BulkMonitoringEvent)
					{
						BulkMonitoringEvent bev = (BulkMonitoringEvent)event;
						if(bev.getBulkEvents().length>0)
						{
							IMonitoringEvent[] events = bev.getBulkEvents();
							for(int i=0; i<events.length; i++)
							{
								intermediateResultAvailable(events[i]);
							}
						}
					}
					else if(event.getType().endsWith(BpmnInterpreter.TYPE_THREAD))
					{
						if(event.getType().startsWith(IMonitoringEvent.EVENT_TYPE_CREATION))
						{
							threadinfos.add((ProcessThreadInfo)event.getProperty("details"));
						}
						else if(event.getType().startsWith(IMonitoringEvent.EVENT_TYPE_DISPOSAL))
						{
							threadinfos.remove((ProcessThreadInfo)event.getProperty("details"));
						}
						else if(event.getType().startsWith(IMonitoringEvent.EVENT_TYPE_MODIFICATION))
						{
							threadinfos.remove((ProcessThreadInfo)event.getProperty("details"));
							threadinfos.add((ProcessThreadInfo)event.getProperty("details"));
						}
					}
					else if(event.getType().endsWith(BpmnInterpreter.TYPE_ACTIVITY))
					{
						if(event.getType().startsWith(IMonitoringEvent.EVENT_TYPE_DISPOSAL))
						{
							historyinfos.add(0, (ProcessThreadInfo)event.getProperty("details"));
						}
					}
	//				System.out.println("ti: "+threadinfos.size()+" "+cce.getSourceName()+" "+cce.getSourceType()+" "+cce.getEventType());
					updateViews();
				}
				
				public void exceptionOccurred(Exception exception)
				{
					// Todo: why future terminated exception thrown?
					if(!(exception instanceof FutureTerminatedException))
					{
						super.exceptionOccurred(exception);
					}
				}
			}));
							
			JButton clear = new JButton("Clear");
			clear.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					historyinfos.clear();
					history.repaint();
				}
			});
			
			JPanel	procp	= new JPanel(new BorderLayout());
			procp.add(new JScrollPane(threads));
			procp.setBorder(BorderFactory.createTitledBorder("Processes"));
			
			JPanel	historyp	= new JPanel(new BorderLayout());
			historyp.add(new JScrollPane(history));
			historyp.setBorder(BorderFactory.createTitledBorder("History"));
			
			JPanel buts = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buts.add(hon);
			buts.add(clear);
			historyp.add(buts, BorderLayout.SOUTH);
	
			JSplitPanel tmp2 = new JSplitPanel(JSplitPane.VERTICAL_SPLIT);
			tmp2.add(bpmnpan);
			tmp2.add(procp);
			tmp2.setDividerLocation(0.7);
			
//			JSplitPane tmp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//			tmp.add(procp);
//			tmp.add(tmp2);
//			tmp.setDividerLocation(200); // Hack?!
			
			setLayout(new BorderLayout());
			add(tmp2, BorderLayout.CENTER);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 */
	protected void loadModel(String file)
	{		
		
	}
	
	//-------- methods --------
	
	/**
	 *  Dispose the panel and remove any listeners.
	 */
	public void	dispose()
	{
		sub.terminate();
	}
	
	//-------- helper methods --------
	
	/**
	 *  Update views.
	 */
	protected void	updateViews()
	{
		ptmodel.fireTableDataChanged();
		hmodel.fireTableDataChanged();
//		if(ptmodel.getRowCount()>0)
//			((ResizeableTableHeader)threads.getTableHeader()).resizeAllColumns();
//		if(hmodel.getRowCount()>0)
//			((ResizeableTableHeader)history.getTableHeader()).resizeAllColumns();
		threads.repaint();
		history.repaint();
		
		if(bpp!=null)
		{
			List	sel_bps	= new ArrayList();
			for(Iterator it=threadinfos.iterator(); it.hasNext(); )
			{
				ProcessThreadInfo	info	= (ProcessThreadInfo)it.next();
				if(info.getActivity()!=null)
					sel_bps.add(info.getActivity());
			}
			bpp.setSelectedBreakpoints((String[])sel_bps.toArray(new String[sel_bps.size()]));
		}
	}
	
	//-------- helper classes --------
	
	/**
	 *  List model for activations.
	 */
	protected class ProcessThreadModel extends AbstractTableModel
	{
		protected String[] colnames = new String[]{"Process-Id", "Activity", "Pool", "Lane", "Exception", "Data", "Status"};
		
		public String getColumnName(int column)
		{
			return colnames[column];
		}

		public int getColumnCount()
		{
			return colnames.length;
		}
		
		public int getRowCount()
		{
			return threadinfos.size();
		}
		
		public Object getValueAt(int row, int column)
		{
			Object ret = null;
			ProcessThreadInfo	info	= (ProcessThreadInfo)threadinfos.toArray()[row];
			
			if(column==0)
			{
				ret = info.getThreadId();
			}
			else if(column==1)
			{
				ret = info.getActivity();
			}
			else if(column==2)
			{
				ret = info.getPool(); 
			}
			else if(column==3)
			{
				ret = info.getLane(); 
			}
			else if(column==4)
			{
				ret = info.getException();
			}
			else if(column==5)
			{
				ret = info.getData();
			}
			else if(column==6)
			{
				ret = info.isWaiting() ? "waiting" : "ready";
			}
			return ret;
		}
	}
	
	/**
	 *  List model for history.
	 */
	protected class HistoryModel extends AbstractTableModel
	{
		protected String[] colnames = new String[]{"Process-Id", "Activity", "Pool", "Lane"};
		
		public String getColumnName(int column)
		{
			return colnames[column];
		}

		public int getColumnCount()
		{
			return colnames.length;
		}
		
		public int getRowCount()
		{
			return historyinfos.size();
		}
		
		public Object getValueAt(int row, int column)
		{
			Object ret = null;
			ProcessThreadInfo	info	= (ProcessThreadInfo)historyinfos.get(row);
			if(column==0)
			{
				ret = info.getThreadId();
			}
			else if(column==1)
			{
				ret = info.getActivity();
			}
			else if(column==2)
			{
				ret = info.getPool(); 
			}
			else if(column==3)
			{
				ret = info.getLane(); 
			}

			return ret;
		}
	}
	
}

