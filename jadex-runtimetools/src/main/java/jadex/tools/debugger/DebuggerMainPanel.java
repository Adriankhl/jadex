package jadex.tools.debugger;

import jadex.base.SComponentFactory;
import jadex.base.gui.SwingDefaultResultListener;
import jadex.base.gui.plugin.AbstractJCCPlugin;
import jadex.base.gui.plugin.IControlCenter;
import jadex.bridge.ICMSComponentListener;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.SServiceProvider;
import jadex.commons.SReflect;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.tools.debugger.common.ObjectInspectorDebuggerPanel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 *  Show details of a debugged agent.
 */
public class DebuggerMainPanel extends JSplitPane
{
	//-------- constants --------
	
	/** The factory properties key for debugger panels
	 * (value is a comma separated list of fully
	 * qualified class names implementing IDebuggerPanel). */
	public static String	KEY_DEBUGGER_PANELS	= "debugger.panels";

	/** The model properties key for breakpoints (should contain a java.util.Collection object). */
	public static String	KEY_DEBUGGER_BREAKPOINTS	= "debugger.breakpoints";

	//-------- attributes --------
	
	/** The control center. */
	protected IControlCenter	jcc;
	
	/** The component description. */
	protected IComponentDescription	desc;
	
	/** The step button. */
	protected JButton	step;

	/** The step button. */
	protected JButton	run;

	/** The pause buuton. */
	protected JButton	pause;
	
	/** The tabs. */
	protected List debuggerpanels;
	
	/** The cms listener. */
	protected ICMSComponentListener listener;
	
	//-------- constructors --------
	
