package ecologylab.services.logging;

import java.util.Date;

import ecologylab.generic.NetTools;
import ecologylab.xml.ElementState;
import ecologylab.xml.XmlTranslationException;
import ecologylab.xml.XmlTools;


/**
 * request message for the Logging server to open new log file
 * and write the header.
 * 
 * @author eunyee
 */
public class Prologue extends ElementState
{
	public String	date					= new Date(System.currentTimeMillis()).toString();
	
	public String	ip						= NetTools.localHost();
	
	public int 		userID					= 0;
	
	public Prologue()
	{
		super();
	}
	
	public void setUserID(int id)
	{
		this.userID = id;
	}
}