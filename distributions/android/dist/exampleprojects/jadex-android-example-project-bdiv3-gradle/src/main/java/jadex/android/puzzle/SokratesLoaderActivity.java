package jadex.android.puzzle;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import jadex.android.exampleproject.bdiv3.R;
import jadex.android.puzzle.SokratesService.PlatformBinder;
import jadex.android.puzzle.SokratesService.PlatformListener;

public class SokratesLoaderActivity extends Activity implements ServiceConnection, PlatformListener
{
	private TextView statusTextView;

	private SokratesService.PlatformBinder service;

	protected boolean platformRunning;

	private Intent serviceIntent;

	private Button startBDIV3Button;
	private View startBDIV3BenchmarkButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		serviceIntent = new Intent(this, SokratesService.class);
		startService(serviceIntent);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setTitle(R.string.app_title);
		this.setContentView(R.layout.mainapp);

		statusTextView = (TextView) findViewById(R.id.statusTextView);

		startBDIV3Button = (Button) findViewById(R.id.startBDIV3);
		startBDIV3Button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Intent i = new Intent(SokratesLoaderActivity.this, SokratesGameActivity.class);
				i.putExtra("mode", SokratesGameActivity.BDIV3);
				startActivity(i);
			}
		});

		startBDIV3BenchmarkButton = (Button) findViewById(R.id.startBDIV3Benchmark);
		startBDIV3BenchmarkButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				final Intent i = new Intent(SokratesLoaderActivity.this, SokratesGameActivity.class);
				i.putExtra("mode", SokratesGameActivity.BDIV3Benchmark);
				startActivity(i);
			}
		});
	}

	@Override
	public void onResume()
	{
		super.onResume();
		startBDIV3Button.setEnabled(false);
		startBDIV3BenchmarkButton.setEnabled(false);
		statusTextView.setText("Connecting to Service...");
		bindService(serviceIntent, this, 0);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		if (service != null) {
			unbindService(this);
		}
	}
	

	@Override
	public void onDestroy()
	{
		super.onDestroy();
//		if (service != null)
//		{
//			unbindService(this);
			Intent intent = new Intent(this, SokratesService.class);
			stopService(intent);
//		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service)
	{
		this.service = (PlatformBinder) service;
		this.service.setPlatformListener(this);
		
		statusTextView.setText("Connected.");
		this.service.startPlatform();
	}

	@Override
	public void onServiceDisconnected(ComponentName name)
	{
		this.service = null;
	}

	@Override
	public void platformStarted()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				startBDIV3BenchmarkButton.setEnabled(true);
				startBDIV3Button.setEnabled(true);
				statusTextView.setText("Platform started.");
			}
		});
		platformRunning = true;
	}

	@Override
	public void platformStarting()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				statusTextView.setText("Platform Starting");
			}
		});
	}
}
