package ecologylab.generic;

public abstract class CappedResourcePool<T> extends ResourcePool<T>
{
	private final int			maximumSize;

	protected CappedResourcePool(boolean instantiateResourcesInPool, int initialPoolSize,
			int minimumPoolSize, int maximumSize, boolean checkMultiRelease)
	{
		super(instantiateResourcesInPool, initialPoolSize, minimumPoolSize, checkMultiRelease);
		this.maximumSize = maximumSize;
	}

	@Override
	protected synchronized void onRelease(T resourceToRelease)
	{
		this.notify();
	}

	/**
	 * Determines if this pool can produce a resource or not. This method is not synchronized, but
	 * might be used in contexts where it is better to fail fast.
	 * 
	 * @return
	 */
	public boolean canAcquire()
	{
		return !(this.getPoolSize() == 0 && this.getCapacity() * 2 > maximumSize);
	}

	@Override
	protected synchronized void onAcquire()
	{
		while (!canAcquire())
		{
			Debug.println("Waiting for pool to free up!");
			try
			{
				this.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

	}
}
