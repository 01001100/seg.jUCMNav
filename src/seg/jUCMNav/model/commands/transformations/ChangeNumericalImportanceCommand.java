package seg.jUCMNav.model.commands.transformations;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import grl.IntentionalElementRef;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.widgets.Shell;

import seg.jUCMNav.model.commands.JUCMNavCommand;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.views.wizards.IntegerInputRangeDialog;

/**
 *
 * @author Andrew Miga
 */

public class ChangeNumericalImportanceCommand extends Command implements JUCMNavCommand
{
	private int operation;
    private boolean cancelled = false;
	private boolean removed = false;
	private CommandStack commandStack;

	private final static int[] values = { 100, 75, 50, 25, 0 };

	public final static int USER_ENTRY = values.length;
	public final static int INCREASE = values.length + 1;
	public final static int DECREASE = values.length + 2;
	
	private class IElementState
	{
		IntentionalElementRef intElemRef;
		int oldValue, newValue;
	}
	
	Vector intElementStates;

	public ChangeNumericalImportanceCommand( List intElemRefs, int id, CommandStack stack )
	{
		int enteredImportance = 0;
		operation = id;
		commandStack = stack;

		if ( id == USER_ENTRY ) { // enter value through dialog
    	    String currentValue = ( intElemRefs.size() > 1 ) ? "" : Integer.toString( ((IntentionalElementRef) (intElemRefs.get(0))).getDef().getImportanceQuantitative() );
    		Integer enteredValue = enterImportance( currentValue );
    		if ( enteredValue == null ) {
    			cancelled = true;
    			return;
    		}
    		else
    			enteredImportance = enteredValue.intValue();        		
    	}
    	
		intElementStates = new Vector();
		
		for ( Iterator iter = intElemRefs.iterator(); iter.hasNext(); ) {
			
			IntentionalElementRef currentIERef = (IntentionalElementRef) iter.next();
			IElementState ies = new IElementState();
			
			ies.intElemRef = currentIERef;
			ies.oldValue = currentIERef.getDef().getImportanceQuantitative();
			intElementStates.add( ies );

			if ( id <= 4 ) { // input from sub-menu +100 -> 0
				ies.newValue = values[ id ];
			} else if ( id == USER_ENTRY ){ // new value entered through dialog
				ies.newValue = enteredImportance;
			} else if ( id == INCREASE ) { // increase evaluation if possible
    		
				if ( ies.oldValue == 100 )
					cancelled = true; // can't increase from 100
				else
					ies.newValue = ies.oldValue + 1;
    		
			} else if ( id == DECREASE ) { // decrease evaluation if possible

				if ( ies.oldValue == 0 )
					cancelled = true; // can't decrease from 0
				else
					ies.newValue = ies.oldValue - 1;
			}
		}
		
		if ( !cancelled && ((id == INCREASE) || (id == DECREASE)) )
		{ // valid increase or decrease operation, check if undo merging is needed	
			if ( commandStack.getUndoCommand() instanceof ChangeNumericalImportanceCommand )
			{		
				ChangeNumericalImportanceCommand prevCommand = (ChangeNumericalImportanceCommand) commandStack.getUndoCommand(); 

				if ( prevCommand.isRepeatedIncDecOperation( intElemRefs, id ) )
				{ // merge undo operations, overwrite new "old values" with those of previous operation 		
					for( Iterator iter = prevCommand.prevElementStates().iterator(); iter.hasNext(); ){
						IElementState pes = (IElementState) iter.next();
						for( Iterator iter2 = intElementStates.iterator(); iter2.hasNext(); ){
							IElementState ces = (IElementState) iter2.next();
							if ( ces.intElemRef == pes.intElemRef )
								ces.oldValue = pes.oldValue;
						}	
					}
					// remove unneeded previous command from undo stack  
					prevCommand.SetRemoved(); // set flag in previous command object so undo does nothing
					commandStack.undo(); // pop previous command from undo stack and do nothing, workaround due to Eclipse GEF API shortcomings
				}
			}
		}    			
		
    }
	
	public void execute()
	{
		if ( cancelled )
			cancelled = false;
		else
			redo();
	}

	public void redo()
	{
		testPreConditions();

		for ( Iterator iter = intElementStates.iterator(); iter.hasNext(); ) {
			IElementState ies = (IElementState) iter.next();
			EvaluationStrategyManager.getInstance().setIntentionalElementQuantitativeImportance( ies.intElemRef.getDef(), ies.newValue );
		}
		
		testPostConditions();
	}

	public void testPostConditions()
	{
		for ( Iterator iter = intElementStates.iterator(); iter.hasNext(); ) {
			IElementState ies = (IElementState) iter.next();
			assert ies.intElemRef != null : "post no element!"; //$NON-NLS-1$
		}
	}

	public void testPreConditions()
	{
		for ( Iterator iter = intElementStates.iterator(); iter.hasNext(); ) {
			IElementState ies = (IElementState) iter.next();
			assert ies.intElemRef != null : "pre no element!"; //$NON-NLS-1$
		}
	}

	public void undo()
	{
		testPostConditions();

		if ( removed ) return; // used in undo merging

		for ( Iterator iter = intElementStates.iterator(); iter.hasNext(); ) {
			IElementState ies = (IElementState) iter.next();
			EvaluationStrategyManager.getInstance().setIntentionalElementQuantitativeImportance( ies.intElemRef.getDef(), ies.oldValue );
		}
		
		testPreConditions();
	}

	private Integer enterImportance( String currentValue )
	{	
		Shell shell = new Shell();
	    IntegerInputRangeDialog dialog = new IntegerInputRangeDialog(shell);
	    	
	    return ( dialog.open( "Enter Numerical Importance   (range: [0, 100])", "Enter the new Numerical Importance: ", currentValue, 0, 100 ) );		
	}
	
	public boolean isRepeatedIncDecOperation( List intElemRefs, int operation )
	{
		if ( this.operation != operation )
			return false;
		
		if ( intElemRefs.size() != intElementStates.size() )
			return false;
		
		for ( Iterator iter = intElementStates.iterator(); iter.hasNext(); ) {
			IElementState ies = (IElementState) iter.next();
			if ( !intElemRefs.contains( ies.intElemRef ) )
				return false;
		}
		
		return true;
	}
		
	public List prevElementStates()
	{
		return intElementStates;
	}
	
	public void SetRemoved()
	{
		removed = true;
	}

}