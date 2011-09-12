package maps;

import java.util.HashMap;

import ecologylab.serialization.ElementState;

public class TranslationS extends ElementState
{
	@simpl_nowrap
	@simpl_map("class_descriptor")
	public HashMap<String, ClassDes>	entriesByTag;
	
	public TranslationS()
	{
		entriesByTag = new HashMap<String, ClassDes>();
	}

}
