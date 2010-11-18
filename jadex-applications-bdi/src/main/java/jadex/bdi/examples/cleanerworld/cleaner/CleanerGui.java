package jadex.bdi.examples.cleanerworld.cleaner;

import jadex.application.space.envsupport.environment.ISpaceObject;
import jadex.application.space.envsupport.environment.space2d.Space2D;
import jadex.application.space.envsupport.math.IVector2;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IAgentListener;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IExpression;
import jadex.bdi.runtime.IGoal;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.SGUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


/**
 *  The gui for the cleaner world example.
 *  Shows the world from the viewpoint of a single agent.
 */
public class CleanerGui	extends JFrame
{
	//-------- constructors --------

	/**
	 *  Shows the gui, and updates it when beliefs change.
	 */
	public CleanerGui(final IBDIExternalAccess agent)
	{
		super(agent.getComponentIdentifier().getName());
		final JPanel map = new JPanel()
		{
			protected boolean printed;
			protected DrawData drawdata = new DrawData();
			
			// overridden paint method.
			protected void	paintComponent(Graphics g)
			{
				try
				{
					agent.scheduleStep(new IComponentStep()
					{
						public Object execute(IInternalAccess ia)
						{
							IBDIInternalAccess bia = (IBDIInternalAccess)ia;
							synchronized(drawdata)
							{
								drawdata.daytime = ((Boolean)bia.getBeliefbase().getBelief("daytime").getFact()).booleanValue();
								drawdata.visited_positions = (MapPoint[])bia.getBeliefbase().getBeliefSet("visited_positions").getFacts();
								drawdata.max_quantity = ((MapPoint)((IExpression)bia.getExpressionbase().getExpression("query_max_quantity")).execute()).getQuantity();
								drawdata.xcnt = ((Integer[])bia.getBeliefbase().getBeliefSet("raster").getFacts())[0].intValue();
								drawdata.ycnt = ((Integer[])bia.getBeliefbase().getBeliefSet("raster").getFacts())[1].intValue();
								drawdata.cleaners = (ISpaceObject[])bia.getBeliefbase().getBeliefSet("cleaners").getFacts();
								drawdata.chargingstations = (ISpaceObject[])bia.getBeliefbase().getBeliefSet("chargingstations").getFacts();
								drawdata.wastebins = (ISpaceObject[])bia.getBeliefbase().getBeliefSet("wastebins").getFacts();
								drawdata.wastes = (ISpaceObject[])bia.getBeliefbase().getBeliefSet("wastes").getFacts();
								drawdata.my_vision = ((Double)bia.getBeliefbase().getBelief("my_vision").getFact()).doubleValue();
								drawdata.my_chargestate = ((Double)bia.getBeliefbase().getBelief("my_chargestate").getFact()).doubleValue();
								drawdata.myself = (ISpaceObject)bia.getBeliefbase().getBelief("myself").getFact();
								drawdata.my_waste = drawdata.myself.getProperty("waste")!=null;
								IGoal[] goals = (IGoal[])bia.getGoalbase().getGoals("achievemoveto");
								drawdata.dests = new IVector2[goals.length];
								for(int i=0; i<goals.length; i++)
								{
									drawdata.dests[i] = (IVector2)goals[i].getParameter("location").getValue();
								}
								drawdata.ready = true;
							}
							return null;
						}
					});
					
					synchronized(drawdata)
					{
						if(!drawdata.ready)
							return;
						//System.out.println("++++++++++++++ GUI repaint from: "+Thread.currentThread());
						
						// As paint components is called on swing thread there is no chance to use
						// callbacks. Instead blocking calls are used.
	//					ThreadSuspendable sus = new ThreadSuspendable();
						
						// Get world state from beliefs.
	//					boolean	daytime	= ((Boolean)agent.getBeliefbase().getBeliefFact("daytime").get(sus)).booleanValue();
						
						
						// Paint background (dependent on daytime).
						Rectangle	bounds	= getBounds();
						g.setColor(drawdata.daytime ? Color.lightGray : Color.darkGray);
						g.fillRect(0, 0, bounds.width, bounds.height);
		
						// Paint map points
//						MapPoint[] mps = (MapPoint[])agent.getBeliefbase().getBeliefSetFacts("visited_positions").get(sus);
//						if(query_max_quantity==null)
//							query_max_quantity	= (IEAExpression)agent.getExpressionbase().getExpression("query_max_quantity").get(sus);
//						double max = ((MapPoint)query_max_quantity.execute().get(sus)).getQuantity();
						//int xcnt = ((int[])getBeliefbase().getBelief("???").getFact("raster"))[0];
						//int ycnt = ((int[])getBeliefbase().getBelief("???").getFact("raster"))[1];
//						int xcnt = ((Integer[])agent.getBeliefbase().getBeliefSetFacts("raster").get(sus))[0].intValue();
//						int ycnt = ((Integer[])agent.getBeliefbase().getBeliefSetFacts("raster").get(sus))[1].intValue();
						double cellh = 1/(double)drawdata.ycnt;
						double cellw = 1/(double)drawdata.xcnt;
						for(int i=0; i<drawdata.visited_positions.length; i++)
						{
							Point	p	= onScreenLocation(drawdata.visited_positions[i].getLocation(), bounds);
							int h = 1;
							if(drawdata.max_quantity>0)
								h	= (int)(((double)drawdata.visited_positions[i].getQuantity())*cellh/drawdata.max_quantity*bounds.height);
							int y = (int)(p.y+cellh/2*bounds.height-h);
							g.setColor(new Color(54, 10, 114));
							//System.out.println("h: "+h);
							g.fillRect(p.x+(int)(cellw*0.3*bounds.width), y,
								Math.max(1, (int)(cellw/10*bounds.width)), h);
						}
		
						for(int i=0; i<drawdata.visited_positions.length; i++)
						{
							Point	p	= onScreenLocation(drawdata.visited_positions[i].getLocation(), bounds);
							int	h = (int)(drawdata.visited_positions[i].getSeen()*cellh*bounds.height);
							int y = (int)(p.y+cellw/2*bounds.height-h);
							g.setColor(new Color(10, 150, 150));
							//System.out.println("h: "+h);
							g.fillRect(p.x+(int)(cellw*0.4*bounds.width), y,
								Math.max(1, (int)(cellw/10*bounds.width)), h);
						}
		
						// Paint the cleaners.
//						ISpaceObject[] cleaners = (ISpaceObject[])agent.getBeliefbase().getBeliefSetFacts("cleaners").get(sus);
						for(int i=0; i<drawdata.cleaners.length; i++)
						{
							// Paint agent.
							Point	p	= onScreenLocation((IVector2)drawdata.cleaners[i].getProperty(Space2D.PROPERTY_POSITION), bounds);
							int w	= (int)(((Double)drawdata.cleaners[i].getProperty("vision_range")).doubleValue()*bounds.width);
							int h	= (int)(((Double)drawdata.cleaners[i].getProperty("vision_range")).doubleValue()*bounds.height);
							g.setColor(new Color(100, 100, 100));	// Vision
							g.fillOval(p.x-w, p.y-h, w*2, h*2);
							g.setColor(new Color(50, 50, 50, 180));
							g.fillOval(p.x-3, p.y-3, 7, 7);
							g.drawString(drawdata.cleaners[i].getProperty(ISpaceObject.PROPERTY_OWNER).toString(),
								p.x+5, p.y-5);
							g.drawString("battery: " + (int)(((Double)drawdata.cleaners[i].getProperty("chargestate")).doubleValue()*100.0) + "%",
								p.x+5, p.y+5);
							g.drawString("waste: " + (drawdata.cleaners[i].getProperty("waste")!=null ? "yes" : "no"),
								p.x+5, p.y+15);
						}
		
						// Draw me additionally.
						// Get world state from beliefs.
//						IVector2 agentloc = (IVector2)agent.getBeliefbase().getBeliefFact("my_location").get(sus);
						if(drawdata.my_location!=null)
						{
//							double	vision	= ((Double)agent.getBeliefbase().getBeliefFact("my_vision").get(sus)).doubleValue();
//							double	charge	= ((Double)agent.getBeliefbase().getBeliefFact("my_chargestate").get(sus)).doubleValue();
//							boolean	waste	= ((ISpaceObject)agent.getBeliefbase().getBeliefFact("myself").get(sus)).getProperty("waste")!=null;
			
							// Paint agent.
							Point	p	= onScreenLocation(drawdata.my_location, bounds);
							int w	= (int)(drawdata.my_vision*bounds.width);
							int h	= (int)(drawdata.my_vision*bounds.height);
							g.setColor(new Color(255, 255, 64, 180));	// Vision
							g.fillOval(p.x-w, p.y-h, w*2, h*2);
							g.setColor(Color.black);	// Agent
							g.fillOval(p.x-3, p.y-3, 7, 7);
							g.drawString(agent.getComponentIdentifier().getLocalName(),
								p.x+5, p.y-5);
							g.drawString("battery: " + (int)(drawdata.my_chargestate*100.0) + "%",
								p.x+5, p.y+5);
							g.drawString("waste: " + (drawdata.my_waste ? "yes" : "no"),
								p.x+5, p.y+15);
						}
		
						// Paint charge Stations.
//						ISpaceObject[] stations = (ISpaceObject[])agent.getBeliefbase()
//							.getBeliefSetFacts("chargingstations").get(sus);
						for(int i=0; i<drawdata.chargingstations.length; i++)
						{
							g.setColor(Color.blue);
							Point	p	= onScreenLocation((IVector2)drawdata.chargingstations[i].getProperty(Space2D.PROPERTY_POSITION), bounds);
							g.drawRect(p.x-10, p.y-10, 20, 20);
							g.setColor(drawdata.daytime ? Color.black : Color.white);
							g.drawString(""+drawdata.chargingstations[i].getType(), p.x+14, p.y+5);
						}
		
						// Paint waste bins.
//						ISpaceObject[] wastebins = (ISpaceObject[])agent.getBeliefbase().getBeliefSetFacts("wastebins").get(sus);
						for(int i=0; i<drawdata.wastebins.length; i++)
						{
							g.setColor(Color.red);
							Point	p	= onScreenLocation((IVector2)drawdata.wastebins[i].getProperty(Space2D.PROPERTY_POSITION), bounds);
							g.drawOval(p.x-10, p.y-10, 20, 20);
							g.setColor(drawdata.daytime ? Color.black : Color.white);
	//						g.drawString(wastebins[i].getName()+" ("+wastebins[i].getWastes().length+"/"+wastebins[i].getCapacity()+")", p.x+14, p.y+5);
							g.drawString(""+drawdata.wastebins[i].getType()+" ("+drawdata.wastebins[i].getProperty("wastes")+"/"+drawdata.wastebins[i].getProperty("capacity")+")", p.x+14, p.y+5);
						}
		
						// Paint waste.
//						ISpaceObject[] wastes = (ISpaceObject[])agent.getBeliefbase().getBeliefSetFacts("wastes").get(sus);
						for(int i=0; i<drawdata.wastes.length; i++)
						{
							g.setColor(Color.red);
							IVector2 pos = (IVector2)drawdata.wastes[i].getProperty(Space2D.PROPERTY_POSITION);
							if(pos!=null)
							{
								Point	p = onScreenLocation(pos, bounds);
								g.fillOval(p.x-3, p.y-3, 7, 7);
							}
						}
		
						// Paint movement targets.
//						IEAGoal[] targets = (IEAGoal[])agent.getGoalbase().getGoals("achievemoveto").get(sus);
						for(int i=0; i<drawdata.dests.length; i++)
						{
//							IVector2	dest	= (IVector2)dests[i].getParameterValue("location").get(sus);
							if(drawdata.dests[i]!=null)	// Hack!!! may want to move to null due to asynchronous update of waste position.
							{
								Point	p = onScreenLocation(drawdata.dests[i], bounds);
								g.setColor(Color.black);
								g.drawOval(p.x-5, p.y-5, 10, 10);
								g.drawLine(p.x-7, p.y, p.x+7, p.y);
								g.drawLine(p.x, p.y-7, p.x, p.y+7);
							}
						}
					}
				}
				catch(ComponentTerminatedException e) 
				{
				}
				catch(Exception e)
				{
					if(!printed)
					{
						System.out.println("Paint problem: "+e);
						e.printStackTrace();
					}
					printed = true;
				}
			}
		};

		getContentPane().add(BorderLayout.CENTER, map);
		setSize(300, 300);
		setLocation(SGUI.calculateMiddlePosition(this));
		setVisible(true);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.killComponent();
			}
		});		
		
		agent.scheduleStep(new IComponentStep()
		{
			public Object execute(IInternalAccess ia)
			{
				IBDIInternalAccess bia = (IBDIInternalAccess)ia;
				bia.addAgentListener(new IAgentListener()
				{
					public void agentTerminating(AgentEvent ae)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								dispose();
							}
						});
					}
					
					public void agentTerminated(AgentEvent ae)
					{
					}
				});
				return null;
			}
		});
