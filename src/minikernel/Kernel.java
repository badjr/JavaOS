/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Modified by Brett Duncan for the operating systems project.
 */

package minikernel;

/* $Id: Kernel.java.src,v 1.4 2007/04/25 14:20:12 solomon Exp $ */

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import static java.lang.System.*;

/** A simple kernel simulation.
 *
 * <p>
 * There is only one public interface to this class: interrupt().
 * System calls, disk notification, and power on messages all arrive
 * by way of this function.
 * <p>
 * See the list of <samp>SYSCALL_XXX</samp> constants to learn what
 * system calls are currently supported.
 *
 * @see Disk
 * @see Library
 */
public class Kernel {

    /** No public instances. */
    private Kernel() {}

    //////////////// Values for the first parameter ("kind") to interrupt()

    /** An interrupt kind indicating that a user program caused the interrupt.
     * <ul>
     * <li><b>Parameter i1</b> -- a valid system call number.
     * </ul>
     * Other parameters depend on the call number.
     */
    public static final int INTERRUPT_USER = 0;

    /** An interrupt kind indicating that a disk caused the interrupt.
     * All other parameters will be null or zero.
     */
    public static final int INTERRUPT_DISK = 1;

    /** An interrupt kind indicating that the system just started.
    * The Kernel should set up any internal state and
    * begin executing the first program.
    * <ul>
    * <li><b>Parameter i1</b> --  the number of blocks to use in the
    * disk cache.
    * <li><b>Parameter o1</b> -- an instance of Disk to use as the disk.
    * <li><b>Parameter o2</b> -- a String containing the name of the shell.
    * </ul>
    */
    public static final int INTERRUPT_POWER_ON = 2;

    //////////////// Values for the second parameter ("i1") for USER interrupts

    /** System call to output text on the console.
     * <ul>
     * <li><b>Parameter o1</b>  -- A string to display
     * <li><b>Returns</b> -- zero.
     * </ul>
     */
    public static final int SYSCALL_OUTPUT = 0;

    /** System call to read text from the console.
     * This function returns when the user presses [Enter].
     * <ul>
     * <li><b>Parameter o1</b> -- A StringBuffer to fill with input text.
     * <li><b>Returns</b> -- zero, ERROR_BAD_ARGUMENT, ERROR_END_OF_FILE,
     * or ERROR_IO.
     * </ul>
     */
    public static final int SYSCALL_INPUT = 1;

    /** System call to execute a new program.
     * The new program will run in parallel to the current program.
     * <ul>
     * <li><b>Parameter o1</b> - The name of a Java class to execute.
     * <li><b>Parameter o2</b> - An array for String arguments.
     * <li><b>Returns</b> - A non-negative process id or ERROR_BAD_ARGUMENT,
     * ERROR_NO_CLASS, ERROR_NO_MAIN, or ERROR_BAD_COMMAND.
     * </ul>
     */
    public static final int SYSCALL_EXEC = 2;

    /** System call to wait for a process to terminate.
     * This call will not return until the indicated process has
     * run to completion.
     * <ul>
     * <li><b>Parameter i2</b> - the process id to wait for.
     * <li><b>Returns</b> -- zero or ERROR_NO_SUCH_PROCESS.
     * </ul>
     */
    public static final int SYSCALL_JOIN = 3;
    
    //************Code added by Brett Duncan*********************//
    //XXX: SYSCALL constants
    
    /**
     * System call to format the disk.
     */
    public static final int SYSCALL_FORMAT = 4;
    
    /** System call to create a new file.
     * 
     */
    public static final int SYSCALL_CREATE = 5;
    
    /**
     * System call to read the contents of a file.
     */
    public static final int SYSCALL_READ = 6;
    
    /**
     * System call to write to the disk.
     */
    public static final int SYSCALL_WRITE = 7;
    
    /**
     * System call to delete a file from the disk.
     */
    public static final int SYSCALL_DELETE = 8;
    
    /**
     * System call to display contents of the current directory.
     */
    public static final int SYSCALL_READDIR = 9;
    
    /**
     * System call to perform safe shutdown of the disk.
     */
    public static final int SYSCALL_SHUTDOWN = 10;
    
    public static final int SYSCALL_GET_BLOCK_SIZE = 11;
    
    //************End code added by Brett Duncan*****************//

