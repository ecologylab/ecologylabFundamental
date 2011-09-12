package test2;

import ecologylab.serialization.ElementState.xml_tag;

@xml_tag("item_one")
public class ItemRandom extends ItemBase
{
	@simpl_scalar
	String randomString;
	
	public ItemRandom()
	{
		
	}
	
	public ItemRandom(String randomString, int var)
	{
		this.randomString = randomString;
		this.var = var;
	}
}
