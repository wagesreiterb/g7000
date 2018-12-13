package g7000;
/*
 * printChar.java
 *
 * Created on 18. November 2006, 17:25
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//package g7000_vdc;

import java.awt.*;
/**
 *
 * @author Bernhard Wagesreiter
 */
public class VDC
{
    private static final int DOT_WIDTH = 4;
    private static final int DOT_HEIGHT = 2;
    
    private static final short[] positionPattern = {0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01};
    
    private static final short[] charArray = {0x7C, 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0x7C, 0x00  // 0x00 - 0 (Zero)
                                            , 0x18, 0x38, 0x18, 0x18, 0x18, 0x18, 0x3C, 0x00  // 0x01 - 1
                                            , 0x3C, 0x66, 0x0C, 0x18, 0x30, 0x60, 0x7E, 0x00  // 0x02 - 2
                                            , 0x7C, 0xC6, 0x06, 0x3C, 0x06, 0xC6, 0x7C, 0x00  // 0x03 - 3
                                            , 0xCC, 0xCC, 0xCC, 0xFE, 0x0C, 0x0C, 0x0C, 0x00  // 0x04 - 4
                                            , 0xFE, 0xC0, 0xC0, 0x7C, 0x06, 0xC6, 0x7C, 0x00  // 0x05 - 5
                                            , 0x7C, 0xC6, 0xC0, 0xFC, 0xC6, 0xC6, 0x7C, 0x00  // 0x06 - 6
                                            , 0xFE, 0x06, 0x0C, 0x18, 0x30, 0x60, 0xC0, 0x00  // 0x07 - 7
                                            , 0x7C, 0xC6, 0xC6, 0x7C, 0xC6, 0xC6, 0x7C, 0x00  // 0x08 - 8
                                            , 0x7C, 0xC6, 0xC6, 0x7E, 0x06, 0xC6, 0x7C, 0x00  // 0x09 - 9
                                            , 0x00, 0x18, 0x18, 0x00, 0x18, 0x18, 0x00, 0x00  // 0x0a - :
                                            , 0x18, 0x7E, 0x58, 0x7E, 0x1A, 0x7E, 0x18, 0x00  // 0x0b - $
                                            , 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  // 0x0c - <SPACE>
                                            , 0x3C, 0x66, 0x0C, 0x18, 0x18, 0x00, 0x18, 0x00  // 0x0d - ?
                                            , 0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xC0, 0xFE, 0x00  // 0x0e - L
                                            , 0xFC, 0xC6, 0xC6, 0xFC, 0xC0, 0xC0, 0xC0, 0x00  // 0x0f - P
                                            , 0x00, 0x18, 0x18, 0x7E, 0x18, 0x18, 0x00, 0x00  // 0x10 - + 
                                            , 0xC6, 0xC6, 0xC6, 0xD6, 0xFE, 0xEE, 0xC6, 0x00  // 0x11 - W
                                            , 0xFE, 0xC0, 0xC0, 0xF8, 0xC0, 0xC0, 0xFE, 0x00  // 0x12 - E
                                            , 0xFC, 0xC6, 0xC6, 0xFC, 0xD8, 0xCC, 0xC6, 0x00  // 0x13 - R
                                            , 0x7E, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x00  // 0x14 - T
                                            , 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0x7C, 0x00  // 0x15 - U
                                            , 0x3C, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C, 0x00  // 0x16 - I
                                            , 0x7C, 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0x7C, 0x00  // 0x17 - O
                                            , 0x7C, 0xC6, 0xC6, 0xC6, 0xDE, 0xCC, 0x76, 0x00  // 0x18 - Q
                                            , 0x7C, 0xC6, 0xC0, 0x7C, 0x06, 0xC6, 0x7C, 0x00  // 0x19 - S
                                            , 0xFC, 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0xFC, 0x00  // 0x1a - D
                                            , 0xFE, 0xC0, 0xC0, 0xF8, 0xC0, 0xC0, 0xC0, 0x00  // 0x1b - F
                                            , 0x7C, 0xC6, 0xC0, 0xC0, 0xCE, 0xC6, 0x7E, 0x00  // 0x1c - G
                                            , 0xC6, 0xC6, 0xC6, 0xFE, 0xC6, 0xC6, 0xC6, 0x00  // 0x1d - H
                                            , 0x06, 0x06, 0x06, 0x06, 0x06, 0xC6, 0x7C, 0x00  // 0x1e - J
                                            , 0xC6, 0xCC, 0xD8, 0xF0, 0xD8, 0xCC, 0xC6, 0x00  // 0x1f - K
                                            , 0x38, 0x6C, 0xC6, 0xC6, 0xFE, 0xC6, 0xC6, 0x00  // 0x20 - A
                                            , 0x7E, 0x06, 0x0C, 0x18, 0x30, 0x60, 0x7E, 0x00  // 0x21 - Z
                                            , 0xC6, 0xC6, 0x6C, 0x38, 0x6C, 0xC6, 0xC6, 0x00  // 0x22 - X
                                            , 0x7C, 0xC6, 0xC0, 0xC0, 0xC0, 0xC6, 0x7C, 0x00  // 0x23 - C
                                            , 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0x6C, 0x38, 0x00  // 0x24 - V
                                            , 0xFC, 0xC6, 0xC6, 0xFC, 0xC6, 0xC6, 0xFC, 0x00  // 0x25 - B
                                            , 0xC6, 0xEE, 0xFE, 0xD6, 0xC6, 0xC6, 0xC6, 0x00  // 0x26 - M
                                            , 0x00, 0x00, 0x00, 0x00, 0x00, 0x18, 0x18, 0x00  // 0x27 - .
                                            , 0x00, 0x00, 0x00, 0x7E, 0x00, 0x00, 0x00, 0x00  // 0x28 - -
                                            , 0x00, 0x66, 0x3C, 0x18, 0x3C, 0x66, 0x00, 0x00  // 0x29 - x (multiply)
                                            , 0x00, 0x18, 0x00, 0x7E, 0x00, 0x18, 0x00, 0x00  // 0x2a - /. (divide)
                                            , 0x00, 0x00, 0x7E, 0x00, 0x7E, 0x00, 0x00, 0x00  // 0x2b - =
                                            , 0x66, 0x66, 0x66, 0x3C, 0x18, 0x18, 0x18, 0x00  // 0x2c - Y
                                            , 0xC6, 0xE6, 0xF6, 0xFE, 0xDE, 0xCE, 0xC6, 0x00  // 0x2d - N
                                            , 0x03, 0x06, 0x0C, 0x18, 0x30, 0x60, 0xC0, 0x00  // 0x2e - /
                                            , 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x00  // 0x2f - block
                                            , 0xCE, 0xDB, 0xDB, 0xDB, 0xDB, 0xDB, 0xCE, 0x00  // 0x30 - 10
                                            , 0x00, 0x00, 0x3C, 0x7E, 0x7E, 0x7E, 0x3C, 0x00  // 0x31 - ball
                                            , 0x1C, 0x1C, 0x18, 0x1E, 0x18, 0x18, 0x1C, 0x00  // 0x32 - man right
                                            , 0x1C, 0x1C, 0x18, 0x1E, 0x18, 0x34, 0x26, 0x00  // 0x33 - man rigt walk
                                            , 0x38, 0x38, 0x18, 0x78, 0x18, 0x2C, 0x64, 0x00  // 0x34 - man left walk
                                            , 0x38, 0x38, 0x18, 0x78, 0x18, 0x18, 0x38, 0x00  // 0x35 - man left
                                            , 0x00, 0x18, 0x0C, 0xFE, 0x0C, 0x18, 0x00, 0x00  // 0x36 - arrow rigt
                                            , 0x18, 0x3C, 0x7E, 0xFF, 0xFF, 0x18, 0x18, 0x00  // 0x37 - tree
                                            , 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xff, 0x00  // 0x38 - slope left
                                            , 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFE, 0xFF, 0x00  // 0x39 - slope rigt
                                            , 0x38, 0x38, 0x12, 0xFE, 0xB8, 0x28, 0x6C, 0x00  // 0x3a - man front
                                            , 0xC0, 0x60, 0x30, 0x18, 0x0C, 0x06, 0x03, 0x00  // 0x3b - \
                                            , 0x00, 0x00, 0x0C, 0x08, 0x08, 0x7F, 0x3E, 0x00  // 0x3c - ship 1
                                            , 0x00, 0x03, 0x63, 0xFF, 0xFF, 0x18, 0x08, 0x00  // 0x3d - plane
                                            , 0x00, 0x00, 0x00, 0x10, 0x38, 0xFF, 0x7E, 0x00  // 0x3e - ship 2
                                            , 0x00, 0x00, 0x00, 0x06, 0x6E, 0xFF, 0x7E, 0x00  // 0x3f - ship 3
                                            };
    
