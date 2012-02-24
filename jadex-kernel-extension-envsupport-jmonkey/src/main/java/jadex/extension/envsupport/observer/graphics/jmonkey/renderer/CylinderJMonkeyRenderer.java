package jadex.extension.envsupport.observer.graphics.jmonkey.renderer;

import jadex.extension.envsupport.observer.graphics.drawable3d.Cylinder3d;
import jadex.extension.envsupport.observer.graphics.drawable3d.DrawableCombiner3d;
import jadex.extension.envsupport.observer.graphics.drawable3d.Primitive3d;
import jadex.extension.envsupport.observer.graphics.jmonkey.ViewportJMonkey;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;


public class CylinderJMonkeyRenderer extends AbstractJMonkeyRenderer
{
	/** Cylinder for jMonkey. */
	private Cylinder	cylinder;

	public Spatial draw(DrawableCombiner3d dc, Primitive3d primitive,
			Object obj, ViewportJMonkey vp)
	{

		float radius = (float)((Cylinder3d)primitive).getRadius();
		float height = (float)((Cylinder3d)primitive).getHeight();
		
		cylinder = new Cylinder(30, 30, radius, height, true);

		geo = new Geometry(identifier, cylinder);

		geo.setModelBound(new BoundingBox());

		return geo;


	}

}
