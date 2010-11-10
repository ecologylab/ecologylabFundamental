package ecologylab.standalone.researchnotebook.testxml;

import java.util.ArrayList;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.ElementState.xml_tag;
import ecologylab.serialization.annotations.simpl_inherit;

@simpl_inherit

public class Query extends ElementState{
	@simpl_nowrap
	@simpl_collection("SearchTerms") ArrayList<String> terms = new ArrayList<String>();  

}
