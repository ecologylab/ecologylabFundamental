package maps;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.TranslationScope;
import ecologylab.serialization.ElementState.FORMAT;

public class TestingMapsWithinMaps
{
	public static void main(String args[]) throws SIMPLTranslationException
	{
		TranslationS trans = createObject();
		TranslationScope transScope = TranslationScope.get("testScope", TranslationS.class, ClassDes.class, FieldDes.class);
	
		testDeSerialization(trans, transScope, FORMAT.XML, false);
		testDeSerialization(trans, transScope, FORMAT.JSON, false);
	}
	
	public static TranslationS createObject()
	{
		TranslationS trans = new TranslationS();
		
		ClassDes cd1 = new ClassDes("cd1");
		
		cd1.fieldDescriptorsByTagName.put("fd1_cd1", new FieldDes("fd1_cd1"));
		cd1.fieldDescriptorsByTagName.put("fd2_cd1", new FieldDes("fd2_cd1"));
		cd1.fieldDescriptorsByTagName.put("fd3_cd1", new FieldDes("fd3_cd1"));
		
		ClassDes cd2 = new ClassDes("cd2");
		cd2.fieldDescriptorsByTagName.put("fd1_cd2", new FieldDes("fd1_cd2"));
		cd2.fieldDescriptorsByTagName.put("fd2_cd2", new FieldDes("fd2_cd2"));
		cd2.fieldDescriptorsByTagName.put("fd3_cd2", new FieldDes("fd3_cd2"));
		
		
		trans.entriesByTag.put("cd1", cd1);
		trans.entriesByTag.put("cd2", cd2);
		
		
		return trans;
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
