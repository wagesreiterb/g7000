
package g7000;

import java.io.*;
import java.util.*;

//Kontrolle von JMP wegen Bank Switching
//Jump kommt nur bis 4097 was ist wenn gr�er
//
//BS bei ROMs gr�er 2048, Game beginnt bei 4K ROMs bei 0x0c00 (siehe o2em)  

//SEL RB0, laut Grokking ist cycles 1, laut debugger vom o2em 2 cyclen???
//MOV R1,A und MOV R1,A -> im o2em cycle=0;

//0x80, 0x81 o2em scheisst auf die VDC Register
//if(accessVDC==true && internalRAM[0 + registerBankSelectPointer]==0xa2)	//0xa2 is the Collision Register
//	RAMPointer[internalRAM[0 + registerBankSelectPointer]] = 0;		//reading this Register clears it



public class CPU {
	
	//Beginn der CPU
	//Java doesn't support 'unsigned byte', therefore I use int
	private int programCounter; //12Bit, independent counter 
	private int accumulator;
	private int bus;
	private int port1;  //= p10-p17; 8 Bit
	private int p10 = 1, p11 = 1, p12 = 1, p13 = 1, 
                    p14 = 1, p15 = 1, p16 = 1, p17 = 1; 	//1 Bit
        private int port2;
	private int dbf;    //DBF = 0; Select Bank 0 (location 0 - 2047) of Program Memory
                            //DBF = 1; Select Bank 1 (location 2048 - 4095) of Program Memory
	
	//PSW initialized with 4 because Bit 4 is not used but is set to 1
	private int psw; //ProgramStatusWord (8 Bit)
	//Bits of PSW
	private int carryFlag;		//Bit 7
	private int auxiliaryCarry;     //Bit 6
	private int flag0;              //Bit 5
	private int registerBankSelect;		//Bit 4 
	private int pswBit3;		//Bit 3
	public int stackPointer;       //Bit 0-2
	//==Bits of PSW	
        
        //Timing
	private int internalTimer,              // internal Timer
                    internalTimerPrescaler;     // 32 Cylcels
        private boolean internalTimerStarted;
        private boolean eventCounterStarted;     //ether internalTimer or eventCounter or nothing
                                                //see figure 12 in Grokking the MCS-48
        private boolean timerFlag;
        private boolean timerOverfolwFlag;
        private boolean timerIRQEnabled;                //(tcnti Flag) Timer/Counter Interrupt (Timer ITQ)
        private boolean timerIRQPending;
        //-----Timing
        
	private int cycles = 1;
	private int flag1 = 1;

	private int internalRAM[] = new int[64];	//on Chip (8048) RAM 64 Byte 
	//private int externalRAM[] = new int[256];	//external RAM 256 Byte
							//P13=1; P14=0
							//maybe only 128 Byte (MCM6810)??? on my G7000
	
        //public int vdcRAM[] = new int[256];		//RAM of the VDC 256 Byte
							//P13=0; P14=1
	
        private int RAMPointer[];			//VDC or external RAM, depends on P13 and P14
        
        //Registers
        //private int R0, R1;
        
	private boolean externalIRQEnabled;	//set or unset wiht EN I or DIS I
        public boolean externalIRQPending;
        private int IRQ;
	
	//private int biosBinarySize;
	//private int gameBinarySize;
	//private int binarySize;
	private int[] memory;
        private int externalRAM[];
        public int vdcRAM[];
        private VirtualMachine virtualMachine;
	
	
	//Help Variables
	private int dataHelp;
	private boolean accessVDC;
	private boolean accessExtRAM;
	private int registerBankSelectPointer;
	private int clock;
	
	//debug only
	public String command = "";
	private boolean stopIt = false;
        private int previousProgramCounter;
        private int previousOpCode;
        private int debug_clk;
        
        private int debug_x = 0;
	//===debug only

	public CPU(int[] mem, int[] eR, int[] vR, VirtualMachine vm)
	{
            memory = mem;
            externalRAM = eR;
            vdcRAM = vR;
            RAMPointer = vdcRAM;
            virtualMachine = vm;
            this.reset();
	}


