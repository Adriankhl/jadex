package jadex.adapter.base.envsupport.observer.graphics.drawable;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextMeasurer;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import com.sun.opengl.util.j2d.TextRenderer;


import jadex.adapter.base.envsupport.environment.space2d.action.GetPosition;
import jadex.adapter.base.envsupport.math.IVector2;
import jadex.adapter.base.envsupport.math.Vector2Double;
import jadex.adapter.base.envsupport.observer.graphics.TextInfo;
import jadex.adapter.base.envsupport.observer.graphics.ViewportJ2D;
import jadex.adapter.base.envsupport.observer.graphics.ViewportJOGL;
import jadex.adapter.base.envsupport.observer.gui.SObjectInspector;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SimpleValueFetcher;

/**
 * Drawable component for displaying text.
 */
public final class Text implements IDrawable
{
	/** Viewport Height on which the base font size is relative to */
	private float BASE_VIEWPORT_HEIGHT = 300.0f;
	
	/** Relative position or binding */
	private Object position;
	
	/** Font used for the text */
	private Font baseFont;
	
	/** Color of the font */
	private Color color;
	
	/** Fixed text */
	private String text;
	
	/** The condition deciding if the drawable should be drawn. */
	private IParsedExpression drawcondition;
	
	public Text()
	{
		this(null, null, null, null, null);
	}
	
	public Text(Object position, Font baseFont, Color color, String text, IParsedExpression drawcondition)
	{
		if (position == null)
			position = Vector2Double.ZERO.copy();
		this.position = position;
		if (baseFont == null)
			baseFont = new Font(null);
		this.baseFont = baseFont;
		if (text == null)
			text = "";
		this.text = text;
		if (color == null)
			color = Color.WHITE;
		this.color = color;
		this.drawcondition = drawcondition;
	}
	
	/**
	 * Initializes the object for a Java2D viewport
	 * 
	 * @param vp the viewport
	 * @param g Graphics2D context
	 */
	public void init(ViewportJ2D vp)
	{
	}

	/**
	 * Initializes the object for an OpenGL viewport
	 * 
	 * @param vp the viewport
	 * @param gl OpenGL context
	 */
	public void init(ViewportJOGL vp)
	{
	}
	
	/**
	 * Draws the object to a Java2D viewport
	 * 
	 * @param dc the DrawableCombiner drawing the object
	 * @param obj the object being drawn
	 * @param vp the viewport
	 */
	public void draw(DrawableCombiner dc, Object obj, ViewportJ2D vp)
	{
		boolean draw = drawcondition==null;
		if(!draw)
		{
			SimpleValueFetcher fetcher = new SimpleValueFetcher();
			fetcher.setValue("$object", obj);
			draw = ((Boolean)drawcondition.getValue(fetcher)).booleanValue();
		}
		
		if (draw)
		{
			IVector2 position = (IVector2)dc.getBoundValue(obj, this.position);
			IVector2 dcPos = (IVector2)dc.getBoundValue(obj, dc.getPosition());//SObjectInspector.getVector2(obj, dc.getPosition());
			IVector2 dcScale = (IVector2)dc.getBoundValue(obj, dc.getSize());//SObjectInspector.getVector2(obj, dc.getSize());
			if((position == null) || (dcPos == null) || (dcScale == null))
			{
				return;
			}
			
			Graphics2D g = vp.getContext();
			Canvas canvas = vp.getCanvas();
			float fontscale = dcScale.getMean().getAsFloat() * (canvas.getHeight() / BASE_VIEWPORT_HEIGHT);
			Font font = baseFont.deriveFont(baseFont.getSize() * fontscale);
			String text = getReplacedText(obj);
			
			IVector2 pos = vp.getPosition().copy().negate().add(vp.getObjectShift()).add(dcPos).add(position).divide(vp.getPaddedSize()).multiply(new Vector2Double(canvas.getWidth(), canvas.getHeight()));
			if (vp.getInvertX())
				pos.negateX().add(new Vector2Double(canvas.getWidth(), 0));
			if (vp.getInvertY())
				pos.negateY().add(new Vector2Double(0, canvas.getHeight()));
			Rectangle2D bounds = font.getStringBounds(text, new FontRenderContext(null, true, true));
			pos.subtract(new Vector2Double(bounds.getWidth() / 2.0, bounds.getHeight() / 2.0));
			
			g.setColor(color);
			g.setFont(font);
			g.drawString(text, pos.getXAsInteger(), pos.getYAsInteger());
			
			/*AffineTransform t = g.getTransform();
			float fontscale = dcScale.getMean().getAsFloat() * (vp.getCanvas().getHeight() / BASE_VIEWPORT_HEIGHT);
			Font font = baseFont.deriveFont(baseFont.getSize() * fontscale);
			BufferedImage image = vp.getTextImage(new TextInfo(font, color, getReplacedText(obj)));
			Canvas canvas = vp.getCanvas();
			IVector2 size = vp.getPaddedSize().copy().divide(new Vector2Double(canvas.getWidth(), canvas.getHeight())).
			multiply(new Vector2Double(image.getWidth(), image.getHeight()));

			
			g.translate(dcPos.getXAsDouble(), dcPos.getYAsDouble());
			g.translate(position.getXAsDouble() - (size.getXAsDouble() / 2.0),
					position.getYAsDouble() - (size.getYAsDouble() / 2.0));
			g.scale(size.getXAsDouble(), size.getYAsDouble());
			
			g.drawImage(image, vp.getImageTransform(image.getWidth(), image.getHeight()), null);
			g.setTransform(t);*/
		}
	}
	
