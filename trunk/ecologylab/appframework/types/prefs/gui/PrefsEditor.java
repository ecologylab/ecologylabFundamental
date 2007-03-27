package ecologylab.appframework.types.prefs.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import ecologylab.appframework.ApplicationEnvironment;
import ecologylab.appframework.types.prefs.MetaPref;
import ecologylab.appframework.types.prefs.MetaPrefSet;
import ecologylab.appframework.types.prefs.Pref;
import ecologylab.appframework.types.prefs.PrefSet;
import ecologylab.generic.Debug;
import ecologylab.net.ParsedURL;
import ecologylab.xml.XmlTranslationException;


public class PrefsEditor
extends Debug
{
    MetaPrefSet metaPrefSet;
    PrefSet     prefSet;
    ParsedURL   savePrefsPURL;
    
    // base setup for gui
    JFrame 		jFrame = null;
    JPanel 		jContentPane = null;
    JButton 	cancelButton = null;
    JButton 	saveButton = null;
    JButton 	revertButton = null;
    JTabbedPane jTabbedPane = null;
    
    boolean		isStandalone;
    
    public PrefsEditor(MetaPrefSet metaPrefSet, PrefSet prefSet, ParsedURL savePrefsPURL, final boolean isStandalone)
    {
        this.metaPrefSet 	= metaPrefSet;
        this.prefSet     	= prefSet;
        this.savePrefsPURL  = savePrefsPURL;
        this.isStandalone	= isStandalone;
        
        final JFrame jFrame = getJFrame();
        jFrame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
            	closeWindow();
            }
       });
        jFrame.setVisible(true);
    }
    
    public JFrame fetchJFrame()
    {
        return getJFrame();
    }

    // static bits of gui
    private JFrame getJFrame() 
    {
        if (jFrame == null) 
        {
            jFrame = new JFrame();
            jFrame.setPreferredSize(new Dimension(603, 532));
            jFrame.setSize(new Dimension(603, 532));
            jFrame.setTitle("combinFormation Preferences");
            jFrame.setContentPane(createJContentPane());
        }
        return jFrame;
    }

	private void closeWindow()
	{
		if (jFrame != null)
		{
			jFrame.setVisible(false);
	    	jFrame.dispose();
		}
    	if (PrefsEditor.this.isStandalone)
    		System.exit(0);
	}
	
    private JPanel createJContentPane() 
    {
        if (jContentPane == null) 
        {
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(createJTabbedPane(), null);
            jContentPane.add(createCancelButton(), null);
            jContentPane.add(createSaveButton(), null);
            jContentPane.add(createRevertButton(), null);
        }
        return jContentPane;
    }

    private JButton createCancelButton() 
    {
        if (cancelButton == null) 
        {
            cancelButton = new JButton();
            cancelButton.setBounds(new Rectangle(482, 435, 89, 35));
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(new ActionListener()
            		{
            			public void actionPerformed(ActionEvent e)
            			{
            				closeWindow();
            			}
	                });
        }
        return cancelButton;
    }

    private JButton createSaveButton() 
    {
        if (saveButton == null) 
        {
            saveButton = new JButton();
            saveButton.setBounds(new Rectangle(379, 435, 89, 35));
            saveButton.setText("Save");
            saveButton.addActionListener(new ActionListener() 
            		{
            			public void actionPerformed(ActionEvent e) 
            			{
            				actionSavePreferences();
            			}
            		});
        }
        return saveButton;
    }

    private JButton createRevertButton() 
    {
        if (revertButton == null) 
        {
            revertButton = new JButton();
            revertButton.setBounds(new Rectangle(15, 435, 137, 35));
            revertButton.setText("Revert to Default");
            revertButton.addActionListener(new ActionListener() 
            		{
            			public void actionPerformed(ActionEvent e) 
            			{
            				actionRevertPreferencesToDefault();
            			}
            		});
        }
        return revertButton;
    }
    // end of static bits of gui
    
    // bits of gui that are all or part auto-generated
    private JTabbedPane createJTabbedPane() 
    {
        if (jTabbedPane == null) 
        {
            jTabbedPane = new JTabbedPane();
            jTabbedPane.setName("jTabbedPane");
            jTabbedPane.setBounds(new Rectangle(0, 0, 595, 416));
            
            for (String cat : metaPrefSet.categoryToMetaPrefs.keySet())
            {
                JScrollPane scrollPane = new JScrollPane();
                scrollPane.setSize(new Dimension(jTabbedPane.getWidth(),jTabbedPane.getHeight()));
                scrollPane.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setName(cat);
                scrollPane.setViewportView(getTabbedBodyFrame(cat,scrollPane));
                jTabbedPane.addTab(cat, null, scrollPane, null);
            }
        }
        return jTabbedPane;
    }

    private JPanel getTabbedBodyFrame(String category, JScrollPane scrollPane)
    {
        JPanel contentPanel = new JPanel()
        {
            private boolean firstTime = true;
            public void paintComponent(Graphics g)
            {
                if (firstTime)
                {
                    firstTime = false;
                    int numberOfEntries = this.getComponentCount();
                    for (int i=0; i < numberOfEntries; i+=2)
                    {
                        // TODO: this only works because we alternate adding JLabels and JPanels
                        if (((JLabel)this.getComponent(i) instanceof JLabel) && ((JPanel)this.getComponent(i+1) instanceof JPanel))
                        {
                            JLabel desc = (JLabel)this.getComponent(i);
                            JPanel val  = (JPanel)this.getComponent(i+1);

                            FontMetrics fm = desc.getFontMetrics(desc.getFont());
                            int actualWidth = (this.getWidth()-val.getWidth());
                            int stringWidth = SwingUtilities.computeStringWidth(fm, desc.getText());
                            
                            desc.setPreferredSize(new Dimension(actualWidth,((stringWidth/actualWidth)+1)*fm.getHeight()));
                        }
                    }
                }
                super.paintComponent(g);
            }
        };
        /*contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.yellow),
                contentPanel.getBorder()));*/
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setMaximumSize(new Dimension(scrollPane.getWidth(),Integer.MAX_VALUE));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 0.1;
        int rowNum = 0;
        for (MetaPref metaPref : metaPrefSet.categoryToMetaPrefs.get(category))
        {
            JLabel subDescription = createDescriptionSection(contentPanel, constraints, rowNum, metaPref);
            JPanel subValue       = createValueSection(constraints, metaPref, rowNum);
            
            subValue.setMaximumSize(new Dimension(scrollPane.getWidth()/2,100));
            subDescription.setMaximumSize(new Dimension(scrollPane.getWidth()/2,50));

            // we have to do this in order for our redraw code to work properly.
            subDescription.setPreferredSize(new Dimension(1,1));

            //add these suckers to the contentpanel.
            constraints.anchor = GridBagConstraints.FIRST_LINE_START;
            contentPanel.add(subDescription, constraints);
            if (subValue != null)
            {
                constraints.anchor = GridBagConstraints.FIRST_LINE_END;
                contentPanel.add(subValue, constraints);
            }
            rowNum++;
        }
        
        return contentPanel;
    }

    private JLabel createDescriptionSection(JPanel contentPanel, GridBagConstraints constraints, int rowNum, MetaPref mp)
    {
        JLabel subDescription = mp.getLabel(contentPanel);
        /*subDescription.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.red),
                subDescription.getBorder()));*/
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.gridx = 0;
        constraints.gridy = rowNum;
        constraints.insets = new Insets(10,20,0,0); // top,left,bottom,right
        return subDescription;
    }
    
    private JPanel createValueSection(GridBagConstraints constraints, MetaPref metaPref, int rownum)
    {
        JPanel subValue = metaPref.jPanel;
        if (subValue != null)
        {
            constraints.gridx = 1;
            constraints.gridy = rownum;
            constraints.insets = new Insets(10,20,0,0); // top,left,bottom,right
//                subValue.setSize(new Dimension(250, subValue.HEIGHT));
            /*subValue.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.blue),
                    subValue.getBorder())); */
            // if we have a prefs value, override it now
            if (Pref.hasPref(metaPref.getID()))
            {
                metaPref.setWidgetToPrefValue(Pref.lookupPref(metaPref.getID()).value());
            }
        }
        return subValue;
    }
    // end of bits of gui that are all or part auto-generated
    
    
    // gui actions for buttons
    private void actionSavePreferences()
    {
        //debug("we pressed the save button");

    	/* we do this with metaprefs because we will always have
    	 * all metaprefs. we may not always have a prefs file to start
    	 * with. */
    	// this iterator organizes them by category
    	for (String cat : metaPrefSet.categoryToMetaPrefs.keySet())
    	{
    		for (MetaPref mp : metaPrefSet.categoryToMetaPrefs.get(cat))
    		{
    			// by casting here we get the proper return type
    			// for getPrefValue
    			String name = mp.getID();
    			//TODO -- i dont believe this lines makes sense -- andruid 3/12/07
    			//mp 			= mp.getClass().cast(mp);
    			Pref pref 	= mp.getAssociatedPref();
    			pref.setValue(mp.getPrefValue());
    			if (!prefSet.contains(pref))
    				prefSet.add(pref);
    			
    			//TODO -- this is not really needed because only the value has been changed. -- andruid 3/12/07
    			//prefSet.modifyPref(name,pref);
    			//prefSet.lookupPref(mp.getID()).print();
    		}
    	}
    	if (savePrefsPURL == null)
    	{
    		//TODO provide better feedback to the user here!!!
    		warning("Not saving Prefs persistently cause savePrefsURL == null.");
    	}
    	else
    	{
    		try
    		{
    			prefSet.saveXmlFile(savePrefsPURL.file(), true, false);
    		}
    		catch (XmlTranslationException e)
    		{
    			// TODO auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    }
    
    /**
     * The function that actually performs the revert-to-default actions
     * is in the MetaPrefType classes
     */
    private void actionRevertPreferencesToDefault()
    {
        for (String cat : metaPrefSet.categoryToMetaPrefs.keySet())
        {
            for (MetaPref mp : metaPrefSet.categoryToMetaPrefs.get(cat))
            {
                mp.revertToDefault();
            }
        }
    }
    // end of gui actions for buttons
}
