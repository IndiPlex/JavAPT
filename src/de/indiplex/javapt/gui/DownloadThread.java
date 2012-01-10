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
package de.indiplex.javapt.gui;

import de.indiplex.javapt.JavAPT;

/**
 *
 * @author IndiPlex <Cartan12@indiplex.de>
 */
public class DownloadThread extends TaskThread {
    
    private String to;
    private boolean withDeps;
    
    public DownloadThread(JavAPT apt, MainFrame mf, String to, boolean withDeps) {
        super(apt, mf);
        this.to = to;
        this.withDeps = withDeps;
    }
    
    @Override
    public void run() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    apt.downloadDeb(to, withDeps);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Error!");
                }
            }
        });
        t.start();
        loop(t);
    }
}