    //////////////// Error codes returned by interrupt()

    /** An error code indicating that one of the system call parameters made no
     * sense.
     */
    public static final int ERROR_BAD_ARGUMENT = -1;

    /** An error code indicating that the class name passed to SYSCALL_EXEC
     * could not be found.
     */
    public static final int ERROR_NO_CLASS = -2;

    /** An error code indicating that the class name passed to SYSCALL_EXEC
     * named a class with no appropriate main() method.
     */
    public static final int ERROR_NO_MAIN = -3;

    /** An error code indicating some unspecified problem running the class
     * passed SYSCALL_EXEC.
     */
    public static final int ERROR_BAD_COMMAND = -4;

    /** An error code indicating that one parameter was too big or too small. */
    public static final int ERROR_OUT_OF_RANGE = -5;

    /** An error code indicating that end of file was reached. */
    public static final int ERROR_END_OF_FILE = -6;

    /** An error code indicating that something went wrong during an I/O
     * operation.
     */
    public static final int ERROR_IO = -7;

    /** An error code indicating that a child program caused an exception and
     * crashed.
     */
    public static final int ERROR_IN_CHILD = -8;

    /** An error code indicating an attempt to join with a non-existant
     * process.
     */
    public static final int ERROR_NO_SUCH_PROCESS = -9;

    //////////////// Transient state of the kernel

    /** The disk to be used */
//    private static Disk disk;
    private static FastDisk disk;


    /** The file system. */
    private static FileSys filesys;

    /** The size of the disk cache */
    private static int cacheSize;

    //////////////// Methods

    /** This is the only entry into the kernel.
    * <p>A user may call this function to perform a system call.
    * In that case, set <tt>kind</tt> to <tt>INTERRUPT_USER</tt>
    * and set <tt>i1</tt> to the system call number.  Other
    * parameters should be set as the system call requires.
    * <p>
    * A disk may call this function to indicate the current operation
    * has completed.  In that case, <tt>kind</tt> will be
    * <tt>INTERRUPT_DISK</tt> and all parameters will be zero or null.
    * <br>
    * <b>Important:</b> If the Disk calls <tt>interrupt()</tt>, the
    * Kernel should take care of business and return from the interrupt
    * as soon as possible.  All Disk I/O is halted while the interrupt is
    * being processed.
    * <p>
    * The boot code may call this function to indicate that the computer
    * has been turned on and it is time to start the first program
    * and use the disk.  In that case, <tt>kind</tt> will be
    * <tt>INTERRUPT_POWER_ON</tt>, o1 will point to the Disk to be
    * used, o2 will be a String containing the name of the shell to use,
    * i1 will indicate the size of the buffer cache,
    * and all other parameters will be zero or null.
    * <p>
    * Since different system calls require different parameters, this
    * method has a variety of arguments of various types.  Any one
    * system call will use at most a few of them.  The others should be
    * zero or null.
    *
    * @param kind the kind of system call, one of the
    *   <samp>INTERRUPT_XXX</samp> codes.
    * @param i1 the first integer parameter.  If <samp>kind ==
    *   INTERRUPT_USER</samp>, <samp>i1</samp> should be one of the
    *   <samp>SYSTEM_XXX</samp> codes to indicate which system call is being
    *   invoked.
    * @param i2 another integer parameter.
    * @param o1 a parameter of some object type.
    * @param o2 another parameter of some object type.
    * @param a a byte-array parameter (generally used for binary input/output).
    * 
    * @return a negative number indicating an error code, or other
    * values depending on the system call.
    */
    public static int interrupt(int kind, int i1, int i2,
            Object o1, Object o2, byte a[])
    {
        try {
            switch (kind) {
            case INTERRUPT_USER:
                switch (i1) {
                case SYSCALL_OUTPUT:
                    return doOutput((String)o1);

                case SYSCALL_INPUT:
                    return doInput((StringBuffer)o1);

                case SYSCALL_EXEC:
                    return doExec((String)o1,(String[])o2);

                case SYSCALL_JOIN:
                    return doJoin(i2);
                    
                //************Code added by Brett Duncan*********************//
                //XXX: SYSCALL cases
                
                case SYSCALL_FORMAT:
                    return doFormat();
                    
                case SYSCALL_CREATE:
                    return doCreateFile(a);
                    
                case SYSCALL_READ:
                    return doRead(a);
                    
                case SYSCALL_WRITE:
                    return doWrite(((String) o1).getBytes(), a);
                    
                case SYSCALL_DELETE:
                    return doDelete(((String) o1).getBytes());
                    
                case SYSCALL_READDIR:
                    return doReadDir();
                    
                case SYSCALL_SHUTDOWN:
                    doShutdown();
                    break;
                    
                case SYSCALL_GET_BLOCK_SIZE:
                    return doGetBlockSize();
                    
                //************End code added by Brett Duncan*********************//

                default:
                    return ERROR_BAD_ARGUMENT;
                }

            case INTERRUPT_DISK:
                break;

            case INTERRUPT_POWER_ON:
                doPowerOn(i1, o1, o2);
//                doShutdown();
                break;

            default:
                return ERROR_BAD_ARGUMENT;
            } // switch (kind)
        } catch (Exception e) {
            // Most likely, we arrived here due to a bad cast. 
            e.printStackTrace();
            return ERROR_BAD_ARGUMENT;
        }
        return 0;
    } // interrupt(int, int, int, Object, Object, byte[])

