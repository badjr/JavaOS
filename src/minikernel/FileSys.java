/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Modified by Brett Duncan for the operating systems project.
 */

package minikernel;

/* $Id: FileSys.java.src,v 1.4 2007/04/25 14:12:25 solomon Exp $ */

import java.util.*;
import java.io.*;
import static java.lang.System.*;

/** A file system. */

public class FileSys {
    /** The disk holding this file system. */
    private FastDisk disk;
    
    //************Code added by Brett Duncan*********************//
    private String currDir;
    private String fileTable[];
    
    //************End code added by Brett Duncan*********************//
    
    /** Initializes a FileSys instance for managing a disk.
     *
     * @param disk the disk containing the persistent data.
     */
    public FileSys(FastDisk disk) {
        this.disk = disk;
        
        //************Code added by Brett Duncan*********************//
        currDir = "/";
        
        
        fileTable = new String[100];
        int startBlock = 1; //The starting block where files begin to be stored.
        
        byte freeMap[] = new byte[disk.getBlockSize()];
        
        disk.read(0, freeMap);
        
        byte tempBuffer[] = new byte[disk.getBlockSize()];
        for (int i = startBlock; i < fileTable.length; i++) {
            if (freeMap[i] == '1') {
                disk.read(i, tempBuffer);
                
                String fileName = "";
                for (int j = 0; j < 32; j++) {
                    fileName += (char) tempBuffer[j];
                }
                fileTable[i] = fileName;
            }
        }
        
        /*
        for (int i = startBlock; i < fileTable.length; i++) {
            byte fileNameBuffer[] = new byte[32];
            disk.read(i, 32, fileNameBuffer);
            fileTable[i] = new String(fileNameBuffer);
        }*/
        
//        For debugging purposes, print out the fileTable[] array.
//        for (int i = startBlock; i < fileTable.length; i++) {
//            System.out.printf("fileTable[%d] = %s\n", i, fileTable[i]);
//        }
        
        //************End code added by Brett Duncan*********************//
        
    } // FileSys(FastDisk)
    
    //************Code added by Brett Duncan*********************//
    
    public FastDisk getDisk() {
        return disk;
    }
    
    public int getBlockSizeOfDisk() {
        return disk.getBlockSize();
    }
    
    public void setDisk(Disk disk) {
        this.disk = (FastDisk) disk;
    }
    
    public String[] getFileTable() {
        return fileTable;
    }
    
    public void updateFileTable(int targetBlock, String newFileName) {
        fileTable[targetBlock] = newFileName;
    }
    
    //************End code added by Brett Duncan*********************//
    
} // FileSys
