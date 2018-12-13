package g7000;
/*
 * g7000Panel.java
 *
 * Created on 18. November 2006, 17:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//package g7000_vdc;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

/**
 *
 * @author Que
 */
public class Panel extends JPanel// implements Runnable
{
    private static final int PWIDTH = 640;
    private static final int PHEIGHT = 480;
    //private static final int PWIDTH = 512;
    //private static final int PHEIGHT = 256;

    
    private Chase g7cTop;
    //private VDC vdc;
    
    private Graphics dbg;
    private Image dbImage = null;

    
    VirtualMachine virtualMachine;
    
    private int charPositionPointer = 0;

    
    public Panel(Chase g7c)
    {
        g7cTop = g7c;
        setBackground(Color.BLUE);
        setPreferredSize(new Dimension(PWIDTH, PHEIGHT));
        setFocusable(true);
        requestFocus();
        //keyboard();
        
        //vdc = new VDC(g7cTop);
    }

    
    public void paintScreen()
    {
        Graphics g;
        try
        {
            g = this.getGraphics();
            if ((g != null) && (dbImage != null))
                g.drawImage(dbImage,0,0,null);
            g.dispose();
        }
        catch(Exception e)
        { System.out.println("Graphics context error: " + e); }
        
        dbImage.flush();
    }
    
    public void gameRender(VDC vdc, CPU cpu)
    {
        if (dbImage == null){
          dbImage = createImage(PWIDTH, PHEIGHT);
          if (dbImage == null) {
            System.out.println("dbImage is null");
            return;
          }
          else
          {
            System.out.println("create Image");
            dbg = dbImage.getGraphics();
          }
          
        }

        // clear the background
        dbg.setColor(Color.BLACK);
        dbg.fillRect (0, 0, PWIDTH, PHEIGHT);
        
        vdc.draw(dbg, charPositionPointer, cpu);
    }
    
    public void addNotify()
    // wait for the JPanel to be added to the JFrame before starting
    {
        super.addNotify();   // creates the peer
        //startGame();         // start the thread
        virtualMachine = new VirtualMachine(g7cTop, this);
        virtualMachine.startVirtualMachine();
    }

}
