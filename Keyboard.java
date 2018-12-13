/*
 * Keyboard.java
 *
 * Created on December 4, 2006, 3:34 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package g7000;

import java.awt.event.*;
/**
 *
 * @author wagesrei
 */
public class Keyboard {
    
    //private static final String[][] keyboard = {{"0", "8", "+", "Q", "A", "-"  },
    //                                            {"1", "9", "W", "S", "Z", "x"  },
    //                                            {"2", " ", "E", "D", "X", "/"  },
    //                                            {"3", " ", "R", "F", "C", "="  },
    //                                            {"4", "Sp","T", "G", "V", "Yes"},
    //                                            {"5", "?", "U", "H", "B", "No" },
    //                                            {"6", "L", "I", "J", "M", "Clr"},
    //                                            {"7", "P", "O", "K", ".", "Ent"}};
    
    private Panel panel;
    private CPU cpu;
    private int pressedKeyRow;
    private boolean pressedKeyPending;
    private VirtualMachine vm;
    
    
    public Keyboard(Panel p, CPU c, VirtualMachine v) {
        panel = p;
        cpu = c;
        initializeKeyboard();
        vm = v;
    }
    
    
    
    public void keyboard() {
        
        panel.addKeyListener( new KeyAdapter() {
            public void keyReleased(KeyEvent e)
            {
                vm.setLeftJoystick(0xFF);
                vm.setRightJoystick(0xFF);
            }
            
            // listen for esc, q, end, ctrl-c on the canvas to
            // allow a convenient exit from the full screen configuration
            public void keyPressed(KeyEvent e)
            {
                int keyCode = e.getKeyCode();
                char keyChar = e.getKeyChar();
                //System.out.println("KeyCode: " + keyCode);
            
         /*
         if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) ||
             (keyCode == KeyEvent.VK_END) ||
             ((keyCode == KeyEvent.VK_C) && e.isControlDown()) )
         {
           //running = false;
         }
          */
                //if(isKeyboardActivated() && 
                if(keyCode == KeyEvent.VK_F5)
                {                    
                    vm.initVM();
                    cpu.reset();
                }


                //if(isKeyboardActivated() &&
                else if(
                        //"0", "8", "+", "Q", "A", "-"
                        keyChar == '0' || keyChar == '8' || keyChar == '+' ||
                        keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_A || keyChar == '-')
                {
                    System.out.println("LINE 0; 0 - 8 - + - Q - A - -");
                    vm.pressedKeyRow = 0xE0;
                    vm.pressedKeyPending = true;
                }
                //else if(isKeyboardActivated() &&
                else if(
                        //"1", "9", "W", "S", "Z", "*"
                        keyChar == '1' || keyChar == '9' || keyCode == KeyEvent.VK_W ||
                        keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_Z || keyChar == '*')
                {
                    System.out.println("LINE 1; 1 - 9 - W - S - Z - *");
                    vm.pressedKeyRow = 0xC0;
                    vm.pressedKeyPending = true;
                }
                //else if(isKeyboardActivated() &&
                else if(
                        //"2", " ", "E", "D", "X", "/"
                        keyChar == '2' || keyCode == KeyEvent.VK_E ||
                        keyCode == KeyEvent.VK_D || keyCode == KeyEvent.VK_X || keyChar == '/')
                {
                    System.out.println("LINE 2; 2 -   - E - D - X - /");
                    vm.pressedKeyRow = 0xA0;
                    vm.pressedKeyPending = true;
                }
                //else if(isKeyboardActivated() &&
                else if(
                        //"3", " ", "R", "F", "C", "="
                        keyChar == '3' || keyCode == KeyEvent.VK_R ||
                        keyCode == KeyEvent.VK_F || keyCode == KeyEvent.VK_C || keyChar == '=')
                {
                    System.out.println("LINE 3; 3 -   - R - F - C - =");
                    vm.pressedKeyRow = 0x80;
                    vm.pressedKeyPending = true;                    
                }
                //else if(isKeyboardActivated() &&
                else if(        
                        //"4", "Sp","T", "G", "V", "Yes"
                        keyChar == '4' || keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_T ||
                        keyCode == KeyEvent.VK_G || keyCode == KeyEvent.VK_V || keyCode == KeyEvent.VK_F10)
                {
                    System.out.println("LINE 4; 4 - Space - T - G - V - Yes");
                    vm.pressedKeyRow = 0x60;
                    vm.pressedKeyPending = true;                       
                }
                //else if(isKeyboardActivated() &&
                else if(
                        //"5", "?", "U", "H", "B", "No"
                        keyChar == '5' || keyChar == '?' || keyCode == KeyEvent.VK_U ||
                        keyCode == KeyEvent.VK_H || keyCode == KeyEvent.VK_B || keyCode == KeyEvent.VK_F11)
                {
                    System.out.println("LINE 5; 5 - ? - U - H - B - No");
                    vm.pressedKeyRow = 0x40;
                    vm.pressedKeyPending = true;                      
                }
                //else if(isKeyboardActivated() &&
                else if(
                        //"6", "L", "I", "J", "M", "Clr"
                        keyChar == '6' || keyCode == KeyEvent.VK_L || keyCode == KeyEvent.VK_I ||
                        keyCode == KeyEvent.VK_J || keyCode == KeyEvent.VK_M || keyCode == KeyEvent.VK_F12)
                {
                    System.out.println("LINE 6; 6 - L - I - J - M - Clr");
                    vm.pressedKeyRow = 0x20;
                    vm.pressedKeyPending = true;                       
                }    
                //else if(isKeyboardActivated() &&
                else if(
                        //"7", "P", "O", "K", ".", "Ent"
                        keyChar == '7' || keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_O ||
                        keyCode == KeyEvent.VK_K || keyChar == '.' || keyCode == KeyEvent.VK_ENTER)
                {
                    System.out.println("LINE 7; 7 - P - O - K - . - Ent");
                    vm.pressedKeyRow = 0x00;
                    vm.pressedKeyPending = true;
                }
                
                if(keyCode == 37)  //Joystick LEFT
                    vm.setLeftJoystick(0xF7);
                else if(keyCode == 38)  //Joystick UP
                    vm.setLeftJoystick(0xFE);
                else if(keyCode == 39)  //Joystick RIGHT
                    vm.setLeftJoystick(0xFD);
                else if(keyCode == 40)  //Joystick DOWN
                    vm.setLeftJoystick(0xFB);

                if(keyCode == 36)  //Joystick LEFT
                    vm.setRightJoystick(0xF7);
                else if(keyCode == 33)  //Joystick UP
                    vm.setRightJoystick(0xFE);
                else if(keyCode == 35)  //Joystick RIGHT
                    vm.setRightJoystick(0xFD);
                else if(keyCode == 34)  //Joystick DOWN
                    vm.setRightJoystick(0xFB);  
            }
        });
    }
    
    public boolean isKeyboardActivated()
    {
        //Negativlogik (!P12)
        if(cpu.isBitTrue(cpu.getPort1(), 2))
            return false;
        else 
            return true;
    }
    
    public boolean isPressedKeyPending()
    {
        return pressedKeyPending;
    }
    
    public void setPressedKeyPending(boolean p)
    {
        pressedKeyPending = p;
    }
    
    public int getPressedKey()
    {
        return pressedKeyRow;
    }
    
    public void initializeKeyboard()
    {
        pressedKeyRow = 0x7f;
        pressedKeyPending = false;
    }
}
