/*
 * Created on Dec 12, 2006
 */
package ecologylab.serialization.library.endnote;

import ecologylab.serialization.TranslationScope;
import ecologylab.serialization.exception.SIMPLTranslationException;

public class TestEndnoteXML
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            XmlState fromFile = (XmlState) EndnoteNameSpace.get().deserialize("/Users/toupsz/Desktop/RSBib.xml");
            
            System.out.println(fromFile.serialize());
        }
        catch (SIMPLTranslationException e)
        {
            e.printStackTrace();
        }
    }

}
