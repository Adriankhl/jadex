package jadex.micro.examples.chat;

import jadex.base.gui.SwingDefaultResultListener;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.gui.JSplitPanel;
import jadex.commons.gui.SGUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *  Panel for displaying the chat.
 */
public class ChatPanel extends JPanel
{
	//-------- constants --------
	
	/** The linefeed separator. */
	public static final String lf = (String)System.getProperty("line.separator");
	
	/** The time format. */
	public static final DateFormat	df	= new SimpleDateFormat("HH:mm:ss");
	
	//-------- attributes --------
	
	/** The agent. */
	protected IExternalAccess agent;
	
	/** The clock service. */
	protected IClockService	clock;
	
	/** The text area. */
	protected JTextArea chatarea;
	
	/** The known chat users (cid->user state). */
	protected Map<IComponentIdentifier, ChatUser>	users;
	
	/** The user table. */
	protected JTable	table;
	
	/** The typing state. */
	protected boolean	typing;

	/** The request counter for coordinating gui updates. */
	protected int	reqcnt;
	
	/** The dead users determined during a request. */
	protected Set<IComponentIdentifier>	deadusers;
	
	//-------- constructors --------
	
	/**
	 *  Create a new chat panel.
	 */
	public ChatPanel(final IExternalAccess agent, IClockService clock)
	{
		this.agent	= agent;
		this.clock	= clock;
		this.users	= new LinkedHashMap<IComponentIdentifier, ChatUser>();
		
		chatarea = new JTextArea(10, 30)
		{
			public void append(String text)
			{
				super.append(text);
				this.setCaretPosition(getText().length());
			}
		};
		chatarea.setEditable(false);
		JScrollPane main = new JScrollPane(chatarea);
		
		table	= new JTable(new UserTableModel());
		JScrollPane userpan = new JScrollPane(table);
		table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer()
		{
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column)
			{
				super.getTableCellRendererComponent(table, value, selected, focus, row, column);
				IComponentIdentifier	cid	= (IComponentIdentifier)value;
				this.setText(cid.getName());
				this.setToolTipText("State: "+users.get(cid));
				Icon	icon	= users.get(cid).getIcon();
				this.setIcon(icon);
				return this;
			}
		});
		JPanel	listpan	= new JPanel(new BorderLayout());
		listpan.add(userpan, BorderLayout.CENTER);

		JPanel south = new JPanel(new BorderLayout());
		final JTextField tf = new JTextField();
		final JButton send = new JButton("Send");
		south.add(tf, BorderLayout.CENTER);
		south.add(send, BorderLayout.EAST);
		tf.getDocument().addDocumentListener(new DocumentListener()
		{
			public void removeUpdate(DocumentEvent e)
			{
				update();
			}
			
			public void insertUpdate(DocumentEvent e)
			{
				update();
			}
			
			public void changedUpdate(DocumentEvent e)
			{
				update();
			}
			
			public void update()
			{
				boolean	newtyping	= tf.getText().length()!=0;
				if(newtyping!=typing)
				{
					typing	= newtyping;
					postStatus(typing ? IChatService.STATE_TYPING : IChatService.STATE_IDLE);
				}
			}			
		});

		ActionListener al = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final String	msg	= tf.getText();
				tf.setText("");
				typing	= false;
				final int	request	= startRequest();
				tell(msg, request).addResultListener(new SwingDefaultResultListener<Void>(ChatPanel.this)
				{
					public void customResultAvailable(Void result)
					{
						endRequest(request);
					}
					
					public void customExceptionOccurred(Exception exception)
					{
						super.customExceptionOccurred(exception);
						endRequest(request);
					}
				});
			}
		};
		tf.addActionListener(al);
		send.addActionListener(al);

		JSplitPanel	split	= new JSplitPanel(JSplitPanel.HORIZONTAL_SPLIT, listpan, main);
		split.setOneTouchExpandable(true);
		split.setDividerLocation(0.3);
		this.setLayout(new BorderLayout());
		this.add(split, BorderLayout.CENTER);
		this.add(south, BorderLayout.SOUTH);
		
		// Post availability, also gets list of initial users.
		postStatus(IChatService.STATE_IDLE);
	}
	
	//-------- methods called from gui --------
	
	/**
	 *  Send a message.
	 *  @param text The text.
	 */
	public IFuture<Void>	tell(final String text, final int request)
	{
		return agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				final Future<Void>	ret	= new Future<Void>();
				
				// Keep track of search and called chats (only accessed from component thread).
				final Set<IFuture<?>>	futures	= new HashSet<IFuture<?>>();
				
				final IIntermediateFuture<IChatService> ifut = ia.getServiceContainer().getRequiredServices("chatservices");
				futures.add(ifut);
				
				ifut.addResultListener(new IntermediateDefaultResultListener<IChatService>()
				{
					public void intermediateResultAvailable(final IChatService chat)
					{
						// Send chat message and wait for future.
						final IFuture<Void>	cfut	= chat.message(text);
						setReceiving(chat, request, true);
						futures.add(cfut);
						cfut.addResultListener(new IResultListener<Void>()
						{
							public void resultAvailable(Void result)
							{
								setReceiving(chat, request, false);
								done(cfut);
							}
							public void exceptionOccurred(Exception exception)
							{
								setReceiving(chat, -2, true);
								done(cfut);
							}
						});
					}
					
					public void finished()
					{
						done(ifut);
					}
					
					public void exceptionOccurred(Exception exception)
					{
						done(ifut);
					}
					
					public void	done(IFuture<?> fut)
					{
						futures.remove(fut);
						if(futures.isEmpty())
						{
							ret.setResult(null);
						}						
					}
				});
				
				return ret;
			}
		});
	}
	
	/**
	 *  Post the local state to available chatters
	 */
	public IFuture<Void>	postStatus(final String status)
	{
		final int	request	= startRequest();
		return agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				final IIntermediateFuture<IChatService> ifut = ia.getServiceContainer().getRequiredServices("chatservices");
				ifut.addResultListener(new IntermediateDefaultResultListener<IChatService>()
				{
					public void intermediateResultAvailable(final IChatService chat)
					{
						setReceiving(chat, -1, false);	// Adds user if not already known.
						
						chat.status(status);
					}
					
					public void finished()
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								endRequest(request);
							}
						});
					}
					
					public void exceptionOccurred(Exception exception)
					{
						finished();
					}
				});
				
				return IFuture.DONE;
			}
		});
	}
	
	//-------- helper methods --------

	/**
	 *  Start an asynchronous request.
	 *  Used to collect dead users.
	 */
	protected int	startRequest()
	{
		assert SwingUtilities.isEventDispatchThread();
		// Remember known users to determine dead ones.
		deadusers	= new HashSet<IComponentIdentifier>(users.keySet());
		return ++reqcnt;	// Keep track of parallel sendings and update gui only for last.		
	}
	
	/**
	 *  Called on request end
	 *  Used to collect dead users.
	 */
	protected void	endRequest(int request)
	{
		assert SwingUtilities.isEventDispatchThread();
		// Set states of unavailable users to dead
		if(request==reqcnt)
		{
			for(IComponentIdentifier cid: deadusers)
			{
				if(users.containsKey(cid))
					users.get(cid).setState(IChatService.STATE_DEAD);
			}
			((DefaultTableModel)table.getModel()).fireTableDataChanged();
			table.getParent().invalidate();
			table.getParent().doLayout();
			table.repaint();
		}
	}
	
	/**
	 *  Add a user or change its state.
	 */
	public void	setReceiving(final IChatService chat, final int receiving, final boolean b)
	{
		// Called on component thread.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				IComponentIdentifier	cid	= ((IService)chat).getServiceIdentifier().getProviderId();
				deadusers.remove(cid);
				ChatUser	cu	= users.get(cid);
				if(cu==null)
				{
					cu	= new ChatUser(chat);
					users.put(cid, cu);
				}
			
				cu.setReceiving(receiving, b);
				((DefaultTableModel)table.getModel()).fireTableDataChanged();
				table.getParent().invalidate();
				table.getParent().doLayout();
				table.repaint();
			}
		});
	}

	//-------- methods called from service --------
	
	/**
	 *  Create a gui frame.
	 */
	public static IFuture<ChatPanel>	createGui(final IExternalAccess agent, final IClockService clock)
	{
		final Future<ChatPanel>	ret	= new Future<ChatPanel>();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final JFrame f = new JFrame(agent.getComponentIdentifier().getName());
				ChatPanel cp = new ChatPanel(agent, clock);
				f.add(cp);
				f.pack();
				f.setLocation(SGUI.calculateMiddlePosition(f));
				f.setVisible(true);
				f.addWindowListener(new WindowAdapter()
				{
					public void windowClosing(WindowEvent e)
					{
						agent.killComponent();
					}
				});
				ret.setResult(cp);
			}
		});
		
		return ret;
	}

	/**
	 *  Close the gui.
	 */
	public void dispose()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				SGUI.getWindowParent(ChatPanel.this).dispose();
			}
		});
	}
	
	/**
	 *  Add a message to the text area.
	 */
	public void addMessage(final IComponentIdentifier cid, final String text)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				StringBuffer buf = new StringBuffer();
				buf.append("[").append(df.format(new Date(clock.getTime()))).append(", ")
					.append(cid.getName()).append("]: ").append(text).append(lf);
				chatarea.append(buf.toString());
				
				deadusers.remove(cid);
				ChatUser	cu	= users.get(cid);
				if(cu==null)
				{
					cu	= new ChatUser(cid);
					users.put(cid, cu);
					((DefaultTableModel)table.getModel()).fireTableDataChanged();
					table.getParent().invalidate();
					table.getParent().doLayout();
					table.repaint();
				}
			}
		});
	}
	
	/**
	 *  Add a user or change its state.
	 */
	public void	setUserState(final IComponentIdentifier cid, final String newstate)
	{
		// Called on component thread.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				deadusers.remove(cid);
				ChatUser	cu	= users.get(cid);
				if(cu==null)
				{
					cu	= new ChatUser(cid);
					users.put(cid, cu);
				}
				cu.setState(newstate);
				((DefaultTableModel)table.getModel()).fireTableDataChanged();
				table.getParent().invalidate();
				table.getParent().doLayout();
				table.repaint();
			}
		});
	}
	
	//-------- helper classes --------
	
	/**
	 *  Table model for list of users.
	 */
	public class UserTableModel	extends DefaultTableModel
	{
		protected String[]	columns	= new String[]{"Users"};
		
		public int getColumnCount()
		{
			return columns.length;
		}
		public String getColumnName(int i)
		{
			return columns[i];
		}
		public Class<?> getColumnClass(int i)
		{
			return String.class;
		}
		public int getRowCount()
		{
			return users.size();
		}
		public Object getValueAt(int row, int column)
		{
			IComponentIdentifier[]	cids	= users.keySet().toArray(new IComponentIdentifier[users.size()]);
			return cids[row];
		}
		
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
		public void setValueAt(Object val, int row, int column)
		{
		}
		
		public void addTableModelListener(TableModelListener l)
		{
		}
		public void removeTableModelListener(TableModelListener l)
		{
		}
	}
}
