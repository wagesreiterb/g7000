package g7000;
/*
 * g7000Chase.java
 *
 * Created on 18. November 2006, 15:28
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//package g7000_vdc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author Que
 */
public class Chase extends JFrame implements WindowListener
{
    private Panel g7p;
    
    public Chase()
    {
        super("Que's G7000 Emu");
        makeGUI();
        addWindowListener(this);
        pack();
        setResizable(false);
        setVisible(true);
        
        //Sound sound = new Sound();
        //sound.actionPerformed();
    }
    
    private void makeGUI()
    {
        Container c = getContentPane();
        
        g7p = new Panel(this);
        c.add(g7p, "Center");
    }
    
    // ----------------- window listener methods -------------
    public void windowActivated(WindowEvent e)
    {}
    //{ g7p.resumeGame();  }
    public void windowDeactivated(WindowEvent e) 
    {}
    //{  g7p.pauseGame();  }
    public void windowDeiconified(WindowEvent e) 
    {}
    //{  g7000Panel.resumeGame();  }
    public void windowIconified(WindowEvent e) 
    {}
    //{  g7000Panel.pauseGame(); }
    public void windowClosing(WindowEvent e)
    {
    System.exit(0);
    }
    //{  g7000Panel.stopGame();  }
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}

    // ----------------------------------------------------

}


