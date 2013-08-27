package jadex.bridge.nonfunctional.annotation;

import jadex.bridge.nonfunctional.INFProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Non-functional property annotation.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NFProperty
{
	/**
	 *  The name of the property.
	 */
	public String name() default "";
	
	/**
	 *  The type of the property.
	 */
	public Class<? extends INFProperty> type();
	
}