	/**
	 *  Create a new debugger panel.
	 *  @param container	The service container.
	 *  @param comp	The identifier of the component to be debugged.
	 */
	public DebuggerMainPanel(final IControlCenter jcc, final IComponentDescription desc)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, new JPanel(), new JPanel());
		this.jcc	= jcc;
		this.desc	= desc;
		this.debuggerpanels = new ArrayList();
		this.setOneTouchExpandable(true);
		setDividerLocation(0.3);
				
		SServiceProvider.getService(jcc.getPlatformAccess().getServiceProvider(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
		{
			public void customResultAvailable(Object result)
			{
				IComponentManagementService	cms	= (IComponentManagementService)result;
				// The right panel (step & custom tabs)
				JPanel	rightpanel	= new JPanel();
				setRightComponent(rightpanel);
				rightpanel.setLayout(new GridBagLayout());
				
				final JTabbedPane tabs = new JTabbedPane();	
				
				cms.getExternalAccess(desc.getName())
					.addResultListener(new IResultListener()
				{			
					public void resultAvailable(final Object result)
					{
						// The left panel (breakpoints)
						final IExternalAccess exta = (IExternalAccess)result;
						final BreakpointPanel[] leftpanel = new BreakpointPanel[1];
						final Map props = exta.getModel().getProperties();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								if(props!=null && props.containsKey(KEY_DEBUGGER_BREAKPOINTS))
								{
									Collection	breakpoints	= (Collection)exta.getModel().getProperty(KEY_DEBUGGER_BREAKPOINTS);
									leftpanel[0] = new BreakpointPanel(breakpoints, desc, jcc.getPlatformAccess());
									DebuggerMainPanel.this.setLeftComponent(leftpanel[0]);
									DebuggerMainPanel.this.setDividerLocation(150);	// Hack???
								}
								else
								{
									JPanel nobreakpoints = new JPanel();
									nobreakpoints.add(new JLabel("no breakpoints"));
									DebuggerMainPanel.this.setLeftComponent(nobreakpoints);
									DebuggerMainPanel.this.setDividerLocation(0);
								}
								
								// Sub panels of right panel.
								SComponentFactory.getProperty(DebuggerMainPanel.this.jcc.getPlatformAccess(), DebuggerMainPanel.this.desc.getType(), KEY_DEBUGGER_PANELS)
									.addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
								{
									public void customResultAvailable(Object result)
									{
										final String	panels	= (String)result;
										if(panels!=null)
										{
											AbstractJCCPlugin.getClassLoader(desc.getName(), jcc).addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
											{
												public void customResultAvailable(Object result)
												{
													final ClassLoader	cl	= (ClassLoader)result;
													StringTokenizer	stok	= new StringTokenizer(panels, ", \t\n\r\f");
													while(stok.hasMoreTokens())
													{
														String classname	= stok.nextToken();
														try
														{
															Class clazz	= SReflect.classForName(classname, cl);
															IDebuggerPanel	panel	= (IDebuggerPanel)clazz.newInstance();
															panel.init(DebuggerMainPanel.this.jcc, leftpanel[0], DebuggerMainPanel.this.desc.getName(), exta);
															debuggerpanels.add(panel);
															tabs.addTab(panel.getTitle(), panel.getIcon(), panel.getComponent(), panel.getTooltipText());
														}
														catch(Exception e)
														{
															e.printStackTrace();
															DebuggerMainPanel.this.jcc.displayError("Error initializing debugger panel.", "Debugger panel class: "+classname, e);
														}
													}
												}
											});
										}
										else
										{
											ObjectInspectorDebuggerPanel panel = new ObjectInspectorDebuggerPanel();
											panel.init(DebuggerMainPanel.this.jcc, leftpanel[0], DebuggerMainPanel.this.desc.getName(), exta);
											debuggerpanels.add(panel);
											tabs.addTab(panel.getTitle(), panel.getIcon(), panel.getComponent(), panel.getTooltipText());
										}
									}
								});
							}
						});
					}
					public void exceptionOccurred(Exception exception)
					{
						DebuggerMainPanel.this.jcc.displayError("Error initializing debugger panels.", null, exception);
					}
				});
				
				pause = new JButton("Pause");
				pause.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						pause.setEnabled(false);
						SServiceProvider.getServiceUpwards(DebuggerMainPanel.this.jcc.getPlatformAccess().getServiceProvider(), IComponentManagementService.class)
							.addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
						{
							public void customResultAvailable(Object result)
							{
								IComponentManagementService	ces	= (IComponentManagementService)result;
								ces.suspendComponent(DebuggerMainPanel.this.desc.getName());
							}
						});
					}
				});
				
				step = new JButton("Step");
				step.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						step.setEnabled(false);
						run.setEnabled(false);
						SServiceProvider.getServiceUpwards(DebuggerMainPanel.this.jcc.getPlatformAccess().getServiceProvider(), IComponentManagementService.class)
							.addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
						{
							public void customResultAvailable(Object result)
							{
								IComponentManagementService	ces	= (IComponentManagementService)result;
								IFuture ret = ces.stepComponent(DebuggerMainPanel.this.desc.getName());
								ret.addResultListener(new IResultListener()
								{
									public void resultAvailable(Object result)
									{
										updatePanel((IComponentDescription)result);
									}
									
									public void exceptionOccurred(Exception exception)
									{
										// Hack!!! keep tool reactive in case of error!?
										step.setEnabled(true);
										run.setEnabled(true);
									}
								});
							}
						});
					}
				});
				
				run = new JButton("Run");
				run.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						step.setEnabled(false);
						run.setEnabled(false);
						pause.setEnabled(true);
						SServiceProvider.getServiceUpwards(DebuggerMainPanel.this.jcc.getPlatformAccess().getServiceProvider(), IComponentManagementService.class)
							.addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
						{
							public void customResultAvailable(Object result)
							{
								final IComponentManagementService	ces	= (IComponentManagementService)result;
								IFuture ret = ces.stepComponent(DebuggerMainPanel.this.desc.getName());
								ret.addResultListener(new IResultListener()
								{
									public void resultAvailable(Object result)
									{
										IFuture ret = ces.resumeComponent(DebuggerMainPanel.this.desc.getName()); 
										ret.addResultListener(new IResultListener()
										{
											public void resultAvailable(Object result)
											{
												updatePanel((IComponentDescription)result);
											}
											
											public void exceptionOccurred(Exception exception)
											{
												step.setEnabled(true);
												run.setEnabled(true);
												pause.setEnabled(false);
											}
										});
									}
									
									public void exceptionOccurred(Exception exception)
									{
										step.setEnabled(true);
										run.setEnabled(true);
										pause.setEnabled(false);
									}
								});
							}
						});
					}
				});
				
				Dimension msize = pause.getMinimumSize();
				Dimension psize = pause.getPreferredSize();
				run.setMinimumSize(msize);
				run.setPreferredSize(psize);
				step.setMinimumSize(msize);
				step.setPreferredSize(psize);
				