    /** Performs the actions associated with a POWER_ON interrupt.
     * @param i1 the first int parameter to the interrupt (the disk cache size)
     * @param o1 the first Object parameter to the interrupt (the Disk).
     * @param o2 the second Object parameter to the interrupt (the shell
     * command-line).
     */
    private static void doPowerOn(int i1, Object o1, Object o2) {
        cacheSize = i1;
        
        //************Code added by Brett Duncan*********************//
//        disk = (Disk)o1;
        disk = (FastDisk)o1;
        //Create new file system object, which makes managing the file system
        //easier.
        filesys = new FileSys(disk);
        //************End code added by Brett Duncan*********************//
        
        String shellCommand = (String) o2;

        doOutput("Kernel: Disk is " + filesys.getBlockSizeOfDisk() + " blocks\n");
        doOutput("Kernel: Disk cache size is " + i1 + " blocks\n");
        
        //Commented this out because we're just gonna use the default shell
        //in FileTester.java.
        /*doOutput("Kernel: Loading initial program.\n");

        StringTokenizer st = new StringTokenizer(shellCommand);
        int n = st.countTokens();
        if (n < 1) {
            doOutput("Kernel: No shell specified\n");
            exit(1);
        }
            
        String shellName = st.nextToken();
        String[] args = new String[n - 1];
        for (int i = 1; i < n; i++) {
            args[i - 1] = st.nextToken();
        }

        if (doExecAndWait(shellName, args) < 0) {
            doOutput("Kernel: Unable to start " + shellCommand + "!\n");
            exit(1);
        } else {
            doOutput("Kernel: " + shellCommand + " has terminated.\n");
        }
        */
        Launcher.joinAll();
    } // doPowerOn(int, Object, Object)

    /** Does any "shutdown" activities required after all activities started by
     * a POWER_ON interrupt have completed.
     */
    private static void doShutdown() {
        
//        disk.flush();
        //************Code added by Brett Duncan*********************//
        //XXX: Shutdown method
        filesys.getDisk().flush();
        //************End code added by Brett Duncan*********************//
    } // doShutdown()

    /** Displays a message on the console.
     * @param msg the message to display
     */
    private static int doOutput(String msg) {
        out.print(msg);
        return 0;
    } // doOutput(String)

    private static BufferedReader br
        = new BufferedReader(new InputStreamReader(in));

    /** Reads a line from the console into a StringBuffer.
     * @param sb a place to put the line of input.
     */
    private static int doInput(StringBuffer sb) {
        try {
            String s = br.readLine();
            if (s==null) {
                return ERROR_END_OF_FILE;
            }
            sb.append(s);
            return 0;
        } catch (IOException t) {
            t.printStackTrace();
            return ERROR_IO;
        }
    } // doInput(StringBuffer)

