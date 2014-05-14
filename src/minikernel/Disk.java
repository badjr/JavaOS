/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Modified by Brett Duncan for the operating systems project.
 */

package minikernel;

/* $Id: Disk.java,v 1.17 2006/11/09 20:42:29 solomon Exp $ */

import java.io.*;
import static java.lang.System.*;

/** A software simulation of a Disk.
 * <p>
 * <b>You may not change this class.</b>
 * <p>
 * This disk is slow and ornery.
 * It contains a number of blocks, all BLOCK_SIZE bytes long.
 * All operations occur on individual blocks.
 * You can't modify any more or any less data at a time.
 * <p>
 * To read or write from the disk, call beginRead() or beginWrite().
 * Each of these functions will start the action and return immediately.
 * When the action has been completed, the Disk calls Kernel.interrupt()
 * to let you know the Disk is ready for more.
 * <p>
 * It may take a while for the disk to seek from one block to another.
 * Seek time is proportional to the difference in block numbers of the 
 * blocks.
 * <p>
 * <b>Warning:</b> Don't call beginRead() or beginWrite() while the 
 * disk is busy! If you don't treat
 * the Disk gently, the system will crash! (Just like a real machine!)
 * <p>
 * This disk saves its contents in the Unix file DISK between runs.
 * Since the file can be large, you should get in the habit of removing it
 * before logging off.
 *
 * @see Kernel
 */
public class Disk implements Runnable {
    /////////////////////////////////////////// Disk geometry parameters

    /** The size of a disk block in bytes. */
    public static final int BLOCK_SIZE = 512;

    /** Total size of this disk, in blocks. */
    public final int DISK_SIZE;

    /////////////////////////////////////////// Transient internal state

    /** Current location of the read/write head. */
    protected int currentBlock = 0;

    /** The data stored on the disk. */
    protected byte[] data;

    /** An indication of whether an I/O operation is currently in progress. */
    protected boolean busy;

    /** An indication whether the current I/O operation is a write operation.
     * Only meaningful if busy == true.
     */
    private boolean isWriting;

    /** The block number to be read/written by the current operation.
     * Only meaningful if busy == true.
     */
    protected int targetBlock;

    /** Memory buffer to/from which current I/O operation is transferring.
     * Only meaningful if busy == true.
     */
    private byte[] buffer;

    /** A flag set by beginRead or beginWrite to indicate that a request
     * has been submitted.
     */
    private boolean requestQueued = false;

    /** A count of read operations performed, for statistics. */
    protected int readCount;

    /** A count of write operations performed, for statistics. */
    protected int writeCount;

    /////////////////////////////////////////// Inner classes

    /** The exception thrown when an illegal operation is attempted on the
     * disk.
     */
    static protected class DiskException extends RuntimeException {
        static final long serialVersionUID = 0;
        /** Creates a new exception.
         *
         * @param s the detail string for the exception.
         */
        public DiskException(String s) {
            super("*** YOU CRASHED THE DISK: " + s);
        }
    }

    /////////////////////////////////////////// Constructors

    /** Creates a new Disk.
     * If a Unix file named DISK exists in the local Unix directory, the
     * simulated disk contents are initialized from the Unix file.
     * It is an error if the DISK file exists but its size does not match
     * "size".
     * If there is no DISK file, the first block of the simulated disk is
     * cleared to nulls and the rest is filled with random junk.
     * 
     * @param size the total size of this disk, in blocks.
     */
    public Disk(int size) {
        File diskName = new File("DISK");
        if (diskName.exists()) {
            if (diskName.length() != size * BLOCK_SIZE) {
                throw new DiskException(
                    "File DISK exists but is the wrong size");
            }
        }
        this.DISK_SIZE = size;
        if (size < 1) {
            throw new DiskException("A disk must have at least one block!");
        }
        // NOTE:  the "new" operator always clears the result object to nulls
        data = new byte[DISK_SIZE * BLOCK_SIZE];
        int count = BLOCK_SIZE;
        try {
            FileInputStream is = new FileInputStream("DISK");
            is.read(data);
            out.printf("Restored %d bytes from file DISK\n", count);
            is.close();
            return;
        } catch (FileNotFoundException e) {
            out.println("Creating new disk");
            this.format();
        } catch (Exception e) {
            e.printStackTrace();
            exit(1);
        }
        //*****************Commented out by Brett Duncan
//        byte[] junk = new byte[BLOCK_SIZE];
//        for (int i = 0; i < BLOCK_SIZE; ) {
//            junk[i++] = 74;
//            junk[i++] = 85;
//            junk[i++] = 78;
//            junk[i++] = 75;
//        }
//        for (int i = 1; i < DISK_SIZE; i++) {
//            arraycopy(
//                junk, 0,
//                data, i * BLOCK_SIZE,
//                BLOCK_SIZE);
//        }
    } // Disk(int)

    /////////////////////////////////////////// Methods

