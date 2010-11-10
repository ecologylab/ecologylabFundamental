package ecologylab.serialization;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.xml.sax.Attributes;

import ecologylab.generic.Debug;
import ecologylab.serialization.TranslationScope.GRAPH_SWITCH;
import ecologylab.serialization.constants.FieldTypes;
import ecologylab.serialization.enums.Format;
import ecologylab.serialization.tools.XMLTools;

public class GraphContext extends Debug implements FieldTypes
{
	public static final String							SIMPL_ID								= "simpl:id";

	public static final String							SIMPL_REF								= "simpl:ref";

	public static final String							SIMPL_NAMESPACE					= " xmlns:simpl=\"http://ecologylab.net/research/simplGuide/serialization/index.html\"";

	private HashMap<Integer, ElementState>	marshalledObjects				= new HashMap<Integer, ElementState>();

	private HashMap<Integer, ElementState>	visitedElements					= new HashMap<Integer, ElementState>();

	private HashMap<Integer, ElementState>	needsAttributeHashCode	= new HashMap<Integer, ElementState>();

	private HashMap<String, ElementState>		unmarshalledObjects			= new HashMap<String, ElementState>();

	private Format													format;

	public GraphContext()
	{

	}

	public GraphContext(ElementState elementState, Format format)
	{
		resolveGraph(elementState);
		this.format = format;
	}

	private void resolveGraph(ElementState elementState)
	{
		if (TranslationScope.graphSwitch == GRAPH_SWITCH.ON)
		{
			visitedElements.put(System.identityHashCode(elementState), elementState);

			ArrayList<FieldDescriptor> elementFieldDescriptors = elementState.classDescriptor()
					.elementFieldDescriptors();

			for (FieldDescriptor elementFieldDescriptor : elementFieldDescriptors)
			{
				Object thatReferenceObject = null;
				Field childField = elementFieldDescriptor.getField();
				try
				{
					thatReferenceObject = childField.get(elementState);
				}
				catch (IllegalAccessException e)
				{
					debugA("WARNING re-trying access! " + e.getStackTrace()[0]);
					childField.setAccessible(true);
					try
					{
						thatReferenceObject = childField.get(elementState);
					}
					catch (IllegalAccessException e1)
					{
						error("Can't access " + childField.getName());
						e1.printStackTrace();
					}
				}
				catch (Exception e)
				{
					System.out.println("yay");
				}
				// ignore null reference objects
				if (thatReferenceObject == null)
					continue;

				int childFdType = elementFieldDescriptor.getType();

				Collection thatCollection;
				switch (childFdType)
				{
				case COLLECTION_ELEMENT:
				case COLLECTION_SCALAR:
				case MAP_ELEMENT:
				case MAP_SCALAR:
					thatCollection = XMLTools.getCollection(thatReferenceObject);
					break;
				default:
					thatCollection = null;
					break;
				}

				if (thatCollection != null && (thatCollection.size() > 0))
				{
					for (Object next : thatCollection)
					{
						if (next instanceof ElementState)
						{
							ElementState compositeElement = (ElementState) next;

							if (alreadyVisited(compositeElement))
							{
								needsAttributeHashCode.put(System.identityHashCode(compositeElement),
										compositeElement);
							}
							else
							{
								resolveGraph(compositeElement);
							}
						}
					}
				}
				else if (thatReferenceObject instanceof ElementState)
				{
					ElementState compositeElement = (ElementState) thatReferenceObject;

					if (alreadyVisited(compositeElement))
					{
						needsAttributeHashCode.put(System.identityHashCode(compositeElement), compositeElement);
					}
					else
					{
						resolveGraph(compositeElement);
					}
				}
			}
		}
	}

	private boolean alreadyVisited(ElementState elementState)
	{
		return visitedElements.containsKey(System.identityHashCode(elementState));
	}

	public void mapCurrentElementState(ElementState elementState)
	{
		if (TranslationScope.graphSwitch == GRAPH_SWITCH.ON)
		{
			marshalledObjects.put(System.identityHashCode(elementState), elementState);
		}
	}

	public boolean alreadyMarshalled(ElementState compositeElementState)
	{
		return marshalledObjects.containsKey(System.identityHashCode(compositeElementState));
	}

	public boolean isGraph()
	{
		return needsAttributeHashCode.size() > 0;
	}

	public ElementState getFromMap(Attributes attributes)
	{
		ElementState unMarshalledObject = null;

		int numAttributes = attributes.getLength();
		for (int i = 0; i < numAttributes; i++)
		{
			final String tag = attributes.getQName(i);
			final String value = attributes.getValue(i);

			if (tag.equals(GraphContext.SIMPL_REF))
			{
				unMarshalledObject = unmarshalledObjects.get(value);
			}
		}

		return unMarshalledObject;
	}

	public void mapUnmarshalledObjects(String value, ElementState elementState)
	{
		unmarshalledObjects.put(value, elementState);
	}

	public boolean needsHashCode(ElementState elementState)
	{
		return needsAttributeHashCode.containsKey(System.identityHashCode(elementState));
	}

	public String elementHashCode(ElementState elementState)
	{
		return ((Integer) System.identityHashCode(elementState)).toString();
	}

	public void appendSimplIdIfRequired(Appendable appendable, ElementState elementState)
			throws IOException
	{
		if (TranslationScope.graphSwitch == GRAPH_SWITCH.ON && needsHashCode(elementState))
		{
			appendSimplIdAttribute(appendable, elementState);
		}
	}

	public void appendSimplNameSpace(Appendable appendable) throws IOException
	{
		appendable.append(GraphContext.SIMPL_NAMESPACE);
	}

	public void appendSimplRefId(Appendable appendable, ElementState elementState,
			FieldDescriptor compositeElementFD) throws IOException
	{
		compositeElementFD.writeElementStart(appendable);
		appendSimplIdAttributeWithTagName(appendable, SIMPL_REF, elementState);
		appendable.append("/>");
	}

	private void appendSimplIdAttributeWithTagName(Appendable appendable, String tagName,
			ElementState elementState) throws IOException
	{
		appendable.append(' ');
		appendable.append(tagName);
		appendable.append('=');
		appendable.append('"');
		appendable.append(elementHashCode(elementState));
		appendable.append('"');
	}

	private void appendSimplIdAttribute(Appendable appendable, ElementState elementState)
			throws IOException
	{
		appendSimplIdAttributeWithTagName(appendable, GraphContext.SIMPL_ID, elementState);
	}
}