    /** Loads a program and runs it.
     * Blocks the caller until the program has terminated.
     * @param command the program to run.
     * @param args command-line args to pass to the program.
     * @return the program's return code on success, ERROR_NO_CLASS,
     * ERROR_NO_MAIN, or ERROR_BAD_COMMAND if the command cannot be run, or
     * ERROR_IN_CHILD if the program throws an uncaught exception.
     */
    private static int doExecAndWait(String command, String args[]) {
        Launcher l;
        try {
            l = new Launcher(command, args);
        } catch (ClassNotFoundException e) {
            return ERROR_NO_CLASS;
        } catch (NoSuchMethodException e) {
            return ERROR_NO_MAIN;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_BAD_COMMAND;
        }
        try {
            l.run();
            l.delete();
            return l.returnCode;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_IN_CHILD;
        }
    } // doExecAndWait(String, String[])

    /** Loads a program and runs it in the background.
     * Does not wait for the program to terminate.
     * @param command the program to run.
     * @param args command-line args to pass to the program.
     * @return a process id on success or ERROR_NO_CLASS, ERROR_NO_MAIN, or
     * ERROR_BAD_COMMAND if the command cannot be run.
     */
    private static int doExec(String command, String args[]) {
        try {
            Launcher l = new Launcher(command, args);
            l.start();
            return l.pid;
        } catch (ClassNotFoundException e) {
            return ERROR_NO_CLASS;
        } catch (NoSuchMethodException e) {
            return ERROR_NO_MAIN;
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_BAD_COMMAND;
        }
    } // doExec(String, String[])

    /** Waits for a program previous started by doExec to terminate.
     * @param pid the process id of the program.
     * @return the return code returned by the program.
     */
    private static int doJoin(int pid) {
        return Launcher.joinOne(pid);
    } // doJoin(int)
    
    //************Code added by Brett Duncan*********************//
    //XXX: New doXXX methods
    
    /**
     * Libary function to get the block size of the file system's disk.
     * @return The block size, in bytes, of the disk.
     */
    private static int doGetBlockSize() {
        return filesys.getBlockSizeOfDisk();
    }
    
    /**
     * This method initializes the contents of the disk with any data structures
     * necessary to represent an "empty" file system. This method should create
     * an "empty" root directory "/".
     * @return 0 if successful, -1 if there was an error.
     */
    private static int doFormat() {
        //Create new file system object.
//        filesys = new FileSys(new FastDisk(100));
        
        filesys.getDisk().format();
        
        doOutput("Kernel: Disk formatted.\n");
        
        return 0;
    }
    
    /**
     * Creates a new file with the indicated filename. The initial contents of
     * the file are all null (zero) bytes. 
     * @param pathName The bytes containing the file name.
     * @return 0 if successful, -1 if there was an error.
     */
    private static int doCreateFile(byte pathName[]) {
        
        //Check if file name length is > 32.
        if (pathName.length > 32) {
            doOutput("Kernel: User error: File name too long!\n");
            return -1;
        }
        
        //A 1 block byte array that will hold the free map
        byte freeMap[] = new byte[filesys.getBlockSizeOfDisk()];
        
        //Read block 0 into freeMap
        filesys.getDisk().read(0, freeMap);
        
        //Check if the file specified by pathName already exists.
        String pathNameString = new String(pathName);
        for (int i = 1; i < 100; i++) {
            if (freeMap[i] == '1'
                && pathNameString.equals(filesys.getFileTable()[i].trim())) {

                doOutput("Kernel: User error: File name already exists at"
                        + "block " + i + "!\n");
                return -1;
            }
        }
        
        //The target block to create the file to.
        int targetBlock = 1;
        //Search freeMap for the next 0, which will be the target block.
        for (int i = 1; i < filesys.getDisk().DISK_SIZE; i++) {
            if (freeMap[i] == '0') {
                targetBlock = i;
                break;
            }
        }
        
        //Byte array that will hold the path name and file contents.
        byte pathAndContents[] = new byte[filesys.getBlockSizeOfDisk()];
        //Copy path name to pathAndContents
        arraycopy(pathName, 0, pathAndContents, 0, pathName.length);
        
        //Write path and contents to the target block.
        filesys.getDisk().write(targetBlock, pathAndContents);
        
        //Update the free map at targetBlock indicating that block is occupied.
        freeMap[targetBlock] = '1';
        
        //Write free map back to block 0 of the disk.
        filesys.getDisk().write(0, freeMap);
        
        //Set the file system's fileTable at targetBlock to the file name so
        //we can look it up and other methods can use it.
        filesys.getFileTable()[targetBlock] = new String(pathName);
        
        //Indicate that file was created successfully.
        doOutput("Kernel: Created file ");
        for (int i = 0; i < pathName.length; i++) {
            doOutput((char) pathName[i] + "");
        }
        doOutput(" at block " + targetBlock + ". \n");
        
        return 0;
    }
    
