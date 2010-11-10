package ecologylab.standalone.researchnotebook.compositionTS;

import ecologylab.net.ParsedURL;
import ecologylab.serialization.ElementState;
import ecologylab.serialization.enums.Hint;

public class Document extends ElementState{
	@simpl_scalar ParsedURL location; 
	@simpl_scalar String title; 
	@simpl_scalar String page_structure; 
	
	@simpl_scalar @simpl_hints(Hint.XML_LEAF) String query; 
	
}