    //vdc RAM
   // private static final int[] vdcRAM = new int[256];
    
    private int vdcRAM[];		//RAM of the VDC 256 Byte
					//P13=0; P14=1
    
        
        // LSSn = R5 ???
        // VertiCam Data = R4 ???
        // X = R3 ???
    
        // 0x10 - 0x3F, Foreground Characters (12 characters / 4 bytes peri character)
        // Byte 0: Y position
        // Byte 1: X position
        // Byte 2: Characterset pointer (Bit 0 - 7)     (char * 8) - (Y / 2)
        // Byte 3:  0:      Characterset pointer (Bit 8)
        //          1 - 3:  Color (1: R 2:G 3: B)
        //          4 - 7:  unused

    
    
    private static final int[][] colorTable = {{0  ,0   ,0   },    // black
                                               {255, 65 , 85 },    // red
                                               {60 , 243, 121},    // green
                                               {219, 174, 93 },    // yellow
                                               {105, 162, 255},    //blue
                                               {255, 154, 255},    //violet
                                               {48 , 255, 255},    //cyan
                                               {255, 255, 255}};   //white
    
    private Chase g7cTop;

    
    public VDC(Chase g7c)
    {
        g7cTop = g7c;
        vdcRAM = new int[256];
    }
    
    public int[] getVdcRAM()
    {
        return vdcRAM;
    }
    
