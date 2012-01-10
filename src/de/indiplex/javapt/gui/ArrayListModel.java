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

import java.util.ArrayList;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author IndiPlex <Cartan12@indiplex.de>
 */
public class ArrayListModel implements ListModel {
    
    private ArrayList<String> data;

    @Override
    public int getSize() {
        return data.size();
    }

    @Override
    public String getElementAt(int index) {
        if (index>=0 && index<data.size()) {
            return data.get(index);
        } else {
            return null;
        }
    }

    @Override
    public void addListDataListener(ListDataListener l) {
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
    }

    public void setData(ArrayList<String> data) {
        this.data = data;
    }
    
    public void setData(String[] data) {
        this.data = new ArrayList<String>();
        for (String d:data) {
            this.data.add(d);
        }
    }
    
}
