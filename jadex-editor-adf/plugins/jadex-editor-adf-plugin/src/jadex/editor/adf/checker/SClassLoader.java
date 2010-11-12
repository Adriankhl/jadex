package jadex.editor.adf.checker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;


/**
 *  Helper class for obtaining the project class loader.
 */
public class SClassLoader
{
	/**
	 * Get the project class loader.
	 */
	public static ClassLoader getProjectClassLoader(IProject project)
	{
		List urls = new ArrayList();
		IJavaProject javaproject = JavaCore.create(project);
		try
		{
			urls.add(new File(project.getLocation() + "/" + javaproject.getOutputLocation().removeFirstSegments(1) + "/").toURI().toURL());

			// Compute the URLs for the output folder of all project
			// dependencies and all libraries needed by the project.
			IClasspathEntry[]	entries	= javaproject.getResolvedClasspath(true);
			for(int i=0; i<entries.length; i++)
			{
				if(entries[i].getEntryKind()==IClasspathEntry.CPE_PROJECT)
				{
					IPath path = entries[i].getPath();
					IProject otherProject = project.getWorkspace().getRoot().getProject(path.segment(0));
					IJavaProject otherJavaProject = JavaCore.create(otherProject);

					try
					{
						urls.add(new File(otherProject.getLocation() + "/" + otherJavaProject.getOutputLocation().removeFirstSegments(1) + "/").toURI().toURL());
					}
					catch (JavaModelException e)
					{
						e.printStackTrace();
					}
					catch (MalformedURLException e)
					{
						e.printStackTrace();
					}
				}

				// add libraries to URLs
				else if(entries[i].getEntryKind()==IClasspathEntry.CPE_LIBRARY)
				{
					IPath libraryPath = entries[i].getPath();
					urls.add(libraryPath.toFile().toURI().toURL());
				}
			}
		}
		catch (JavaModelException e)
		{
			e.printStackTrace();
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		
//		System.out.println("Classpath:");
//		for(int i=0; i<urls.size(); i++)
//			System.out.println(urls.get(i));
		
		return new URLClassLoader((URL[])urls.toArray(new URL[urls.size()]), SClassLoader.class.getClassLoader());
	}
}
