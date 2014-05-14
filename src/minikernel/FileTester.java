/**
 * Code acquired from http://pages.cs.wisc.edu/~solomon/cs537/project5/.
 * Modified by Brett Duncan for the operating systems project.
 */

package minikernel;

import java.io.*;
import java.util.*;
import static java.lang.System.*;

/** Basic driver program to be used as a shell for the MiniKernel for project 5.
 * It can be run in two modes:
 * <dl compact>
 *        <dt>Interactive:              <dd>java Boot ... FileTester
 *        <dt>With a test script file:  <dd>java Boot ... FileTester script
 * </dl>
 * To get a list of supported commands, type 'help' at the command prompt.
 * <p>
 * The testfile consists of commands to the driver program (one per line) as
 * well as comments.  Comments beginning with /* will be ignored completely by
 * the driver.  Comments beginning with // will be echoed to the output.
 * <p>
 * See the test files test*.data for examples.
 *
 * Revised, May 7, 2007.
 * $Id: FileTester.java,v 1.21 2007/05/07 15:04:02 solomon Exp $
 */
public class FileTester {
    /** Synopsis of commands. */
    private static String[] helpInfo = {
        "help",
        "quit",
        "format",
        "cd pathname",
        "pwd",
        "create pathname",
        "read pathname",
        "write pathname data",
        "writeln pathname",
        "rm pathname",
        "mkdir pathname",
        "rmdir pathname",
        "ln oldpath newpath",
        "readlink pathname",
        "ls [ dirname ]",
    };

    /** Disk block size, as retrieved from the Disk (cheating!). */
    private static int blockSize;