	public void nextStep(VirtualMachine virtualMachine)
	{
			//INT' is sampled each cycle
			//IRQ?   INT' must be held low for at least 3 cycles
			//save PC und PSW in Stack
			//call to subroutine at location 3 in program memory
			//	as soon as all cycles of the current instruction are complete
			
                        //System.out.printf("ACC: %1$02x, P1: %2$02x\n", accumulator, port1);
                        //access the VDC
                        if(p13 == 0 && p14 == 1)
                        {	
                                accessVDC = true;
                                accessExtRAM = false;
                                RAMPointer = vdcRAM;
                                //System.out.println("vdcRAM");
                        }
                        //access the external RAM
                        else if (p13 == 1 && p14 == 0)
                        {		
                                accessVDC = false;
                                accessExtRAM = true;
                                RAMPointer = externalRAM;
                                //System.out.println("extRAM");
                        }
                        //virtualMachine.read_BUS(p12, p13, p14, port2);

                        previousProgramCounter = programCounter;

                        previousOpCode = memory[programCounter];

                        
                        /*
                        System.out.println(programCounter);                        
                        if(programCounter > 1024)
                        {
                            System.out.println(programCounter);
                            System.exit(1);
                        }*/
			
                        
			switch(memory[programCounter])
			{
                                //Checked
				//NOP; Cycle:1; Bytes:1; Flags: no; - PC=PC+1; 
				case 0x00:
					programCounter++;
					//System.out.println("0x00; NOP");
					cycles = 1;
					break;
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x01:
					programCounter++;
					cycles = 1;
					System.out.println("0x01; ILL - Illegal OpCode");
					break;
				
                                //Checked - Not implemented in o2em
				//0x02; OUTL BUS,A;  - (BUS) <- A;
				//WR' (Output strobe) probably no need for emulation???.
				//Cycles: 2; Bytes: 1; Flags: no
				case 0x02:
                                        //Not implemented in o2em
					programCounter++;
					bus=accumulator;
					cycles = 2;
					//System.out.println("0x02; OUTL BUS,A");
					break;
				
                                //Checked
				//ADD A,data
				//(A)<-(A)+data
				//Cycles: 2; Bytes: 2; Flags, C, AC;
				case 0x03: 
					dataHelp = memory[++programCounter];
					programCounter++;
                                        if(((accumulator & 0x0f) + (dataHelp & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + dataHelp;
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 2;
					break;
				
                                //Checked
				//JMP $0??; JMP addr
				//(PC 8-10)<-addr 8-10
                                //(PC 0-7)<-addr 0-7
                                //(PC 11)<-DBF;
				//Bytes: 2; Cycles: 2
				case 0x04: 
					//dataHelp = programCounter + 1;
					//programCounter = 0x000 + memory[dataHelp];
                                        dataHelp = memory[programCounter+1];
                                        programCounter = dataHelp;
					programCounter = programCounter | (dbf * 0x800); //BankSelect
					cycles = 2;
					//System.out.printf("0x04: JMP $%1$03x\n", programCounter);
					break;
				
                                //Checked
				//EN I
				//Enable external interrupt requests
				//Bytes: 1; Cycles: 1; Flags: no						
				case 0x05:
					programCounter++;
					externalIRQEnabled = true;
					cycles = 1;
					//System.out.printf("0x05: EN I\n");
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x06:
                                    	programCounter++;
					cycles = 1;
					System.out.println("0x06; ILL - Illegal OpCode - Call Que");
					break;
                                
                                //Checked
                                //DEC A
                                //(A)<-(A)-1
                                //Bytes: 1, Cycles: 1; Flags: no
				case 0x07:
                                    	programCounter++;
					accumulator = accumulator - 1 & 0xFF;
					cycles = 1;
					break;
                                
                                //Checked
                                //INS A,BUS
                                //(A)<-(BUS)
                                //Bytes 1, Cycles 2, 
				case 0x08: 
                                        programCounter++;
                                        //accumulator = bus;
                                        //accumulator = read_BUS();
                                        accumulator = virtualMachine.read_BUS(p12, p13, p14, port2);
                                        cycles = 2;
					break;
					
                                //Checked
				//IN A,P1
				//(A)<-(P1)
				//Bytes 1; Cycles 2
				case 0x09: 
					accumulator = port1;
					programCounter++;
					cycles = 2;
                                        break;
                                
                                //NotChecked (aber schon angeschaut)
				//IN A,P2
				//(A)<-(P2)
				//Bytes 1; Cycles 2				
				case 0x0a:
                                        //System.out.printf("PORT2: %1$02Xh\n", port2);
                                        //port2 = (port2 & 0x0f) | 0x60;
                                        //System.out.printf("PORT2: %1$02Xh\n", port2);
                                        accumulator = port2;
                                        //accumulator = 0x00;
					programCounter++;
					cycles = 2;
					//System.out.printf("0x0a: IN A,P2\n");
					break;
                                
                                //Checked
                                //ILL
                                //Illegal OpCode
				case 0x0b:
                                        programCounter++;
					cycles = 1;
					System.out.println("0x0b; ILL - Illegal OpCode");
					break;
                                   
                                //VPP
				case 0x0c: System.out.println("0x0c fixme");stopIt=true;
					break;
                                        
                                //VPP
				case 0x0d: System.out.println("0x0d fixme");stopIt=true;
					break;
                                        
                                //VPP
				case 0x0e: System.out.println("0x0e fixme");stopIt=true;
					break;
                                        
                                //VPP
				case 0x0f: System.out.println("0x0f fixme");stopIt=true;
					break;
                                        
                                //Checked
				//INC @R0
				//((R0))<-((R0))+1
				//Bytes: 1; Cycles: 1; Flags: no						
				case 0x10:
					programCounter++;
					internalRAM[internalRAM[0 + registerBankSelectPointer] & 0x3F] = internalRAM[internalRAM[0 + registerBankSelectPointer] & 0x3F] + 1 & 0xFF;
					cycles = 1;
					break;
				
                                //Checked        
				//INC @R1
				//((R1))<-((R1))+1
				//Bytes: 1; Cycles: 1; Flags: no						
				case 0x11:
					programCounter++;
					internalRAM[internalRAM[1 + registerBankSelectPointer] & 0x3F] = internalRAM[internalRAM[1 + registerBankSelectPointer] & 0x3F] + 1 & 0xFF;
					cycles = 1;
					break;
                                
                                //Checked       
                                //JB0 addr
				//if AccumulatorBit0 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit0 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2 					
				case 0x12:
                                        dataHelp = memory[++programCounter];
					if((accumulator & 0x01) == 0x01)                        //Is Bit 0 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else							//Bit 0 is set to 0
					{
						programCounter++;
					}
					cycles = 2;
					break;
                                        
                                //Checked
                                //ADDC A,data
                                //(A)<-(A)+(C)+data
                                //Bytes 2, Cycles 2, Flags: C, AC
				case 0x13:
                                    	dataHelp = memory[++programCounter];
					programCounter++;
                                        if(((accumulator & 0x0f) + (dataHelp & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + dataHelp + carryFlag;
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 2;
					break;
				
                                //Checked
				//CALL $0??
				//((SP)<-(PC),(PSW 4-7))
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no
				case 0x14:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0); 
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;
					programCounter = (dbf * 0x800) | 0x0000 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;
					
                                //Checked
				//DIS I
				//Disable external interrupt requests
				//Bytes: 1; Cycles: 1; Flags: no						
				case 0x15:
					programCounter++;
					externalIRQEnabled = false;
					cycles = 1;
					break;
				
                                //Checked
                                //JTF
                                //Jump if Timer/Counter Flag is set
                                //(PC 0-7) <- addr; if T = 1
                                //(PC)<-(PC)+2; if T = 0
                                //Bytes: 2, Cylces 2; Flags: no
				case 0x16:
					dataHelp = memory[++programCounter];
					if(timerFlag == true)                        //Is Bit 1 set to 1?
					{
						programCounter = (programCounter & 0xff00) | dataHelp;	//set Bit 0-7 to 0
					}
					else							//Bit 1 is set to 0
					{
						programCounter++;
					}
                                        timerFlag=false;
					cycles = 2;
					break;
				
                                //Checked
				//INC A
				//(A)<-(A)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x17:
					programCounter++;
					accumulator = accumulator + 1 & 0xFF;
					cycles = 1;
					break;
				
                                //Checked
				//INC R0
				//(R0)<-(R0)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x18:
					programCounter++;
					internalRAM[0 + registerBankSelectPointer] = internalRAM[0 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;					
					break;

                                //Checked
				//INC R1
				//(R1)<-(R1)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x19:
					programCounter++;
					internalRAM[1 + registerBankSelectPointer] = internalRAM[1 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;
					break;
				
                                //Checked
				//INC R2
				//(R2)<-(R2)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x1a:
					programCounter++;
					internalRAM[2 + registerBankSelectPointer] = internalRAM[2 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;					
					break;

                                //Checked
				//INC R3
				//(R3)<-(R3)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x1b:
					programCounter++;
					internalRAM[3 + registerBankSelectPointer] = internalRAM[3 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;					
					break;

                                //Checked
				//INC R4
				//(R4)<-(R4)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x1c:
					programCounter++;
					internalRAM[4 + registerBankSelectPointer] = internalRAM[4 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;
					break;
                                        
                                //Checked
				//INC R5
				//(R5)<-(R5)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x1d:
					programCounter++;
					internalRAM[5 + registerBankSelectPointer] = internalRAM[5 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;
					break;
                                        
                                //Checked
				//INC R6
				//(R6)<-(R6)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x1e:
					programCounter++;
					internalRAM[6 + registerBankSelectPointer] = internalRAM[6 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;					
					break;
			
                                //Checked
				//INC R7
				//(R7)<-(R7)+1
				//Bytes: 1; Cycles: 1; Flags: no
				case 0x1f:
					programCounter++;
					internalRAM[7 + registerBankSelectPointer] = internalRAM[7 + registerBankSelectPointer] + 1 & 0xFF;
					cycles = 1;					
					break;
                                
                                //Checked
                                //XCH A,@R0
                                //Exchange Accumulator and Data Memory Contents
                                //(A)<->((R0))
                                //Bytes: 1, Cycles1, Flags: no
				case 0x20:
                                        programCounter++;
                                        dataHelp = accumulator;
                                        accumulator = internalRAM[this.getRegister(0) & 0x3f];
                                        internalRAM[this.getRegister(0) & 0x3f] = dataHelp;
                                        cycles = 1;
					break;

                                //Checked
                                //XCH A,@R1
                                //Exchange Accumulator and Data Memory Contents
                                //(A)<->((R1))
                                //Bytes: 1, Cycles1, Flags: no                                        
				case 0x21: 
                                        programCounter++;
                                        dataHelp = accumulator;
                                        accumulator = internalRAM[this.getRegister(1) & 0x3f];
                                        internalRAM[this.getRegister(1) & 0x3f] = dataHelp;
                                        cycles = 1;
					break;
                                
                                //Checked
                                //ILL
                                //Illegal OpCode
				case 0x22:
                                    	programCounter++;
					cycles = 1;
					System.out.println("0x22; ILL - Illegal OpCode");
					break;
					
                                //Checked
				//MOV A,data
				//(A)<-data
				//Bytes: 2; Cycles: 2
				case 0x23:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Byte
					accumulator = dataHelp;
					cycles = 2;
					break;

                                //Checked
				//JMP $1??; JMP addr
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF;	
				//Bytes: 2; Cycles: 2					
				case 0x24:
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x100 | (dbf * 0x800); //BankSelect
					cycles = 2;
					break;
                                
                                //Checked
                                //EN TCNTI
                                //Enable Timer/Counter Interrupt
                                //Bytes: 1, Cycles: 2; Flags: no
				case 0x25: 
                                        programCounter++;
                                        timerIRQEnabled = true;
                                        cycles = 1;
					break;
                                
                                //NotChecked
                                //JNT0 $
                                //Jump if Test 0 (T0) is Low        
                                //Cycles: 2; Bytes: 2; Flags: no
				case 0x26: 
					break;

                                //Checked
				//CLR A
				//(A)<-0
				//Bytes: 1, Cycles: 1						
				case 0x27:
					programCounter++;
                                        accumulator = 0;
					cycles = 1;
					break;

                                //Checked                                
				//XCH A,R0
				//Bytes: 1; Cylces: 1
				//(A)<->(R0)
				case 0x28:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[0 + registerBankSelectPointer];
					internalRAM[0 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;

                                //Checked
				//XCH A,R1
				//Bytes: 1; Cylces: 1
				//(A)<->(R1)							
				case 0x29:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[1 + registerBankSelectPointer];
					internalRAM[1 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;
                                        
                                //Checked
				//XCH A,R2
				//Bytes: 1; Cylces: 1
				//(A)<->(R2)	
				case 0x2a:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[2 + registerBankSelectPointer];
					internalRAM[2 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;
                                        
                                //Checked
				//XCH A,R3
				//Bytes: 1; Cylces: 1
				//(A)<->(R3)		
				case 0x2b:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[3 + registerBankSelectPointer];
					internalRAM[3 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;

                                //Checked                                        
				//XCH A,R4
				//Bytes: 1; Cylces: 1
				//(A)<->(R4)								
				case 0x2c:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[4 + registerBankSelectPointer];
					internalRAM[4 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;

                                //Checked   
				//XCH A,R5
				//Bytes: 1; Cylces: 1
				//(A)<->(R5)							
				case 0x2d:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[5 + registerBankSelectPointer];
					internalRAM[5 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;

                                //Checked   
				//XCH A,R6
				//Bytes: 1; Cylces: 1
				//(A)<->(R6)
				case 0x2e:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[6 + registerBankSelectPointer];
					internalRAM[6 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;

                                //Checked   
				//XCH A,R7
				//Bytes: 1; Cylces: 1
				//(A)<-->(R7)
				case 0x2f:
					programCounter++;
					dataHelp = accumulator;
					accumulator = internalRAM[7 + registerBankSelectPointer];
					internalRAM[7 + registerBankSelectPointer] = dataHelp;
					cycles = 1;
					break;
				
                                //Checked
                                //XCHD A,@R0
                                //Exchange indirect 4-bit contents of Accumulator and data memory
                                //(A 0-3) <-->((R0) 0-3)
                                //Bytes: 1; Cycles: 1; Flags: no
				case 0x30: 
                                        programCounter++;
                                        dataHelp = accumulator;
                                        accumulator &= 0xf0;
                                        accumulator |= (internalRAM[this.getRegister(0) & 0x3f] & 0x0f);   //accu = Bit 0-3 from @R0
                                        internalRAM[this.getRegister(0) & 0x3f] &= 0xf0;
                                        internalRAM[this.getRegister(0) & 0x3f] |= dataHelp & 0x0f; 
					break;
 
                                //Checked
                                //XCHD A,@R1
                                //Exchange indirect 4-bit contents of Accumulator and data memory
                                //(A 0-3) <-->((R0) 0-3)
                                //Bytes: 1; Cycles: 1; Flags: no
				case 0x31: 
                                        programCounter++;
                                        dataHelp = accumulator;
                                        accumulator &= 0xf0;
                                        accumulator |= (internalRAM[this.getRegister(1) & 0x3f] & 0x0f);   //accu = Bit 0-3 from @R0
                                        internalRAM[this.getRegister(1) & 0x3f] &= 0xf0;
                                        internalRAM[this.getRegister(1) & 0x3f] |= dataHelp & 0x0f; 
					break;	
                                
                                //Checked
				//JB1 addr
				//if AccumulatorBit1 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit1 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2                                         
				case 0x32:
					dataHelp = memory[++programCounter];
					if((accumulator & 0x02) == 0x02)                        //Is Bit 1 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else							//Bit 1 is set to 0
					{
						programCounter++;
					}
					cycles = 2;                              
					break;              
                                
                                //Checked
                                //ILL
                                //Illegal OpCode        
				case 0x33:
                                    	programCounter++;
					cycles = 1;
					System.out.println("0x33; ILL - Illegal OpCode");
					break;					

                                //Checked
				//CALL $1??
				//((SP)<-(PC),(PSW 4-7))
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF		
				//Bytes 2, Cycles 2, Flags: no
				case 0x34:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;
					programCounter = (dbf * 0x800) | 0x0100 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;
 
                                //Checked       
                                //DIS TCNTI (Disable Timer/Counter Interrupt)
                                //Bytes: 1, Cycles: 1
				case 0x35:
                                        programCounter++;
                                        timerIRQEnabled = false;
                                        timerIRQPending = false;
                                        cycles = 1;
					break;
                                
                                //NotChecked
                                //JT0 $
                                //Jump if Test 0 is High
                                //(PC 0-7) <- addr; if T0 = 1
                                //(PC) <- (PC) + 2; if T0 = 0
				case 0x36: 
                                        dataHelp = memory[++programCounter];
                                        if(virtualMachine.read_T0() == true)
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else
					{
						programCounter++;
					}
					cycles = 1;  
					break;
				
                                //Checked
				//CPL A
				//(A)=!(A)
				//Bytes: 1, Cycles: 1						
				case 0x37:
					programCounter++;
					accumulator = ~accumulator & 0xff;
					cycles = 1;
					break;
				
                                //Checked
                                //ILL
                                //Illegal OpCode
				case 0x38: 
                                        programCounter++;
					cycles = 1;
					System.out.println("0x38; ILL - Illegal OpCode");
					break;
				
                                //Checked
				//OUTL P1,A
				//(P1)<-(A)
				//Bytes:1; Cycles: 2; Flags: no
				case 0x39:
					programCounter++;
                                        port1 = virtualMachine.writePort1(accumulator);
					//port1 = accumulator;
					cycles = 2;
					break;
                                
                                //checked
				//OUTL P2,A
				//(P2)<-(A)
				//Bytes:1; Cycles: 2; Flags: no					
				case 0x3a: 
                                    	programCounter++;
					port2 = accumulator;
					cycles = 2;
                                        break;
                                
                                        
                                //Checked
                                //ILL
                                //Illegal OpCode
				case 0x3b: 
                                        programCounter++;
					cycles = 1;
					System.out.println("0x3b; ILL - Illegal OpCode");
					break;
                                

                                //VPP
                                //MOVD P4,A
				case 0x3c: System.out.println("0x3c fixme");stopIt=true;
					break;
                                        
                                //VPP
                                //MOVD P5,A
				case 0x3d: System.out.println("0x3d fixme");stopIt=true;
					break;
                                        
                                //VPP
                                //MOVD P6,A
				case 0x3e: System.out.println("0x3e fixme");stopIt=true;
					break;
                                        
                                //VPP
                                //MOVD P7,A
				case 0x3f: System.out.println("0x3f fixme");stopIt=true;
					break;
                                
                                //Cheched        
                                //ORL A,@R0
                                //(A) <- (A) OR ((R0))
                                //Bytes: 1; Cycles: 1; Flags: no
				case 0x40: 
                                        programCounter++;
                                        accumulator |= internalRAM[this.getRegister(0) & 0x3f];
					cycles = 1;
                                        break;
                                                
                                //Cheched                                        
                                //ORL A,@R1
                                //(A) <- (A) OR ((R1))
                                //Bytes: 1; Cycles: 1; Flags: no                                        
				case 0x41: 
                                        programCounter++;
                                        accumulator |= internalRAM[this.getRegister(1) & 0x3f];
					cycles = 1;                                   
					break;
                                 
                                //Checked        
                                //MOV A,T
                                //(A)<-(T)
                                //Cycles: 1, Bytes: 1
				case 0x42:
                                    	programCounter++;
					accumulator = internalTimer;
					cycles = 1;
					break;
				
                                //Checked        
				//ORL A,data
				//(A)<-(A) OR data
				//Bytes: 2, Cycles: 2							
				case 0x43:
					dataHelp = memory[++programCounter];
					programCounter++;
					accumulator = accumulator | dataHelp;
					cycles = 2;
					break;
				
                                //Checked        
				//JMP $2??; JMP addr
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF;	
				//Bytes: 2; Cycles: 2
				case 0x44:
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x200 | (dbf * 0x800); //BankSelect
					cycles = 2;
					break;
				       
                                //STRT CNT
                                //Start Event Counter
                                //see figure 12 in Grokking the MCS-48
                                //Bytes: 1; Cycles: 1; Flags: no
				case 0x45:
                                        programCounter++;
                                        this.startEventCounter();
                                        //siehe figure 12
                                        //???
                                        this.stopInternalTimer();
                                        cycles=1;
					break;
                                
                                //JNT1 addr
                                //Jump if Test 1 is Low
                                //(PC 0-7) <- addr, if T1 = 0
                                //(PC) <- (PC) + 2; if T1 = 1
				case 0x46: System.out.println("0x46 fixme");stopIt=true;
					break;
                                 
                                //Checked
                                // SWAP A
                                // swap Nibbles withing accumulator
                                //(A 4 - 7) <--> (A 0 - 3)
                                //Bytes: 1; Cycles:1; Flags: no 
				case 0x47:
                                        programCounter++;
                                        dataHelp = (accumulator >> 4) & 0x0F;
                                        accumulator = ((accumulator << 4) & 0xF0) | dataHelp;
                                        cycles = 1;
					break;

                                //Checked
				//ORL A,R0
				//(A)<-(A) OR (R0)
				//Bytes: 1, Cycles: 1								
				case 0x48:
					programCounter++;
					accumulator = (accumulator | internalRAM[0 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
                                //Checked					
				//ORL A,R1
				//(A)<-(A) OR (R1)
				//Bytes: 1, Cycles: 1								
				case 0x49:
					programCounter++;
					accumulator = (accumulator | internalRAM[1 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
				//Checked	
				//ORL A,R2
				//(A)<-(A) OR (R2)
				//Bytes: 1, Cycles: 1								
				case 0x4a:
					programCounter++;
					accumulator = (accumulator | internalRAM[2 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
                                //Checked					
				//ORL A,R3
				//(A)<-(A) OR (R3)
				//Bytes: 1, Cycles: 1								
				case 0x4b:
					programCounter++;
					accumulator = (accumulator | internalRAM[3 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
                                //Checked					
				//ORL A,R4
				//(A)<-(A) OR (R4)
				//Bytes: 1, Cycles: 1								
				case 0x4c:
					programCounter++;
					accumulator = (accumulator | internalRAM[4 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
                                //Checked					
				//ORL A,R5
				//(A)<-(A) OR (R5)
				//Bytes: 1, Cycles: 1								
				case 0x4d:
					programCounter++;
					accumulator = (accumulator | internalRAM[5 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
                                //Checked
				//ORL A,R6
				//(A)<-(A) OR (R6)
				//Bytes: 1, Cycles: 1								
				case 0x4e:
					programCounter++;
					accumulator = (accumulator | internalRAM[6 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                        
                                //Checked					
				//ORL A,R7
				//(A)<-(A) OR (R7)
				//Bytes: 1, Cycles: 1								
				case 0x4f:
					programCounter++;
					accumulator = (accumulator | internalRAM[7 + registerBankSelectPointer]);
					cycles = 1;
					break;
                                
                                //Checked       
				//ANL A,@R0
                                //Logical AND Accumulator with Memory Mask
                                //(A) <- (A) AND ((R0))
                                //Bytes: 1, Cycles: 1; Flags: no
				case 0x50: 
					programCounter++;
					accumulator &= internalRAM[this.getRegister(0) & 0x3f];
					cycles = 1;                                        
					break;
                                
                                //Checked
				//ANL A,@R1
                                //Logical AND Accumulator with Memory Mask
                                //(A) <- (A) AND ((R1))
                                //Bytes: 1, Cycles: 1; Flags: no                                        
				case 0x51: 
                                    	programCounter++;
					accumulator &= internalRAM[this.getRegister(1) & 0x3f];
					cycles = 1;       
					break;
                                
                                //Checked
				//JB2 addr
				//if AccumulatorBit2 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit2 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2                                        
				case 0x52:
					dataHelp = memory[++programCounter];
					if((accumulator & 0x04) == 0x04)                        //Is Bit 2 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else							//Bit 2 is set to 0
					{
						programCounter++;
					}
					cycles = 2;                                 
					break;
				
                                //Checked       
				//ANL A,data
				//(A)<-(A) AND data
				//Bytes: 2; Cycles: 2						
				case 0x53:
					dataHelp = memory[++programCounter];
					programCounter++;  //ANL needs 2 Byte
					accumulator = accumulator & dataHelp;
					cycles = 2;
					break;
				
                                //Checked
				//CALL $2??
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no							
				case 0x54:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;
					programCounter = (dbf * 0x800) | 0x0200 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;                                     
                                        
                                //STRT T
                                //Start Timer
                                //see figure 12 in Grokking the MCS-48
                                //Bytes: 1, Cycles: 1; Flags: no
				case 0x55: 
                                        programCounter++;
                                        this.startInernalTimer();
                                        //siehe figure 12
                                        //???
                                        this.stopEventCounter();
                                        cycles=1;
					break;
                                        
                                //JT1 $
                                //Jump if Test 1 is High
                                //(PC 0-7) <- addr; if T1 = 1
                                //(PC) <- (PC) + 2; if T1 = 0
                                //Bytes: 2; Cycles: 1; Flags: no
				case 0x56: 
                                        dataHelp = memory[++programCounter];
                                        if(virtualMachine.read_T1() == true)
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else
					{
						programCounter++;
					}
					cycles = 1;  
					break;
                                        
                                //Checked     
                                //DA A
                                //Decimal Adjust Accumulator
                                //The 8-bit value in the Accumulator is modified 
                                //resulting in two BCD-encoded values.
                                //      1) If the value of bits 0 – 3 is greater than 9 or AC is set
                                //         then 6 is added to the contents of the Accumulator.
                                //      2) If the value of bits 4 – 7 is now greater than 9 or C is set
                                //         then 6 is added to the most significant nibble.
                                //Bytes: 1; Cycles: 1; Flags: Carry        
				case 0x57:
					programCounter++;
                                        if((accumulator & 0x0F) > 0x09 || (auxiliaryCarry == 1))
                                        {
                                            accumulator += 0x06;
                                            if(accumulator > 0xFF)
                                            {
                                                carryFlag = 1;
                                                accumulator &= 0xFF;
                                            }
                                        }
                                        if(((accumulator & 0xF0) >> 4) > 0x09 || (carryFlag == 1))
                                        {
                                            carryFlag = 1;
                                            accumulator += 0x60;
                                        }
					cycles = 1;
					break;
                                
                                //Checked
				//ANL A,R0
				//(A) <- (A) AND (R0)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x58:
					programCounter++;
					accumulator &= internalRAM[0 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R1
				//(A) <- (A) AND (R1)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x59:
					programCounter++;
					accumulator &= internalRAM[1 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R2
				//(A) <- (A) AND (R2)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x5a:
					programCounter++;
					accumulator &= internalRAM[2 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R3
				//(A) <- (A) AND (R3)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x5b:
					programCounter++;
					accumulator &= internalRAM[3 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R4
				//(A) <- (A) AND (R4)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x5c:
					programCounter++;
					accumulator &= internalRAM[4 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R5
				//(A) <- (A) AND (R5)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x5d:
					programCounter++;
					accumulator &= internalRAM[5 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R6
				//(A) <- (A) AND (R6)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x5e:
					programCounter++;
					accumulator &= internalRAM[6 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
				//ANL A,R7
				//(A) <- (A) AND (R7)
				//Bytes: 1, Cycles: 1; Flags: no								
				case 0x5f:
					programCounter++;
					accumulator &= internalRAM[7 + registerBankSelectPointer];
					cycles = 1;
					break;
                                        
                                //Checked
                                //ADD A,@R0 (Add Data Memory Contents to Accumulator)
                                //(A)<-(A)+((R0))
                                //Cycles: 1, Bytes: 1; Flags, C, AC;
				case 0x60:
					programCounter++;
                                        dataHelp = internalRAM[(this.getRegister(0) & 0x3f)];   //@R0 addressed by Bits 0-5
                                        if(((accumulator & 0x0f) + (dataHelp & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
                                        accumulator = accumulator + dataHelp;
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;                                    
					break;
                                
                                //Checked
                                //ADD A,@R1 (Add Data Memory Contents to Accumulator)
                                //(A)<-(A)+((R1))
                                //Cycles: 1, Bytes: 1; Flags, C, AC;
				case 0x61:
					programCounter++;
                                        dataHelp = internalRAM[(this.getRegister(1) & 0x3f)];   //@R1 addressed by Bits 0-5
                                        if(((accumulator & 0x0f) + (dataHelp & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
                                        accumulator = accumulator + dataHelp;
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;                                    
					break;
                                        
				//Checked	
				//MOV T,A
				//(T)<-(A)
				//Bytes: 1; Cycles: 1
				case 0x62:
					programCounter++;
					internalTimer = accumulator;
					cycles = 1;
					break;
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x63:
					programCounter++;
					cycles = 1;
					System.out.println("0x63; ILL - Illegal OpCode");
					break;
                                                                                
                                //Checked        
				//JMP $3??; JMP addr
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF;
				//Bytes: 2; Cycles: 2                                        
				case 0x64:
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x300 | (dbf * 0x800); //BankSelect
					cycles = 2;
					break;
                                        
                                //Checked         
                                //STOP TCNT
                                //Stop Timer/Counter
                                //see figure 12 in Grokking the MCS-48
                                //Bytes: 1; Cycles: 1; Flags: no
				case 0x65:
                                        programCounter++;
                                        this.stopInternalTimer();
                                        this.stopEventCounter();
                                        cycles = 1;
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x66:
					programCounter++;
					cycles = 1;
					System.out.println("0x66; ILL - Illegal OpCode");
					break;   
                                        
                                //Checked        
                                //RRC A
				//(A n)<-(A n+1); n = 0-6
				//(A 7)<-(C)
				//(C)<-(A 0)
                                //Cycles: 1; Bytes 1
				case 0x67:
					programCounter++;
                                        dataHelp = accumulator & 0x01;      //save new CarryFlag
					accumulator = accumulator >> 1;
                                        if(carryFlag == 1)                                            
                                            accumulator = accumulator | 0x80;
                                        else
                                            accumulator = accumulator & 0x7F;
					carryFlag = dataHelp;
					cycles = 1;
					break;
                                        
                                //Checked    	
				//ADD A,R0
				//(A)<-(A)+(R0)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x68:
					programCounter++;
					//if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(0), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(0) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[0 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(0);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  					
				//ADD A,R1
				//(A)<-(A)+(R1)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x69:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
                                        //	((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(1), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(1) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[1 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(1);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  
				//ADD A,R2
				//(A)<-(A)+(R2)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x6a:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
					//		((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(2), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(2) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[2 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(2);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  				
				//ADD A,R3
				//(A)<-(A)+(R3)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x6b:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
					//		((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(3), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(3) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[3 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(3);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  					
				//ADD A,R4
				//(A)<-(A)+(R4)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x6c:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
					//		((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(4), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(4) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[4 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(4);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  					
				//ADD A,R5
				//(A)<-(A)+(R5)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x6d:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
					//		((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(5), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(5) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[5 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(5);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  						
				//ADD A,R6
				//(A)<-(A)+(R6)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x6e:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
					//		((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(6), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(6) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[6 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(6);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked  						
				//ADD A,R7
				//(A)<-(A)+(R7)
				//Cycles: 1; Bytes: 1; Flags, C, AC;						
				case 0x6f:
					programCounter++;
					//if(((accumulator & 0x00000008) == 0x00000008) &&
					//		((dataHelp & 0x00000008) == 0x00000008))
                                        //if(this.isBitTrue(accumulator, 3) && 
                                        //        this.isBitTrue(this.getRegister(7), 3))
                                        if(((accumulator & 0x0f) + (this.getRegister(7) & 0x0f)) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					//accumulator = accumulator + internalRAM[7 + registerBankSelectPointer];
                                        accumulator = accumulator + this.getRegister(7);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked        
                                //ADDC A,@R0
                                //Add Carry and Data Memory Contents to Accumulator
                                //(A) <- (A)+(C)+((R0))
                                //Bytes 1, Cycles 1, Flags: C, AC
				case 0x70:
					programCounter++;
                                        dataHelp = internalRAM[(this.getRegister(0) & 0x3f)];   //@R1 addressed by Bits 0-5
                                        if(((accumulator & 0x0f) + (dataHelp & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
                                        accumulator = accumulator + dataHelp + carryFlag;
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;   
					break;
                                        
                                //Checked                                                                     
                                //ADDC A,@R1
                                //Add Carry and Data Memory Contents to Accumulator
                                //(A) <- (A)+(C)+((R1))
                                //Bytes 1, Cycles 1, Flags: C, AC
				case 0x71:
					programCounter++;
                                        dataHelp = internalRAM[(this.getRegister(1) & 0x3f)];   //@R1 addressed by Bits 0-5
                                        if(((accumulator & 0x0f) + (dataHelp & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
                                        accumulator = accumulator + dataHelp + carryFlag;
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;   
					break;
				
                                //Checked
				//JB3 addr
				//if AccumulatorBit3 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit3 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2
				case 0x72:
					dataHelp = memory[++programCounter];
					if((accumulator & 0x08) == 0x08)	//Is Bit 3 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else								//Bit 3 is set to 0
					{
						programCounter++;
					}
					cycles = 2;
					break;
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x73:
					programCounter++;
					cycles = 1;
					System.out.println("0x73; ILL - Illegal OpCode");
					break;	
				
                                //Checked
				//CALL $3??
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no
				case 0x74: 
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;                                        
					programCounter = (dbf * 0x800) | 0x0300 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;
					
				//ENT0 CLK
                                //Enable Clock Output      
				case 0x75: System.out.println("0x75 fixme");stopIt=true;
					break;
				
                                //Checked        
				//JF1 addr
                                //Jump if Flag 1 is set
				//(PC 0-7)<-addr; if F1 =1
				//(PC)<-(PC) + 2; if F1 =0
				//Bytes: 2; Cycles: 2							
				case 0x76:
					dataHelp = memory[++programCounter];
					if(flag1 == 1)
					{
						programCounter = (programCounter & 0xff00) | dataHelp;
					}
					else
					{
						programCounter++;
					}
					cycles = 2;
					break;
				
                                //Checked
                                //RR A      (rotate right without carry)
                                //(A n)<-(A n+1); n=0-6
                                //(A 7)<-(A 0)
                                //Bytes: 1; Cycles: 1
				case 0x77:
					programCounter++;
                                        dataHelp = accumulator & 0x01;
					accumulator = accumulator >> 1;
                                        if(dataHelp == 1)                                            
                                            accumulator = accumulator | 0x80;
                                        else
                                            accumulator = accumulator & 0x7F;
                                        cycles = 1;
					break;
                                        
                                //Checked           
                                //ADDC A,R0
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R0)
				case 0x78:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(0) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(0);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked        
                                //ADDC A,R1
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R1)
				case 0x79:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(1) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(1);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked       
                                //ADDC A,R2
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R2)
				case 0x7a:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(2) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(2);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked       
                                //ADDC A,R3
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R3)
				case 0x7b:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(3) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(3);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked
                                //ADDC A,R4
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R4)
				case 0x7c:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(4) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(4);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked        
                                //ADDC A,R5
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R5)
				case 0x7d:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(5) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(5);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked       
                                //ADDC A,R6
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R6)
				case 0x7e:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(6) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(6);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
                                        
                                //Checked
                                //ADDC A,R7
                                //Cycles 1, Bytes 1
                                //(A)<-(A)+(C)+(R7)
				case 0x7f:
                                	programCounter++;
                                        if(((accumulator & 0x0f) + (this.getRegister(7) & 0x0f) + carryFlag) > 0x0f)
					{
						auxiliaryCarry = 1;
					}
					else
					{
						auxiliaryCarry = 0;
					}
					accumulator = accumulator + carryFlag + this.getRegister(7);
					if(accumulator > 0xff)
					{
						accumulator = accumulator & 0xff;
						carryFlag = 1;
					}
					else
					{
						carryFlag = 0;
					}
					cycles = 1;
					break;
						
			
				//MOVX A,@R0
				//(A)<-((R0))
				//Bytes: 1; Cycles: 2
				//---VDC
				case 0x80:
					programCounter++;
                                        //System.out.println("accumulator: " + accumulator);
                                        accumulator = read_BUS(internalRAM[0 + registerBankSelectPointer]);
                                        //System.out.println("accumulator: " + accumulator);
					//accumulator = RAMPointer[internalRAM[0 + registerBankSelectPointer]];
					//if(accessVDC==true && internalRAM[0 + registerBankSelectPointer]==0xa2)	//0xa2 is the Collision Register
					//	RAMPointer[internalRAM[0 + registerBankSelectPointer]] = 0;		//reading this Register clears it
					cycles = 2;
					break;
			
				//MOVX A,@R1
				//(A)<-((R1))
				//Bytes: 1; Cycles: 2
				//---VDC
				case 0x81:
					programCounter++;
                                        //accumulator = RAMPointer[internalRAM[1 + registerBankSelectPointer]];
                                        accumulator = read_BUS(internalRAM[1 + registerBankSelectPointer]);
					//accumulator = RAMPointer[internalRAM[1 + registerBankSelectPointer]];
					//if(accessVDC==true && internalRAM[1 + registerBankSelectPointer]==0xa2)	//0xa2 is the Collision Register
					//{								//reading this Register clears it
					//	RAMPointer[internalRAM[1 + registerBankSelectPointer]] = 0;
					//}
					cycles = 2;
					break;
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x82:
					programCounter++;
					cycles = 1;
					System.out.println("0x82; ILL - Illegal OpCode");
					break;
                                 
                                        /*
                                //NotChecked	
				//RET
				//(SP)<-(SP)-1
				//(PC)<-((SP))
				//PSW is not restored
				//Bytes: 1; Cycles: 2					
				case 0x83:
					stackPointer--;
                                        if(stackPointer < 0)
                                            stackPointer = 7;
					programCounter = ((internalRAM[(stackPointer * 2) + 9] & 0x0f) << 8)
							| (internalRAM[(stackPointer * 2) + 8])
                                                        | (internalRAM[(stackPointer * 2) + 9] >> 3) & 0x01;
                                        //System.out.println("-------------------------dbf: " + dbf);
                                        dbf = (internalRAM[(stackPointer * 2) + 9] >> 3) & 0x01;       
                                        //System.out.println("-------------------------dbf: " + dbf);
					cycles = 2;
                                        //System.out.println("PC: " + programCounter);
					break; 
                                        */
                                        
				//NotChecked	
				//RET
				//(SP)<-(SP)-1
				//(PC)<-((SP))
				//PSW is not restored
				//Bytes: 1; Cycles: 2					
				case 0x83:
					stackPointer--;
                                        if(stackPointer < 0)
                                            stackPointer = 7;
					programCounter = ((internalRAM[(stackPointer * 2) + 9] & 0x0f) << 8)
							| (internalRAM[(stackPointer * 2) + 8])
                                                        | (dbf * 0x800);
					cycles = 2;
					break; 
                                        
				//Checked	
				//JMP $4??; JMP addr
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//(PC 11)<-DBF;
				//Bytes: 2; Cycles: 2
				case 0x84: 
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x400 | (dbf * 0x800); //BankSelect
					cycles = 2;                                        
                                        break;
                                        
				//Checked				
				//CLR F0
				//(F0)<-0
				//Bytes: 1, Cycles: 1						
				case 0x85:
					programCounter++;
					flag0 = 0;
					cycles = 1;
					break;
				
                                //JNI addr
                                //Jump if Interrrupt Input is Low
                                //(PC 0-7)<-addr, if I = 0
                                //(PC)<-(PC)+2, if I = 1
                                //Cycles: 2, Bytes 2  
				case 0x86: System.out.println("0x86 fixme");stopIt=true;
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x87:
					programCounter++;
					cycles = 1;
					System.out.println("0x87; ILL - Illegal OpCode");
					break;
                                
                                //ORL BUS, data
				case 0x88: System.out.println("0x88 OpCode Not defined - fixme");stopIt=true;
					break;
                                        
				//Checked	
				//ORL P1,data
				//(p1) <- (p1) OR data
				//Bytes: 2, Cycles: 2
				case 0x89: 
					dataHelp = memory[++programCounter];
					programCounter++;
					port1 = port1 | dataHelp;
					cycles = 2;
					break;
                                        
				//Checked	
				//ORL P2,data
				//(p2) <- (p2) OR data
				//Bytes: 2, Cycles: 2
				case 0x8a: 
					dataHelp = memory[++programCounter];
					programCounter++;
					port2 = port2 | dataHelp;
					cycles = 2;
					break;
                                        
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x8b:
					programCounter++;
					cycles = 1;
					System.out.println("0x8b; ILL - Illegal OpCode");
					break;
                                        
                                //VPP        
                                //ORLD P4,A        
				case 0x8c: System.out.println("0x8c fixme");stopIt=true;
					break;
                                        
                                //VPP        
                                //ORLD P5,A                                 
				case 0x8d: System.out.println("0x8d fixme");stopIt=true;
					break;
                                        
                                //VPP        
                                //ORLD P6,A                                         
				case 0x8e: System.out.println("0x8e fixme");stopIt=true;
					break;
                                        
                                //VPP        
                                //ORLD P7,A                                         
				case 0x8f: System.out.println("0x8f fixme");stopIt=true;
					break;
										
				//MOVX @R0,A
				//((R0))<-(A)
				//Bytes: 1; Cycles: 2
				case 0x90:		
					programCounter++;
                                        //virtualMachine.write_BUS(accumulator, internalRAM[0 + registerBankSelectPointer], p13, p14);
                                        //RAMPointer[internalRAM[0 + registerBankSelectPointer]] = accumulator;
                                        this.write_BUS(internalRAM[0 + registerBankSelectPointer], accumulator);
					cycles = 2;
					break;
					
				//MOVX @R1,A
				//((R1))<-(A)
				//Bytes: 1; Cycles: 2
				case 0x91:
					programCounter++;
					//RAMPointer[internalRAM[1 + registerBankSelectPointer]] = accumulator;
                                        //virtualMachine.write_BUS(accumulator, internalRAM[1 + registerBankSelectPointer], p13, p14);
                                        
                                        
                                        //int register = internalRAM[1 + registerBankSelectPointer];
                                        this.write_BUS(internalRAM[1 + registerBankSelectPointer], accumulator);
                                        
                                        /*
                                        //VDC enabled, RAM disabled
                                        if(p13 == 0)// && p14 == 1)
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
                                            else
                                            {
                                                RAMPointer[register] = accumulator;
                                            }                                            
                                        }
                                        */
					cycles = 2;
					break;
                                        
                                //Checked
                                //JB4 addr
				//if AccumulatorBit4 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit4 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2	                                        
				case 0x92:
					dataHelp = memory[++programCounter];
					if((accumulator & 0x10) == 0x10)                        //Is Bit 4 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else							//Bit 4 is set to 0
					{
						programCounter++;
					}
					cycles = 2;                                  
					break;	     
					
				//RETR
				//(SP)<-(SP)-1
				//(PC)<-((SP))
				//(PSW 4-7)<-((SP))
				//Bytes: 1; Cycles: 2
				//Possibly here is a failure, must be clarified
				//What happens, when RETR but no Interrupt was released??
				case 0x93:	
                                        stackPointer--;
                                        dataHelp=internalRAM[(stackPointer * 2) + 9];
                                        if(stackPointer < 0)
                                            stackPointer = 7;                                        
                                        programCounter = 0;
                                        programCounter = ((dataHelp & 0x0f) << 8)
                                                        | (internalRAM[(stackPointer * 2) + 8])
                                                        | (dbf * 0x800);
                                        carryFlag = ((dataHelp & 0x80) >> 7);
                                        auxiliaryCarry = ((dataHelp & 0x40) >> 6);
                                        flag0 = ((dataHelp & 0x20) >> 5);
                                        registerBankSelect = ((dataHelp & 0x10) >> 4);
                                        if(registerBankSelect == 0)
                                            registerBankSelectPointer = 0;
                                        else
                                            registerBankSelectPointer = 0x18;
                                        IRQ = 0;
					cycles = 2;
					break;
                                /*        
                                //RET
				//(SP)<-(SP)-1
				//(PC)<-((SP))
				//PSW is not restored
				//Bytes: 1; Cycles: 2					
				case 0x83:
					stackPointer--;
                                        if(stackPointer < 0)
                                            stackPointer = 7;
					programCounter = ((internalRAM[(stackPointer * 2) + 9] & 0x0f) << 8)
							| (internalRAM[(stackPointer * 2) + 8])
                                                        | (dbf * 0x800);
					cycles = 2;
					break; 
                                  */      
                                /*        
                                //Checked
                                //CALL $4??                                        
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no					
				case 0x94:
					dataHelp = memory[++programCounter] + (dbf * 0x800);
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
                                        internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;                                        
					programCounter = 0x0400 + dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;   
                                 */       
                                        
                                        
                                //Checked
                                //CALL $4??                                        
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no					
				case 0x94:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
                                        internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;                                        
					programCounter = (dbf * 0x800) | 0x0400 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;   
                                     
				//CPL F0
				//(F0)=!(F0)
				//Bytes: 1, Cycles: 1							
				case 0x95:
					programCounter++;
					flag0 = ~flag0 & 0x01;
					cycles = 1;
					//System.out.printf("0x95: CPL F0\n");
					break;
                                        
				//Checked	
				//JNZ $addr
				//(PC 0-7) <- addr; if accu != 0
				//(PC)<-(PC)+2; if Accu == 0
				//Bytes: 2, Cycles: 2
				case 0x96:
					dataHelp = memory[programCounter + 1];
					if(accumulator != 0)
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else
						programCounter = programCounter + 2;
					cycles = 2;
					break;
                                        
                                //Checked
				//CLR C
				//(C)<-0
				//Bytes: 1, Cycles: 1
				case 0x97:
					programCounter++;
					carryFlag = 0;
					cycles = 1;
					break;
					
				case 0x98: System.out.println("0x98 fixme");stopIt=true;
					break;
					
				//ANL P1,date
				//(P1)<-(P1) & data
				//Bytes: 2, Cycles: 2
				case 0x99:
					dataHelp = memory[++programCounter];
					programCounter++;
					port1 = (port1 & dataHelp);
					cycles = 2;
					//System.out.printf("0x99: ANL P1,#%1$02x\n", dataHelp);
					break;
					
				case 0x9a: System.out.println("0x9a fixme");stopIt=true;
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0x9b:
					programCounter++;
					cycles = 1;
					System.out.println("0x9b; ILL - Illegal OpCode");
					break;
                                        
                                //VPP               
                                //ANLD P4,A
                                //Bytes: 1; Cycles: 2; Flags: no        
				case 0x9c: System.out.println("0x9c fixme, VPP");stopIt=true;
					break;
                                        
                                //VPP               
                                //ANLD P5,A
                                //Bytes: 1; Cycles: 2; Flags: no        
				case 0x9d: System.out.println("0x9d fixme, VPP");stopIt=true;
					break;
                                        
                                //VPP               
                                //ANLD P6,A
                                //Bytes: 1; Cycles: 2; Flags: no
				case 0x9e: System.out.println("0x9e fixme, VPP");stopIt=true;
					break;
                                        
                                //VPP               
                                //ANLD P7,A
                                //Bytes: 1; Cycles: 2; Flags: no
				case 0x9f: System.out.println("0x9f fixme , VPP");stopIt=true;
					break;
				
                                //Checked
				//MOV @R0,A
				//((R0))<-(A)
				//Bytes: 1; Cycles: 1						
				case 0xa0:
					programCounter++;
					internalRAM[internalRAM[0 + registerBankSelectPointer] & 0x3F] = accumulator;	//update @R0 or @R0'
					cycles = 1;
					break;
                                        
                                //Checked
				//MOV @R1,A
				//((R1))<-(A)
				//Bytes: 1; Cycles: 1
				case 0xa1:
					programCounter++;
					internalRAM[internalRAM[1 + registerBankSelectPointer] & 0x3F] = accumulator;	//update @R1 or @R1'
					cycles = 1;
					break;
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xa2:
					programCounter++;
					cycles = 1;
					System.out.println("0xa2; ILL - Illegal OpCode");
					break;
                                
                                //Checked
				//MOVP A,@A
				//(PC 0-7)<-(A)
				//(A)<-((PC))
				//Bytes: 1, Cycles: 2							
				case 0xa3:
                                        programCounter++;
                                        accumulator = memory[((programCounter & 0xf00) | accumulator)];
                                        cycles = 2;
                                        break;

                                //Checked       
                                //JMP $5?; JMP addr
				//(PC 8-10)<-addr 8-10; (PC 0-7)<-addr 0-7; (PC 11)<-DBF;
				//Bytes: 2; Cycles: 2                                        
				case 0xa4:
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x500 | (dbf * 0x800); //BankSelect
					cycles = 2;
					break;                                       
					
				//CLR F1
				//(F1)<-0
				//Bytes: 1, Cycles: 1
				case 0xa5:
					programCounter++;
					flag1 = 0;
					cycles = 1;
					//System.out.printf("0xa5: CLR F1\n");
					break;
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xa6:
					programCounter++;
					cycles = 1;
					System.out.println("0xa6; ILL - Illegal OpCode");
					break;
                                        
                                //Checked        
                                //CPL C
                                //Complement Carry Flag
                                //(C) <- NOT(C)
                                //Bytes: 1, Cycles: 1: Flags: C
				case 0xa7: 
                                        programCounter++;
                                        carryFlag ^= 0x01;
                                        cycles = 1;
					break;
                                        
				//Checked	
				//MOV R0,A
				//(R0)<-(A)
				//Cycles: 1, Bytes 1, Flags: no							
				case 0xa8:
                                    	programCounter++;
					internalRAM[0 + registerBankSelectPointer] = accumulator;	//update R0 or R0'
					cycles = 1;						
					break;
                                        
				//Checked					
				//MOV R1,A
				//(R1)<-(A)
				//Cycles: 1, Bytes 1, Flags: no							
				case 0xa9:
					programCounter++;
                                        internalRAM[1 + registerBankSelectPointer] = accumulator;	//update R1 or R1'
					cycles = 1;
					break;
                                        
				//Checked	
				//MOV R2,A
				//(R2)<-(A)
				//Cycles: 1, Bytes 1, Flags: no							
				case 0xaa:
					programCounter++;                                    
					internalRAM[2 + registerBankSelectPointer] = accumulator;	//update R2 or R2'
					cycles = 1;
					break;
                                        
				//Checked					
				//MOV R3,A
				//(R3)<-(A)
				//Cycles: 1, Bytes 1, Flags: no						
				case 0xab:
					programCounter++;                                    
					internalRAM[3 + registerBankSelectPointer] = accumulator;	//update R3 or R3'
					cycles = 1;						
					break;
                                        
				//Checked					
				//MOV R4,A
				//(R4)<-(A)
				//Cycles: 1, Bytes 1, Flags: no						
				case 0xac:
					programCounter++;                                    
					internalRAM[4 + registerBankSelectPointer] = accumulator;	//update R4 or R4'
					cycles = 1;
					break;
                                        
				//Checked					
				//MOV R5,A
				//(R5)<-(A)
				//Cycles: 1, Bytes 1, Flags: no
				case 0xad: 
 					programCounter++;                                   
					internalRAM[5 + registerBankSelectPointer] = accumulator;	//update R5 or R5'
					cycles = 1;
					break;
                                        
				//Checked						
				//MOV R6,A
				//(R6)<-(A)
				//Cycles: 1, Bytes 1, Flags: no
				case 0xae: 
 					programCounter++;                                   
					internalRAM[6 + registerBankSelectPointer] = accumulator;	//update R6 or R6'
					cycles = 1;
					break;
                                        
				//Checked
				//MOV R7,A
				//(R7)<-(A)
				//Cycles: 1, Bytes 1, Flags: no
				case 0xaf:
 					programCounter++;                                     
					internalRAM[7 + registerBankSelectPointer] = accumulator;	//update R7 or R7'
					cycles = 1;
					break;
                                        
				//Checked	
                                //MOV @R0,data
				//((R0 0-5))<-data
                                //The byte that follows the opcode is copied into the Data Memory
                                //location addressed by bits 0-5 of register Rr.
				//Bytes: 2; Cycles: 2
				case 0xb0:
                                        dataHelp = memory[++programCounter];
                                        programCounter++;  //MOV needs 2 Bytes
                                        internalRAM[(internalRAM[0 + registerBankSelectPointer] & 0x3f)] = dataHelp;	//update @R0 or @R0'
					cycles = 2;
					break;
                                        
				//Checked	
                                //MOV @R1,data
				//((R1 0-5))<-data
                                //The byte that follows the opcode is copied into the Data Memory
                                //location addressed by bits 0-5 of register Rr.
				//Bytes: 2; Cycles: 2
				case 0xb1:
                                        dataHelp = memory[++programCounter];
                                        programCounter++;  //MOV needs 2 Bytes
                                        internalRAM[(internalRAM[1 + registerBankSelectPointer] & 0x3f)] = dataHelp;	//update @R1 or @R1'
					cycles = 2;
					break;
                                        
                                //Checked       
                                //JB5 addr
				//if AccumulatorBit5 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit5 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2	                                        
				case 0xb2:
					dataHelp = memory[++programCounter];
					if((accumulator & 0x20) == 0x20)                        //Is Bit 5 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else							//Bit 5 is set to 0
					{
						programCounter++;
					}
					cycles = 2;                                    
					break;
                                        
                                //Checked                                
                                //JMPP @A
                                //Indirect Jump Within Page
                                //(PC 0-7) <- ((A))
                                //Bytes: 1; Cylces: 2; Flags: no
				case 0xb3:                                         
                                        dataHelp = (programCounter & 0xFF00) | accumulator;
                                        programCounter = (programCounter & 0xFF00) | memory[dataHelp];
					cycles = 2;
					break;
                                        
                                //Checked     
                                //CALL $5??
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no                                        
				case 0xb4:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;                                        
					programCounter = (dbf * 0x800) | 0x0500 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;
                                        
                                //Checked 					
				//CPL F1
				//(F1) = !(F1)
				//Bytes: 1, Cycles: 1						
				case 0xb5:
					programCounter++;
					flag1 ^= 0x01;
					cycles = 1;
					break;
                                        
                                //Checked				
                                //JF0 data
                                //Jump if Flag 0 is set
				//(PC 0-7)<-addr; if F0 =1
				//(PC)<-(PC) + 2; if F0 =0
                                //Bytes: 2; Cycles: 2; Flags: no       
                                case 0xb6:
					dataHelp = memory[++programCounter];
					if(flag0 == 1)
					{
						programCounter = (programCounter & 0xff00) | dataHelp;
					}
					else
					{
						programCounter++;
					}
					cycles = 2;
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xb7:
					programCounter++;
					cycles = 1;
					System.out.println("0xb7; ILL - Illegal OpCode");
					break;
                                        
                                //Checked				
				//MOV R0,data
				//(R0)<-data
				//Bytes: 2; Cycles: 2
				case 0xb8:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[0 + registerBankSelectPointer] = dataHelp;	//update R0 or R0'
					cycles = 2;
					break;
                                        
                                //Checked					
				//MOV R1,data
				//(R1)<-data
				//Bytes: 2; Cycles: 2	
				case 0xb9:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[1 + registerBankSelectPointer] = dataHelp;	//update R1 or R1'
					cycles = 2;
					break;
                                        
                                //Checked					
				//MOV R2,data
				//(R2)<-data
				//Bytes: 2; Cycles: 2						
				case 0xba: 
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[2 + registerBankSelectPointer] = dataHelp;	//update R2 or R2'
					cycles = 2;
					break;
                                        
                                //Checked					
				//MOV R3,data
				//(R3)<-data
				//Bytes: 2; Cycles: 2					
				case 0xbb:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[3 + registerBankSelectPointer] = dataHelp;	//update R3 or R3'
					cycles = 2;
					break;
                                        
                                //Checked					
				//MOV R4,data
				//(R4)<-data
				//Bytes: 2; Cycles: 2							
				case 0xbc:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[4 + registerBankSelectPointer] = dataHelp;	//update R4 or R4'
					cycles = 2;
					break;
                                        
                                //Checked
				//MOV R5,data
				//(R5)<-data
				//Bytes: 2; Cycles: 2
				case 0xbd:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[5 + registerBankSelectPointer] = dataHelp;	//update R5 or R5'
					cycles = 2;
					break;
                                        
                                //Checked					
				//MOV R6,data
				//(R6)<-data
				//Bytes: 2; Cycles: 2
				case 0xbe:
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[6 + registerBankSelectPointer] = dataHelp;	//update R6 or R6'
					cycles = 2;						
					break;
                                        
                                //Checked					
				//MOV R7,data
				//(R7)<-data
				//Bytes: 2; Cycles: 2						
				case 0xbf: 
					dataHelp = memory[++programCounter];
					programCounter++;  //MOV needs 2 Bytes
					internalRAM[7 + registerBankSelectPointer] = dataHelp;	//update R7 or R7'
					cycles = 2;
					break;					
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xc0:
					programCounter++;
					cycles = 1;
					System.out.println("0xc0; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xc1:
					programCounter++;
					cycles = 1;
					System.out.println("0xc1; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xc2:
					programCounter++;
					cycles = 1;
					System.out.println("0xc2; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xc3:
					programCounter++;
					cycles = 1;
					System.out.println("0xc3; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
                                //JMP $6?; JMP addr
				//(PC 8-10)<-addr 8-10; (PC 0-7)<-addr 0-7; (PC 11)<-DBF;
				//Bytes: 2; Cycles: 2                                         
				case 0xc4:
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x600 | (dbf * 0x800); //BankSelect
					cycles = 2;
					break;
                                        
				//Checked	
				//SEL RB0
				//(BS)<-0; Bit 4 of PSW
				//Bytes 1, Cycles 1, Flags: no
				case 0xc5:
					registerBankSelect = 0;
					registerBankSelectPointer = 0;
					programCounter++;
					cycles = 1;
					break;
                                        
				//Checked
				//JZ addr
				//(PC 0-7)<-addr, if Accu = 0
				//(PC)-(PC)+2; if Accu != 0
				//Bytes: 2; Cycles: 2
				case 0xc6:
					dataHelp = memory[programCounter + 1];
					if(accumulator == 0x00)
					{
						programCounter = (programCounter & 0xff00) | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;  //JZ needs 2 Byte
					}
					cycles = 2;
					break;
					
				case 0xc7: System.out.println("0xc7 fixme");stopIt=true;
					break;
                                        
				//Checked	
				//DEC R0
				//(R0)<-(R0)-1
				//Bytes: 1; Cycles: 1		
				case 0xc8:
					programCounter++;
					internalRAM[0 + registerBankSelectPointer] = internalRAM[0 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked
				//DEC R1
				//(R1)<-(R1)-1
				//Bytes: 1; Cycles: 1
				case 0xc9:
					programCounter++;
					internalRAM[1 + registerBankSelectPointer] = internalRAM[1 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked
				//DEC R2
				//(R2)<-(R2)-1
				//Bytes: 1; Cycles: 1
				case 0xca:
					programCounter++;
					internalRAM[2 + registerBankSelectPointer] = internalRAM[2 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked
				//DEC R3
				//(R3)<-(R3)-1
				//Bytes: 1; Cycles: 1
				case 0xcb:
					programCounter++;
					internalRAM[3 + registerBankSelectPointer] = internalRAM[3 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked
				//DEC R4
				//(R4)<-(R4)-1
				//Bytes: 1; Cycles: 1
				case 0xcc:
					programCounter++;
					internalRAM[4 + registerBankSelectPointer] = internalRAM[4 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked
				//DEC R5
				//(R5)<-(R5)-1
				//Bytes: 1; Cycles: 1
				case 0xcd:
					programCounter++;
					internalRAM[5 + registerBankSelectPointer] = internalRAM[5 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked					
				//DEC R6
				//(R6)<-(R6)-1
				//Bytes: 1; Cycles: 1	
				case 0xce:
					programCounter++;
					internalRAM[6 + registerBankSelectPointer] = internalRAM[6 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
                                        
				//Checked					
				//DEC R7
				//(R7)<-(R7)-1
				//Bytes: 1; Cycles: 1	
				case 0xcf:
					programCounter++;
					internalRAM[7 + registerBankSelectPointer] = internalRAM[7 + registerBankSelectPointer] - 1 & 0xFF;
					cycles = 1;
					break;
					
				case 0xd0: System.out.println("0xd0 fixme");stopIt=true;
					break;
				case 0xd1: System.out.println("0xd1 fixme");stopIt=true;
					break;
				
                                //Checked
				//JB6 addr
				//if AccumulatorBit6 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit6 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2							
				case 0xd2:
					dataHelp = memory[++programCounter];
                                        //System.out.println("ACCU: " + accumulator);
					if((accumulator & 0x40) == 0x40)	//Is Bit 6 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else								//Bit 6 is set to 0
					{
						programCounter++;
					}
					cycles = 2;
					break;
                                        
				//Checked					
				//XRL A,data
				//(A)<-(A) XOR data
				//Bytes: 2; Cycles: 2							
				case 0xd3:
					dataHelp = memory[++programCounter];
					programCounter++;  //XRL needs 2 Byte
					accumulator = accumulator ^ dataHelp;
					cycles = 2;
					break;
                                        
                                //Checked
                                //CALL $6??
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no                                          
				case 0xd4:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;                                        
					programCounter = (dbf * 0x800) | 0x0600 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;

				//SEL RB1
				//(BS)<-1; Bit 4 of PSW
				//Bytes 1, Cycles 1, Flags: no						
				case 0xd5:
					registerBankSelect = 1;
					registerBankSelectPointer = 0x18;
					programCounter++;
					cycles = 1;
					break;
					
				case 0xd6: System.out.println("0xd6 fixme");stopIt=true;
					break;
				case 0xd7: System.out.println("0xd7 fixme");stopIt=true;
					break;                                       
                                
                                //Checked
                                //XRL A,R0
                                //(A)<-(A) XOR (R0)
                                //Bytes: 1; Cycles: 1; Flags: no  
				case 0xd8:
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[0 + registerBankSelectPointer];
                                        cycles = 1;
					break;
                                        
                                //Checked
                                //XRL A,R1
                                //(A)<-(A) XOR (R1)
                                //Bytes: 1; Cycles: 1; Flags: no  
				case 0xd9:
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[1 + registerBankSelectPointer];
                                        cycles = 1;                          
					break;
                                        
                                //Checked                                  
                                //XRL A,R2
                                //(A)<-(A) XOR (R2)
                                //Bytes: 1; Cycles: 1; Flags: no                                  
				case 0xda:
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[2 + registerBankSelectPointer];
                                        cycles = 1;     
					break;
                                        
                                //Checked                                        
                                //XRL A,R3
                                //(A)<-(A) XOR (R3)
                                //Bytes: 1; Cycles: 1; Flags: no                                            
				case 0xdb:                                         
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[3 + registerBankSelectPointer];
                                        cycles = 1; 
					break;
                                        
                                //Checked                                        
                                //XRL A,R4
                                //(A)<-(A) XOR (R4)
                                //Bytes: 1; Cycles: 1; Flags: no                                             
				case 0xdc:                                         
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[4 + registerBankSelectPointer];
                                        cycles = 1; 
					break;
                                        
                                //Checked                                        
                                //XRL A,R5
                                //(A)<-(A) XOR (R5)
                                //Bytes: 1; Cycles: 1; Flags: no                                            
				case 0xdd:                                         
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[5 + registerBankSelectPointer];
                                        cycles = 1; 
					break;
                                        
                                //Checked                                        
                                //XRL A,R6
                                //(A)<-(A) XOR (R6)
                                //Bytes: 1; Cycles: 1; Flags: no                                             
				case 0xde:                                         
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[6 + registerBankSelectPointer];
                                        cycles = 1; 
					break;
                                        
                                //Checked                                        
                                //XRL A,R7
                                //(A)<-(A) XOR (R7)
                                //Bytes: 1; Cycles: 1; Flags: no                                          
				case 0xdf:                                         
                                        programCounter++;
                                        accumulator = accumulator ^ internalRAM[7 + registerBankSelectPointer];
                                        cycles = 1; 
					break;
                                        
                                //Checked
                                //ILL
				case 0xe0:   
                                        programCounter++;
					cycles = 1;
					System.out.println("0xe0; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
                                //ILL                                        
				case 0xe1:                                         
                                        programCounter++;
					cycles = 1;
					System.out.println("0xe1; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
                                //ILL                                        
				case 0xe2:                                         
                                        programCounter++;
					cycles = 1;
					System.out.println("0xe2; ILL - Illegal OpCode");
					break;
                                        
                                //MOVP3 A,@A       
                                //Move Page 3 Data to Accumulator
                                //(PC 0-7) <- (A)
                                //(PC 8-11) <- 0011
                                //(A) <- ((PC))
                                //Bytes: 1; Cycles: 2; Flags: no
				case 0xe3: 
                                        programCounter++;
                                        accumulator = memory[accumulator | 0x300];
                                        cycles = 2;
					break;
                                
                                //Checked
                                //JMP $7?; JMP addr
				//(PC 8-10)<-addr 8-10; (PC 0-7)<-addr 0-7; (PC 11)<-DBF;
				//Bytes: 2; Cycles: 2                                        
				case 0xe4:
                                        dataHelp = memory[programCounter+1];
					programCounter = dataHelp | 0x700 | (dbf * 0x800); //BankSelect
					cycles = 2;
					break;

                                //SEL MB0
                                //(DBF)<-0
                                //Bytes: 1; Cycles: 1                                        
				case 0xe5:
                                  	programCounter++;
                                        dbf=0;
					cycles = 1;   
					break;
                                        
                                //Checked              
                                //JNC addr
                                //Jump if Carry is Not Set
				//(PC 0-7) <- addr; if Carry = 0
				//(PC) <- (PC) + 2; if Carry =1
				//Bytes: 2; Cycles: 2; Flags: no                                        
				case 0xe6: 
                                        dataHelp = memory[++programCounter];
					if(carryFlag != 1)
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else
					{
						programCounter++;
					}
					cycles = 2;
					break;
                                        
                                //Checked     
                                //RL A (Rotate Left Without Carry)
                                //(A n+1)<-(A n); n=0-6
                                //(A 0)<-(A 7)
                                //Bytes: 1; Cycles: 1; Flags: no
				case 0xe7:
                                        programCounter++;
                                        dataHelp = accumulator & 0x80;
                                        accumulator = accumulator << 1;
                                        if(dataHelp != 0)
                                        {
                                            accumulator = accumulator | 0x01;
                                        }
                                        else
                                        {
                                            accumulator = accumulator & 0xFE;
                                        }
					break;
					
                                //Checked       
				//DJNZ R0 $addr
				//(R0)<-(R0)-1
				//(PC 0-7) <- addr; if R0 != 0
				//(PC)<-(PC)+2; if R0 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page						
				case 0xe8:
					dataHelp = memory[programCounter + 1];
					internalRAM[0 + registerBankSelectPointer] = internalRAM[0 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[0 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;
					break;
                                        
                                //Checked  					
				//DJNZ R1 $addr
				//(R1)<-(R1)-1
				//(PC 0-7) <- addr; if R1 != 0
				//(PC)<-(PC)+2; if R1 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page
				case 0xe9:
					dataHelp = memory[programCounter + 1];
					internalRAM[1 + registerBankSelectPointer] = internalRAM[1 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[1 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;
					break;
                                        
                                //Checked  					
				//DJNZ R2 $addr
				//(R2)<-(R2)-1
				//(PC 0-7) <- addr; if R2 != 0
				//(PC)<-(PC)+2; if R2 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page
				case 0xea:
					dataHelp = memory[programCounter + 1];
					internalRAM[2 + registerBankSelectPointer] = internalRAM[2 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[2 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;						
					break;
                                        
                                //Checked  					
				//DJNZ R3 $addr
				//(R3)<-(R3)-1
				//(PC 0-7) <- addr; if R3 != 0
				//(PC)<-(PC)+2; if R3 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page
				case 0xeb:
					dataHelp = memory[programCounter + 1];
					internalRAM[3 + registerBankSelectPointer] = internalRAM[3 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[3 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;								
					break;
                                        
                                //Checked  
				//DJNZ R4 $addr
				//(R4)<-(R4)-1
				//(PC 0-7) <- addr; if R4 != 0
				//(PC)<-(PC)+2; if R4 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page
				case 0xec:
					dataHelp = memory[programCounter + 1];
					internalRAM[4 + registerBankSelectPointer] = internalRAM[4 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[4 + registerBankSelectPointer] != 0)
					{
                                                //if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;						
					break;
                                        
                                //Checked  
				//DJNZ R5 $addr
				//(R5)<-(R5)-1
				//(PC 0-7) <- addr; if R5 != 0
				//(PC)<-(PC)+2; if R5 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page
				case 0xed:
					dataHelp = memory[programCounter + 1];
					internalRAM[5 + registerBankSelectPointer] = internalRAM[5 + registerBankSelectPointer]- 1 & 0xFF;
					if(internalRAM[5 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;							
					break;
                                        
                                //Checked  
				//DJNZ R6 $addr
				//(R6)<-(R6)-1
				//(PC 0-7) <- addr; if R6 != 0
				//(PC)<-(PC)+2; if R6 == 0
				//Bytes: 2, Cycles: 2
				//if DJNZ instruction is at location 255 of a page then the
				//jump destination will be located in the following page
				case 0xee:
					dataHelp = memory[programCounter + 1];
					internalRAM[6 + registerBankSelectPointer] = internalRAM[6 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[6 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;						
					break;
                                        
                                //Checked
				//DJNZ R7 $addr
				//(R7)<-(R7)-1
				//(PC 0-7) <- addr; if R7 != 0
				//(PC)<-(PC)+2; if R7 == 0
				//Bytes: 2, Cycles: 2  
				//??? if DJNZ instruction is at location 255 of a page then the
				//???jump destination will be located in the following page
				case 0xef:
					dataHelp = memory[programCounter + 1];
					internalRAM[7 + registerBankSelectPointer] = internalRAM[7 + registerBankSelectPointer] - 1 & 0xFF;
					if(internalRAM[7 + registerBankSelectPointer] != 0)
					{
						//if(programCounter % 0xff == 0)	//DJNZ instruction is at location 255 of a page
						//{
						//	programCounter = programCounter + 256;
						//}
						programCounter = programCounter & 0xff00;	//delete Bit 0 - 7
						programCounter = programCounter | dataHelp;
					}
					else
					{
						programCounter = programCounter + 2;
					}
					cycles = 2;							
					break;
                                        
				//Checked	
				//MOV A,@R0
				//(A)<-((R0))
				//Bytes: 1; Cycles: 1						
				case 0xf0:						
					programCounter++;
                                        //System.out.println("@R0: " + this.getRegister(0));
					accumulator = internalRAM[this.getRegister(0) & 0x3f];
                                        //this.debug(true, true,false,false);
                                        //System.out.println("@R0: " + this.getRegister(0));
					cycles = 1;
					break;
                                        
				//Checked
				//MOV A,@R1
				//(A)<-((R1))
				//Bytes: 1; Cycles: 1
				case 0xf1: 
					programCounter++;
					accumulator = internalRAM[this.getRegister(1) & 0x3f];
					cycles = 1;
					break;	
                                        
				//Checked	
				//JB7 addr
				//if AccumulatorBit7 = 1 then (PC 0-7)<-addr
				//if AccumulatorBit7 = 0 then (PC)<-(PC)+2
				//Bytes: 2; Cycles: 2				
				case 0xf2:
					dataHelp = memory[++programCounter];
					if((accumulator & 0x80) == 0x80)	//Is Bit 7 set to 1?
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else								//Bit 7 is set to 0
					{
						programCounter++;
					}
					cycles = 2;
					break;
					
					
                                //Checked
				//ILL
                                //Illegal OpCode
				case 0xf3:
					programCounter++;
					cycles = 1;
					System.out.println("0xf3; ILL - Illegal OpCode");
					break;
                                        
                                //Checked
                                //CALL $7??
				//((SP))<-(PC),(PSW 4-7))  //new PC; is PC+2
				//(SP)<-(SP)+1
				//(PC 8-10)<-addr 8-10
				//(PC 0-7)<-addr 0-7
				//PC 11)<-DBF
				//Bytes 2, Cycles 2, Flags: no                                        
				case 0xf4:
					dataHelp = memory[++programCounter];
					programCounter++;  //CALL need 2 Bytes
					//save Bit 0-8 of the programCounter at the Stack
					internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
					//save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
					internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
					stackPointer++;
                                        if(stackPointer > 7)
                                            stackPointer = 0;                                       
					programCounter = (dbf * 0x800) | 0x0700 | dataHelp;
                                        dataHelp = programCounter;  //for the Debugger
					cycles = 2;
					break;
                                
                                //SEL MB1
                                //(DBF)<-1
                                //Bytes: 1; Cycles: 1
				case 0xf5: 
					programCounter++;
                                        if(IRQ != 0)
                                        {
                                            System.out.println("0xf5");
                                        }
                                        else
                                        {
                                            dbf = 1;
                                        }
					cycles = 1;                                        
					break;
                                        
				//Checked
				//JC addr
				//(PC 0-7)<-addr; if C =1
				//(PC)<-(PC) + 2; if C =0
				//Bytes: 2; Cycles: 2; Flags: no
				case 0xf6:
					dataHelp = memory[++programCounter];
					if(carryFlag == 1)
					{
						programCounter = programCounter & 0xff00 | dataHelp;
					}
					else
					{
						programCounter++;
					}
					cycles = 2;
					break;
				
                                //Checked
				//RLC A
				//(A n+1)<-(A n); n = 0-6
				//(A 0)<-(C)
				//(C)<-(A 7)
				//Bytes: 1; Cycles: 1; Falgs: CY	
				case 0xf7:
					programCounter++;
                                        int oldCarryFlag = carryFlag;
                                        carryFlag=((accumulator & 0x80) >>> 7) & 0x01;
					accumulator = (accumulator << 1) & 0xFF;
                                        if(oldCarryFlag == 1)
                                            accumulator |= 0x01;
                                        else
                                            accumulator &= 0xfe;
					//carryFlag = (accumulator & 0x100) >>> 8;
					//accumulator = accumulator & 0xFF;
					cycles = 1;
					break;
                                        
                                //Checked					
				//MOV A,R0
				//(A)<-(R0)
				//Bytes: 1; Cycles: 1; Falgs: no						
				case 0xf8:
					programCounter++;
					//accumulator = internalRAM[0 + registerBankSelectPointer];
                                        accumulator = this.getRegister(0);
					cycles = 1;
					break;
                                        
                                //Checked					
				//MOV A,R1
				//(A)<-(R1)
				//Bytes: 1; Cycles: 1; Falgs: no
				case 0xf9:
					programCounter++;
					//accumulator = internalRAM[1 + registerBankSelectPointer];
                                        accumulator = this.getRegister(1);
					cycles = 1;				
					break;
                                        
                                //Checked					
				//MOV A,R2
				//(A)<-(R2)
				//Bytes: 1; Cycles: 1; Falgs: no						
				case 0xfa:
					programCounter++;
					//accumulator = internalRAM[2 + registerBankSelectPointer];
                                        accumulator = this.getRegister(2);
					cycles = 1;
					break;
                                        
                                //Checked
				//MOV A,R3
				//(A)<-(R3)
				//Bytes: 1; Cycles: 1; Falgs: no
				case 0xfb:
					programCounter++;
					//accumulator = internalRAM[3 + registerBankSelectPointer];
                                        accumulator = this.getRegister(3);
					cycles = 1;						
					break;
                                        
                                //Checked
				//MOV A,R4
				//(A)<-(R4)
				//Bytes: 1; Cycles: 1; Falgs: no
				case 0xfc:
					programCounter++;
					//accumulator = internalRAM[4 + registerBankSelectPointer];
                                        accumulator = this.getRegister(4);
					cycles = 1;
					break;
                                        
                                //Checked					
				//MOV A,R5
				//(A)<-(R5)
				//Bytes: 1; Cycles: 1; Falgs: no
				case 0xfd: 
					programCounter++;
					//accumulator = internalRAM[5 + registerBankSelectPointer];
                                        accumulator = this.getRegister(5);
					cycles = 1;
					break;
                                        
                                //Checked					
				//MOV A,R6
				//(A)<-(R6)
				//Bytes: 1; Cycles: 1; Falgs: no
				case 0xfe:
					programCounter++;
					//accumulator = internalRAM[6 + registerBankSelectPointer];
                                        accumulator = this.getRegister(6);
					cycles = 1;
					break;
                                        
                                //Checked                                        
				//MOV A,R7
				//(A)<-(R7)
				//Bytes: 1; Cycles: 1; Falgs: no					
				case 0xff:
					programCounter++;
					//accumulator = internalRAM[7 + registerBankSelectPointer];
                                        accumulator = this.getRegister(7);
					cycles = 1;                                 
					break;
                                        
				default: System.out.println("Something wrong! Call Que");
                                        System.exit(0);
			}//switch
			clock = clock + cycles;
                        //this.debug(true,true,false,true);
                        //if(programCounter >= 0x400 && programCounter <= 0xF00)
                        //this.debug(true,false,false,false);
                        
                        //if(programCounter >= 0x089 && programCounter <= 0x0a2)
                        //{
                             //System.out.print("\t_GAME_");
                        
                        
                            //System.out.printf("ACCUMULATOR: %1$02x, PORT1: %2$02x$",accumulator, port1);
                            //this.debug(true,true,false,false);
                            //System.out.println();System.out.println();
                            
                        
                        
                            //if(programCounter >= 0x89 && programCounter <= 0xaf)
                            //    this.debug(true,true,true,true);
                            
                        
                        
                        //}
                        //System.out.print(": " + (debug_clk+=cycles) + ": ");
                        
                        //int fuck = (internalRAM[(stackPointer * 2) + 9] & 0x0f) << 8;
                        //int fuck1 = internalRAM[(stackPointer * 2) + 8];
  
                        
                        
                        //System.out.printf("\t\tP2:%1$02Xh\n", port2);
                        
                        //Internal Timer/Counter
                        if(isInternalTimerStarted())
                        {
                            internalTimerPrescaler += cycles;
                            if(internalTimerPrescaler >= 32)
                            {
                                internalTimer++;
                                internalTimerPrescaler = 0;
                                if(internalTimer > 0xff)
                                {
                                    internalTimer=0;
                                    this.setTimerOverflowFlag(true);
                                    this.triggerTimerIRQ();
                                }
                            }
                        }//---Internal Timer/Counter
                        else if(this.isEventCounterStarted())
                        {
                            System.out.println("Event Counter is started");
                            ///???
                        }
                        
                        
			//internalRAM[8]=0x0d; internalRAM[13]=0xf3;
			psw = (carryFlag * 128) + (auxiliaryCarry * 64) +  (flag0 * 32) + (registerBankSelect * 16)
					+ (pswBit3 * 8) + stackPointer;
			
			p10 = port1 & 0x01; p11 = port1 & 0x02; p12 = port1 & 0x04; p13 = port1 & 0x08;
			p14 = port1 & 0x10; p15 = port1 & 0x20; p16 = port1 & 0x40; p17 = port1 & 0x80;
			p17 = p17 >> 7; p16 = p16 >> 6;  p15 = p15 >> 5; p14 =  p14 >> 4; 
			p13 = p13 >> 3; p12 = p12 >> 2; p11 =  p11 >> 1;
                        

		}//---nextStep
	
	
	public void debug(boolean registers, boolean internalRam, boolean externalRam, boolean vdc)
	{
		if(registers)
		{
			System.out.printf("\t\tPC:0x%1$04x  A:%2$02Xh  BUS:%3$02Xh  DBF:%4$d  T:%5$02Xh", 
					programCounter, accumulator,bus, dbf, port1, internalTimer);
			System.out.printf("\tF1:%1$1x\n", flag1);
			System.out.printf("\t\tPSW:%1$02xh (CY:%2$1x AC:%3$1x F0:%4$1x BS:%5$1x ",
								psw, carryFlag, auxiliaryCarry, flag0, registerBankSelect);
			System.out.printf("NA:%1$1x s2:%2$1x S1:%3$x S0:%4$x)\n", 
								pswBit3, (stackPointer & 0x4) >> 2, (stackPointer & 0x2) >> 1, stackPointer & 0x1);
			System.out.printf("\t\tR0: %1$02Xh R1: %2$02Xh R2: %3$02Xh R3: %4$02Xh " +
								"R4: %5$02Xh R5: %6$02Xh R6: %7$02Xh R7: %8$02Xh\n",
								internalRAM[0], internalRAM[1],
								internalRAM[2], internalRAM[3],
								internalRAM[4], internalRAM[5],
								internalRAM[6], internalRAM[7]);
			
			System.out.printf("\t\tR0':%1$02Xh R1':%2$02Xh R2':%3$02Xh R3':%4$02Xh " +
								"R4':%5$02Xh R5':%6$02Xh R6':%7$02Xh R7':%8$02Xh\n",
								internalRAM[0 + 0x18], internalRAM[1 + 0x18],
								internalRAM[2 + 0x18], internalRAM[3 + 0x18],
								internalRAM[4 + 0x18], internalRAM[5 + 0x18],
								internalRAM[6 + 0x18], internalRAM[7 + 0x18]);
			
			//System.out.printf("\t\tP1:%1$02Xh (P17:%2$1X P16:%3$1X P15:%4$1X P14:%5$1X " +
			//		"P13:%6$1X P12:%7$1X P11:%8$1X P10:%9$1X)\n",
			//		port1, p17, p16, p15, p14, p13, p12, p11, p10);
                        System.out.printf("\t\tP1:%1$02Xh", port1);
                        System.out.printf("  P2:%1$02Xh\n", port2);
			System.out.print("\t\textIRQEnabled: " + externalIRQEnabled);
			System.out.println("   clock: " + clock);
		}
		
                
		if(internalRam)
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
                 	
		
                
		if(externalRam)
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
           
		
		if(vdc)
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
	}
        
	public int getProgramCounter()
	{
		return programCounter;
	}
	
	public void setStopIt(boolean stopit)
	{
		stopIt = stopit;
	}
	
	public boolean getStopIt()
	{
		return stopIt;
	}
        
        public int[] getInternalRam()
        {
            return internalRAM;
        }
        
        public int[] getExternalRam()
        {
            return externalRAM;
        }
        
        public int[] getVdcRam()
        {
            return vdcRAM;
        }
        
        public int getCycles()
        {
            return cycles;
        }

        public int getPreviousProgramCounter()
        {
            return previousProgramCounter;
        }
        
        public int getOpCode()
        {
            //return memory[programCounter];
            return previousOpCode;
        }
        
        public int getOpCodeData()
        {
            return dataHelp;
        }
        public int getPort1()
        {
            return port1;
        }
        public int getPort2()
        {
            return port2;
        }
        public void setPort2(int p2)
        {
            port2 = p2;
        }
        
        private boolean isInternalTimerStarted()
        {
            return internalTimerStarted;
        }
        
        private void stopInternalTimer()
        {
            internalTimerStarted = false;
        }        
        
        private void startInernalTimer()
        {
            internalTimerStarted = true;
        } 
        
        private boolean isEventCounterStarted()
        {
            return eventCounterStarted;
        }
        
        private void startEventCounter()
        {
            eventCounterStarted = true;
        }
        
        private void stopEventCounter()
        {
            eventCounterStarted = false;
        }
        
        private void setTimerOverflowFlag(boolean flag)
        {
            timerOverfolwFlag = true;
        }
                
        public void triggerExternalIRQ()
        {
            //int_clk
            //System.out.println("externalIRQEnabled: " + externalIRQEnabled);
            //System.out.println("IRQ: " + IRQ);
            if(externalIRQEnabled == true && IRQ == 0)
            {
                IRQ = 1;
                externalIRQPending = false;
                //save Bit 0-8 of the programCounter at the Stack
                internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
                //save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
                internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
                stackPointer++;
                
                if(stackPointer > 7)
                    stackPointer = 0;
                programCounter = 0x003;
            }
            //if(externalIRQEnabled == false)
            //    externalIRQPending = true;
            
            //flag1 = 1;  //???
            cycles = 2;
            //System.out.println("HERE WE ARE - INTERRUPT: " + debug_x++);
            //this.debug(true,true,true,true);
        }
        
        public void triggerTimerIRQ()
        {
            //System.out.println("timerIRQEnabled: " + timerIRQEnabled);
            if(timerIRQEnabled == true && IRQ == 0)
            {
                IRQ = 2;
                //save Bit 0-8 of the programCounter at the Stack
                internalRAM[(stackPointer * 2) + 8] = programCounter & 0xff;
                //save Bit 9-11 + DBF + Bit 4-7 of PDS at the Stack
                internalRAM[(stackPointer * 2) + 9] = (programCounter >> 8) | (dbf << 3) | (psw & 0xf0);  
                stackPointer++;
                if(stackPointer > 7)
                    stackPointer = 0;
                timerIRQPending = false;
                programCounter = 0x007;
                cycles = 2;
            }
            cycles = 2;
            //if(timerIRQEnabled == false)
            //    timerIRQPending = true;
            //if(tcnti == true)
        }
        
        public boolean isExternalIRQPending()
        {
            return externalIRQPending;
        }
        
        public boolean isTimerIRQPending()
        {
            return timerIRQPending;            
        }
        
        /**
        * Tests if the Bit at Position p of the Byte b is true or false.
        * 
        * @param b
        *           the Byte which should be tested
        * @param p
        *           the Position of the Bits which should be tested
        *           begins with 0 (0-7), 8 Bit
        * 
        * @return true if the bit at Position p is set (1), or
        *          false if the bit at Position p is not set (0)
        */
        public static boolean isBitTrue(int b, int p)
        {
            int hexValue = 0x99;

            switch(p)
            {
                case 0:
                        hexValue = 0x01;
                        break;
                case 1:
                        hexValue = 0x02;
                        break;
                case 2:
                        hexValue = 0x04;
                        break;
                case 3:
                        hexValue = 0x08;
                        break;
                case 4:
                        hexValue = 0x10;
                        break;
                case 5:
                        hexValue = 0x20;
                        break;
                case 6:
                        hexValue = 0x40;
                        break;
                case 7:
                        hexValue = 0x80;
                        break;
                default:
                        System.out.println("ERROR");
                        System.exit(0);
            }

            if((b & hexValue) == hexValue)
                return true;
            else
                return false;
        }

        /**
        * Returns the content of the Register reg.
        * 
        * @param reg
        *           the Register n (n = 0-7)
        * 
        * @return 
        *           the content of the Register reg.
        */
        private int getRegister(int reg)
        {
            if(reg > 8)
            {
                System.out.println("the 8048 has just 8 Registers. reg was: " + reg);
                System.out.println("error in getRegister(int reg)");
                System.exit(0);
            }
            return internalRAM[reg + registerBankSelectPointer];
        }
        
        public void reset()
        {
            programCounter = 0;
            accumulator = 5;
            bus = 0;
            this.stopInternalTimer();
            timerOverfolwFlag = false;
            port1 = 0xff;
            port2 = 0xff;
            dbf = 0;
            //PSW initialized with 4 because Bit 4 is not used but is set to 1
            psw = 4; //ProgramStatusWord (8 Bit)
            //Bits of PSW
            carryFlag = 0;		//Bit 7
            auxiliaryCarry = 0;         //Bit 6
            flag0 = 0;			//Bit 5
            registerBankSelect = 0;		//Bit 4 
            pswBit3 = 1;		//Bit 3
            stackPointer = 0;           //Bit 0-2
            //==Bits of PSW	  
            
            //IRQs
            externalIRQEnabled = false;
            timerIRQEnabled = false;
            externalIRQPending = false;
            timerIRQPending = false;
            IRQ = 0;
                    
            internalTimer = 0;              // internal Timer
            internalTimerPrescaler = 0;     // 32 Cylcels
            internalTimerStarted = false;
            timerOverfolwFlag = false;
            timerFlag=false;
            
            //Help Variables
            dataHelp = 0;
            accessVDC = false;
            accessExtRAM = true;
            registerBankSelectPointer = 0x0;
            clock = -2;
            RAMPointer = vdcRAM;
            
            
            for(int i = 0; i < internalRAM.length; i++)
            {
                internalRAM[i] = 0;
            }
        }
        
        private int read_BUS()
        {
            int bus = 0x00;
            
            //Read the Joysticks
            if (p13 == 1 && p14 == 1       //disable VDC & RAM
                    && p12 == 0)                 //enable Keyboard scanner
            {
                    if(!this.isBitTrue(port2,0) && !this.isBitTrue(port2,1) && 
                    !this.isBitTrue(port2,2))   //read the right joystick (P20 && P21 && P22)
                    {
                        //System.out.println("RIGHT JOYSTICK");
                        bus = 0x00;
                        bus = 0xFE;  //Joystick UP
                        //bus = bus & 0xFD  //Joystick RIGHT
                        bus = 0xFB;  //Joystick DOWN
                        //bus = bus & 0xF7  //Joystick LEFT
                        //bus = bus & 0xEF  //Both Main Buttons
                    }
                    else if(!this.isBitTrue(port2,0) && !this.isBitTrue(port2,1) && 
                    this.isBitTrue(port2,2))   //read the left joystick   (P20 && P21 && !P22)
                    {
                        //System.out.println("LEFT JOYSTICK");
                        bus = 0x00;
                        bus = 0xFE;  //Joystick UP
                        //bus = bus & 0xFD  //Joystick RIGHT
                        //bus = bus & 0xFB  //Joystick DOWN
                        //bus = bus & 0xF7  //Joystick LEFT
                        //bus = bus & 0xEF  //Both Main Buttons
                    }
            }  
            return bus;
        }
        
        private int read_BUS(int register)
        {
            int bus = 0x00;
            
            //access the VDC
            if(p13 == 0 && p16 == 0)
            {	
                bus = vdcRAM[register];
                
                if(register == 0xa2)	//0xa2 is the Collision Register
                {			//reading this Register clears it
                        vdcRAM[register] = 0;
                }
                else if(register == 0xa1)
                {
                    //Bit 0: Horizontal Status - Starts 20 us before
                    //       horizontal blank starts. Ends 5 us before
                    //       horizontal blank ends. (grokking MCS 48)
                    if(virtualMachine.getHsync() >= 4 && virtualMachine.getHsync() <= 17)
                        vdcRAM[0xa1] = vdcRAM[0xa1] | 0x01; //Set Bit 0
                    else
                        vdcRAM[0xa1] = vdcRAM[0xa1] & 0xFE; //Delete Bit 0
                    
                    //Bit 1: Position Strobe Status - Status of X-Y Register strobe
                    //'1' = Follow Beam Location Ctr,
                    //'0' = latched
                    
                    //Bit 2: Sound Needs Service - Sound register empty
                    
                    //Bit 3: Vertical Status = Bertical Blanking
                    //if(virtualMachine.getVsync() <= (7825-22))
                    //    vdcRAM[0xa1] = vdcRAM[0xa1] | 0x08; //Set Bit 3
                    //else
                    //    vdcRAM[0xa1] = vdcRAM[0xa1] & 0xF7; //Delete Bit 7
                    
                    //Theory of Rene_G7400
                    //I have another theory (based on an experiment):
                    //bit 3 of A1 is kept high until A1 has been read.
                    vdcRAM[0xa1] = vdcRAM[0xa1] & 0xF7; //Delete Bit 7
                    
                    //Bit 4-5: N/C
                    
                    //Bit 6 - External Chip Overlap Interrupt - Set when an
                    //overlap occurs with signal on 'CX' Pin.
                    //(not used on the O2 - Daniel Boris)
                    
                    //Bit 7 - Major System Overlap (Character Overlap)
                    //(This is set when 2 or more character objects are overlapping)
                }
            }
            
            //access the external RAM
            else //
            {			
                bus = externalRAM[register];
            }
            
            return bus;            
        }
        
        
        
        public void writeExternalRAM(int accumulator, int register)
        {
            externalRAM[register] = accumulator;
        }
        
        public void writeVDCRAM(int accumulator, int register)
        {
            vdcRAM[register] = accumulator;
        }
        
        private void write_BUS(int register, int accumulator)
        {
            //int register = internalRAM[1 + registerBankSelectPointer];
            //VDC enabled, RAM disabled
            if(p13 == 0)//&& p14 == 1)
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
                //Simulate Quads
                //Maby this should be handled in the VDC directly
                else if(register >= 0x40 && register <= 0x7F && (register & 2) == 0)
                {
                    register = register & 0x71; //Register darf nicht groe�er als 0x71 sein
                    
                    if ((register & 1) == 0) accumulator = accumulator & 0xfe;
                    
                        vdcRAM[register] = accumulator;
                        vdcRAM[register+4] = accumulator;
                        vdcRAM[register+8] = accumulator;
                        vdcRAM[register+12] = accumulator;
                }
                else
                {
                    RAMPointer[register] = accumulator;
                }                                            
            }
            //else
            if(p13==1 && p14==0)
            {
                RAMPointer[register] = accumulator;
            }
        }
         
}


