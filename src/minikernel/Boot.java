/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Used by Brett Duncan for the operating systems project.
 */
package minikernel;

/* $Id: Boot.java,v 1.18 2006/11/09 20:42:29 solomon Exp $ */

import java.lang.reflect.*;
import java.util.*;
import static java.lang.System.*;

/** A bootstrap program for the MiniKernel.
 * <p>
 * This program creates a Disk and launches the kernel by calling the POWER_ON
 * interrupt.  When the Kernel returns from the interrupt, we assume it wants
 * to shut down.
 * <p>
 * The program expects four or more command-line arguments:
 * <ul>
 * <li> a numeric parameter to pass to the Kernel's POWER_ON interrupt.
 *      The kernel stores this number in its bufferSize field.
 * <li> the name of a class that implements the disk,
 * <li> the size of the disk, in blocks,
 * <li> the name of shell program, and
 *      any arguments to the shell program.
 * </ul>
 * <p>
 * An example invocation is
 * <pre>
 *    java Boot 10 Disk 100 Shell
 * </pre>
 *
 * @see Kernel
 * @see Disk
 */
public class Boot {
    /** No public instances. */
    private Boot() {}

    /** Prints a help message and exits. */
    private static void usage() {
        err.println("usage: java Boot"
            + " <cacheSize> <diskName> <diskSize> <shell>"
            + " [ <shell parameters> ... ]");
        exit(-1);
    } // usage

    /** The main program.
     * @param args the command-line arguments
     */
    public static void main(String args[]) {        
        if (args.length < 4) {
            usage();
        }

        int cacheSize = Integer.parseInt(args[0]);
        String diskName = args[1];
        int diskSize = Integer.parseInt(args[2]);
        StringBuffer shellCommand = new StringBuffer(args[3]);
        for (int i = 4; i < args.length; i++) {
            shellCommand.append(" ").append(args[i]);
        }

        // Create a Disk drive and start it spinning
        Object disk = null;
        try {
            Class diskClass = Class.forName(diskName);
            Constructor ctor
                = diskClass.getConstructor(new Class[] { Integer.TYPE });
            disk = ctor.newInstance(new Object[] { new Integer(diskSize) });
            if (! (disk instanceof Disk)) {
                err.printf("%s is not a subclass of Disk\n", diskName);
                usage();
            }
            if (!diskName.equals("FastDisk")) {
                new Thread((Disk) disk, "DISK").start();
            }
        } catch (ClassNotFoundException e) {
            err.printf("%s: class not found\n", diskName);
            usage();
        } catch (NoSuchMethodException e) {
            err.printf("%s(int): no such constructor\n", diskName);
            usage();
        } catch (InvocationTargetException e) {
            err.printf("%s: %s\n", diskName, e.getTargetException());
            usage();
        } catch (Exception e) {
            err.printf("%s: %s\n", diskName, e);
            usage();
        }
        out.println("Boot: Starting kernel.");

        Kernel.interrupt(Kernel.INTERRUPT_POWER_ON,
                         cacheSize, 0, disk, shellCommand.toString(), null);

        out.println("Boot: Kernel has stopped.");
        exit(0);
    } // main
} // Boot
