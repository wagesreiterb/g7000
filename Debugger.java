package g7000;
/*
 * Debugger.java
 *
 * Created on 30. November 2006, 21:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Que
 */
public class Debugger
{
    private static final String[] opCodeMatrix = {
                        //00 - 0F
                        //	00----01---,02-----------,03---------,
                                "NOP", "ILL", "OUTL BUS,A", "ADD A,#",
                        //	04------,05-----,06----,07------,
                                "JMP $0", "EN I", "ILL", "DEC A",
                        //	08---------,09--------,0A--------,0B----,
                                "INS A,BUS", "IN A,P1", "IN A,P2", "ILL",
                        //	0C---------,0D----------,0E----------,0F----------,
                                "MOVD A,P4", "MOVD A,P5", "MOVD A,P6", "MOVD A,P7",

                        // 10 - 1F
                        //	10-------,11--------,12------,13----------,
                                "INC @R0", "INC @R1", "JB0 $", "ADDC A,#",
                        //	14-------,15------,16------,17------,
                                "CALL $", "DIS I", "JTF $", "INC A", 
                        //      18------,19-------,1A-------,1B-------,
                                "INC R0", "INC R1", "INC R2", "INC R3",
                        //	1C------,1D-------,1E-------,1F-------,
                                "INC R4", "INC R5", "INC R6", "INC R7", 


                        //20 - 2F
                        //	20----------,21----------,22----,23---------,
                                "XCH A,@R0", "XCH A,@R1", "ILL", "MOV A,#",
                        //	24------,25---------,26-------,27------,
                                "JMP $1", "EN TCNTI", "JNT0 $", "CLR A",
                        //	28---------,29--------,2A---------,2B---------,
                                "XCH A,R0", "XCH A,R1", "XCH A,R2", "XCH A,R3",
                        //	2C--------,2D---------,2E---------,2F---------,
                                "XCH A,R4", "XCH A,R5", "XCH A,R6", "XCH A,R7", 

                        //30 - 3F
                        //	30----------,31-----------,32------,33----,
                                "XCHD A,@R0", "XCHD A,@R1", "JB1 $", "ILL",
                        //	34-------,35----------,36------,37------,
                                "CALL $", "DIS TCNTI", "JT0 $", "CPL A",
                        //	38---,39----------,3A---------,3B----,
                                "ILL", "OUTL P1,A", "OUTL P2,A", "ILL",
                        //	3C---------,3D----------,3E----------,3F----------,
                                "MOVD P4,A", "MOVD P5,A", "MOVD P6,A", "MOVD P7,A",

                        //40 - 4F
                        //	40---------,41----------,42--------,43--------,
                                "ORL A,@R0", "ORL A,@R1", "MOV A,T", "ORL A,#",
                        //	44------,45---------,46-------,47-------,
                                "JMP $2", "STRT CNT", "JNT1 $", "SWAP",
                        //	48--------,49---------,4A---------,4B---------,
                                "ORL A,R0", "ORL A,R1", "ORL A,R2", "ORL A,R3",
                        //	4C--------,4D---------,4E---------,4F---------,
                                "ORL A,R4", "ORL A,R5", "ORL A,R6", "ORL A,R7",


                        //50 - 5F
                        //	50---------,51----------,52------,53--------,
                                "ANL A,@R0", "ANL A,@R1", "JB2 $", "ANL A,#", 
                        //	54-------,55-------,56------,57----,
                                "CALL $", "STRT T", "JT1 $", "ILL",
                        //	58--------,59---------,5A---------,5B---------,
                                "ANL A,R0", "ANL A,R1", "ANL A,R2", "ANL A,R3",
                        //	5C--------,5D---------,5E---------,5F---------,
                                "ANL A,R4", "ANL A,R5", "ANL A,R6", "ANL A,R7",

                        //60 - 6F
                        //	60---------,61----------,62--------,63----,	
                                "ADD A,@R0", "ADD A,@R1", "MOV T,A", "ILL",
                        //	64------,65----------,66----,67------,
                                "JMP $3", "STOP TCNT", "ILL", "RRC A",
                        //	68--------,69---------,6A---------,6B---------,
                                "ADD A,R0", "ADD A,R1", "ADD A,R2", "ADD A,R3",
                        //	6C--------,6D---------,6E---------,6F---------,
                                "ADD A,R4", "ADD A,R5", "ADD A,R6", "ADD A,R7",

                        //70 - 7F
                        //	70---------,71----------,72------,73------,
                                "ADDC A,@R0", "ADDC A,@R1", "JB3 $", "ILL",
                        //	74-------,75---------,76------,77---- ,
                                "CALL $", "ENT0 CLK", "JF1 $ ", "RR A",
                        //	78---------,79----------,7A----------,7B----------,
                                "ADDC A,R0", "ADDC A,R1", "ADDC A,R2", "ADDC A,R3",
                        //	7C---------,7D----------,7E----------,7F----------,
                                "ADDC A,R4", "ADDC A,R5", "ADDC A,R6", "ADDC A,R7",

                        //80 - 8F
                        //	80----------,81-----------,82----,83----,
                                "MOVX A,@R0", "MOVX A,@R1", "ILL", "RET",
                        //	84------,85-------,86------,87----,
                                "JMP $4", "CLR F0", "JNI $", "ILL",
                        //	88----------,89----------,8A---------,8B-----,
                                "ORL BUS,#", "ORL P1,#", "ORL P2,#", "ILL",
                        //	8C---------,8D----------,8E----------,8F----------,
                                "ORLD P4,A", "ORLD P5,A", "ORLD P6,A", "ORLD P7,A",

                        //90 - 9F
                        //	90----------,91-----------,92------,93-----,
                                "MOVX @R0,A", "MOVX @R1,A", "JB4 $", "RETR",
                        //	94-------,95-------,96------,97------,
                                "CALL $", "CPL F0", "JNZ $", "CLR C",
                        //	98----------,99----------,9A----------,9B----,
                                "ANL BUS,#", "ANL P1,#", "ANL P2,#", "ILL",
                        //	9C---------,9D----------,9E----------,9F----------,
                                "ANLD P4,A", "ANLD P5,A", "ANLD P6,A", "ANLD P7,A",

                        //A0 - AF
                        //	A0---------,A1----------,A2----,A3----------,
                                "MOV @R0,A", "MOV @R1,A", "ILL", "MOVP A,@A",
                        //	A4------,A5------,A6----,A7-------,
                                "JMP $5","CLR F1", "ILL", "CPL C",
                        //	A8--------,A9---------,AA---------,AB---------,
                                "MOV R0,A", "MOV R1,A", "MOV R2,A", "MOV R3,A",
                        //	AC--------,AD---------,AE---------,AF---------,
                                "MOV R4,A", "MOV R5,A", "MOV R6,A", "MOV R7,A",

                        //B0 - BF
                        //	B0----------,B1-----------,B2------,B3--------,
                                "MOV @R0,#", "MOV @R1,#", "JB5 $", "JMPP @A",
                        //	B4-------,B5--------,B6------,B7----,
                                "CALL $", "CPL F1", "JF0 $", "ILL",
                        //	B8---------,B9----------,BA----------,BB----------,
                                "MOV R0,#", "MOV R1,#", "MOV R2,#", "MOV R3,#",
                        //	BC---------,BD----------,BE----------,BF----------,
                                "MOV R4,#", "MOV R5,#", "MOV R6,#", "MOV R7,#",

                        //C0 - CF
                        //	C0---,C1----,C2----,C3----,
                                "ILL", "ILL", "ILL", "ILL",
                        //	C4------,C5--------,C6-------,C7--------,
                                "JMP $6", "SEL RB0", "JZ $", "MOV A,PSW",
                        //	C8------,C9-------,CA-------,CB-------,
                                "DEC R0", "DEC R1", "DEC R2", "DEC R3",
                        //	CC------,CD-------,CE-------,CF-------,
                                "DEC R4", "DEC R5", "DEC R6", "DEC R7",

                        //D0 - DF
                        //	D0---------,D1----------,D2------,D3---------,
                                "XRL A,@R0", "XRL A,@R1", "JB6 $", "XRL A,#",
                        //	D4-------,D5--------,D6----,D7----------,
                                "CALL $", "SEL RB1", "ILL", "MOV PSW,A",
                        //	D8--------,D9---------,DA---------,DB---------,
                                "XRL A,R0", "XRL A,R1", "XRL A,R2", "XRL A,R3",
                        //	DC--------,DD---------,DE---------,DF---------,
                                "XRL A,R4", "XRL A,R5", "XRL A,R6", "XRL A,R7",

                        //E0 - EF
                        //	E0---,E1----,E2----,E3-----------,
                                "ILL", "ILL", "ILL", "MOVP3 A,@A",
                        //	E4------,E5--------,E6------,E7-----,
                                "JMP $7", "SEL MB0", "JNC $", "RL A",
                        //	E8----------,E9-----------,EA-----------,EB-----------,
                                "DJNZ R0,$", "DJNZ R1,$", "DJNZ R2,$", "DJNZ R3,$",
                        //	EC----------,ED-----------,EE-----------,EF-----------,
                                "DJNZ R4,$", "DJNZ R5,$", "DJNZ R6,$", "DJNZ R7,$",

                        //F0 - FF
                        //	F0---------,F1----------,F2------,F3----,
                                "MOV A,@R0", "MOV A,@R1", "JB7 $", "ILL",
                        //	F4-------,F5--------,F6-----,F7------,
                                "CALL $", "SEL MB1", "JC $", "RLC A",
                        //	F8---------,F9---------,FA---------,FB---------,	
                                "MOV A,R0", "MOV A,R1", "MOV A,R2", "MOV A,R3",
                        //	FC--------,FD---------,FE---------,FF---------,
                                "MOV A,R4", "MOV A,R5", "MOV A,R6", "MOV A,R7",
                        };
    
    
    
    
    private static final int[] opCodeLength = {
                        //	00,01,02,03,04,05,06,07,08,09,0A,0B,0C,0D,0E,0F
                                0 ,3 ,0 ,1 ,1 ,0 ,3 ,0 ,0 ,0 ,0 ,3 ,0 ,0 ,0 ,0 ,
                        //      10,11,12,13,14,15,16,17,18,19,1A,1B,1C,1D,1E,1F
                                0 ,0 ,1 ,1 ,1 ,0 ,1 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0, 0 ,
                        //      20,21,22,23,24,25,26,27,28,29,2A,2B,2C,2D,2E,2F
                                0 ,0 ,3 ,1 ,1 ,0 ,1 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,
                        //      30,31,32,33,34,35,36,37,38,39,3A,3B,3C,3D,3E,3F
                                0 ,0 ,1 ,0 ,1 ,0 ,1 ,0 ,3 ,0 ,0 ,0 ,2 ,0 ,0 ,0 ,
                        //      40,41,42,43,44,45,46,47,48,49,4A,4B,4C,4D,4E,4F
                                0 , 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        //      50,51,52,53,54,55,56,57,58,59,5A,5B,5C,5D,5E,5F
                                0 ,0 ,1 ,1 ,1 ,0 ,1 ,3 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,
                        //      60,61,62,63,64,65,66,67,68,69,6A,6B,6C,6D,6E,6F
                                0 ,0 ,0 ,3 ,1 ,0 ,3 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,
                        //      70,71,72,73,74,75,76,77,78,79,7A,7B,7C,7D,7E,7F
                                0 ,0 ,1 ,3 ,1 ,0 ,1 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,0 ,
                        //      80,81,82,83,84,85,86,87,88,89,8A,8B,8C,8D,8E,8F
                                0 , 0, 3, 0, 1, 0, 1, 3, 1, 1, 1, 3, 0, 0, 0, 0,
                        //      90,91,92,93,94,95,96,97,98,99,9A,9B,9C,9D,9E,9F
                                0 ,0 ,1 ,0 ,1 ,0 ,1 ,0 ,1 ,1 ,1 ,3 ,0 ,0 ,0 ,0 ,
                        //      A0,A1,A2,A3,A4,A5,A6,A7,A8,A9,AA,AB,AC,AD,AE,AF
                                0 ,0 ,3 ,0 , 1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        //      B0,B1,B2,B3,B4,B5,B6,B7,B8,B9,BA,BB,BC,BD,BE,BF
                                1 , 1, 1, 0, 1, 0, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1,
                        //      C0,C1,C2,C3,C4,C5,C6,C7,C8,C9,CA,CB,CC,CD,CE,CF
                                3 ,3 ,3 ,3 ,1 , 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        //      D0,D1,D2,D3,D4,D5,D6,D7,D8,D9,DA,DB,DC,DD,DE,DF
                                0 , 0, 1, 1, 1, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        //      E0,E1,E2,E3,E4,E5,E6,E7,E8,E9,EA,EB,EC,ED,EE,EF
                                3 ,3 ,3 ,0 , 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1,
                        //      F0,F1,F2,F3,F4,F5,F6,F7,F8,F9,FA,FB,FC,FD,FE,FF
                                0 , 0, 1, 3, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0
                        }; 


    
    public Debugger() {
    }
    