    /**
     * Reads and displays the contents of the file specified by pathName[].
     * @param pathName The file name to read the contents of.
     * @return 0 if successful, -1 if there was an error.
     */
    private static int doRead(byte pathName[]) {
        
        //Buffer holding the data read from a block.
        byte tempBuffer[] = new byte[FastDisk.BLOCK_SIZE];
        
        int targetBlock = findTargetBlock(pathName);
        
        if (targetBlock == -1) {
            doOutput("Kernel: User error: File not found.\n");
            return -1;
        }
        
        filesys.getDisk().read(targetBlock, tempBuffer);
        
        doOutput("Kernel: File name: ");
        for (int i = 0; i < 32; i++) {
            doOutput((char) tempBuffer[i] + "");
        }
        doOutput(" at block " + targetBlock + ". ");
        doOutput("Contents: \n");
        for (int i = 32; i < 512; i++) {
            doOutput((char) tempBuffer[i] + "");
        }
        doOutput("\n");
        return 0;
    }
    
    /**
     * Writes the contents of buffer[] into the file specified by pathName[].
     * @param pathName The file to write the buffer to.
     * @param buffer The contents to be written to the file.
     * @return 0 if successful, -1 if there was an error.
     */
    private static int doWrite(byte pathName[], byte buffer[]) {
        
        int targetBlock = findTargetBlock(pathName);
            
        if (targetBlock == -1) {
            doOutput("Kernel: User error: File not found.\n");
            return -1;
        }
        
        //The byte array that will hold the file name and buffer (contents to
        //write). Do this because in FileTester, it strips off the file name.
        byte fileNameAndBuffer[] = new byte[filesys.getBlockSizeOfDisk()];
        
        //Copy buffer (the contents to write) into fileNameAndBuffer, starting
        //at position 32 because in FileTester, it strips off the file name.
        arraycopy(buffer, 0,
                  fileNameAndBuffer, 32,
                  filesys.getBlockSizeOfDisk() - 32);
        
        //Add the file name to the beginning of fileNameAndBuffer.
        System.arraycopy(pathName, 0,
                         fileNameAndBuffer, 0,
                         pathName.length);
        
        //Now we can write, keeping the file name at the beginning instead of
        //it being overwritten by the contents.
        filesys.getDisk().write(targetBlock, fileNameAndBuffer);
        
        return 0;
    }
    
    /**
     * Deletes a file from the disk, making the block occupied by the file null.
     * @param pathName The file to be deleted.
     * @return 0 if successful, -1 if there was an error.
     */
    private static int doDelete(byte pathName[]) {
        //Find target block to delete.
        int targetBlock = findTargetBlock(pathName);
        
        if (targetBlock == -1) {
            doOutput("Kernel: User error: File not found.\n");
            return -1;
        }
        
        //Create a null byte array of size block size.
        byte nullByteArray[] = new byte[filesys.getBlockSizeOfDisk()];
        for (int i = 0; i < nullByteArray.length; i++) {
            nullByteArray[i] = '\0';
        }
        
        //Write the null byte array to the disk at the target block.
        filesys.getDisk().write(targetBlock, nullByteArray);
        
        //Set the file table at the targetBlock's index to 0, indicating the
        //file was deleted.
        filesys.getFileTable()[targetBlock] = null;
        
        //A 1 block byte array that will hold the free map
        byte freeMap[] = new byte[filesys.getBlockSizeOfDisk()];
        
        //Read block 0 into freeMap
        filesys.getDisk().read(0, freeMap);
        
        //Change the bit at the free map to 0, indicating the block is now free.
        freeMap[targetBlock] = '0';
        
        //Write the updated freeMap back to block 0.
        filesys.getDisk().write(0, freeMap);
        
        return 0;
        
    }
    
    /**
     * Displays the contents of the current directory.
     * @return 0 if successful, -1 if there was an error.
     */
    private static int doReadDir() {
        
        doOutput("Kernel: ");
        for (int i = 1; i < filesys.getFileTable().length; i++) {
            
            doOutput(
                    filesys.getFileTable()[i] == null ?
                    "" :
                    /*"Block " + i + ": " +*/
                    filesys.getFileTable()[i].trim() + " "
                    );
            
        }
        doOutput("\n");
        
        return 0;
    }
    
