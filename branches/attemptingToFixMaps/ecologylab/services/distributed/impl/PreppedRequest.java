/*
 * Created on Apr 4, 2007
 */
package ecologylab.services.distributed.impl;

/**
 * Represents a RequestMessage that has been translated to XML. This object encapsulates the XML String, along with the
 * request's UID.
 * 
 * @author Zachary O. Toups (toupsz@cs.tamu.edu)
 */
// FIXME -- Can we use StringBuilders in here directly to utilize memory better
public class PreppedRequest implements Comparable<PreppedRequest>
{
	private long				uid;

	private StringBuilder	request	= new StringBuilder();

	private boolean disposable = false;
	
	/**
	 * 
	 */
	public PreppedRequest(StringBuilder request, long uid)
	{
		this.uid = uid;

		this.setRequest(request);
	}

	/**
	 * 
	 */
	public PreppedRequest(StringBuilder request, long uid, boolean disposable)
	{
		this(request, uid);

		this.disposable = disposable;
	}

	
	/**
	 * @return the request
	 */
	public StringBuilder getRequest()
	{
		return request;
	}

	public void setRequest(StringBuilder request)
	{
		this.request.setLength(0);
		this.request.append(request);
	}

	/**
	 * @return the uid
	 */
	public long getUid()
	{
		return uid;
	}

	public int compareTo(PreppedRequest arg0)
	{
		return (int) (this.uid - arg0.getUid());
	}

	/**
	 * @param uid
	 *           the uid to set
	 */
	public void setUid(long uid)
	{
		this.uid = uid;
	}

	/**
	 * @return the disposable
	 */
	public boolean isDisposable()
	{
		return disposable;
	}

	/**
	 * @param disposable the disposable to set
	 */
	public void setDisposable(boolean disposable)
	{
		this.disposable = disposable;
	}
}