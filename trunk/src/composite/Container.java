package composite;

import java.io.IOException;
import java.io.OutputStream;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.TranslationScope;
import ecologylab.serialization.ElementState.FORMAT;

public class Container extends ElementState
{

	//@simpl_wrap
	@simpl_composite
	@simpl_classes({WCBase.class, WCSubOne.class, WCSubTwo.class})
	WCBase wc;
	
	
	public Container()
	{
		wc = new WCBase(0);
	}
	
	/**
	 * @param args
	 * @throws SIMPLTranslationException 
	 */
	public static void main(String[] args) throws SIMPLTranslationException
	{
		StringBuilder sb = new StringBuilder();	
		Container c = new Container();
		
		//c.wc = new WCBase(224234);
		
		c.wc = new WCSubTwo(true);
		
		TranslationScope containerTest = TranslationScope.get("containerTranslationscope", Container.class, WCBase.class, WCSubOne.class, WCSubTwo.class);
		
		
		
		testDeSerialization(c, containerTest, FORMAT.JSON, true);
	}
	
	public static void testDeSerialization(ElementState test, TranslationScope translationScope,
			FORMAT format, boolean setGraphSwitch) throws SIMPLTranslationException
	{
		System.out.println();
		
		if (setGraphSwitch)
		{
			TranslationScope.setGraphSwitch();
		}

		final StringBuilder output = new StringBuilder();
		OutputStream outputStream = new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				output.append((char) b);
			}
		};

		ElementState deserializedObject = null;

		test.serialize(outputStream, format);

		System.out.println("Initialized object serialized into " + format + " representation.");
		System.out.println();
		
		System.out.println(output);
		
		System.out.println();

		deserializedObject = translationScope.deserializeCharSequence(output, format);

		System.out.println("Deserilized object serialized into " + format + "  representation");
		System.out.println();
		deserializedObject.serialize(System.out, format);

		System.out.println();

	}
}
