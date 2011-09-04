package ecologylab.serialization.deserializers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.jackson.JsonParseException;

import ecologylab.generic.Debug;
import ecologylab.generic.StringInputStream;
import ecologylab.serialization.ClassDescriptor;
import ecologylab.serialization.DeserializationHookStrategy;
import ecologylab.serialization.FieldDescriptor;
import ecologylab.serialization.FieldTypes;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.TranslationContext;
import ecologylab.serialization.TranslationScope;

public class XMLPullDeserializer extends Debug implements FieldTypes
{
	TranslationScope						translationScope;

	TranslationContext					translationContext;

	DeserializationHookStrategy	deserializationHookStrategy;

	XMLStreamReader							xmlStreamReader	= null;

	/**
	 * Constructs that creates a XML deserialization handler
	 * 
	 * @param translationScope
	 *          translation scope to use for de/serializing subsequent char sequences
	 * @param translationContext
	 *          used for graph handling
	 */
	public XMLPullDeserializer(TranslationScope translationScope,
			TranslationContext translationContext)
	{
		this.translationScope = translationScope;
		this.translationContext = translationContext;
		this.deserializationHookStrategy = null;
	}

	public Object parse(CharSequence charSequence) throws XMLStreamException,
			FactoryConfigurationError, SIMPLTranslationException, IOException
	{
		InputStream xmlStream = new StringInputStream(charSequence, StringInputStream.UTF8);
		xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(xmlStream, "UTF-8");

		// printParse();

		Object root = null;

		xmlStreamReader.next();

		ClassDescriptor<? extends FieldDescriptor> rootClassDescriptor = translationScope
				.getClassDescriptorByTag(xmlStreamReader.getLocalName());

		root = rootClassDescriptor.getInstance();
		deserializeAttributes(root, rootClassDescriptor);

		createObjectModel(root, rootClassDescriptor);

		return root;
	}

	private void deserializeAttributes(Object root,
			ClassDescriptor<? extends FieldDescriptor> rootClassDescriptor)
	{
		for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
		{
			String tag = xmlStreamReader.getAttributeLocalName(i);
			String value = xmlStreamReader.getAttributeValue(i);

			FieldDescriptor attributeFieldDescriptor = rootClassDescriptor.getFieldDescriptorByTag(tag,
					translationScope);

			if (attributeFieldDescriptor != null)
				attributeFieldDescriptor.setFieldToScalar(root, value, translationContext);
		}
	}

	/**
	 * Recursive method that moves forward in the CharSequence through JsonParser to create a
	 * corresponding object model
	 * 
	 * @param root
	 *          instance of the root element created by the calling method
	 * @param rootClassDescriptor
	 *          instance of the classdescriptor of the root element created by the calling method
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws SIMPLTranslationException
	 * @throws XMLStreamException
	 */
	private void createObjectModel(Object root,
			ClassDescriptor<? extends FieldDescriptor> rootClassDescriptor) throws JsonParseException,
			IOException, SIMPLTranslationException, XMLStreamException
	{
		FieldDescriptor currentFieldDescriptor = null;
		Object subRoot = null;
		int event;

		while (xmlStreamReader.hasNext() && (event = nextEvent()) != XMLStreamConstants.END_ELEMENT)
		{
			if (event == XMLStreamConstants.START_ELEMENT)
			{
				currentFieldDescriptor = (currentFieldDescriptor != null)
						&& (currentFieldDescriptor.getType() == IGNORED_ELEMENT) ? FieldDescriptor.IGNORED_ELEMENT_FIELD_DESCRIPTOR
						: (currentFieldDescriptor != null && currentFieldDescriptor.getType() == WRAPPER) ? currentFieldDescriptor
								.getWrappedFD()
								: rootClassDescriptor.getFieldDescriptorByTag(xmlStreamReader.getLocalName(),
										translationScope, null);

				int fieldType = currentFieldDescriptor.getType();

				switch (fieldType)
				{
				case SCALAR:
					xmlStreamReader.next();
					String value = xmlStreamReader.getText();
					currentFieldDescriptor.setFieldToScalar(root, value, translationContext);
					xmlStreamReader.next();					
					break;
				case COMPOSITE_ELEMENT:
					String tagName = xmlStreamReader.getLocalName();
					subRoot = getSubRoot(currentFieldDescriptor, tagName);
					currentFieldDescriptor.setFieldToComposite(root, subRoot);
					break;
				case COLLECTION_ELEMENT:
					while(currentFieldDescriptor.getCollectionOrMapTagName().equals(xmlStreamReader.getLocalName()))
					{
						if (event == XMLStreamConstants.START_ELEMENT)
						{
							String compositeTagName = xmlStreamReader.getLocalName();
							subRoot = getSubRoot(currentFieldDescriptor, compositeTagName);
							Collection collection = (Collection) currentFieldDescriptor
									.automaticLazyGetCollectionOrMap(root);
							collection.add(subRoot);
							
							event = nextEvent();
						}
					}
					break;
				case MAP_ELEMENT:
					break;
				case WRAPPER:
					break;
				}
			}
		}
	}

	public void printParse() throws XMLStreamException
	{
		int event;
		while (xmlStreamReader.hasNext())
		{
			event = xmlStreamReader.getEventType();
			switch (event)
			{
			case XMLStreamConstants.START_ELEMENT:
				System.out.println(xmlStreamReader.getLocalName());
				break;
			case XMLStreamConstants.END_ELEMENT:
				System.out.println(xmlStreamReader.getLocalName());
				break;
			case XMLStreamConstants.CHARACTERS:
				System.out.println(xmlStreamReader.getText());
				break;
			case XMLStreamConstants.CDATA:
				System.out.println("cdata " + xmlStreamReader.getText());
				break;
			} // end switch
			xmlStreamReader.next();
		} // end while
	}

	/**
	 * Gets the sub root of the object model if its a composite object. Does graph handling Handles
	 * simpl.ref tag to assign an already created instance of the composite object instead of creating
	 * a new one
	 * 
	 * @param currentFieldDescriptor
	 * @return
	 * @throws SIMPLTranslationException
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private Object getSubRoot(FieldDescriptor currentFieldDescriptor, String tagName)
			throws SIMPLTranslationException, JsonParseException, IOException, XMLStreamException
	{

		Object subRoot = null;
		ClassDescriptor<? extends FieldDescriptor> subRootClassDescriptor = currentFieldDescriptor
				.getChildClassDescriptor(tagName);
		subRoot = subRootClassDescriptor.getInstance();
		deserializeAttributes(subRoot, subRootClassDescriptor);
		createObjectModel(subRoot, subRootClassDescriptor);
		return subRoot;
	}

	private int nextEvent() throws XMLStreamException
	{
		int eventType = 0;

		// skip events that we don't handle.
		while (xmlStreamReader.hasNext())
		{
			eventType = xmlStreamReader.next();
			if (xmlStreamReader.getEventType() == XMLStreamConstants.START_DOCUMENT
					|| xmlStreamReader.getEventType() == XMLStreamConstants.START_ELEMENT
					|| xmlStreamReader.getEventType() == XMLStreamConstants.END_ELEMENT
					|| xmlStreamReader.getEventType() == XMLStreamConstants.END_DOCUMENT
					|| xmlStreamReader.getEventType() == XMLStreamConstants.CHARACTERS)
			{
				break;
			}
		}

		return eventType;
	}
}