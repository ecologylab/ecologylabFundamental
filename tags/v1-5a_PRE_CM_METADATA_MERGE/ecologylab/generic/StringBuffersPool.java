package cm.generic;


import java.util.*;

/**
 * A pool of reusable {@link java.lang.StringBuffer StringBuffer}s.
 */
public class StringBuffersPool
extends Debug
{
	public static int DEFAULT_POOL_SIZE	=	64;
	public Vector bufferPool;
	
	int	bufferSize;
	int	poolSize;
	
	public StringBuffersPool(int bufferSize)
	{
	   this(bufferSize, DEFAULT_POOL_SIZE);
	}
	public StringBuffersPool(int bufferSize, int poolSize)
	{
	   this.bufferSize	= bufferSize;
	   bufferPool		= new Vector(poolSize);
	   for(int i = 0 ; i < poolSize; i++)
	   {
	      bufferPool.add(new StringBuffer(bufferSize));
	   }	
	}
	
	public StringBuffer nextBuffer()
	{
	   synchronized (bufferPool)
	   {
	      int freeIndex = bufferPool.size() - 1;
	      if (freeIndex == -1)
	      {
		 debug("extending pool??!");
		 return (new StringBuffer(bufferSize));
	      }
	      StringBuffer b = (StringBuffer) bufferPool.remove(freeIndex);
	      return b;
	   }
	}
	
	public void release(StringBuffer b)
	{
//	   b.setLength(0); // UGH! actually reallocates due to Sun stupidity!
	   b.delete(0, b.length());
	   bufferPool.add(b);		
	}
}