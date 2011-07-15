/**
 * 
 */
package ecologylab.serialization.types;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ecologylab.generic.Describable;
import ecologylab.generic.ReflectionTools;
import ecologylab.serialization.ElementState;

/**
 * Basic cross-platform unit for managing Collection and Map types in S.IM.PL Serialization.
 * 
 * @author andruid
 */
public class CollectionType extends ElementState
implements MappingConstants, Describable
{
	/**
	 * This is a platform-independent identifier that S.IM.PL uses for the CollectionType.
	 */
	@simpl_scalar
	private String			name;
	
	@simpl_scalar
	private String			javaName;
	
	private String			javaSimpleName;
	
	private Class				javaClass;
	
	@simpl_scalar
	private boolean			isMap;
	
	@simpl_scalar
	private String			cSharpName;
	
	@simpl_scalar
	private String			objCName;

	private static final  HashMap<String, CollectionType>	mapByName 			= new HashMap<String, CollectionType>();
	
	private static final  HashMap<String, CollectionType>	mapByClassName	= new HashMap<String, CollectionType>();
	
	/**
	 * 
	 */
	public CollectionType()
	{
	}
	
	public CollectionType(String name, Class javaClass, String cSharpName, String objCName, boolean isMap)
	{
		this.name				= name;
		this.javaClass	= javaClass;
		this.javaName		= javaClass.getName();
		this.javaSimpleName	= javaClass.getSimpleName();
		
		this.cSharpName	= cSharpName;
		this.objCName		= objCName;
		this.isMap			= isMap;
		
		mapByName.put(name, this);
		mapByClassName.put(javaName, this);
	}
	
	public Object getInstance()
	{
		return ReflectionTools.getInstance(javaClass);
	}
	
	public Collection getCollection()
	{
		return isMap ? null : (Collection) getInstance();
	}
	
	public Map getMap()
	{
		return isMap ? (Map) getInstance() : null;
	}
	
	/**
	 * Get by unique, cross-platform name.
	 * 
	 * @param crossPlatformName
	 * @return
	 */
	public static CollectionType getCollectionTypeByCrossPlatformName(String crossPlatformName)
	{
		return mapByName.get(crossPlatformName);
	}
	
	/**
	 * Lookup a collection type using the Java class or its full unqualifiedName.
	 * 
	 * @param javaField	Declaring class of this field is key for lookup
	 * 
	 * @return
	 */
	public static CollectionType getCollectionType(Field javaField)
	{
		return getCollectionType(javaField.getType());
	}
	/**
	 * Lookup a collection type using the Java class or its full unqualifiedName.
	 * @param javaClass
	 * @return
	 */
	public static CollectionType getCollectionType(Class javaClass)
	{
		return getCollectionType(javaClass.getName());
	}
	/**
	 * Lookup a collection type using the Java class or its full unqualifiedName.
	 * 
	 * @param javaClassName
	 * @return
	 */
	public static CollectionType getCollectionType(String javaClassName)
	{
		return mapByClassName.get(javaClassName);
	}
	
	/**
	 * This is the cross-platform S.IM.PL name.
	 * 
	 * @return
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * The full, qualified name of the class that this describes
	 * @return
	 */
	public String getJavaName()
	{
		return javaName;
	}

	/**
	 * The full, qualified name of the class that this describes
	 * 
	 * @return	Full Java class name.
	 */
	@Override
	public String getDescription()
	{
		return getJavaName();
	}

	/**
	 * This is the unqualified Java class name, without package.
	 * 
	 * @return
	 */
	public String getJavaSimpleName()
	{
		return javaSimpleName;
	}

	public Class getJavaClass()
	{
		return javaClass;
	}

	public boolean isMap()
	{
		return isMap;
	}

	public String getcSharpName()
	{
		return cSharpName;
	}

	public String getObjCName()
	{
		return objCName;
	}

}
