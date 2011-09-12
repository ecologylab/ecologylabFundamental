package test;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.ElementState.xml_other_tags;
import ecologylab.serialization.ElementState.xml_tag;

@xml_other_tags({"the_field"})
@xml_tag("fred")
public class BaseClass extends ElementState
{
	@xml_tag("new_tag_var")
	@xml_other_tags("other_tag_var")
	@simpl_scalar
	int var = 3;
	
	public BaseClass()
	{
	}
}
