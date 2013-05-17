package jadex.micro.annotation;

import jadex.commons.Boolean3;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 */
/**
 *  Component annotation.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreationInfo
{
	/**
	 *  The suspend state.
	 */
	public Boolean3 suspend() default Boolean3.NULL;
	
	/**
	 *  The master flag.
	 */
	public Boolean3 master() default Boolean3.NULL;
	
	/**
	 *  The daemon flag.
	 */
	public Boolean3 daemon() default Boolean3.NULL;
	
	/**
	 *  The autoshutdown flag.
	 */
	public Boolean3 autoshutdown() default Boolean3.NULL;

	/**
	 *  The monitoring flag.
	 */
	public Boolean3 monitoring() default Boolean3.NULL;

	/**
	 *  The synchronous flag.
	 */
	public Boolean3 synchronous() default Boolean3.NULL;

	/**
	 *  The component name.
	 */
	public String name() default "";
	
	/**
	 *  The local component type.
	 */
	public String type() default "";
	
	/**
	 *  The configuration name.
	 */
	public String configuration() default "";
	
	/**
	 *  The number of components to start (parsed).
	 */
	public String number() default "";
	
	/**
	 *  The argument values.
	 */
	public NameValue[] arguments() default {};
	
//	/**
//	 *  The argument values.
//	 */
//	public Binding[] bindings() default {};
}

