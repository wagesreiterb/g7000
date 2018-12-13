/*
 * VirtualMachine.java
 *
 * Created on 8. Juli 2007, 23:10
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//!p13 && !p16 => read from VDC

package g7000;

import java.io.*;

//import java.awt.event.*;

/**
 *
 * @author Que
 */
public class VirtualMachine implements Runnable
{
    /**VSYNC
            t = 1 / XTAL frequency
            XTAL freq = 5,91MHz (PAL)
            t = 0,169us
    each Cycle needs 15 Cycles from the XTAL Timer
            cy = 15 * t
            cy = 2,538us
    PAL has 50 Frames per Second (25 PAL-B)
            t_Frame = 1/50 = 20ms
    Cycles per VSYNC = t_Frame/cy
            20ms/2,538us = 7880 @ 50 Frames per Second (50 Halbbilder)
     */
    /*HSYNC
     *  25 Lines per Second
        Given PAL time intervals from CCIR specification:
        Hsync Start                   = 1.5us
        Hsync Width                   = 4.7us   
        Hsync Blank Width             = 12us
        Horizontal Line Interval Time = 64us
     *  64us * 312 Lines * 50 frames = 1 Second (312,5 Lines/Frame)
     *
     *  Horizontal Scan active by Que = 52us / 2,538 = 20,488 Cycles
    */
             

    //private static final int VSYNC = 15760;      //VSYNC = 7880 Cycles @ 25 Lines per Second
    //private static final int VSYNC = //7880;     //VSYNC = 7880 Cycles @ 50 Lines per Second
    private static final int VSYNC = 7825;      //50 frames, 313 Lines, 25 Cycles/Line
    private static final int VBI = 625;         //VERTICAL BLANK INTERVAL = 50 Lines (PAL)
                                                //, 21Lines = 1,33ms (Grokking)
    private static final int HSYNC = 25; //25 Cycles per Lines
    
    private volatile boolean running = false;
    private Thread animator;
    
    private int vsync;
    private int hsync;
    private int keyboardInput;
    
    //test only
    private int charPositionPointer = 0;

    private CPU cpu;
    private CommandLine cl;
    private Keyboard g7000keyboard;
    private VDC vdc;
    private Panel panel;
    private SoundSystem sound;

    //DEBUG
    private boolean stopIt = true;
    private int intque = 0;
    //private int[] dbgInternalRam;
    
    public int pressedKeyRow;
    public boolean pressedKeyPending;
    private int pressedKeyTimer;
    
    private int externalRAM[] = new int[256];	//external RAM 256 Byte
                                                //P13=1; P14=0
                                                //maybe only 128 Byte (MCM6810)??? on my G7000
    
    private int leftJoystick = 0xFF, rightJoystick = 0xFF;
    public int bus = 0xFF;
    
    
    /** Creates a new instance of VirtualMachine */
    public VirtualMachine(Chase g7cTop, Panel p)
    {
        panel = p;
        int[] memory = initVM();
        
        
        vdc = new VDC(g7cTop);
        cpu = new CPU(memory, externalRAM, vdc.getVdcRAM(), this);
        cl = new CommandLine();
        sound = new SoundSystem();
        //sound.run();
        
        
        g7000keyboard = new Keyboard(panel, cpu, this);
        g7000keyboard.keyboard();
    }
    
