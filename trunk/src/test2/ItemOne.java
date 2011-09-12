package test2;

import ecologylab.serialization.simpl_inherit;

@simpl_inherit
public class ItemOne extends ItemBase
{
	@simpl_scalar
	int testing;
	
	
	public ItemOne()
	{
		
	}
	
	public ItemOne(int testing, int var)
	{
		this.testing = testing;
		this.var = var;
	}
}
