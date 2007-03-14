/*
 * Created on Dec 31, 2004 at the Interface Ecology Lab.
 */
package ecologylab.types;

import java.lang.reflect.Field;

/**
 * Type system entry for double, a built-in primitive.
 * 
 * @author andruid
 */
public class DoubleType extends Type 
{
	public DoubleType()
	{
		super("double", true);
	}

	/**
	 * Convert the parameter to double.
	 */
	public double getValue(String valueString)
	{
		return Double.parseDouble(valueString);
	}
	
	/**
	 * This is a primitive type, so we set it specially.
	 * 
	 * @see ecologylab.types.Type#setField(java.lang.reflect.Field, java.lang.String)
	 */
	public boolean setField(Object object, Field field, String value) 
	{
		boolean result	= false;
		try
		{
			field.setDouble(object, getValue(value));
			result		= true;
		} catch (Exception e)
		{
			debug(errorString(field) + "to " + value);
		}
		return result;
	}
/**
 * The string representation for a Field of this type
 */
	public String toString(Object object, Field field)
	{
	   String result	= "COULDN'T CONVERT!";
	   try
	   {
		  result		= Double.toString(field.getDouble(object));
	   } catch (Exception e)
	   {
		  e.printStackTrace();
	   }
	   return result;
	}

/**
 * The default value for this type, as a String.
 * This value is the one that translateToXML(...) wont bother emitting.
 * 
 * @return		"1"
 */
	public String defaultValue()
	{
	   return "1.0";
	}
}