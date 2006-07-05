package ecologylab.net;

import java.io.File;
import java.net.URL;

import ecologylab.generic.Debug;

/**
 * Adapter class to make it easier to use ParsedURL.connect().
 * The only method that you need to implement is 
 * parseFilesWithSuffix(String suffix).
 *
 * @author andruid
 */
public abstract class ConnectionAdapter extends Debug
implements ConnectionHelper
{
	/**
	 * When this method is called, you know the file is a directory.
	 * This implementation does nothing.
	 * connect() will return null in this special case.
	 * 
	 * @param file
	 */
	public void handleFileDirectory(File file)
	{
	}
	
	/**
	 * Tells the connect() method that it should go ahead and create a PURLConnection
	 * for files that have this suffix.
	 * 
	 * An implementation (usually using a HashMap) must be provided in order
	 * for connect() to work.
	 * 
	 * @param suffix
	 * @return
	 */
	abstract public boolean parseFilesWithSuffix(String suffix);
	
	/**
	 * Used to provid status feedback to the user.
	 * The default implementation prints the message to the console.
	 * 
	 * @param message
	 */
	public void displayStatus(String message)
	{
		println(message);
	}
	
	/**
	 * Shuffle referential models when a redirect is observed, if you like.
	 * The default implementation allows all re-directs and keeps track of nothing.
	 * 
	 * @param connectionURL
	 * 
	 * @return		true if the redirect is o.k., and we should continue processing the connect().
	 * 				false if the redirect is unacceptable, and we should terminate processing.
	 * @throws Exception 
	 */
	public boolean processRedirect(URL connectionURL) throws Exception
	{
		return true;
	}
}
