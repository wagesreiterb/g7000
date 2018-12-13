package g7000;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLine
{	
	public CPU command(CPU cpu, VirtualMachine virtualMachine)
	{
		Scanner sc = new Scanner(System.in);
		String command = sc.nextLine();
		Pattern[] p = new Pattern[4];
		p[0] = Pattern.compile("^show int$");
		p[1] = Pattern.compile("^show ext$");
		p[2] = Pattern.compile("^show vdc$");
		p[3] = Pattern.compile("^run 0x[0-9|a-f]{4}$");
		int i=0;
		for(Pattern pat : p)
		{
			Matcher m = p[i].matcher(command);
			if(m.find())
			{
				switch (i)
				{
				case 0:
					System.out.println("command: " + command);
					cpu.debug(false, true, false, false); //intRam, extRam, vdc
					break;
				case 1:
					System.out.println("command: " + command);
					cpu.debug(false, false, true, false); //intRam, extRam, vdc
					break;
				case 2:
					System.out.println("command: " + command);
					cpu.debug(false, false, false, true); //intRam, extRam, vdc		
					break;
				case 3:
					System.out.println("command: " + command);
					Scanner sc1 = new Scanner(command).useDelimiter("x");
					sc1.next();
					String hex = sc1.next();
					int newProgramCounter = Integer.parseInt(hex, 16);;
					System.out.println(newProgramCounter);
					cpu.setStopIt(false);
					while(cpu.getProgramCounter() != newProgramCounter &&
							 cpu.getStopIt() == false)
					{
						cpu.nextStep(virtualMachine);
                                                cpu.debug(true, false, false, false);
					}
					cpu.setStopIt(true);
					System.out.println(newProgramCounter);
					break;
					
				default:
					System.out.println("command: something wrong");
					break;
				}
			}
			i++;
		}
		return cpu;
	}
}
