/*
 * JavAPT
 * Copyright (C) 2012 IndiPlex
 * 
 * JavAPT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.indiplex.javapt;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author IndiPlex <Cartan12@indiplex.de>
 */
public class Start {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            File errFolder = new File("Error logs");
            errFolder.mkdir();
            System.setErr(new PrintStream(new File(errFolder, df.format(new Date())+".err")));
            JavAPT apt = new JavAPT();
            apt.init();
            if (args.length != 0) {
                if (args[0].equalsIgnoreCase("gui")) {
                    apt.gui();
                }
                if (args[0].equalsIgnoreCase("update")) {
                    if (args.length == 1) {
                        apt.updateSources(true);
                    } else {
                        apt.updateSources(false);
                    }
                    System.out.println("Updated!");
                }
                if (args[0].equalsIgnoreCase("check")) {
                    apt.checkSources();
                    System.out.println("Checked sources!");
                }
                if (args[0].equalsIgnoreCase("list")) {
                    apt.readDB();
                    System.out.println(apt.debs.size());
                    for (DEB d : apt.debs) {
                        System.out.println(d.packagename);
                    }
                }
                if (args[0].equalsIgnoreCase("search")) {
                    apt.readDB();
                    if (args.length > 1) {
                        for (String s : apt.search(args[1])) {
                            System.out.println(s);
                        }
                    } else {
                        System.out.println("Error!");
                    }
                }
                if (args[0].equalsIgnoreCase("addsource")) {
                    if (args.length > 1) {
                        apt.addSource(args[1]);
                    }
                }
                if (args[0].equalsIgnoreCase("download")) {
                    apt.readDB();
                    if (args.length > 1) {
                        apt.downloadDeb(args[1], true);
                    }
                }
            }
            apt.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
