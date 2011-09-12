package maps;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.types.element.Mappable;

public class FieldDes extends ElementState implements Mappable<String>
{
	@simpl_scalar
	public String	fieldName;
	
	public FieldDes()
	{
		fieldName = "";
	}
	
	public FieldDes(String fieldName)
	{
		this.fieldName = fieldName;
	}

	@Override
	public String key()
	{
		return this.fieldName;
	}
}
