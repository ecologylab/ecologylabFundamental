//Item.java

package ecologylab.tutorials.rss;

import java.util.ArrayList;
import ecologylab.net.ParsedURL;
import ecologylab.xml.ElementState;

public class Item extends ElementState
{
   @xml_leaf String			title;
   @xml_leaf String			description;
   @xml_leaf ParsedURL		link;
   @xml_leaf ParsedURL		guid;
   @xml_leaf String			author;
   
   @xml_nowrap
   @xml_collection("category") 
   ArrayList<String>		categorySet;
   
   public Item() {}
}