    /**
     * Formats the disk.
     */
    public void format() {
        data = new byte[DISK_SIZE * BLOCK_SIZE];
        //*****************Commented out by Brett Duncan
//        byte[] junk = new byte[BLOCK_SIZE];
//        for (int i = 0; i < BLOCK_SIZE; ) {
//            junk[i++] = 74;
//            junk[i++] = 85;
//            junk[i++] = 78;
//            junk[i++] = 75;
//        }
//        for (int i = 1; i < DISK_SIZE; i++) {
//            arraycopy(
//                junk, 0,
//                data, i * BLOCK_SIZE,
//                BLOCK_SIZE);
//        }
    }
    
    /** Saves the contents of this Disk.
     * The contents of this disk will be forced out to a file named
     * DISK so that they can be restored on the next run of this program.
     * This file could be quite big, so delete it before you log out.
     * Also prints some statistics on disk operations.
     */
    public void flush() {
        try {
            out.println("Saving contents to DISK file...");
            FileOutputStream os = new FileOutputStream("DISK");
            os.write(data);
            os.close();
            out.printf(
                "%d read operations and %d write operations performed\n",
                readCount, writeCount);
        } catch(Exception e) {
            e.printStackTrace();
            exit(1);
        }
    } // flush()

    /** Sleeps for a while to simulate the delay in seeking and transferring
     * data.
     * @param targetBlock the block number to which we have to seek.
     */
    protected void delay(int targetBlock) {
        int sleepTime = 10 + Math.abs(targetBlock - currentBlock) / 5;
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // delay(int)

    /** Starts a new read operation.
    * @param blockNumber The block number to read from.
    * @param buffer A data area to hold the data read.  This array must be
    *               allocated by the caller and have length of at least
    *               BLOCK_SIZE.  If it is larger, only the first BLOCK_SIZE
    *               bytes of the array will be modified.
    */
    public synchronized void beginRead(int blockNumber, byte[] buffer) {
        if (
                blockNumber < 0
                || blockNumber >= DISK_SIZE
                || buffer == null
                || buffer.length < BLOCK_SIZE)
        {
            throw new DiskException("Illegal disk read request: "
                        + " block number " + blockNumber
                        + " buffer " + buffer);
        }

        if (busy) {
            throw new DiskException("Disk read attempted "
                        + " while the disk was still busy.");
        }

        isWriting = false;
        this.buffer = buffer;
        targetBlock = blockNumber;
        requestQueued = true;

        notify();
    } // beginRead(int, byte[])

    /** Starts a new write operation.
    * @param blockNumber The block number to write to.
    * @param buffer A data area containing the data to be written.  This array
    *               must be allocated by the caller and have length of at least
    *               BLOCK_SIZE.  If it is larger, only the first BLOCK_SIZE
    *               bytes of the array will be sent to the disk.
    */
    public synchronized void beginWrite(int blockNumber, byte[] buffer) {
        if (
                blockNumber < 0
                || blockNumber >= DISK_SIZE
                || buffer == null
                || buffer.length < BLOCK_SIZE)
        {
            throw new DiskException("Illegal disk write request: "
                        + " block number " + blockNumber
                        + " buffer " + buffer);
        }

        if (busy) {
            throw new DiskException("Disk write attempted "
                        + " while the disk was still busy.");
        }

        isWriting = true;
        this.buffer = buffer;
        targetBlock = blockNumber;
        requestQueued = true;

        notify();
    } // beginWrite(int, byte[])

    /** Waits for a call to beginRead or beginWrite. */
    protected synchronized void waitForRequest() {
        while(!requestQueued) {
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        requestQueued = false;
        busy = true;
    } // waitForRequest()

    /** Indicates to the CPU that the current operation has completed. */
    protected void finishOperation() {
        synchronized (this) {
            busy = false;
            currentBlock = targetBlock;
        }
        // NOTE:  The interrupt needs to be outside the critical section
        // to avoid a race condition:  The interrupt handler in the kernel
        // may wish to call beginRead or beginWrite (perhaps indirectly),
        // which would deadlock if the interrupt handler were invoked with
        // the disk mutex locked.
        Kernel.interrupt(Kernel.INTERRUPT_DISK,
            0,0,null,null,null);
    } // finishOperation()

    /** This method simulates the internal microprocessor of the disk
     * controler.  It repeatedly waits for a start signal, does an I/O
     * operation, and sends an interrupt to the CPU.
     * This method should <em>not</em> be called directly.
     */
    public void run() {
        for (;;) {
            waitForRequest();

            // Pause to do the operation
            delay(targetBlock);

            // Move the data.
            if (isWriting) {
                arraycopy(
                    buffer, 0,
                    data, targetBlock * BLOCK_SIZE,
                    BLOCK_SIZE);
                writeCount++;
            } else {
                arraycopy(
                    data, targetBlock * BLOCK_SIZE,
                    buffer, 0,
                    BLOCK_SIZE);
                readCount++;
            }

            // Signal completion
            finishOperation();
        }
    } // run()
} // Disk
