package ecologylab.standalone.researchnotebook.compositionTS;

import java.util.ArrayList;

import ecologylab.serialization.ElementState;

public class Paragraphs extends ElementState{
	@simpl_nowrap @simpl_collection("paragraph") ArrayList<Paragraph>	paragraph = new ArrayList<Paragraph>(); 

}
