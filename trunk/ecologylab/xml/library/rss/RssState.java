package ecologylab.xml.library.rss;

import java.util.ArrayList;

import java.io.File;

import ecologylab.net.ParsedURL;
import ecologylab.xml.*;

/**
 * {@link ecologylab.xml.ElementState ElementState} for the root element of the RSS parser.
 * In particular, this supports RSS versions such as .91, .92, .93, .94, and 2.0.
 *
 * @author andruid
 */
public class RssState extends ElementState
{
	@xml_attribute	float		version;
   
	@xml_nested		Channel		channel;

	/**
	 * @return Returns the channel.
	 */
	public Channel getChannel()
	{
		return channel;
	}

	/**
	 * @param channel The channel to set.
	 */
	public void setChannel(Channel channel)
	{
		this.channel = channel;
	}

	/**
	 * @return Returns the version.
	 */
	public float getVersion()
	{
		return version;
	}

	/**
	 * @param version The version to set.
	 */
	public void setVersion(float version)
	{
		this.version = version;
	}
	
	public static final ParsedURL NYT_TECH_FEED	= ParsedURL.getAbsolute("http://www.nytimes.com/services/xml/rss/nyt/Technology.xml");
	public static final ParsedURL CNN_TOP_FEED	= ParsedURL.getAbsolute("http://rss.cnn.com/rss/cnn_topstories.rss");
	
	public static final ParsedURL BBC_FRONT_FEED	= ParsedURL.getAbsolute("http://news.bbc.co.uk/rss/newsonline_world_edition/front_page/rss.xml");
	
	public static final File 	outputFile			= new File("/temp/rss.xml");
	public static void main(String[] args)
	{
		try
		{
			ParsedURL feedPURL	= BBC_FRONT_FEED;
			println("Translating RSS feed: " + feedPURL+"\n");

//			RssState rssState	= (RssState) ElementState.translateFromXMLSAX(feedPURL, RssTranslations.get());
			RssState rssState	= (RssState) ElementState.translateFromXML(feedPURL, RssTranslations.get());

			ArrayList<Item> items	= rssState.getChannel().set(); //rssState.getChannel().getItems();
			//println("items: " +  .size());
			for (Item item : items)
			{
				println(item.description);
			}

			StringBuilder retranslated	= rssState.translateToXML();
			println(retranslated);
			println("\n");
			RssState rssState2	= (RssState) ElementState.translateFromXMLCharSequence(retranslated, RssTranslations.get());
			rssState2.translateToXML(System.out);

			rssState.writePrettyXML(outputFile);
		} catch (XMLTranslationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
