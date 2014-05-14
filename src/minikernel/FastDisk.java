/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Modified by Brett Duncan for the operating systems project.
 */

package minikernel;

/* $Id: FastDisk.java,v 1.15 2006/11/22 21:47:00 solomon Exp $ */

/** A new and improved Disk.
 * <b>You may not change this class.</b>
 * <p>
 * This disk is so much faster than the previous model that read and write
 * operations appear to finish in no time.   Because disk is so fast, beginRead
 * and beginWrite wait for the operation to finish rather than causing a CPU
 * interrupt when they complete.
 * <p>
 * @see Disk
 * @see Kernel
 */
public class FastDisk extends Disk {
    /** Creates a new FastDisk.
     * @param size the total size of this disk, in blocks.
     */
    public FastDisk(int size) {
        super(size);
        if (size < 0 || size >= (1<<15)) {
            throw new DiskException(
                String.format(
                    "Cannot make a FastDisk with %d blocks.  Max size is %d.",
                    size, 1<<15));
        }
    } // FastDisk

    /** Performs a read operation.
     * When this method returns, the operation is complete.
     * @param blockNumber The block number to read from.
     * @param buffer a data area to hold the data read.
     * @see Disk#beginRead(int, byte[])
     */
    public void read(int blockNumber, byte buffer[]) {
        System.arraycopy(
            data, blockNumber * BLOCK_SIZE,
            buffer, 0,
            BLOCK_SIZE);
        readCount++;
    } // read(int, byte[])

    /** Performs a write operation.
     * When this method returns, the operation is complete.
     * @param blockNumber The block number to write to.
     * @param buffer a data area to hold the data to be written.
     * @see Disk#beginWrite(int, byte[])
     */
    public void write(int blockNumber, byte buffer[]) {
        System.arraycopy(
            buffer, 0,
            data, blockNumber * BLOCK_SIZE,
            BLOCK_SIZE);
        writeCount++;
    } // write(int, byte[])
    
    //************Code added by Brett Duncan*********************//
    @Override
    public void format() {
        data = new byte[DISK_SIZE * BLOCK_SIZE];
        for (int i = 1; i < DISK_SIZE; i++) {
            data[i] = '0';
        }
    }
    
    public int getBlockSize() {
        return BLOCK_SIZE;
    }
    
    //************End code added by Brett Duncan*********************//

    /** Starts a new read operation.
     * @param blockNumber The block number to read from.
     * @param buffer A data area to hold the data read.  This array must be
     *               allocated by the caller and have length of at least
     *               BLOCK_SIZE.  If it is larger, only the first BLOCK_SIZE
     *               bytes of the array will be modified.
     * @deprecated Do not use this method.  Use read instead.
     */
    @Deprecated
    public synchronized void beginRead(int blockNumber, byte buffer[]) {
        throw new UnsupportedOperationException(
                        "Don't use beginRead.  Use read");
    } // beginRead(int, byte[])

    /** Starts a new write operation.
     * @param blockNumber The block number to write to.
     * @param buffer A data area containing the data to be written.  This array
     *               must be allocated by the caller and have length of at least
     *               BLOCK_SIZE.  If it is larger, only the first BLOCK_SIZE
     *               bytes of the array will be sent to the disk.
     * @deprecated Do not use this method.  Use read instead.
     */
    @Deprecated
    public synchronized void beginWrite(int blockNumber, byte buffer[]) {
        throw new UnsupportedOperationException(
                        "Don't use beginWrite.  Use write");
    } // beginWrite byte[])
} // FastDisk
