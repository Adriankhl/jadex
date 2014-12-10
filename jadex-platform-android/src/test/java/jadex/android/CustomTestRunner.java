package jadex.android;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.res.ActivityData;
import org.robolectric.res.Fs;
import org.robolectric.res.FsFile;

public class CustomTestRunner extends RobolectricTestRunner {

    private static final int MAX_SDK_SUPPORTED_BY_ROBOLECTRIC = 18;

    public CustomTestRunner(Class testClass) throws InitializationError {
//        super(testClass, new File("src/test"));
        super(testClass);
    }

    
    @Override
	protected AndroidManifest getAppManifest(
			org.robolectric.annotation.Config arg0) {
		String manifestProperty = "AndroidManifest.xml";
		String resProperty = "src/main/res";
		
		// calls that could potentially fail later:
		FsFile resDir = Fs.fileFromPath(resProperty);
		FsFile parent = resDir.getParent();
		FsFile join = parent.join(new String[] { "assets" });
		
		return new AndroidManifest(Fs.fileFromPath(manifestProperty), resDir) {
			@Override
			public int getTargetSdkVersion() {
				return MAX_SDK_SUPPORTED_BY_ROBOLECTRIC;
			}
		};
	}

}
