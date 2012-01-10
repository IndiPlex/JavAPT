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
public abstract class TaskThread implements Runnable {
    
    protected JavAPT apt;
    protected MainFrame mf;

    public TaskThread(JavAPT apt, MainFrame mf) {
        this.apt = apt;
        this.mf = mf;
    }
    
    protected void loop(Thread t) {
        long oldFinish = apt.getFinish();
        long oldState = apt.getState();
        
        mf.setPBMax(oldFinish);
        mf.setProgressBar(oldState);
        
        while(t.isAlive()) {
            long newFinish = apt.getFinish();
            long newState = apt.getState();
            
            if (newFinish!=oldFinish) {
                mf.resetPB();
                mf.setPBMax(newFinish);
                oldFinish = newFinish;
            }
            if (newState!=oldState) {
                mf.setProgressBar(newState);
                oldState = newState;
            }
        }
        mf.resetPB();
        mf.taskPerforming = false;
    }
}