    public void run()
    {
        running = true;
        //debug
        int stackPointerDebug = cpu.stackPointer;
        
        
        //DEBUG
        Debugger dbg = new Debugger();
        //---DEBUG
        int i = 0;
        int port2;
        
        int i_debug=0;
        
        while(running)
        {
            i_debug++;
            //cpu.setStopIt(false);
            
            //Theory of Rene_G7400
            //I have another theory (based on an experiment):
            //bit 3 of A1 is kept high until A1 has been read.
            cpu.vdcRAM[0xa1] = cpu.vdcRAM[0xa1] | 0x08; //Set Bit 3 
            
            vsync = VSYNC;
            hsync = HSYNC;
            while(vsync > 0)
            {
                if(hsync == 0)
                    hsync = HSYNC;
                else if(hsync == -1)
                    hsync = HSYNC-1;

                /*
                //VDC: 0xA1, Bit3, This bit is normally 0 and goes to 1 for 40us at the start of VBLANK
                if(vsync > (VSYNC-VBI))
                    //set Bit 3 of VDC Statusregister
                    //cpu.writeVDCRAM(0x08, 0xa1);    //CORRECT ME
                    cpu.vdcRAM[0xA1] |= 0x08;
                else
                    //unset Bit 3 of VDC Statusregister
                    //cpu.writeVDCRAM(0x00, 0xa1);    //CORRECT ME
                    cpu.vdcRAM[0xA1] |= 0x08;
                */
                //cpu.vdcRAM[0xA1] |= 0x08;


                cpu.nextStep(this);

                if(pressedKeyPending == true)   //& !p12
                {
                    if(pressedKeyTimer > 0)
                    {
                        cpu.setPort2(cpu.getPort2() & 0x0f | pressedKeyRow);
                        //System.out.println("-----------------------------------------------------------KEY");
                    }
                    else
                    {
                        pressedKeyPending = false;
                        pressedKeyTimer = VSYNC;
                    }
                    pressedKeyTimer--;
                }
                //pressedKeyRow = 0xE0;
                //pressedKeyPending = true;
                
                //System.out.print("VSYNC: " + vsync + " -- ");
                //System.out.print(" -- StackPointer: " + cpu.stackPointer + " -- ");
                


                
                /*
                if(stackPointerDebug != cpu.stackPointer)
                {
                    //System.out.print("VSYNC: " + vsync + " -- ");
                    System.out.println("StackPointer: " + cpu.stackPointer);
                    debug(dbg);
                    stackPointerDebug = cpu.stackPointer;
                }*/
                
                /*
                if(i_debug >= 10 && cpu.vdcRAM[0x10] != 0x70)
                {
                    debug(dbg);  
                    cpu.debug(true,true,false,true); 
                    System.out.println("\n");
                    
                }

                if(intque > 260)
                {
                    debug(dbg);  
                    cpu.debug(true,true,false,true); 
                    System.out.println("\n");
                }
  */         
                //debug(dbg);  
                
                vsync -= cpu.getCycles();
                hsync -= cpu.getCycles();
                
                if(vsync == 2607 || vsync == 2608 )
                {
                    sound.shiftByte(vdc.getSoundByteOne(), vdc.getSoundByteTwo(), vdc.getSoundByteThree());
                    //System.out.println((int) vdc.getSoundByteOne() + " - " + (int) vdc.getSoundByteTwo() + " - " + (int) vdc.getSoundByteThree());
                }
                if(vsync == 5213 || vsync == 5214 )
                {
                    sound.shiftByte(vdc.getSoundByteOne(), vdc.getSoundByteTwo(), vdc.getSoundByteThree());     
                    //System.out.println((int) vdc.getSoundByteOne() + " - " + (int) vdc.getSoundByteTwo() + " - " + (int) vdc.getSoundByteThree());
                }
            
                //Interrupts
                //System.out.println("isExternalIRQPending: " + cpu.isExternalIRQPending());
                if(cpu.isExternalIRQPending())
                    cpu.triggerExternalIRQ();
                //System.out.println("isTimerIRQPending: " + cpu.isTimerIRQPending());
                if(cpu.isTimerIRQPending())
                    cpu.triggerTimerIRQ();
            }
            //Interrupts
            //System.out.println("isExternalIRQPending: " + cpu.isExternalIRQPending());
            //cpu.triggerExternalIRQ();
            cpu.externalIRQPending = true;
            
            // if(stopIt) {cl.command(cpu); }

            panel.gameRender(vdc, cpu);
            panel.paintScreen(); 
                       
            
            try
            {   
                //schlafe 1000ms/50frames - bereits verbrauchter Zeit= 20ms
                Thread.sleep(10);    //normal 20ms
            }
            catch(InterruptedException e) {}
            
            intque++;
             
        }
    }
    
        public void startVirtualMachine()
        // initialise and start the thread 
        { 
            if (animator == null || !running)
            {
                animator = new Thread(this);
                animator.start();
            }
        }
        
        private void debug(Debugger dbg)
        {
            dbg.printPreviousProgramCounter(cpu.getPreviousProgramCounter());
            dbg.printOpCode(cpu.getOpCode(), cpu.getOpCodeData());
            dbg.printOperation(cpu.getOpCode(), cpu.getOpCodeData());
            //System.out.print("; ");
            dbg.printBiosRoutine(cpu.getPreviousProgramCounter());
            System.out.println();
        }
        