    synchronized public void draw(Graphics g, int romObjectPointer, CPU cpu)
    {
        drawCharacters(g, romObjectPointer);
        drawMinorSystem(g);
        drawGrid(g);   
    }
    
    public byte getSoundByteOne()
    {
        return (byte) vdcRAM[0xA7];
    }
    public byte getSoundByteTwo()
    {
        return (byte) vdcRAM[0xA8];
    }
    public byte getSoundByteThree()
    {
        return (byte) vdcRAM[0xA9];
    }
    
    private void drawMinorSystem(Graphics g)
    {
        int TestVDC[] = new int[256];
        int xPosition;
        int yPosition;
        int color;
        int faktor;
        int dotHeight = DOT_HEIGHT;
        int dotWidth = DOT_WIDTH;
        Rectangle dot;
        Color charColor;
        
        /*
        //TestVDC = vdcRAM;
        //50 4a 38 f8    50 52 38 f8    80 4a 38 f8    80 52 38 f8
        TestVDC[0x00] = 0x50;
        TestVDC[0x01] = 0x4a;

        
        TestVDC[0x80] = 0x18;
        TestVDC[0x81] = 0x04;
        TestVDC[0x82] = 0xc4;
        TestVDC[0x83] = 0x2e;
        TestVDC[0x84] = 0x24;
        TestVDC[0x85] = 0xc4;
        TestVDC[0x86] = 0x00;
        TestVDC[0x87] = 0x00;

        
        TestVDC[00+4*1] = 0x50;
        TestVDC[01+4*1] = 0x52;
//00 00 34 0d 05 04 00 00        
        TestVDC[0x88] = 0x00;
        TestVDC[0x89] = 0x00;
        TestVDC[0x8a] = 0x34;
        TestVDC[0x8b] = 0x0d;
        TestVDC[0x8c] = 0x05;
        TestVDC[0x8d] = 0x04;
        TestVDC[0x8e] = 0x00;
        TestVDC[0x8f] = 0x00;

        TestVDC[00+4*2] = 0x80;
        TestVDC[01+4*2] = 0x4a;
//00 00 ae 69 29 2e 00 00        
        TestVDC[0x90] = 0x00;
        TestVDC[0x91] = 0x00;
        TestVDC[0x92] = 0xae;
        TestVDC[0x93] = 0x69;
        TestVDC[0x94] = 0x29;
        TestVDC[0x95] = 0x2e;
        TestVDC[0x96] = 0x00;
        TestVDC[0x97] = 0x00;

        TestVDC[00+4*3] = 0x80;
        TestVDC[01+4*3] = 0x52;
//20 20 38 25 25 39 00 00        
        TestVDC[0x98] = 0x20;
        TestVDC[0x99] = 0x20;
        TestVDC[0x9a] = 0x38;
        TestVDC[0x9b] = 0x25;
        TestVDC[0x9c] = 0x25;
        TestVDC[0x9d] = 0x39;
        TestVDC[0x9e] = 0x00;
        TestVDC[0x9f] = 0x00;
        */
        TestVDC = vdcRAM;
        
        //System.out.println("0xAA: " + vdcRAM[0xAA] + " " + vdcRAM[0xA7] + " " + vdcRAM[0xA8] + " " + vdcRAM[0xA9]);
        
        
        //TestVDC[0x02] = 0x0c;        
        for(int minorSystem = 0; minorSystem < 4; minorSystem++)    // 4 Minor Systems
        {
            color = (vdcRAM[0x00+4*minorSystem+2] & 0x38) >> 3;   // Byte4:3-5    R:G:B
            charColor = new Color(colorTable[color][0],
                                        colorTable[color][1],
                                        colorTable[color][2]);
    
            g.setColor(charColor);
            
            int LINES = 2;
            for(int i = 0x80+8*minorSystem; i < 0x88+8*minorSystem; i++)
            {
                int y = TestVDC[i];
                for(int k = 0; k < 8; k++)
                {
                    if((y & 0x01) == 0x01)
                    {    
                        xPosition = (TestVDC[0x01+4*minorSystem] -2 + k)  * DOT_WIDTH;
                        yPosition = (TestVDC[0x00+4*minorSystem] + (i-0x80-8*minorSystem)*2 - DOT_HEIGHT) * DOT_HEIGHT; 
                        
                        //double Size or not?
                        if((TestVDC[0x00+(4*minorSystem)+2] & 0x04) != 0x04)
                        {
                            dot = new Rectangle(xPosition, yPosition, dotWidth, dotHeight*2);
                            g.fillRect(dot.x, dot.y, dot.width, dot.height);
                        }
                        else
                        {
                            //double Size
                            dot = new Rectangle(xPosition + k*dotWidth, yPosition + (i-0x80-8*minorSystem)*LINES*dotHeight, dotWidth*2, dotHeight*LINES*2);
                            g.fillRect(dot.x, dot.y, dot.width, dot.height);
                        }
                    }
                    y = y >> 1 & 0xFF;
                }
            }
        }
    }
    