	/**
	 * Draws the object to an OpenGL viewport
	 * 
	 * @param dc the DrawableCombiner drawing the object
	 * @param obj the object being drawn
	 * @param vp the viewport
	 */
	public void draw(DrawableCombiner dc, Object obj, ViewportJOGL vp)
	{
		boolean draw = drawcondition==null;
		if(!draw)
		{
			SimpleValueFetcher fetcher = new SimpleValueFetcher();
			fetcher.setValue("$object", obj);
			draw = ((Boolean)drawcondition.getValue(fetcher)).booleanValue();
		}
		
		if (draw)
		{
			IVector2 position = (IVector2)dc.getBoundValue(obj, this.position);
			IVector2 dcPos = (IVector2)dc.getBoundValue(obj, dc.getPosition());//SObjectInspector.getVector2(obj, dc.getPosition());
			IVector2 dcScale = (IVector2)dc.getBoundValue(obj, dc.getSize());//SObjectInspector.getVector2(obj, dc.getSize());
			if((position == null) || (dcPos == null) || (dcScale == null))
			{
				return;
			}
			
			Canvas canvas = vp.getCanvas();
			float fontscale = dcScale.getMean().getAsFloat() * (canvas.getHeight() / BASE_VIEWPORT_HEIGHT);
			Font font = baseFont.deriveFont(baseFont.getSize() * fontscale);
			String text = getReplacedText(obj);
			
			TextRenderer tr = vp.getTextRenderer(font);
			tr.setColor(color);
			IVector2 pos = vp.getPosition().copy().negate().add(vp.getObjectShift()).add(dcPos).add(position).divide(vp.getPaddedSize()).multiply(new Vector2Double(canvas.getWidth(), canvas.getHeight()));
			if (vp.getInvertX())
				pos.negateX().add(new Vector2Double(canvas.getWidth(), 0));
			if (vp.getInvertY())
				pos.negateY().add(new Vector2Double(0, canvas.getHeight()));
			Rectangle2D bounds = tr.getBounds(text);
			pos.subtract(new Vector2Double(bounds.getWidth() / 2.0, bounds.getHeight() / 2.0));
			
			tr.beginRendering(canvas.getWidth(), canvas.getHeight());
			tr.draw(text, pos.getXAsInteger(), pos.getYAsInteger());
			tr.endRendering();
		}
	}
	
	private String getReplacedText(Object obj)
	{
		String[] tokens = text.split("\\$");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; ++i)
		{
			if ((i & 1) == 0)
			{
				sb.append(tokens[i]);
			}
			else
			{
				if (tokens[i] == "")
				{
					sb.append("$");
				}
				else
				{
					sb.append(String.valueOf(SObjectInspector.getProperty(obj, tokens[i])));
				}
			}
		}
		return sb.toString();
	}
}
