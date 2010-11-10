/*
 * Created on Dec 12, 2006
 */
package ecologylab.serialization.library.endnote;

import ecologylab.serialization.ElementState;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.enums.Hint;

public @simpl_inherit
class Author extends ElementState
{
	@simpl_scalar @simpl_hints(Hint.XML_TEXT)
	String	authorName;

	public Author()
	{

	}

	/**
	 * @return the authorName
	 */
	public String getAuthorName()
	{
		return authorName;
	}
}
