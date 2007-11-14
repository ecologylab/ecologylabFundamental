/**
 * 
 */
package ecologylab.appframework;

import ecologylab.generic.Debug;
import ecologylab.generic.DispatchTarget;
import ecologylab.generic.DownloadProcessor;
import ecologylab.generic.Downloadable;

/**
 * A simple download processor:
 * just download the Downloadable immediately.
 * 
 * @author andruid
 */
public class SimpleDownloadProcessor extends Debug implements DownloadProcessor
{
	/**
	 * 
	 */
	public SimpleDownloadProcessor()
	{
		super();
	}

	/* A no-op to conform to the interface spec.
	 * We have no threads to stop :-)
	 */
	public void stop()
	{
	}

/**
 * Download it now, in this thread.
 * 
 * @param	thatDownloadable	The thing to download.
 * @param	dispatchTarget		Ignored, since we are not asynchronous, there are no callbacks.
 */
	//TODO improve error handling here
	public void download(Downloadable thatDownloadable,
			DispatchTarget dispatchTarget)
	{
		try
		{
			thatDownloadable.performDownload();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}