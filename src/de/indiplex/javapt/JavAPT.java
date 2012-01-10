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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import de.indiplex.javapt.gui.MainFrame;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 *
 * @author IndiPlex <Cartan12@indiplex.de>
 */
public class JavAPT {

    private File fSources;
    public File dDownloads;
    private File dTemp;
    public ArrayList<String> sourcesURLs = new ArrayList<String>();
    public ArrayList<DEB> debs = new ArrayList<DEB>();
    private HashMap<String, DEB> hashDEB = new HashMap<String, DEB>();
    public DB db;
    private long State = 0;
    private long Finish = 0;
    private MainFrame mf;
    private static final String SQLite_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS packages ("
            + "ID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "name varchar(100),"
            + "provides varchar(100),"
            + "depends varchar(100),"
            + "filename varchar(100)"
            + ")";

    public void init() throws IOException, URISyntaxException, SQLException {
        fSources = new File("sources.list");
        dDownloads = new File("downloads");
        dTemp = new File("tmp");
        Util.checkFiles(fSources);
        Util.checkDirs(dDownloads, dTemp);

        BufferedReader br = new BufferedReader(new FileReader(fSources));
        while (br.ready()) {
            String line = br.readLine();
            if (!line.equals("")) {
                sourcesURLs.add(line);
            }
        }
        if (!sourcesURLs.contains("http://apt.saurik.com/dists/ios/675.00/main/binary-iphoneos-arm/")) {
            sourcesURLs.add("http://apt.saurik.com/dists/ios/675.00/main/binary-iphoneos-arm/");
        }
        br.close();

        db = new DB();
        db.createDatabaseConnection("jdbc:sqlite:javAPT.db", "reata", "tata");
        Statement stat = db.getStat();
        stat.executeUpdate(SQLite_CREATE_TABLE);
        checkSources();
    }

    public long getState() {
        return State;
    }

    public long getFinish() {
        return Finish;
    }

    public boolean hasPackage(String pkg) {
        return hashDEB.containsKey(pkg);
    }

    public void readDB() throws SQLException {
        System.out.println("Reading DB...");
        Statement stat = db.getStat();
        ResultSet rsF = stat.executeQuery("SELECT count(*) FROM packages;");
        Finish = rsF.getInt(1);

        State = 0;
        ResultSet rs = stat.executeQuery("SELECT * FROM packages;");
        while (rs.next()) {
            State++;
            DEB deb = new DEB(rs.getString("name"));
            deb.provides = rs.getString("provides");
            deb.depends = rs.getString("depends");
            deb.filename = rs.getString("filename");
            debs.add(deb);
        }
        rs.close();

        if (isGUI()) {
            mf.fillPackages(debs);
        }

        System.out.println("Loaded DB! " + debs.size() + " packages found.");
        hashDEB();
    }

    private String checkURL(String u) throws IOException {
        String s = null;
        if (!u.endsWith("/")) {
            u = u + "/";
        }
        try {
            URLConnection conn = new URL(u + "Packages.bz2").openConnection();
            conn.setConnectTimeout(2000);
            conn.getInputStream().close();
            s = u;
        } catch (IOException ex) {
            URLConnection conn = new URL(u + "dists/stable/main/binary-iphoneos-arm/Packages.bz2").openConnection();
            conn.setConnectTimeout(12000);
            conn.getInputStream().close();
            s = u + "dists/stable/main/binary-iphoneos-arm";
        }
        if (s != null) {
            if (!s.endsWith("/")) {
                s = s + "/";
            }
        }
        return s;
    }
    
    public void removeSource(String source) throws FileNotFoundException {
        if (source.contains("apt.saurik.com")) {
            System.out.println("You cannot remove sauriks source!");
            return;
        }
        if (sourcesURLs.contains(source)) {
            sourcesURLs.remove(source);
            PrintStream pw = new PrintStream(fSources);
            for (String s:sourcesURLs) {
                pw.println(s);
            }
            pw.close();
            if (isGUI()) {
                mf.fillSources(sourcesURLs);
            }
        }
    }

    private void hashDEB() {
        for (DEB d : debs) {
            hashDEB.put(d.packagename, d);
        }
    }

    public void downloadDeb(String packagename, boolean withDeps) throws IOException, SQLException {
        ArrayList<String> toD = new ArrayList<String>();
        if (withDeps) {
            getDepends(packagename, toD);
        } else {
            DEB deb = hashDEB.get(packagename);
            if (deb == null) {
                if (!packagename.equals("")) {
                    System.out.println("Can't find " + packagename);
                }
                return;
            }
            toD.add(packagename);
        }
        PrintWriter pw = new PrintWriter(new File(dTemp, packagename + ".download"));
        for (String n : toD) {
            System.out.println("Downloading " + n);
            DEB deb = hashDEB.get(n);
            File to = new File(dDownloads, n+".deb");
            URLConnection con = new URL(deb.filename).openConnection();

            download(con, to);
            pw.println(n);
        }
        pw.close();
        if (isGUI()) {
            mf.fillDownloads(dDownloads);
        }
    }
    
    public ArrayList<String> getDepends(String pkg) throws IOException, SQLException {
        ArrayList<String> result = new ArrayList<String>();
        getDepends(pkg, result);
        return result;
    }

    private void getDepends(String packagename, ArrayList<String> depends) throws IOException, SQLException {
        if (packagename.equals("")) {
            return;
        }
        if (depends.contains(packagename)) {
            depends.remove(packagename);
            depends.add(packagename);
            return;
        }
        DEB deb = hashDEB.get(packagename);
        if (deb == null) {
            ResultSet rs = db.getStat().executeQuery("SELECT name FROM packages WHERE provides LIKE '%" + packagename + "%'");
            int i = 0;
            while (rs.next()) {
                getDepends(rs.getString("name"), depends);
                i++;
            }
            if (i == 0) {
                System.out.println("Can't find " + packagename);
            }
            return;
        }
        depends.add(packagename);
        String[] deps = deb.depends.split("\\,");
        for (String dep : deps) {
            dep = dep.trim();
            getDepends(dep, depends);
        }
    }

    public void shutdown() {
        db.shutdown();
    }

    public void checkSources() throws IOException {
        FileWriter fw = new FileWriter(fSources, false);
        ArrayList<String> newURLs = new ArrayList<String>();
        for (String url : sourcesURLs) {
            String s = checkURL(url);
            if (s == null) {
                System.out.println(url + " is not a valid Cydia repo!");
            } else {
                newURLs.add(s);
                fw.write(s + "\n");
            }
        }
        fw.close();
        sourcesURLs = newURLs;
    }

    public void addSource(String source) throws IOException {
        String s = checkURL(source);
        if (s == null) {
            System.out.println(source + " is not a valid Cydia repo!");
        } else {
            FileWriter fw = new FileWriter(fSources, true);
            fw.write(s + "\n");
            fw.close();
            sourcesURLs.add(s);
            System.out.println("Added source " + source);
            if (isGUI()) {
                mf.fillSources(sourcesURLs);
            }
        }
    }

    public void updateSources(boolean downloadSs) throws URISyntaxException, IOException, SQLException {
        Statement stat = db.getStat();
        stat.executeUpdate("DELETE FROM packages;");
        debs = new ArrayList<DEB>();
        int last = 0;
        for (String u : sourcesURLs) {
            String pre = new URL(u).getHost();
            File out = new File(dTemp, pre + ".source");
            if (downloadSs) {
                URLConnection con = new URL(u.toString() + "Packages.bz2").openConnection();
                File tmpOut = new File(dTemp, pre + ".source.bz2");

                System.out.println("Downloading " + con.getURL() + "...");

                download(con, tmpOut);

                System.out.println("Decompressing...");
                CountingFileInputStream fis = new CountingFileInputStream(tmpOut);
                BZip2CompressorInputStream in = new BZip2CompressorInputStream(fis);
                Finish = tmpOut.length();

                Util.checkFiles(out);
                OutputStream fout = new FileOutputStream(out);
                int i = in.read();
                while (i != -1) {
                    State = fis.getCount();
                    fout.write(i);
                    i = in.read();
                }
                fout.close();

                in.close();
            }

            BufferedReader br = new BufferedReader(new FileReader(out));
            DEB deb = null;
            ArrayList<String> lines = new ArrayList<String>();
            while (br.ready()) {
                String line = br.readLine();
                lines.add(line);
            }
            Finish = lines.size();
            State = 0;
            System.out.println("Parsing...");
            for (String line : lines) {
                State++;
                if (line.startsWith("Package")) {
                    if (deb != null) {
                        debs.add(deb);
                    }
                    deb = new DEB(p(line));
                }
                if (line.startsWith("Depends")) {
                    if (deb == null) {
                        continue;
                    }
                    deb.depends = p(line).trim();
                    deb.depends = deb.depends.replaceAll("\\({1}[^\\(]*\\)", "");
                }
                if (line.startsWith("Provides")) {
                    if (deb == null) {
                        continue;
                    }
                    deb.provides = p(line);
                }
                if (line.startsWith("Filename")) {
                    if (deb == null) {
                        continue;
                    }
                    deb.filename = u + p(line);
                    if (u.equals("http://apt.saurik.com/dists/ios/675.00/main/binary-iphoneos-arm/")) {
                        deb.filename = "http://apt.saurik.com/" + p(line);
                    }
                    if (u.contains("apt.thebigboss")) {
                        deb.filename = deb.filename.replace("dists/stable/main/binary-iphoneos-arm/", "");
                    }
                    deb.filename = deb.filename.replace("http://", "");
                    deb.filename = deb.filename.replace("//", "/");
                    deb.filename = "http://" + deb.filename;
                }
            }
            System.out.println("Updated " + u + "! " + (debs.size() - last) + " packages found");
            last = debs.size();
        }
        PreparedStatement pstat = db.getPreparedStatement("INSERT INTO packages (name, depends, provides, filename) VALUES (?,?,?,?);");
        for (DEB deb : debs) {
            pstat.setString(1, deb.packagename);
            pstat.setString(2, deb.depends);
            pstat.setString(3, deb.provides);
            pstat.setString(4, deb.filename);
            pstat.addBatch();
        }
        db.closePS();
        if (isGUI()) {
            mf.fillPackages(debs);
        }
        hashDEB();
    }

    private boolean isGUI() {
        return mf != null;
    }

    private String p(String s) {
        return s.split("\\:")[1].trim();
    }

    public void gui() {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        MainFrame mafr = new MainFrame(this);
        mafr.setVisible(true);
        mafr.init();
        this.mf = mafr;
    }

    public ArrayList<String> search(String pkg) throws SQLException {
        ArrayList<String> result = new ArrayList<String>();
        ResultSet rs = db.getStat().executeQuery("SELECT name FROM packages WHERE name LIKE '%" + pkg + "%';");

        while (rs.next()) {
            result.add(rs.getString("name"));
        }

        return result;
    }

    private void copy(InputStream in, File out) throws IOException {
        Util.checkFiles(out);
        OutputStream fout = new FileOutputStream(out);
        int i = in.read();
        int r = 0;
        while (i != -1) {
            r++;
            State = r;
            fout.write(i);
            i = in.read();
        }
        fout.close();
    }

    private void download(URLConnection con, File out) throws IOException {
        Finish = con.getContentLength();
        InputStream in = con.getInputStream();

        Util.checkFiles(out);
        OutputStream fout = new FileOutputStream(out);
        int i = in.read();
        int r = 0;
        while (i != -1) {
            r++;
            fout.write(i);
            i = in.read();
            State = r;
        }
        fout.close();
    }
}