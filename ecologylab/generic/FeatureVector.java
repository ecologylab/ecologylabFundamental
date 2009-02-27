package ecologylab.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

public class FeatureVector<T> extends Observable implements IFeatureVector<T>
{

	protected HashMap<T, Double>	values;

	private double					norm, max;

	public FeatureVector ()
	{
		values = new HashMap<T, Double>(20);
	}

	public FeatureVector ( int size )
	{
		values = new HashMap<T, Double>(size);
	}

	public FeatureVector ( IFeatureVector<T> copyMe )
	{
		values = new HashMap<T, Double>(copyMe.map());
		norm = copyMe.norm();
	}
	
	protected void reset() 
	{
		values = new HashMap<T, Double>();
		resetNorm();
	}

	public FeatureVector<T> copy ( )
	{
		return new FeatureVector<T>(this);
	}

	public double get ( T term )
	{
		Double d = values.get(term);
		if (d == null)
			return 0;
		return d;
	}

	public void add ( T term, double val )
	{
		synchronized (values)
		{
			if (values.containsKey(term))
				val += values.get(term);
			values.put(term, val);
			resetNorm();
		}
	}

	public void set ( T term, double val )
	{
		synchronized (values)
		{
			values.put(term, val);
			resetNorm();
		}
	}

	/**
	 * Pairwise multiplies this Vector by another Vector, in-place.
	 * 
	 * @param v
	 *            Vector by which to multiply
	 */
	public void multiply ( IFeatureVector<T> v )
	{
		HashMap<T, Double> other = v.map();
		if (other == null)
			return;
		synchronized (values)
		{
			this.values.keySet().retainAll(other.keySet());
			for (T term : this.values.keySet())
				this.values.put(term, other.get(term) * this.values.get(term));
		}
		resetNorm();
	}

	/**
	 * Scalar multiplication of this vector by some constant
	 * 
	 * @param c
	 *            Constant to multiply this vector by.
	 */
	public void multiply ( double c )
	{
		synchronized (values)
		{
			ArrayList<T> terms_to_delete = new ArrayList<T>();
			for (T term : this.values.keySet())
			{
				double new_val = c * this.values.get(term);
				if (Math.abs(new_val) < 0.001)
					terms_to_delete.add(term);
				else
					this.values.put(term, new_val);
			}
			for (T t : terms_to_delete)
				values.remove(t);
		}
		resetNorm();
	}

	/**
	 * Pairwise addition of this vector by some other vector times some constant.<br>
	 * i.e. this + (c*v)<br>
	 * Vector v is not modified.
	 * 
	 * @param c
	 *            Constant which Vector v is multiplied by.
	 * @param v
	 *            Vector to add to this one
	 */
	public void add ( double c, IFeatureVector<T> v )
	{
		HashMap<T, Double> other = v.map();
		if (other == null)
			return;
		synchronized (other)
		{
			synchronized (values)
			{
				for (T term : other.keySet())
					if (this.values.containsKey(term))
						this.values.put(term, c * other.get(term) + this.values.get(term));
					else
						this.values.put(term, c * other.get(term));
			}
		}
		resetNorm();
	}

	/**
	 * Adds another Vector to this Vector, in-place.
	 * 
	 * @param v
	 *            Vector to add to this
	 */
	public void add ( IFeatureVector<T> v )
	{
		add(1, v);
	}

	public double dot ( IFeatureVector<T> v )
	{
		return dot(v, false);
	}

	public double dot ( IFeatureVector<T> v, boolean simplex )
	{
		HashMap<T, Double> other = v.map();
		if (other == null || v.norm() == 0 || this.norm() == 0)
			return 0;

		double dot = 0;
		HashMap<T, Double> vector = this.values;
		synchronized (values)
		{
			for (T term : vector.keySet())
				if (other.containsKey(term))
					if (simplex)
						dot += vector.get(term);
					else
						dot += vector.get(term) + other.get(term);
		}
		return dot;
	}

	public Set<T> elements ( )
	{
		return new HashSet<T>(values.keySet());
	}

	public Set<Double> values ( )
	{
		return new HashSet<Double>(values.values());
	}

	public HashMap<T, Double> map ( )
	{
		return values;
	}

	public int size ( )
	{
		return values.size();
	}

	private void recalculateNorm ( )
	{
		double norm = 0;
		for (double d : this.values.values())
		{
			norm += Math.pow(d, 2);
		}
		this.norm = Math.sqrt(norm);
	}

	private void resetNorm ( )
	{
		norm = -1;
		max = -1;
	}

	public double norm ( )
	{
		if (norm == -1)
			recalculateNorm();
		return norm;
	}

	public double max ( )
	{
		if (max == -1)
			recalculateMax();
		return max;
	}

	private void recalculateMax ( )
	{
		for (Double d : values.values())
			if (Math.abs(d) > max)
				max = d;
	}

	/**
	 * Linearly scales the vector such that the max value in the vector is no more than the passed
	 * in value.<br/>
	 * <br/>
	 * Pivots around zero so that negative values with a magnitude greater than the clamp will be
	 * scaled appropriately.
	 * 
	 * @param clampTo
	 *            The new max value of the vector.
	 */
	public void clamp ( double clampTo )
	{
		double max = 0;
		clampTo = Math.abs(clampTo);
		synchronized (values)
		{
			for (Double d : values.values())
			{
				double d2 = Math.abs(d);
				if (d2 > max)
					max = d2;
			}
			if (!(max > clampTo))
				return;

			synchronized (values)
			{
				for (T term : this.values.keySet())
				{
					double old_value = this.values.get(term);
					double new_value = clampTo * old_value / max;
					this.values.put(term, new_value);
				}
			}
			resetNorm();
		}
	}

	public void clampExp ( double clampTo )
	{
		clampTo = Math.abs(clampTo);
		double max = 0;
		synchronized (values)
		{
			for (Double d : values.values())
			{
				double d2 = Math.abs(d);
				if (d2 > max)
					max = d2;
			}
			if (!(max > clampTo))
				return;
			
			ArrayList<T> terms_to_delete = new ArrayList<T>();
			for (T term : this.values.keySet())
			{
				double old_value = this.values.get(term);
				double new_value = clampTo * old_value / max;
				if (Math.abs(new_value) < 0.001)
					terms_to_delete.add(term);
				else
					this.values.put(term, new_value);
			}
			for (T t : terms_to_delete)
				values.remove(t);
		}
		resetNorm();
	}

	public IFeatureVector<T> unit ( )
	{
		FeatureVector<T> v = new FeatureVector<T>(this);
		v.clamp(1);
		return v;
	}

	public double dotSimplex ( IFeatureVector<T> v )
	{
		return dot(v, true);
	}

	public int commonDimensions ( IFeatureVector<T> v )
	{
		Set<T> v_elements = v.elements();
		v_elements.retainAll(this.elements());
		return v_elements.size();
	}

	public FeatureVector<T> simplex ( )
	{
		FeatureVector<T> v = new FeatureVector<T>(this);
		
		for (T t : v.values.keySet())
		{
			v.values.put(t, 1.0);
		}
		return v;
	}
}