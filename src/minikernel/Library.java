/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Modified by Brett Duncan for the operating systems project.
 */

package minikernel;

import main.*;

/* $Id: Library.java.src,v 1.4 2007/04/25 14:20:12 solomon Exp $ */
import static java.lang.System.*;

/** Convenience calls for using the Kernel.
 * Each function in this class makes a system call.  Sometimes, the arguments
 * are manipulated to make their user representation more convenient.
 * Note that this class contains only static methods.
 * All methods return integers.  Negative return values are error codes.
 * Some methods return positive values; others simply return 0 to mean "ok".
 *
 * @see Kernel
 */
public class Library {
    /** This private constructor ensures that no instances of Library are
     * ever created.
     */
    private Library() {}

    /** A table of error messages corresponding to Kernel error return codes.
     * This table should be indexed by the negative of rc, where
     * <pre>
     *          rc = Kernel.interrupt(Kernel.INTERRUPT_USER, ... )
     * </pre>
     * and rc is less than 0.
     */
    public static final String[] errorMessage = {
        "OK",                           // 0
        "Invalid argument",             // ERROR_BAD_ARGUMENT = -1
        "No such class",                // ERROR_NO_CLASS = -2
        "Class has no main method",     // ERROR_NO_MAIN = -3
        "Command aborted",              // ERROR_BAD_COMMAND = -4
        "Argument out of range",        // ERROR_OUT_OF_RANGE = -5
        "End of file on console input", // ERROR_END_OF_FILE = -6
        "I/O error on console input",   // ERROR_IO = -7
        "Exception in user program",    // ERROR_IN_CHILD = -8
        "No such process"               // ERROR_NO_SUCH_PROCESS = -9
    };

    /** Performs SYSCALL_OUTPUT.
     * Displays text on the console.
     * @param s a String to display
     * @return zero
     */
    public static int output(String s) {
        return Kernel.interrupt(Kernel.INTERRUPT_USER,
            Kernel.SYSCALL_OUTPUT, 0, s, null, null);
    } // output

    /** Performs SYSCALL_INPUT.
     * Waits for the user to type some text and hit [return].
     * The input line is returned in the supplied StringBuffer
     * @param result a place to put the result
     * @return zero on success, or one of the error codes Kernel.END_OF_FILE or
     * Kernel.ERROR_IO.
     */
    public static int input(StringBuffer result) {
        result.setLength(0);
        return Kernel.interrupt(Kernel.INTERRUPT_USER,
                            Kernel.SYSCALL_INPUT, 0, result, null, null);
    } // input

    /** Performs SYSCALL_EXEC.
     * Launches the named program, and lets it run in parallel
     * to the current program.
     * @param command The name of a Java class to execute.
     * @param args The arguments to give the new program
     * @return a non-negative process id, or ERROR_BAD_COMMAND.
     */
    public static int exec(String command, String args[]) {
        return Kernel.interrupt(Kernel.INTERRUPT_USER,
            Kernel.SYSCALL_EXEC, 0, command, args, null);
    } // exec

    /** Performs SYSCALL_JOIN.
     * Waits for a process to terminate
     * @param pid a process id returned by a previous call to exec.
     * @return zero or ERROR_NO_SUCH_PROCESS
     */
    public static int join(int pid) {
        return Kernel.interrupt(Kernel.INTERRUPT_USER,
            Kernel.SYSCALL_JOIN, pid, null, null, null);
    } // join
    
    //************Code added by Brett Duncan*********************//
    /**
     * Gets block size of the disk.
     * @return The block size of the disk.
     */
    public static int getBlockSizeOfDisk() {
        return Kernel.interrupt(Kernel.INTERRUPT_USER, Kernel.SYSCALL_GET_BLOCK_SIZE, 0, null, null, null);
    }
    //************End code added by Brett Duncan*********************//

    //XXX: Implementing methods
    
    /** Formats the disk.  If the disk is already formatted, this system call
     * will destroy all data on it.
     * @return 0 on success and -1 on failure.
     */
    public static int format() {
//        err.println("format system call not implemented yet");
//        return -1;
        
        //************Code added by Brett Duncan*********************//
       
        return Kernel.interrupt(Kernel.INTERRUPT_USER, Kernel.SYSCALL_FORMAT, 0, null, null, null);
        
        //************End code added by Brett Duncan*********************//
        
    } // format(int)

