/**
 * 
 */
package ecologylab.serialization.library.jnlp.application;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import ecologylab.appframework.types.prefs.PrefSet;
import ecologylab.serialization.ElementState;
import ecologylab.serialization.exception.SIMPLTranslationException;

/**
 * @author Zachary O. Toups (zach@ecologylab.net)
 * 
 */
public class ApplicationDesc extends ElementState
{
	@simpl_scalar
	@xml_tag("main-class")
	String						mainClass;

	@simpl_nowrap
	@simpl_collection("argument")
	ArrayList<String>	arguments							= new ArrayList<String>();

	/**
	 * The PrefSet that is to be passed as an argument through the JNLP file. This object is
	 * serialized separately and added to the list of arguments at serialization time. After the JNLP
	 * file has been serialized, it is removed from the arguments. It may be safely modified between
	 * serializations to provide different sets of prefs.
	 */
	private PrefSet		prefSet;

	private int				prefSetArgumentIndex	= -1;

	/**
	 * 
	 */
	public ApplicationDesc()
	{
		super();
	}

	/**
	 * Add the argument to the JNLP application description.
	 * 
	 * @param argument
	 */
	public void add(String argument)
	{
		this.arguments.add(argument);
	}

	public ArrayList<String> getArguments()
	{
		return arguments;
	}

	/**
	 * @return the prefSet
	 */
	public PrefSet getPrefSet()
	{
		return prefSet;
	}

	/**
	 * @param prefSet
	 *          the prefSet to set
	 */
	public void setPrefSet(PrefSet prefSet)
	{
		this.prefSet = prefSet;
	}

	/**
	 * @see ecologylab.serialization.ElementState#serializationPreHook()
	 */
	@Override
	protected void serializationPreHook()
	{
		if (prefSet != null)
		{
			try
			{
				this.add(URLEncoder.encode(prefSet.serialize().toString(), "UTF-8"));

				this.prefSetArgumentIndex = this.arguments.size()-1;
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch (SIMPLTranslationException e)
			{
				e.printStackTrace();
			}
		}

		super.serializationPreHook();
	}

	/**
	 * @see ecologylab.serialization.ElementState#serializationPostHook()
	 */
	@Override
	protected void serializationPostHook()
	{
		if (this.prefSetArgumentIndex > -1)
		{ // we need to remove it from the arguments list
			this.arguments.remove(this.prefSetArgumentIndex);

			this.prefSetArgumentIndex = -1;
		}

		super.serializationPostHook();
	}
}
