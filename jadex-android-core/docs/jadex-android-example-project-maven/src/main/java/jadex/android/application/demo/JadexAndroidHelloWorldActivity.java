package jadex.android.application.demo;

import jadex.android.JadexAndroidActivity;
import jadex.base.Starter;
import jadex.bpmn.runtime.task.PrintTask;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.xml.annotation.XMLClassname;

import java.util.HashMap;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class JadexAndroidHelloWorldActivity extends JadexAndroidActivity {
	
	private Button startAgentButton;
	private Button startBPMNButton;
	
	private IExternalAccess extAcc;
	
	private int num;
	
	private Button startPlatformButton;

	private TextView textView;
	
	private static Handler handler;
	
	public static Handler getHandler() {
		return handler;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		startPlatformButton = (Button) findViewById(R.id.startPlatformButton);
		startPlatformButton.setOnClickListener(buttonListener);
		
		startAgentButton = (Button) findViewById(R.id.startAgentButton);
		startAgentButton.setOnClickListener(buttonListener);
		startAgentButton.setEnabled(false);
		
		startBPMNButton = (Button) findViewById(R.id.startBpmnButton);
		startBPMNButton.setOnClickListener(buttonListener);
		startBPMNButton.setEnabled(false);
		
		textView = (TextView) findViewById(R.id.infoTextView);
		
		handler = new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				runOnUiThread(new Runnable() {
					
					public void run() {
						Toast makeText = Toast.makeText(JadexAndroidHelloWorldActivity.this, msg.getData().getString("text"), Toast.LENGTH_SHORT);
						makeText.show();
					}
				});
			}
		};
	}

	private OnClickListener buttonListener = new OnClickListener() {

		public void onClick(View view) {
			if (view == startPlatformButton) {
				startPlatformButton.setEnabled(false);
				textView.setText("Starting Jadex Platform...");
				new Thread(new Runnable() {
					public void run() {
						IFuture<IExternalAccess> future = Starter
								.createPlatform(new String[] {
										"-logging_level", "java.util.logging.Level.INFO",
										"-extensions", "null",
										"-wspublish", "false",
										"-android", "true",
										"-kernels", "\"component, micro, bpmn\"",
//										"-tcptransport", "false",
//										"-niotcptransport", "false",
//										"-relaytransport", "true",
//										"-relayaddress", "\"http://134.100.11.200:8080/jadex-platform-relay-web/\"",					
//										"-saveonexit", "false", "-gui", "false",
										"-autoshutdown", "false",
										"-platformname", "and-" + createRandomPlattformID(),
										"-saveonexit", "false", "-gui", "false" });
						future.addResultListener(platformResultListener);
					}
				}).start();
				
			} else if (view == startAgentButton) {
				startAgentButton.setEnabled(false);
				
				IFuture<IComponentManagementService> scheduleStep = extAcc
						.scheduleStep(new IComponentStep() {
							@XMLClassname("create-component")
							public IFuture<IComponentManagementService> execute(IInternalAccess ia) {
								Future<IComponentManagementService> ret = new Future<IComponentManagementService>();
								SServiceProvider.getService(
										ia.getServiceContainer(),
										IComponentManagementService.class,
										RequiredServiceInfo.SCOPE_PLATFORM)
										.addResultListener(
												ia.createResultListener(new DelegationResultListener<IComponentManagementService>(
														ret)));

								return ret;
							}
						});
				scheduleStep.addResultListener(new DefaultResultListener<IComponentManagementService>() {

					public void resultAvailable(IComponentManagementService cms) {
						HashMap<String, Object> args = new HashMap<String, Object>();

						cms.createComponent(
								"HelloWorldAgent " + num,
								AndroidAgent.class.getName().replaceAll("\\.",
										"/")
										+ ".class", new CreationInfo(args),
								null).addResultListener(
								agentCreatedResultListener);
					}
				});
			} else if (view == startBPMNButton) {
				startBPMNButton.setEnabled(false);
				
				IFuture<IComponentManagementService> scheduleStep = extAcc
						.scheduleStep(new IComponentStep() {
							@XMLClassname("create-component")
							public IFuture<IComponentManagementService> execute(IInternalAccess ia) {
								Future<IComponentManagementService> ret = new Future<IComponentManagementService>();
								SServiceProvider.getService(
										ia.getServiceContainer(),
										IComponentManagementService.class,
										RequiredServiceInfo.SCOPE_PLATFORM)
										.addResultListener(
												ia.createResultListener(new DelegationResultListener<IComponentManagementService>(
														ret)));

								return ret;
							}
						});
				scheduleStep.addResultListener(new DefaultResultListener<IComponentManagementService>() {

					public void resultAvailable(IComponentManagementService cms) {
						HashMap<String, Object> args = new HashMap<String, Object>();

						args.put("androidContext", JadexAndroidHelloWorldActivity.this);
						cms.createComponent("SimpleWorkflow " + num,
								"jadex/android/application/demo/bpmn/SimpleWorkflow.bpmn", new CreationInfo(args), null).addResultListener(bpmnCreatedResultListener);
					}
				});
			}
		}
	};

	private IResultListener<IExternalAccess> platformResultListener = new DefaultResultListener<IExternalAccess>() {

		public void resultAvailable(IExternalAccess result) {
			extAcc = result;
			runOnUiThread(new Runnable() {

				public void run() {
					startAgentButton.setEnabled(true);
					startBPMNButton.setEnabled(true);
					textView.setText("Platform started");
				}
			});
		}
	};

	private IResultListener<IComponentIdentifier> agentCreatedResultListener = new DefaultResultListener<IComponentIdentifier>() {

		public void resultAvailable(IComponentIdentifier arg0) {
			runOnUiThread(new Runnable() {

				public void run() {
					textView.setText("Agents started: " + num);
					num++;
					startAgentButton.setEnabled(true);
				}
			});
		}
	};
	
	private IResultListener<IComponentIdentifier> bpmnCreatedResultListener = new DefaultResultListener<IComponentIdentifier>() {

		public void resultAvailable(IComponentIdentifier arg0) {
			runOnUiThread(new Runnable() {

				public void run() {
					textView.setText("BPMN component started: " + num);
					num++;
					startBPMNButton.setEnabled(true);
				}
			});
		}
	};
	
	protected String createRandomPlattformID() {
		UUID randomUUID = UUID.randomUUID();
		return randomUUID.toString().substring(0, 5);
	}
}