    /** Changes the current working directory.
     * @param pathname the name of the directory to go to.  If it is a relative
     * pathname (does not start with '/'), it is relative to the current
     * working directory.
     * @return 0 on success and -1 on failure.
     */
    public static int chdir(String pathname) {
        err.println("chdir system call not implemented yet");
        return -1;
    } // chdir(String)

    /** Creates a new "ordinary" file.
     * @param pathname the name of the new file being created.
     * @return 0 on success and -1 on failure.
     */
    public static int create(String pathname) {
//        err.println("create system call not implemented yet");
//        return -1;
        
        //************Code added by Brett Duncan*********************//
        
        return Kernel.interrupt(
                Kernel.INTERRUPT_USER, Kernel.SYSCALL_CREATE, 0, null, null, pathname.getBytes());
        //************End code added by Brett Duncan*********************//
        
    } // create(String)

    /** Reads from a file.
     * @param pathname the name of the file to read from.
     * @param buffer the destination for the data.
     * @return 0 on success and -1 on failure.
     */
    public static int read(String pathname, byte[] buffer) {
        
//        err.println("read system call not implemented yet");
//        return -1;
        
        //************Code added by Brett Duncan*********************//
        return Kernel.interrupt(
                Kernel.INTERRUPT_USER, Kernel.SYSCALL_READ, 0, null, null, pathname.getBytes());
        //************End code added by Brett Duncan*********************//
        
    } // read(String, byte[])

    /** Writes to a file.
     * @param pathname the name of the file to write to.
     * @param buffer the source of the data.
     * @return 0 on success and -1 on failure.
     */
    public static int write(String pathname, byte[] buffer) {
//        err.println("write system call not implemented yet");
        //************Code added by Brett Duncan*********************//
        return Kernel.interrupt(
                Kernel.INTERRUPT_USER, Kernel.SYSCALL_WRITE, 0, pathname, null, buffer);
        //************End code added by Brett Duncan*********************//
//        return -1;
    } // write(String, byte[])

    /** Deletes an "ordinary" file.
     * @param pathname the name of the file to delete.
     * @return 0 on success and -1 on failure.
     */
    public static int delete(String pathname) {
        return Kernel.interrupt(
                Kernel.INTERRUPT_USER, Kernel.SYSCALL_DELETE, 0, pathname, null, null);
//        return -1;
    } // delete(String)

    /** Creates an empty directory.
     * @param pathname the name of the new directory being created
     * @return 0 on success and -1 on failure.
     */
    public static int mkdir(String pathname) {
        err.println("mkdir system call not implemented yet");
        return -1;
    } // mkdir(String)

    /** Removes a directory.  The directory must be empty.
     * @param pathname the name of the directory to remove.
     * @return 0 on success and -1 on failure.
     */
    public static int rmdir(String pathname) {
        err.println("rmdir system call not implemented yet");
        return -1;
    } // rmdir(String)

    /** Creates a symbolic link.
     * @param oldName a pathname that will be target of the symlink.  It need
     * not exist.
     * @param newName the name of the new symlink.
     * @return 0 on success and -1 on failure.
     */
    public static int symlink(String oldName, String newName) {
        err.println("symlink system call not implemented yet");
        return -1;
    } // symlink(String,String)

    /** Reads the contents of a symbolic link.
     * @param pathname the name of the symbolic link.
     * @param buffer the destination for its pathname.
     * @return 0 on success and -1 on failure.
     */
    public static int readlink(String pathname, byte[] buffer) {
        err.println("readlink system call not implemented yet");
        return -1;
    } // readlink(String, byte[])

    /** Reads the contents of a directory.
     * @param pathname the name of the directory.
     * @param buffer the destination for its contents.
     * @return 0 on success and -1 on failure.
     */
    public static int readdir(String pathname, byte[] buffer) {
        return Kernel.interrupt(Kernel.INTERRUPT_USER, Kernel.SYSCALL_READDIR,
                                                       0, null, null, null);
//        return -1;
    } // readdir(String, byte[])

    //************Code added by Brett Duncan*********************//
    public static void shutdown() {
        Kernel.interrupt(
                Kernel.INTERRUPT_USER, Kernel.SYSCALL_SHUTDOWN, 0, null, null, null);
    }
    //************End code added by Brett Duncan*********************//

} // Library
