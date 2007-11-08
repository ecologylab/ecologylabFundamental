package ecologylab.services.logging;

import java.util.Date;

import ecologylab.io.Files;
import ecologylab.net.NetTools;
import ecologylab.xml.XmlTools;
import ecologylab.xml.XmlTranslationException;
import ecologylab.xml.xml_inherit;

/**
 * Allows the application to send application-specific content to the log, at the beginning of a session.
 * <p/>
 * NB: this class should *never* be extended in an application specific way, because the LoggingServer should never
 * need to know the TranslationSpace for such a super class.
 * What you do extend is the {@link Prologue Prologue} object.
 * 
 * @author andruid
 * @author eunyee
 */
@xml_inherit
public class SendPrologue extends LogueMessage
{
	@xml_attribute protected String	date					= new Date(System.currentTimeMillis()).toString();
	
	@xml_attribute protected String	ip						= NetTools.localHost();
	
	@xml_attribute protected String	userID					= "0";
	
	@xml_attribute protected String studyName;

	public SendPrologue(Logging logging, Prologue prologue)
	{
		super(logging);
		this.date		= prologue.date;
		this.ip			= prologue.ip;
		this.userID		= prologue.userID;
		this.studyName  = prologue.getStudyName();
		try
		{
			bufferToLog		= prologue.translateToXML((StringBuilder) null);
			bufferToLog.insert(0, beginLog());
			bufferToLog.append(Logging.OP_SEQUENCE_START);
		} catch (XmlTranslationException e)
		{
			e.printStackTrace();
		}
	}

	public SendPrologue()
	{
		super();
	}

	/**
	 * Stuff to write to the log file based on the contents of this message.
	 * 
	 * @return	The end of the op_sequence element, the epilogue, and the end of the log.
	 */
//	@Override
//	protected StringBuilder bufferToLog()
//	{
//		try
//		{
//			StringBuilder buffy	= prologue.translateToXML((StringBuilder) null);
//			buffy.insert(0, beginLog());
//			buffy.append(Logging.OP_SEQUENCE_START);
//			return buffy;
//		} catch (XmlTranslationException e) 
//		{
//			e.printStackTrace();
//			return null;
//		}
//	}

	public String getFileName()
	{
		String tempDate = date.replace(' ','_');
		tempDate = tempDate.replace(':', '_');
		/**
		 * A session log file name of a user
		 */
		String sessionLogFile	=	// "/project/ecologylab/studyResults/CF_LOG/" + 
				//	"LogFiles/" + 
					ip + "__" + tempDate + "_" + userID + ".xml";
		String studyName = getStudyName();
		if (studyName != null)
			sessionLogFile		= studyName + Files.sep + sessionLogFile;
		return sessionLogFile;
	}
 	public String beginLog()
 	{
 		return XmlTools.xmlHeader() + "\n<" + logName() + ">\n\n";
 	}
 	
 	public String getStudyName()
 	{
 		return studyName;
 	}
	
}