    public void printBiosRoutine(int programCounter)
    {
        if(programCounter >= 0x00 && programCounter <= 0x07)
                System.out.printf("\tVECTOREN");
        if(programCounter >= 0x09 && programCounter <= 0x18)
                System.out.printf("\tIRQ - VBlank Interrupt Routine 1");
        if(programCounter >= 0x1a && programCounter <= 0x42)
                System.out.printf("\tVSYNCIRQ - VBlank Interrupt Routine 2 (Collision & Clock)");
        if(programCounter >= 0x44 && programCounter <= 0x88)
                System.out.printf("\tsoundirq - VBlank Interrupt Routine 3 (Tune)");
        if(programCounter >= 0x89 && programCounter <= 0xa2)
                System.out.printf("\tRAM to VDC VBlank Copying Check Code"); 
        if(programCounter >= 0xa3 && programCounter <= 0xaf)
                System.out.printf("\tCopying Code");         
        if(programCounter >= 0xb0 && programCounter <= 0xe5)
                System.out.printf("\tKeyboard Check Routine");
        if(programCounter >= 0xE7 && programCounter <= 0xEB)
                System.out.printf("\tVDCENABLE");
        if(programCounter >= 0xEC && programCounter <= 0xF0)
                System.out.printf("\tEXTRAMENABLE");
        if(programCounter >= 0xF1 && programCounter <= 0x11B)
                System.out.printf("\tReset the Odyssey II");
        if(programCounter >= 0x11c && programCounter <= 0x126)
                System.out.printf("\tTurn off the Display");
        if(programCounter >= 0x127 && programCounter <= 0x131)
                System.out.printf("\tTurn the display back on");
        if(programCounter >= 0x132 && programCounter <= 0x13c)
                System.out.printf("\ttableend - Enable a Data Copy at the next VSYNC");        
        if(programCounter >= 0x13d && programCounter <= 0x14a)
                System.out.printf("\tGet Keystroke into A");
        if(programCounter >= 0x14b && programCounter <= 0x16a)
                System.out.printf("\tcalcchar23 - Convert for suitable use in VDC registers");                
        if(programCounter >= 0x16b && programCounter <= 0x175)
                System.out.printf("\tclearchar - Delete all characters by moving horizontal position to 248 which is off the right hand side.");            
        if(programCounter >= 0x176 && programCounter <= 0x17a)
                System.out.printf("\tWAITVSYNC - Wait for Interrupt");
        if(programCounter >= 0x17c && programCounter <= 0x1a0)
                System.out.printf("\ttablebcdbyte - Display BCD 2 digit A at VDC address R1.");              
        if(programCounter >= 0x1a2 && programCounter <= 0x1af)
                System.out.printf("\tStart playing tune. Tune data at 03<Ac>");
        if(programCounter >= 0x22c && programCounter <= 0x239)
                System.out.printf("\ttablechar23 - the BCD Version routine jumps in here. R5 contains the character (from $01A0)");                
        if(programCounter >= 0x261 && programCounter <= 0x269)
                System.out.printf("\tputchar23");        
        if(programCounter >= 0x376 && programCounter <= 0x37d)
                System.out.printf("\tthe tail of the keyboard code");               
        if(programCounter >= 0x2c3 && programCounter <= 0x2f0)
                System.out.printf("\tSelect Game Routine");
        if(programCounter >= 0x3ea && programCounter <= 0x3fe)
                System.out.printf("\tprintchar - Part of \"Select Game\" code");        
        
            
        if(programCounter >= 0x400 && programCounter <= 0xF00)
                System.out.printf("\t_GAME_");
    }
    
    
    public void printInternalRam(int internalRAM[])
    {
        System.out.print("\n\t\tinternalRAM ");
        for(int i=0; i<internalRAM.length; i++)
        {
                if(i%8 == 0)
                        System.out.printf("\n\t\t0x%1$02x: ", i);
                System.out.printf("%1$02x ", internalRAM[i]);
        }
        System.out.printf("\n");
    }
    
