/**
 * Based on code acquired from http://webhost.bridgew.edu/sattar/os.zip.
 * Modified by Brett Duncan for the operating systems project.
 */

package main;

import java.util.Scanner;
import minikernel.FastDisk;

/**
 * Implementation of the shell based on steps provided from
 * http://webhost.bridgew.edu/sattar/os.zip/hw1-shell.
 * 
 * @author Brett
 */
public class Main {

    public static FastDisk disk;
    
    public static void main(String[] args) {
        
        //Outline of your main method:

        /* Loop through commands on command line */
        try {
            while (true) {
                /* Display your Shell cursor */
                System.out.print("> ");

                /* Get new command line input */
                Scanner s = new Scanner(System.in);
                String input = s.nextLine();

                /* Break up commands and store them in an array using '&' as delimeter */
                String commands[] = input.split("&");

                /* Create a thread array for each command detected */
                CommandExecutorThread commandExecutorThread[] = new CommandExecutorThread[commands.length];

                /* Loop to allow a new thread per command to be created */
//                for (String command : commands) {
                for (int i = 0; i < commands.length; i++) {
                    commandExecutorThread[i] = new CommandExecutorThread(i, commands[i].trim());
                    commandExecutorThread[i].start();
                }

                /* If it is EXIT command then close your Shell by executing System.exit(0)*/

                /* Add new thread to the thread array and run it */
                /* Join the threads */
                for (int i = 0; i < commandExecutorThread.length; i++) {
                    commandExecutorThread[i].join();
                }
            } //while

        }  /* Check for a keyboard interupt or other non-serious system problem */

        catch (Exception e) {
            System.out.println("\n\nInterrupt was detected. my Shell is closing.");
//            e.printStackTrace();
            System.exit(0);
        }

    }

}

class CommandExecutorThread extends Thread {

    private int myID = 0;
    private String command;

    public CommandExecutorThread(int myID, String command) {
        this.myID = myID;
        this.command = command;
    }

    public void run() {
        System.out.println("myID = " + myID + ". Running command " + command);
        
        switch (command) {
            case "format":
                break;
        }
        
    }

}
