package test2;

import ecologylab.serialization.simpl_inherit;

@simpl_inherit
public class ItemTwo extends ItemBase
{
	@simpl_scalar
	String testString;
	
	public ItemTwo()
	{
		
	}
	
	public ItemTwo(String testString, int var)
	{
		this.testString = testString;
		this.var = var;
	}
}
