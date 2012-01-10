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
public class UpdateThread extends TaskThread {
    
    private boolean reloadDB;
    
    public UpdateThread(JavAPT apt, MainFrame mf, boolean reloadDB) {
        super(apt, mf);
        this.reloadDB = reloadDB;
    }

    @Override
    public void run() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (!reloadDB) {
                        apt.updateSources(true);
                    } else {
                        apt.readDB();
                    }
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
