package jadex.android.puzzle;

import jadex.android.puzzle.SokratesService.PlatformBinder;
import jadex.android.puzzle.SokratesService.SokratesListener;
import jadex.android.puzzle.ui.SokratesView;
import jadex.android.standalone.clientapp.ClientAppMainFragment;
import jadex.bdiv3.examples.puzzle.IBoard;
import jadex.bdiv3.examples.puzzle.Move;
import jadex.commons.beans.PropertyChangeEvent;
import jadex.commons.future.ThreadSuspendable;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SokratesFragment extends ClientAppMainFragment implements ServiceConnection
{
	protected static final String BDI = "BDI";
	protected static final String BDIBenchmark = "BDIBenchmark";
	protected static final String BDIV3 = "BDIV3";
	protected static final String BDIV3Benchmark = "BDIV3Benchmark";
	
	private PlatformBinder service;
	private TextView statusTextView;
	private SokratesView sokratesView;
	
	private Intent serviceIntent;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		serviceIntent = new Intent(getContext(), SokratesService.class);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.sokrates, container, false);
		return view;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		setTitle(R.string.app_title);
		bindService(serviceIntent, this, 0);
		View view = getView();
		sokratesView = (SokratesView) view.findViewById(R.id.sokrates_gameView);
		statusTextView = (TextView) view.findViewById(R.id.sokrates_statusTextView);
		statusTextView.setText("starting Platform...");
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		if (!isRemoving()) {
			// if isRemoving, onDestroy will be called where
			// we want to stop sokrates before unbinding
			unbindService(this);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder)
	{
		this.service = (SokratesService.PlatformBinder) binder;
		this.service.setSokratesListener(sokratesListener);
		String mode = getIntent().getStringExtra("mode");
		
		if (!service.isSokratesRunning()) {
			statusTextView.setText("starting Game in mode: " + mode);
			if (mode.equals(BDI)) {
				this.service.startSokrates();
			} else if (mode.equals(BDIBenchmark)) {
				this.service.startSokratesBench();
			} else if (mode.equals(BDIV3)) {
				this.service.startSokratesV3();
			} else if (mode.equals(BDIV3Benchmark)) {
				this.service.startSokratesV3Bench();
			}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name)
	{
		this.service = null;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (service != null)
		{
			service.stopSokrates().get();
			unbindService(this);
		}
	}

	SokratesListener sokratesListener = new SokratesListener()
	{
		@Override
		public void setBoard(IBoard board)
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					statusTextView.setText("Game started!");
				}
			});
			sokratesView.setBoard(board);
		}

		@Override
		public void handleEvent(PropertyChangeEvent event)
		{
			Move move = (Move) event.getNewValue();
			sokratesView.performMove(move);
		}

		@Override
		public void showMessage(final String text)
		{
			runOnUiThread(new Runnable()
			{

				@Override
				public void run()
				{
					statusTextView.setText(text);
				}
			});
		}

	};
	

}