//				stepmode = new JCheckBox("Step Mode");
//				stepmode.addActionListener(new ActionListener()
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						SServiceProvider.getServiceUpwards(DebuggerMainPanel.this.jcc.getPlatformAccess().getServiceProvider(), IComponentManagementService.class)
//							.addResultListener(new SwingDefaultResultListener(DebuggerMainPanel.this)
//						{
//							public void customResultAvailable(Object result)
//							{
//								IComponentManagementService	ces	= (IComponentManagementService)result;
//								if(stepmode.isSelected())
//								{
//									ces.suspendComponent(DebuggerMainPanel.this.desc.getName());
//								}
//								else
//								{
//									ces.resumeComponent(DebuggerMainPanel.this.desc.getName());
//								}
//							}
//						});
//					}
//				});
						
				int row	= 0;
				int	col	= 0;
				rightpanel.add(tabs, new GridBagConstraints(col++, row, GridBagConstraints.REMAINDER, 1,
					1,1, GridBagConstraints.LINE_END, GridBagConstraints.BOTH, new Insets(1,1,1,1), 0,0));
				row++;
				col	= 0;
				rightpanel.add(pause, new GridBagConstraints(col++, row, 1, 1,
					1,0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(1,1,1,1), 0,0));
				rightpanel.add(step, new GridBagConstraints(col++, row, 1, 1,
					0,0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(1,1,1,1), 0,0));
				rightpanel.add(run, new GridBagConstraints(col, row++, GridBagConstraints.REMAINDER, 1,
					0,0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(1,1,1,1), 0,0));

				
				updatePanel((IComponentDescription)desc);

				listener = new ICMSComponentListener()
				{			
					public IFuture componentChanged(IComponentDescription desc)
					{
//						System.out.println("changed: "+desc.getName());
						if(desc.getName().equals(DebuggerMainPanel.this.desc.getName()))
							updatePanel(desc);
						return IFuture.DONE;
					}
					public IFuture componentRemoved(IComponentDescription desc, Map results)
					{
						return IFuture.DONE;
					}			
					public IFuture componentAdded(IComponentDescription desc)
					{
						return IFuture.DONE;
					}
				};		
//				jcc.getPlatformAccess().getComponentIdentifier()
				jcc.getCMSHandler().addCMSListener(desc.getName().getRoot(), listener);
			}
		});
	}

	/**
	 *  Dispose the panel, i.e. remove any associated resources (listeners etc.).
	 */
	public void dispose()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				for(int i=0; i<debuggerpanels.size(); i++)
				{
					IDebuggerPanel panel = (IDebuggerPanel)debuggerpanels.get(i);
					panel.dispose();
				}
				if(listener!=null)
				{
					jcc.getCMSHandler().removeCMSListener(desc.getName().getRoot(), listener);
				}
			}
		});
	}

	protected void updatePanel(final IComponentDescription desc)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
//				System.out.println("update: "+desc);
				pause.setEnabled(!IComponentDescription.STATE_SUSPENDED.equals(desc.getState()));
				step.setEnabled(IComponentDescription.STATE_SUSPENDED.equals(desc.getState()));
//					&& IComponentDescription.PROCESSINGSTATE_READY.equals(desc.getProcessingState()));
				run.setEnabled(IComponentDescription.STATE_SUSPENDED.equals(desc.getState()));
			}
		});
	}
}
