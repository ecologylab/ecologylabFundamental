/**
 * 
 */
package test;

import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.TranslationScope;
import ecologylab.serialization.library.jnlp.information.Description;

/**
 * @author Zachary O. Toups (zach@ecologylab.net)
 *
 */
public class TestXMLTextHint
{
  public static void main(String[] args) throws SIMPLTranslationException
  {
  	String descChunk = "<description kind=\"short\">A demo of the capabilities of the Swing Graphical User Interface.</description>";
  	
  	TranslationScope translationScope = TranslationScope.get("JNLPDesc", Description.class);
		Description descObj = (Description) translationScope.deserializeCharSequence(descChunk);
  	
  	System.out.println(descObj.getDesc());
  	System.out.println(descChunk);
  	descObj.serialize(System.out);
  }
}