    public void printExternalRam(int externalRAM[])
    {
            System.out.print("\n\t\texternalRAM ");
            for(int i=0; i < externalRAM.length; i++)
            {
                    if(i%16 == 0)
                            System.out.printf("\n\t\t0x%1$02x: ", i);
                    System.out.printf("%1$02x ", externalRAM[i]);
            }
            System.out.printf("\n");
    }
    
    public void printVdcRam(int vdcRAM[])
    {
        System.out.print("\n\t\tVDC ");
        for(int i=0; i < vdcRAM.length; i++)
        {
                if(i%16 == 0)
                        System.out.printf("\n\t\t0x%1$02x: ", i);
                System.out.printf("%1$02x ", vdcRAM[i]);
        }
        System.out.printf("\n");
    }
    
    public void printOpCode(int opCode, int opCodeData)
    {
        System.out.printf("0x%1$02x ", opCode);
        if(opCodeLength[opCode] == 1)
        {
            System.out.printf("%1$02x", opCodeData);
        }
        else
            System.out.printf("  ");
    }
    
    public void printPreviousProgramCounter(int previousProgramCounter)
    {
        System.out.printf("0x%1$04x; ", previousProgramCounter);
    }
  
    public void printOperation(int opCode, int opCodeData)
    {
        System.out.print(" ;" + opCodeMatrix[opCode]);
        if(opCodeLength[opCode] == 1)
        {
            System.out.printf("%1$02x", opCodeData);
        }
        else
            System.out.print("   ");
        System.out.print(" ");
    }
    
}
