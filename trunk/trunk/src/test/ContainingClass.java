package test;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.TranslationScope;

public class ContainingClass extends ElementState
{
	@simpl_classes({BaseClass.class, ChildClass1.class, ChildClass2.class})
	@simpl_composite
	BaseClass theField;
	
	public ContainingClass()
	{
	}
	
	public static void main(String[] args) throws SIMPLTranslationException 
	{
//		ContainingClass cc = new ContainingClass();
//		
//		StringBuilder test = new StringBuilder();
//		cc.translateToXML(test);
//		
//		ContainingClass ccoutput = (ContainingClass) ElementState.translateFromXMLCharSequence(test, TranslationScope.get("test", ContainingClass.class, ChildClass1.class, ChildClass2.class, BaseClass.class));
//		
//		ccoutput.translateToXML(System.out);
		
		TranslationScope translationScope = TranslationScope.get("test", ContainingClass.class, ChildClass1.class, ChildClass2.class, BaseClass.class);
		
		ContainingClass ccb = new ContainingClass();
		ccb.theField = new BaseClass();
		
		ContainingClass cc1 = new ContainingClass();
		cc1.theField = new ChildClass1();
		
		ContainingClass cc2 = new ContainingClass();
		cc2.theField = new ChildClass2();
		
		StringBuilder test = new StringBuilder();

		System.out.println("base");

		test.delete(0, test.length());
		ccb.serialize(test);
		
		String test1 = "<containing_class><the_field other_tag_var=\"3\"/></containing_class>";
//		String test1 = "<containing_class><fred new_tag_var=\"3\"/></containing_class>";

		ContainingClass ccoutput = (ContainingClass) translationScope.deserializeCharSequence(test1);
		
		System.out.println(test1);
		System.out.println(ccoutput.serialize());
		
		System.out.println();
		
		System.out.println("child1");
		test.delete(0, test.length());
		cc1.serialize(test);
		
		ccoutput = (ContainingClass) translationScope.deserializeCharSequence(test);

		System.out.println(test);
		System.out.println(ccoutput.serialize());

		System.out.println();
		
		System.out.println("child2");
		test.delete(0, test.length());
		cc2.serialize(test);

		ccoutput = (ContainingClass) translationScope.deserializeCharSequence(test);

		System.out.println(test);
		System.out.println(ccoutput.serialize());
	}
}