    //draw Characters
    private void drawCharacters(Graphics g, int romObjectPointer)
    {
        Rectangle dot;
        Color charColor;
        int farbe;
                
        //p = 0x10 - 0x3F
        for(int p = 0x10; p < 0x7F; p = p + 4)
        {
            // LSSn = ROM object Address - ( (VertiCam Data)n / 2 )
            // ROM object Address = LSSn + ( (VertiCam Data)n / 2 )
            // 0x10 - 0x3F, Foreground Characters (12 characters / 4 bytes peri character)
            // Byte 0: Y position
            // Byte 1: X position
            // Byte 2: Characterset pointer (Bit 0 - 7)     (char * 8) - (Y / 2)
            // Byte 3:  0:      Characterset pointer (Bit 8)
            //          1 - 3:  Color (1: R 2:G 3: B)
            //          4 - 7:  unused
            
            int dotY = vdcRAM[p];   // Y Position, Byte0
            int dotX = vdcRAM[p+1] * DOT_WIDTH - DOT_WIDTH;
            
            if(p == 0x40)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 0*DOT_WIDTH - DOT_WIDTH;
            if(p == 0x44)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 16*DOT_WIDTH - DOT_WIDTH;
            if(p == 0x48)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 32*DOT_WIDTH - DOT_WIDTH;
            if(p == 0x4c)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 48*DOT_WIDTH - DOT_WIDTH;
            
            if(p == 0x50)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 0*DOT_WIDTH - DOT_WIDTH;
            if(p == 0x54)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 16*DOT_WIDTH - DOT_WIDTH;
            if(p == 0x58)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 32*DOT_WIDTH - DOT_WIDTH;
            if(p == 0x5c)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 48*DOT_WIDTH - DOT_WIDTH;
            
            if(p == 0x60)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 0*DOT_WIDTH  - DOT_WIDTH;
            if(p == 0x64)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 16*DOT_WIDTH  - DOT_WIDTH;
            if(p == 0x68)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 32*DOT_WIDTH  - DOT_WIDTH;
            if(p == 0x6c)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 48*DOT_WIDTH  - DOT_WIDTH;
            
            if(p == 0x70)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 0*DOT_WIDTH  - DOT_WIDTH;
            if(p == 0x74)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 16*DOT_WIDTH  - DOT_WIDTH;
            if(p == 0x78)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 32*DOT_WIDTH  - DOT_WIDTH;
            if(p == 0x7c)
                dotX = vdcRAM[p+1] * DOT_WIDTH + 48*DOT_WIDTH  - DOT_WIDTH;            
            
            //romObjectPointer = vdcRAM[p+2] + ((vdcRAM[p+3] & 0x01) << 8); // Byte2 + Byte3:0)            
            //romObjectPointer = vdcRAM[p+2] + (dotY / 2);

            //System.out.println("Byte 2: " + vdcRAM[p+2]);
            //System.out.println("Byte 3: " + vdcRAM[p+3]);
            //System.out.println("dotY: " + dotY);
            
            romObjectPointer = vdcRAM[p+2] | ((vdcRAM[p+3] & 0x01) << 8); // Byte2 + Byte3:0)            
            romObjectPointer = romObjectPointer + (dotY / 2);
            romObjectPointer = romObjectPointer % 0x200;    //romObjectPointer kann/darf ueberlaufen
            
            //System.out.println("romObjectPointer: " + romObjectPointer + "\n");
            
            farbe = (vdcRAM[p+3] / 2) & 0x07;   // Byte4:1-3    R:G:B
            charColor = new Color(colorTable[farbe][0],
                                    colorTable[farbe][1],
                                    colorTable[farbe][2]);            
            g.setColor(charColor);            
            
            for(int i = 0; i < 8; i++)  // print each row of the character (Byte 0 - 7)
            {
                int charDotPosition = 0;
                for(int k = 0x80; k > 0; k = k / 2) //print each column of row (Bit 0 - 7)
                {
                    //try
                    //{
                        if( (charArray[romObjectPointer + i] & positionPattern[charDotPosition]) == k)
                        {

                                //Line 1
                                dot = new Rectangle(dotX + charDotPosition * DOT_WIDTH, dotY + i * DOT_HEIGHT, DOT_WIDTH, DOT_HEIGHT);
                                g.fillRect(dot.x, dot.y * DOT_HEIGHT - DOT_HEIGHT, dot.width, dot.height);
                                //Line 2
                                dot = new Rectangle(dotX + charDotPosition * DOT_WIDTH, dotY + 1 + i * DOT_HEIGHT, DOT_WIDTH, DOT_HEIGHT);
                                g.fillRect(dot.x, dot.y * DOT_HEIGHT - DOT_HEIGHT, dot.width, dot.height);
                        }
                        charDotPosition++;
                    //}
                    /*
                    catch(ArrayIndexOutOfBoundsException e)
                    {
                        //System.out.printf("\ncharArray[romObjectPointer + i]: %1$02Xh\n", charArray[romObjectPointer + i]);
                        
                        System.out.printf("\nromObjectPointer + i: %1$02Xh\n", romObjectPointer + i);                       
                        System.out.printf("positionPattern[charDotPosition]) %1$02Xh\n", positionPattern[charDotPosition]);
                        System.out.printf("vdcRAM[p+0] %1$02Xh\n", vdcRAM[p+0]);
                        System.out.printf("vdcRAM[p+1] %1$02Xh\n", vdcRAM[p+1]);
                        System.out.printf("vdcRAM[p+2] %1$02Xh\n", vdcRAM[p+2]);
                        System.out.printf("vdcRAM[p+3] %1$02Xh\n", vdcRAM[p+3]);
                        System.out.printf("((vdcRAM[p+3] & 0x01) << 8)  %1$02Xh\n", ((vdcRAM[p+3] & 0x01) << 8) );
                        
                               
                        //System.out.printl("positionPattern[charDotPosition]) %1$02Xh", positionPattern[charDotPosition]);
                    }*/
                }
            }
        }       
    }// END draw Characters
    
