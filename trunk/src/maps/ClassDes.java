package maps;

import java.util.HashMap;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.types.element.Mappable;

public class ClassDes extends ElementState implements Mappable<String>
{
	@simpl_scalar
	public String														tagName;
	
	@simpl_nowrap
	@simpl_map("field_descriptor")
	public HashMap<String, FieldDes>	fieldDescriptorsByTagName;
	
	public ClassDes()
	{
		tagName = "";
		fieldDescriptorsByTagName = new HashMap<String, FieldDes>();
	}
	
	public ClassDes(String tagName)
	{
		this.tagName = tagName;
		fieldDescriptorsByTagName = new HashMap<String, FieldDes>();
	}

	@Override
	public String key()
	{
		return tagName;
	}
}