    /**
     * Helper method for finding the block indicated by pathName[].
     * @param pathName The file name to find the block index of.
     * @return 0 if successful, -1 if the file was not found.
     */
    private static int findTargetBlock(byte pathName[]) {
        
        int targetBlock = -1;
        
        String pathNameString = new String(pathName);
        for (int i = 1; i < filesys.getDisk().DISK_SIZE; i++) {
            
            //Check if null so .trim() doesn't throw null pointer exception.
            String fileTableString = 
                    filesys.getFileTable()[i] == null ?
                    "" :
                    filesys.getFileTable()[i].trim();
            
            if (pathNameString.equals(fileTableString)) {
                targetBlock = i;
                break;
            }
        }
        
        return targetBlock;
        
    }
    
    //************End code added by Brett Duncan*********************//

    /** A Launcher instance represents one atomic command being run by the
     * Kernel.  It has associated with it a process id (pid), a Java method
     * to run, and a list of arguments to the method.
     * Do not modify any part of this class.
     */
    static private class Launcher extends Thread {
        /** Mapping of process ids to Launcher instances. */
        static Map<Integer,Launcher> pidMap = new HashMap<Integer,Launcher>();

        /** Source of unique ids for Launcher instances. */
        static private int nextpid = 1;

        /** The method being run by this command. */
        private Method method;

        /** The list of arguments to this command. */
        private Object arglist[];

        /** The process id of this command. */
        private int pid;

        /** Return code returned by this command (0 if the command has not yet
         * completed.
         */
        private int returnCode = 0;

        /** Creates a new Launcher for a program.
         * @param command the name of the program (new name of a class with
         * a main(String[]) method.
         * @param args command-line arguments to the program.
         */
        public Launcher(String command, String args[])
                throws ClassNotFoundException, NoSuchMethodException
        {
            /* If the user supplied no args, make a dummy. */
            if (args==null) {
                args = new String[0];
            }

            /* Create an array of the method types */
            Class params[] = new Class[] { args.getClass() };

            /* Find the program and look up its main method */
            Class programClass = Class.forName(command);
            method = programClass.getMethod("main",params); 

            /* Assemble an argument list for the method. */
            arglist = new Object[] { args };

            pid = nextpid++;
            synchronized (pidMap) {
                pidMap.put(pid, this);
            }
        } // Launcher.Launcher(String, String[])

        /** Main loop of the Launcher */
        public void run() {
            /* Launch the method using the arglist */
            try {
                method.invoke(null,arglist);
            } catch (InvocationTargetException e) {
                /* Give the user a message */
                out.println("Kernel: User error:");
                e.getTargetException().printStackTrace();

                returnCode = ERROR_IN_CHILD;
            } catch (Exception e) {
                out.printf("Kernel: %s\n", e);
                returnCode = ERROR_IN_CHILD;
            }
        } // Launcher.run()

        /** Waits for <em>all</em> existing Launchers to complete. */
        static public void joinAll() {
            for (Launcher l : pidMap.values()) {
                try {
                    l.join();
                } catch (InterruptedException ex) {
                    out.printf("Kernel: join: %s\n", ex);
                    ex.printStackTrace();
                }
            }
        } // Launcher.joinAll()

        /** Waits for a particular Launcher to complete.
         * @param pid the process id of the desired process.
         * @return the return code of the indicated process, or 
         *      ERROR_NO_SUCH_PROCESS if the pid is invalid.
         */
        static public int joinOne(int pid) {
            Launcher l;
            synchronized (pidMap) {
                l = pidMap.remove(pid);
            }
            if (l == null) {
                return ERROR_NO_SUCH_PROCESS;
            }
            try {
                l.join();
            } catch (InterruptedException e) {
                out.printf("Kernel: join: %s\n", e);
            }
            return l.returnCode;
        } // Launcher.joinOne(int)

        /** Removes this Launcher from the set of all active Launchers. */
        public void delete() {
            synchronized (pidMap) {
                pidMap.remove(pid);
            }
        } // Launcher.delete()
    } // class Launcher
} // class Kernel