    // DRAW GRID
    private void drawGrid(Graphics g)
    {     
        int upperLeftXPosition = 26;    //0Bh
        int upperLeftYPosition = 44;    //13h
        int ySpacer = 24*2;
        int xSpacer = 64;
        int horizontalElementWidth = DOT_WIDTH*18;  //16
        int verticalElementWidth = DOT_WIDTH*2;
        int horizontalElementHeight = DOT_HEIGHT*3;
        int verticalElementHeight = DOT_HEIGHT*27;
        
        int color;
        Color charColor;
        
        Rectangle gridElement;
        
        
        int TestVDC[] = new int[256];
        /*
        TestVDC[0xC0] = 0xff;
        TestVDC[0xc1] = 0xff;
        TestVDC[0xC2] = 0xff;
        TestVDC[0xc3] = 0x12;
        TestVDC[0xC4] = 0x01;
        TestVDC[0xc5] = 0x12;
        TestVDC[0xC6] = 0x01;
        TestVDC[0xc7] = 0x12;
        
        TestVDC[0xc8] = 0xff;
        */
        TestVDC = vdcRAM;
        
        /*
        color = (vdcRAM[0x03] & 0x07) >> 3;   // Byte4:3-5    R:G:B
        charColor = new Color(colorTable[color][0],
                                    colorTable[color][1],
                                    colorTable[color][2]);

        g.setColor(charColor);
        */
        //Horizontal Elements
        for(int element = 0xC0; element <= 0xC8; element++)
        {
            if((TestVDC[element] & 0x01) == 0x01)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*0, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x02) == 0x02)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*1, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x04) == 0x04)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*2, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x08) == 0x08)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*3, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x10) == 0x10)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*4, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x20) == 0x20)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*5, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x40) == 0x40)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*6, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x80) == 0x80)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*7, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
        }
        
        for(int element = 0xD0; element <= 0xD8; element++)
        {
            if((TestVDC[element] & 0x01) == 0x01)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*8, horizontalElementWidth, horizontalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
        }        
        
        //Vertical Elements
        for(int element = 0xE0; element <= 0xE9; element++)
        {
            if((TestVDC[element] & 0x01) == 0x01)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*0, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }
            if((TestVDC[element] & 0x02) == 0x02)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*1, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }                
            if((TestVDC[element] & 0x04) == 0x04)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*2, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }                
            if((TestVDC[element] & 0x80) == 0x80)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*3, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }                
            if((TestVDC[element] & 0x10) == 0x10)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*4, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }                
            if((TestVDC[element] & 0x20) == 0x20)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*5, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }                
            if((TestVDC[element] & 0x40) == 0x40)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*6, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }                
            if((TestVDC[element] & 0x80) == 0x80)
            {
                gridElement = new Rectangle(upperLeftXPosition + (element & 0x0F)*xSpacer, upperLeftYPosition + ySpacer*7, verticalElementWidth, verticalElementHeight);
                g.fillRect(gridElement.x, gridElement.y, gridElement.width, gridElement.height);
            }    
        }        
    } // END DRAW GRID
}

