/**
 * 
 */
package ecologylab.services.logging;

import ecologylab.xml.ElementState;

/**
 * A user operation, which can be serialized, logged, Undo/Redo'ed, played in history,
 * and so on.
 * 
 * @author andruid
 */
abstract public class MixedInitiativeOp extends ElementState
{
	/**
	 * Elapsed time since the session started.
	 */
	public long		sessionTime;

	/**
	 * 
	 */
	public MixedInitiativeOp()
	{
		super();
		sessionTime	= System.currentTimeMillis() - Logging.time();		
	}

	/**
	 * Perform the op. Perhaps invert it, as for undo.
	 * 
	 * @param invert
	 */
	abstract public void performAction(boolean invert);
	
	   /**
	    * In a mixed initiative system, some ops are by the human, while others
	    * are by the agent.
	    * 
	    * The presence of this here at the moment may be a hack.
	    * It may be good design :-)
	    * Human and dyadic undo should probably be split into 2 separte UndoRedo classes.
	    * 
	    * @return
	    */
	   public boolean isHuman()
	   {
		   return true;
	   }

	/**
	 * Free resources associated with this.
	 *
	 */
	public void recycle(boolean invert)
	{
	
	}

}