        public int[] initVM()
        {
            int biosBinarySize;
            int gameBinarySize;
            int binarySize;
            int[] memory = new int[0];
            
            pressedKeyRow = 0xE0;
            pressedKeyPending = false;
            pressedKeyTimer = VSYNC;
                        
            try
            {
                FileInputStream biosFileInputStream = new FileInputStream("binaries/o2rom.bin"); //BIOS
                //FileInputStream gameFileInputStream = new FileInputStream("binaries/hello.bin"); //game Binary
                //FileInputStream gameFileInputStream = new FileInputStream("binaries/hellot.rom"); //game Binary
                //FileInputStream gameFileInputStream = new FileInputStream("binaries/U.bin"); //game Binary 
                //FileInputStream gameFileInputStream = new FileInputStream("binaries/kcmunch.bin"); //game Binary VPP???
                FileInputStream gameFileInputStream = new FileInputStream("binaries/new_pong.bin"); //game Binary
                
                biosBinarySize = biosFileInputStream.available();
                gameBinarySize = gameFileInputStream.available();
                             
                binarySize = biosBinarySize + gameBinarySize;
                memory = new int[binarySize];
                //memory = new int[4048];
                System.out.println("gameFileInputStream.available(): " + binarySize); //debug
                System.out.printf("Groesse: 0x%1$x\n\n", binarySize);

                int counterBios;
                for(counterBios=0; counterBios < biosBinarySize; counterBios++)
                {
                        memory[counterBios] = biosFileInputStream.read();
                        //System.out.printf("%1$04d - 0x%2$03x - %3$02x\n",counterBios,counterBios, memory[counterBios]);
                }
                System.out.println("************************************");
                for(int counterGame=counterBios; counterGame < binarySize; counterGame++)
                {
                        memory[counterGame] = gameFileInputStream.read();
                        //System.out.printf("%1$04d - 0x%2$03x - %3$02x\n",counterGame, counterGame, memory[counterGame]);
                }
            }
            catch(IOException e)
            {
                    System.out.println(e.getMessage());
                    System.exit(0);
            }
            return memory;
        }
        /*
        public void write_BUS(int accumulator, int register, int p13, int p14)
        {
            //VDC enabled, RAM disabled
            if(p13 == 0 && p14 == 1)
            {
                //VDC Control Register
                if(register == 0xA0)
                {
                    
                }
                else if(register == 0xA3)
                //Color register
                {
                    
                }
                else if(register == 0xAA)
                //Sound Control Register
                {
                 
                }
                else cpu.writeVDCRAM(accumulator, register);
            }

            
            //VDC disabled, RAM enabled
            else if (p13 == 1 && p14 == 0)
            {
                cpu.writeExternalRAM(accumulator, register);
            }
        }*/
        
         public int read_BUS(int p12, int p13, int p14, int port2)
        {
            bus = 0xFF;
            
            //Read the Joysticks
            if (p13 == 1 && p14 == 1       //disable VDC & RAM
                    && p12 == 0)                 //enable Keyboard scanner
            {
                    //if(!this.isBitTrue(port2,0) && !this.isBitTrue(port2,1) && 
                    //!this.isBitTrue(port2,2))   //read the right joystick (!P20 && !P21 && !P22)
                    if(!((port2 & 0x01) == 0x01) && !((port2 & 0x02) == 0x02) && !((port2 & 0x04) == 0x04))
                    {
                        //System.out.println("RIGHT JOYSTICK");
                        //System.out.println("port2: " + port2);
                        //bus = 0x00;
                        //bus = 0xFE;  //Joystick UP
                        //bus = bus & 0xFD  //Joystick RIGHT
                        //bus = 0xFB;  //Joystick DOWN
                        //bus = 0xF7;  //Joystick LEFT
                        //bus = 0xf9;
                        //bus = bus & 0xEF  //Both Main Buttons
                        //leftJoystick = 0xFF;
                        bus = rightJoystick;
                    }
                    //else if(!this.isBitTrue(port2,0) && !this.isBitTrue(port2,1) && 
                    //this.isBitTrue(port2,2))   //read the left joystick   (!P20 && !P21 && P22)

                    //if(!((port2 & 0x01) == 0x01) && !((port2 & 0x02) == 0x02) && ((port2 & 0x04) == 0x04))
                    else
                    {
                        //System.out.println("LEFT JOYSTICK");
                        //System.out.println("port2: " + port2);
                        //bus = 0x00;
                        //bus = 0xFE;  //Joystick UP
                        //bus = bus & 0xFD  //Joystick RIGHT
                        //bus = 0xFB;  //Joystick DOWN
                        //bus = bus & 0xF7  //Joystick LEFT
                        //bus = bus & 0xEF  //Both Main Buttons
                        //rightJoystick = 0xFF;
                        bus = leftJoystick;
                    }
            }  
            return bus;
        }
         
        public void setLeftJoystick(int leftJoystick)
        {
            this.leftJoystick = leftJoystick;
        }
        public void setRightJoystick(int rightJoystick)
        {
            this.rightJoystick = rightJoystick;
        }         
        public int getHsync()
        {
            return hsync;
        }
        
        public int getVsync()
        {
            return vsync;
        }
        public int getVBI()
        {
            return VBI;
        }
        
        public boolean read_T1()
        {
            //Stimmt nicht
            if(vsync > (VSYNC-VBI)  //Vertical Blanking 
                || vsync % 25 == 0 || vsync+1 % 25 == 0)    //Horizontal Blanking
            {
                return true;
            }
            else
                return false;
        }
        
        public boolean read_T0()
        {
            return false;
        }
        
        public int writePort1(int accu)
        {
            return accu;
        }
}