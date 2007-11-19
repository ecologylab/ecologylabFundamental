/*
 * Created on Dec 31, 2004 at the Interface Ecology Lab.
 */
package ecologylab.xml.types.scalar;

import ecologylab.xml.XMLTools;

/**
 * Type system entry for {@link java.lang.String String}. A very simple case.
 * 
 * @author andruid
 */
public class StringType extends ReferenceType<String>
{
/**
 * This constructor should only be called once per session, through
 * a static initializer, typically in TypeRegistry.
 * <p>
 * To get the instance of this type object for use in translations, call
 * <code>TypeRegistry.get("java.lang.String")</code>.
 * 
 */
	public StringType()
	{
		super(String.class);
	}

	/**
	 * Just return the value itself. A transparent pass-through.
	 * 
	 * @see ecologylab.xml.types.scalar.ScalarType#getInstance(java.lang.String)
	 */
	public String getInstance(String value)
	{
		return value;
	}

	/**
	 * Append the String directly, unless it needs escaping, in which case, call escapeXML.
	 * 
	 * @param instance
	 * @param buffy
	 * @param needsEscaping
	 */
	@Override 
	protected void appendValue(String instance, StringBuilder buffy, boolean needsEscaping)
    {
		if (needsEscaping)
			XMLTools.escapeXML(buffy, instance);
		else
			buffy.append(instance);
    }
}