    /** Main program.
     * @param args command-line arguments (there should be at most one:
     *      the name of a test file from which to read commands).
     */
    public static void main(String [] args) {
        
        //************Code added by Brett Duncan*********************//
        
        //Power on the disk
        Kernel.interrupt(Kernel.INTERRUPT_POWER_ON, 10, 0, new FastDisk(100), null, null);
        //XXX: Script to test commands
//        args = new String[1];
////        args[0] = "test1.script";
//        args[0] = "mytest.script";
        
        //************End code added by Brett Duncan*********************//
        
        // NB:  This program is designed only to test the file system support
        // of the kernel, so it "cheats" in using non-kernel operations to
        // read commands and write diagnostics.
        if (args.length > 1) {
            System.err.println("usage: FileTester [ script-file ]");
            System.exit(0);
        }

//        blockSize = Disk.BLOCK_SIZE;
            // This is a bit of a cheat.  We really should have a Kernel call
            // to get this information.
        blockSize = Library.getBlockSizeOfDisk();
        //XXX: Fixed it.

        // Is the input coming from a file?
        boolean fromFile = (args.length == 1);

        // Create a stream for input
        BufferedReader input = null;

        // Open our input stream
        if (fromFile) {
            try {
                input = new BufferedReader(new FileReader(args[0]));
            } catch (FileNotFoundException e) {
                System.err.println("Error: Script file "
                        + args[0] + " not found.");
                System.exit(1);
            }
        } else {
            input = new BufferedReader(new InputStreamReader(System.in));
        }

        // Cycle through user or file input
        for (;;) {
            String cmd = null;
            try {
                // Print out the prompt for the user
                if (!fromFile) {
                    out.printf("--> ");
                    System.out.flush();
                }

                // Read in a line
                String line = input.readLine();

                // Check for EOF and empty lines
                if (line == null) {
                    // End of file (Ctrl-D for interactive input)
                    return;
                }
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                // Handle comments and echoing
                if (line.startsWith("//")) {
                    if (fromFile) {
                        out.printf("%s\n", line);
                    }
                    continue;
                }
                if (line.startsWith("/*")) {
                    continue;
                }

                // Echo the command line
                if (fromFile) {
                    out.printf("--> %s\n", line);
                }

                // Parse the command line
                StringTokenizer st = new StringTokenizer(line);
                cmd = st.nextToken();

                // Call the function that corresponds to the command
                int result = 0;
                if (cmd.equalsIgnoreCase("quit")) {
                    Library.shutdown();
                    return;
                } else if (cmd.equalsIgnoreCase("help") || cmd.equals("?")) {
                    help();
                    continue;
                } else if (cmd.equalsIgnoreCase("format")) {
                    result = Library.format();
                } else if (cmd.equalsIgnoreCase("cd")) {
                    result = Library.chdir(st.nextToken());
                } else if (cmd.equalsIgnoreCase("pwd")) {
                    result = pwd();
                } else if (cmd.equalsIgnoreCase("create")) {
                    result = Library.create(st.nextToken());
                } else if (cmd.equalsIgnoreCase("read")) {
                    result = readTest(st.nextToken(), false);
                } else if (cmd.equalsIgnoreCase("write")) {
                    result = writeTest(st.nextToken(), line);
                } else if (cmd.equalsIgnoreCase("writeln")) {
                    result = writeLines(st.nextToken(), input);
                } else if (cmd.equalsIgnoreCase("rm")) {
                    result = Library.delete(st.nextToken());
                } else if (cmd.equalsIgnoreCase("mkdir")) {
                    result = Library.mkdir(st.nextToken());
                } else if (cmd.equalsIgnoreCase("rmdir")) {
                    result = Library.rmdir(st.nextToken());
                } else if (cmd.equalsIgnoreCase("ln")) {
                    String oldName = st.nextToken();
                    String newName = st.nextToken();
                    result = Library.symlink(oldName, newName);
                } else if (cmd.equalsIgnoreCase("readlink")) {
                    result = readTest(st.nextToken(), true);
                } else if (cmd.equalsIgnoreCase("ls")) {
                    if (st.hasMoreTokens()) {
                        result = dumpDir(st.nextToken());
                    } else {
                        result = dumpDir(".");
                    }
                } else {
                    out.printf("unknown command\n");
                    continue;
                }

                // Print out the result of the function call
                if (result != 0) {
                    if (result == -1) {
                        out.printf("*** System call failed\n");
                    } else {
                        out.printf("*** Bad result %d from system call\n",
                                    result);
                    }
                }
            } catch (NoSuchElementException e) {
                // Handler for nextToken()
                out.printf("Incorrect number of arguments\n");
                help(cmd);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } // for (;;)
    } // main(String[])

    /** Prints a list of available commands. */
    private static void help() {
        out.printf("Commands are:\n");
        for (int i = 0; i < helpInfo.length; i++) {
            out.printf("    %s\n", helpInfo[i]);
        }
    } // help()

    /** Prints help for command "cmd".
     * @param cmd the name of the command.
     */
    private static void help(String cmd) {
        for (int i = 0; i < helpInfo.length; i++) {
            if (helpInfo[i].startsWith(cmd)) {
                out.printf("usage: %s\n", helpInfo[i]);
                return;
            }
        }
        out.printf("unknown command '%s'\n", cmd);
    } // help(String)

    /** Reads data from a (simulated) file or symlink using Library.read
     * or Library.readlink and displays the results.
     * @param fname the name of the file or symlink.
     * @param isLink true to read a symlink, false to read an ordinary file.
     * @return the result of the Library.read call.
     */
    private static int readTest(String fname, boolean isLink) {
        byte[] buf = new byte[blockSize];
        int n = isLink
                ? Library.readlink(fname, buf)
                : Library.read(fname, buf);
        boolean needNewline = false;
        if (n < 0) {
            return n;
        }
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != 0) {
                showChar(buf[i] & 0xff);
                needNewline = (buf[i] != '\n');
            }
        }
        if (needNewline) {
            out.printf("\n");
        }
        return n;
    } // readTest(String,boolean)

    /** Writes data to a (simulated) file using Library.write.
     * @param fname the name of the file.
     * @param info a source of data.
     * @return the result of the Library.write call.
     */
    private static int writeTest(String fname, String info) {
        // Info has the format 'write fname one two three ...
        int p;
        p = info.indexOf(' ');
        if (p >= 0) {
            p = info.indexOf(' ', p + 1);
            if (p < 0) {
                p = info.length();
            } else {
                p++;
            }
        } else {
            p = 0;
        }
        byte[] buf = new byte[Math.max(blockSize, info.length() - p)];
        int i = 0;
        while (p < info.length()) {
            buf[i++] = (byte) info.charAt(p++);
        }
        return Library.write(fname, buf);
    } // writeTest(String, byte[])

    /** Write data to a (simulated) file using Library.write.
     * Data comes from the following lines in the input stream.
     * @param fname the name of the file.
     * @param in the input stream.
     * @return the result of the Library.write call.
     */
    private static int writeLines(String fname, BufferedReader in) {
        try {
            byte[] buf = new byte[blockSize];
            int i = 0;
            for (;;) {
                String line = in.readLine();
                if (line == null || line.equals(".")) {
                    break;
                }
                for (int j = 0; j < line.length(); j++) {
                    if (i >= buf.length) {
                        byte[] newBuf = new byte[buf.length * 2];
                        System.arraycopy(buf, 0, newBuf, 0, buf.length);
                        buf = newBuf;
                    }
                    buf[i++] = (byte) line.charAt(j);
                }
                if (i >= buf.length) {
                    break;
                }
                buf[i++] = '\n';
            }
            return Library.write(fname, buf);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    } // writeLines(String, BufferedReader)

    /** Display a readable representation of a byte.
     * @param b the byte to display as a number in the range 0..255.
     */
    private static void showChar(int b) {
        if (b >= ' ' && b <= '~') {
            out.printf("%c", (char)b);
            return;
        }
        if (b == '\n') {
            out.printf("\\n\n");
            return;
        }
        if (b == '\\') {
            out.printf("\\\\");
            return;
        }
        out.printf("\\%03o", b);
    } // showChar(int)

    /** Displays the contents of a directory.
     * @param dirname the name of the directory.
     * @return the result of the readdir call.
     */
    private static int dumpDir(String dirname) {
        byte[] buf = new byte[blockSize];
        int n = Library.readdir(dirname, buf);
        if (n < 0) {
            return n;
        }
        for (int i = 0; i < buf.length; i += 16) {
            int block = ((buf[i] & 0xff) << 8) + (buf [i+1] & 0xff);
            if (block == 0) {
                continue;
            }
            StringBuffer sb = new StringBuffer();
            for (int j = 3; j < 16; j++) {
                if (buf[i + j] == 0) {
                    break;
                }
                sb.append((char) buf[i + j]);
            }
            String fname = sb.toString();
            out.printf("%s %s", block, fname);
            switch (buf[i + 2]) {
            case 'O':
                break;
            case 'D':
                out.printf("/");
                break;
            case 'L':
                out.printf(" -> ");
                byte[] buf1 = new byte[blockSize];
                n = Library.readlink(dirname + "/" + fname, buf1);
                if (n < 0) {
                    return n;
                }
                for (int j = 0; j < buf1.length; j++) {
                    if (buf1[j] == 0) {
                        break;
                    }
                    out.printf("%c", (char) buf1[j]);
                }
                break;
            default:
                out.printf("?type \\%03o?", buf[i + 2]);
                //out.printf("<type %d>", buf[i + 2]);
            }
            out.printf("\n");
        }
        return n;
    } // dumpDir(String)

    /** Prints the current working directory, as determined from inspecting
     * various directories with readdir().  Prints an error message if
     * something is wrong.
     *
     * @return 0 if everything is ok, or -1 if there is an error.
     */
    private static int pwd() {
        int rc, dot, dotdot;
        int child = 0;
        String relPath = ".";
        List<String> path = new LinkedList<String>();
        byte[] buf = new byte[blockSize];
        for (;;) {
            rc = Library.readdir(relPath, buf);
            if (rc != 0) {
                out.printf("pwd:  cannot read directory \"%s\"\n", relPath);
                return -1;
            }
            dot = dirSearch(buf, ".");
            if (dot == 0) {
                out.printf("pwd: bad directory \"%s\": no . entry\n",
                            relPath);
                return -1;
            }
            if (child != 0) {
                String cname = dirSearch(buf, child);
                if (cname == null) {
                    out.printf("pwd: bad directory \"%s\": "
                                + " no entry for %d\n", relPath, child);
                    return -1;
                }
                path.add(0, cname);
            }
            dotdot = dirSearch(buf, "..");
            if (dotdot == 0) {
                out.printf("pwd: bad directory \"%s\": no .. entry\n",
                            relPath);
                return -1;
            }
            if (dot == dotdot) {
                break;
            }
            child = dot;
            relPath += "/..";
        }
        if (path.size() == 0) {
            out.printf("/");
        } else {
            for (String s : path) {
                out.printf("/%s", s);
            }
        }
        out.printf("\n");
        return 0;
    } // pwd()

    /** Searches a directory for a particular name.
     *
     * @param buf the raw contents of the directory
     * @param s the name to look for
     * @return the corresponding block number, or 0 for errors.
     */
    private static int dirSearch(byte[] buf, String s) {
        for (int offset = 0; offset < buf.length; offset += 16) {
            int j;
            for (j = 0; j < 13; j++) {
                if (j >= s.length()
                    || buf[offset + j + 3] != (byte) s.charAt(j))
                {
                    break;
                }
            }
            if (j == s.length() && buf[offset + j + 3] == 0) {
                return (((buf[offset] & 0xff) << 8) + (buf[offset+1] & 0xff));
            }
        }
        return 0;
    } // dirSearch(byte[],String)

    /** Searches a directory for an entry with a particular block number
     *
     * @param buf the raw contents of the directory
     * @param n the block num to look for
     * @return the corresponding name, or null for errors.
     */
    private static String dirSearch(byte[] buf, int n) {
        for (int offset = 0; offset < buf.length; offset += 16) {
            int blk = (((buf[offset] & 0xff) << 8) + (buf[offset+1] & 0xff));
            if (blk == n) {
                int j;
                for (j = 0; j < 13; j++) {
                    if (buf[offset + j + 3] == 0) {
                        break;
                    }
                }
                return new String(buf, offset + 3, j);
            }
        }
        return null;
    } // dirSearch(byte[],String)
} // FileTester
