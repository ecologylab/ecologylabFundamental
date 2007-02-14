/**
 * 
 */
package ecologylab.appframework.types.prefs;

import java.awt.Rectangle;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import ecologylab.appframework.types.prefs.MetaPref;
import ecologylab.xml.xml_inherit;

/**
 * Metadata about an Integer Preference.
 * Defines information to enable editing the Preference.
 * 
 * @author andruid
 *
 */
@xml_inherit
public class MetaPrefInt extends MetaPref<Integer>
{
	@xml_attribute	int		defaultValue;
	
	/**
	 * 
	 */
	public MetaPrefInt()
	{
		super();
	}
	
    public Integer getDefaultValue()
	{
		return defaultValue;
	}

    public @Override
    void revertToDefault()
    {
        JTextField textField = (JTextField)lookupComponent(this.id+"textField");
        textField.setText(this.getDefaultValue().toString());
    }

    public @Override
    JPanel getWidget()
    {
        JPanel panel = new JPanel();
        
        this.createLabel(panel);
        // TODO: widget here needs to check what type of thing we are actually creating
        // ie, metapref.widget (this also needs to be done in other metapref type classes)
        if (widgetIsTextField())
        {
            this.createTextField(panel,this.getDefaultValue().toString(),"textField");
        }
        else if (widgetIsRadio())
        {
            // we know here that if we are a radio, we are a mutex of 3 or more
            // because otherwise we would be a bool.
            if (choices != null)
            {
                ButtonGroup buttonGroup = new ButtonGroup();
                for (Choice choice : choices)
                {
                    // TODO: there's a better way to do this than in an if-else
                    if (this.getDefaultValue().equals(choice.getValue()))
                        this.createRadio(panel, buttonGroup, true, choice.getLabel(), choice.getName(), 405);
                    else
                        this.createRadio(panel, buttonGroup, false, choice.getLabel(), choice.getName(), 405);
                }
            }
        }
        // TODO: drop-down list
        
        panel.setSize(new java.awt.Dimension(586,35));
        panel.setLayout(null);
        panel.setVisible(true);
        
        return panel;
    }
	
/*
	public boolean isWithinRange(Integer newValue)
	{
		return (range == null) ? true :  range.isWithinRange(newValue);
	}
	*/
}