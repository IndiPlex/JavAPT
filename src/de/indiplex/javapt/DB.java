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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author IndiPlex <Cartan12@indiplex.de>
 */
public class DB {

    private Statement stat;
    private PreparedStatement pstat;
    private Connection con;
    private String url;
    private String user;
    private String pass;

    public Statement getStat() {
        try {
            if (con.isClosed()) {
                createDatabaseConnection(url, user, pass);
            }            
            
            return stat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public PreparedStatement getPreparedStatement(String sql) {
        try {
            if (con.isClosed()) {
                createDatabaseConnection(url, user, pass);
            }
            
            con.setAutoCommit(false);
            if (pstat == null) {
                pstat = con.prepareStatement(sql);
            }
            return pstat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void closePS() throws SQLException {
        pstat.executeBatch();
        pstat.close();
        pstat = null;
        con.setAutoCommit(true);
    }

    public void createDatabaseConnection(String url, String user, String pass) {
        try {
            this.url = url;
            this.user = user;
            this.pass = pass;
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection(url);
            stat = con.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            stat.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }
}