/*
 * sound.java
 *
 * Created on 11. Juli 2007, 20:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package g7000;

import javax.sound.sampled.*;
/**
 *
 * @author Que
 */
public class SoundSystem implements Runnable
{
    private Thread animator;
    private byte[] samples = new byte[8000];
    /** Creates a new instance of sound */
    public SoundSystem()
    {
        for(int i=0; i < 8000; i++)
        {
            samples[i] = 0;
        }
        startSoundSystem();
        
        //actionPerformed();
    }
    
    public void startSoundSystem()
    // initialise and start the thread 
    { 
        //if (animator == null || !running)
        //{
            animator = new Thread(this);
            animator.start();
        //}
    }
    
    
    
    public void shiftByte(byte byteOne, byte byteTwo, byte byteThree)
    {
        int addr = 0;
        samples[addr+0] = (byte) (byteOne);
        samples[addr+1] = (byte) (byteTwo >>> 8);
        System.arraycopy(samples, 0, samples, 1, samples.length-1); 
        
        samples[addr+2] = (byte) (byteThree);
        samples[addr+3] = (byte) (byteOne >>> 8);
        System.arraycopy(samples, 0, samples, 1, samples.length-1);
        
        samples[addr+4] = (byte) (byteTwo);
        samples[addr+5] = (byte) (byteThree >>> 8);
        System.arraycopy(samples, 0, samples, 1, samples.length-1);
        
        /*
        for(int i = 0; i < 8000; i++)
        {
            System.out.print(samples[i]);
        }
        System.out.println();
        */
    }

    public void run()
    {
        System.out.println("Sound System started");
            try
            {
                    int sampleRate = 981;
                    AudioFormat audioformat =
                            new AudioFormat (
                                            AudioFormat.Encoding.PCM_SIGNED,
                                            sampleRate,
                                            24,
                                            1,
                                            1,
                                            sampleRate,
                                            false);
                    DataLine.Info datalineinfo =
                            new DataLine.Info(SourceDataLine.class, audioformat);
                    if (!AudioSystem.isLineSupported(datalineinfo))
                    {
                            System.out.println("Line matching " + datalineinfo + " is not supported.");
                    }
                    else
                    {
                            SourceDataLine sourcedataline =
                                    (SourceDataLine) AudioSystem.getLine(datalineinfo);
                            sourcedataline.open(audioformat);
                            sourcedataline.start();
                            
                            /*
                            //byte[] samples = new byte[1000];
                            for(int ka = 0; ka < 1; ka++)  
                            {
                                    for (int freq = 1000; freq < 2000; freq += 1)   
                                    {
                                            float size = ((float) sampleRate) / ((float) 883);
                                            float amplitude = 32000;    
                                            int adr = 0;
                                            for (int i = 0; i < size; i++, adr += 8)
                                            {
                                                    double sin = Math.sin(
                                                                    (double) i / (double) size * 2.0 * Math.PI);
                                                    int sample = (int) (sin * amplitude);
                                                    //int sample = 0;
                                                    samples[adr + 0] = (byte) (sample);
                                                    samples[adr + 1] = (byte) (sample >>> 8);
                                                    samples[adr + 2] = (byte) (sample);
                                                    samples[adr + 3] = (byte) (sample >>> 8);									
                                            }
                                            sourcedataline.write(samples, 0 , adr);
                                    }
                            
                            }
                            */
                            for(int addr = 0; addr < 8000; addr=addr+4)
                            {
                                sourcedataline.write(samples, 0 , addr);
                            }
                            sourcedataline.drain();
                            
                            sourcedataline.stop();
                            sourcedataline.close();
                    }   //for(int ka = 0; ka < 1; ka++)
            } catch (LineUnavailableException e) {
                     e.printStackTrace(); }
    }    
    
    
    
    /*
    public void actionPerformed()
    {
            try
            {
                    int sampleRate = 8000;
                    AudioFormat audioformat =
                            new AudioFormat (
                                            AudioFormat.Encoding.PCM_SIGNED,
                                            sampleRate,
                                            16,
                                            2,
                                            4,
                                            sampleRate,
                                            false);
                    DataLine.Info datalineinfo =
                            new DataLine.Info(SourceDataLine.class, audioformat);
                    if (!AudioSystem.isLineSupported(datalineinfo))
                    {
                            System.out.println("Line matching " + datalineinfo + " is not supported.");
                    }
                    else
                    {
                            SourceDataLine sourcedataline =
                                    (SourceDataLine) AudioSystem.getLine(datalineinfo);
                            sourcedataline.open(audioformat);
                            sourcedataline.start();
                            
                            
                            byte[] samples = new byte[1000];
                            for(int ka = 0; ka < 1; ka++)   
                            {
                                    for (int freq = 1000; freq < 2000; freq += 1)  
                                    {
                                            float size = ((float) sampleRate) / ((float) 883);
                                            float amplitude = 32000;   
                                            int adr = 0;
                                            for (int i = 0; i < size; i++, adr += 8)
                                            {
                                                    double sin = Math.sin(
                                                                    (double) i / (double) size * 2.0 * Math.PI);
                                                    int sample = (int) (sin * amplitude);
                                                    samples[adr + 0] = (byte) (sample);
                                                    samples[adr + 1] = (byte) (sample >>> 8);
                                                    samples[adr + 2] = (byte) (sample);
                                                    samples[adr + 3] = (byte) (sample >>> 8);									
                                            }
                                            sourcedataline.write(samples, 0 , adr);
                                    }
                             
                            
                            //sourcedataline.write(samples, 0 , adr);
                            }
                            sourcedataline.drain();
                            
                            sourcedataline.stop();
                            sourcedataline.close();
                    }   //for(int ka = 0; ka < 1; ka++)
            } catch (LineUnavailableException e) {
                     e.printStackTrace(); }
    }
     */
    
}
