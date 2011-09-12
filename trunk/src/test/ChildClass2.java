package test;

import ecologylab.serialization.Hint;

public class ChildClass2 extends BaseClass
{
	@simpl_scalar @simpl_hints(Hint.XML_LEAF)
	int ccvar2 = 2;
	
	public ChildClass2()
	{
	}
	
}