//		agent.addAgentListener(new IAgentListener()
//		{
//			public void agentTerminating(AgentEvent ae)
//			{
//				SwingUtilities.invokeLater(new Runnable()
//				{
//					public void run()
//					{
//						dispose();
//					}
//				});
//			}
//			
//			public void agentTerminated(AgentEvent ae)
//			{
//			}
//		});
		
		Timer	timer	= new Timer(50, new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				map.invalidate();
				map.repaint();
			}
		});
		timer.start();
	}	
	
	//-------- helper methods --------

	/**
	 *  Get the on screen location for a location in  the world.
	 */
	protected static Point	onScreenLocation(IVector2 loc, Rectangle bounds)
	{
		assert loc!=null;
		assert bounds!=null;
		return new Point((int)(bounds.width*loc.getXAsDouble()),
			(int)(bounds.height*(1.0-loc.getYAsDouble())));
	}
	
	/**
	 *  Data for drawing.
	 */
	public static class DrawData
	{
		public boolean ready;
		public boolean daytime;
		public MapPoint[] visited_positions;
		public double max_quantity;
		public int xcnt;
		public int ycnt;
		public ISpaceObject[] cleaners;
		public double chargestate;
		public IVector2 my_location;
		public double my_vision;
		public double my_chargestate;
		public boolean my_waste;
		public ISpaceObject myself;
		public ISpaceObject[] chargingstations;
		public ISpaceObject[] wastebins;
		public ISpaceObject[] wastes;
		public IVector2[] dests;
	}
}