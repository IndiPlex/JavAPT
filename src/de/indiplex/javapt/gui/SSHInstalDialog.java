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

/*
 * SSHInstalDialog.java
 *
 * Created on 08.01.2012, 15:07:42
 */
package de.indiplex.javapt.gui;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import de.indiplex.javapt.JavAPT;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 *
 * @author IndiPlex <Cartan12@indiplex.de>
 */
public class SSHInstalDialog extends javax.swing.JDialog {

    private MainFrame mf;
    private JavAPT apt;
    private String pkg;
    private ArrayList<String> deps = new ArrayList<String>();
    private int currDep = 0;
    private State state;
    private String host;
    private String user = "User";
    private String pass;
    private Connection conn;
    private Session ssh;
    private Running reader;
    private boolean needInput = false;
    private SCPClient scp;    

    public SSHInstalDialog(java.awt.Frame parent, boolean modal, String pkg, boolean withDepends) {
        super(parent, modal);
        initComponents();
        MainFrame mafr = (MainFrame) parent;
        this.mf = mafr;
        apt = mf.apt;
        pkg = pkg.substring(0, pkg.length() - 4);
        if (!apt.hasPackage(pkg)) {
            System.out.println("[ERROR] The package " + pkg + " doesn't exists...");
            pkg = null;
            this.dispose();
            return;
        }
        this.pkg = pkg;
        try {
            if (withDepends) {
                deps = apt.getDepends(pkg);
            } else {
                deps = new ArrayList<String>();
                deps.add(pkg);
            }
            Collections.reverse(deps);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error!");
            dispose();
            return;
        }
        state = State.ENTER_HOST;
        printConsole("Enter host-IP:");
    }

    private synchronized void printConsole(String msg) {
        jTextArea1.setText(jTextArea1.getText() + msg + "\n");
    }

    private void printConsole(int data) {
        jTextArea1.setText(jTextArea1.getText() + ((char) data));
    }

    private void parseInput(String in) {
        printConsole(in);
        switch (state) {
            case ENTER_HOST:
                String[] split = in.split("\\.");
                if (in.equals("") || split.length != 4) {
                    printConsole("Please enter a valid host-IP!");
                } else {
                    host = in;
                    printConsole("Enter username:");
                    state = State.ENTER_USER;
                }
                break;
            case ENTER_USER:
                if (in.equals("")) {
                    printConsole("Please enter a valid username!");
                } else {
                    user = in;
                    JPasswordField passwordField = new JPasswordField(10);
                    passwordField.setEchoChar('*');
                    JOptionPane.showMessageDialog(
                            null,
                            passwordField,
                            "Enter password:",
                            JOptionPane.PLAIN_MESSAGE);
                    while (new String(passwordField.getPassword()).equals("")) {
                        JOptionPane.showMessageDialog(
                                this,
                                passwordField,
                                "Enter password:",
                                JOptionPane.PLAIN_MESSAGE);
                    }
                    pass = new String(passwordField.getPassword());
                    printConsole("Connecting with " + user + ":*****@" + host + "...");
                    state = State.CONNECTING;
                    try {
                        connectWithDevice();
                        printConsole("Connected!");
                        sendToDevice("cd Media");
                        state = State.ASKING;
                        askDep();
                    } catch (Exception e) {
                        e.printStackTrace();
                        printConsole("Error! Try \"reconnect\"");
                        state = State.FAILED;
                    }
                }
                break;
            case ASKING:
                if (needInput) {
                    if (in.equals("yes") || in.equals("y")) {
                        if (!askDep()) {
                            state = State.INSTALLING;
                            if (!install()) {
                                return;
                            }
                            printConsole("Finished!");
                            close();
                        }
                    } else if (in.equals("no") || in.equals("n")) {
                        String c = deps.get(currDep - 1);
                        if (!deps.remove(c)) {
                            printConsole("[???] "+c);
                        }
                        currDep--;
                        if (!askDep()) {                            
                            state = State.INSTALLING;
                            if (!install()) {
                                return;
                            }
                            printConsole("Finished!");
                            close();
                        }
                    }
                }
                break;
            case FAILED:
                if (in.equals("reconnect")) {
                    state = State.ENTER_HOST;
                    printConsole("Enter host-IP:");
                }
                break;
        }
    }

    private boolean install() {
        try {
            sendToDevice("rm *.deb");
            ssh.getExitStatus();
            for (String c : deps) {
                c = c + ".deb";
                scp.put("downloads/" + c, "/var/root/Media");                
                ssh.getExitStatus();
            }
            sendToDevice("dpkg -i *.deb");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            printConsole("Error! Try \"reconnect\"");
            state = State.FAILED;
            return false;
        }
    }

    private boolean askDep() {
        if (currDep >= deps.size()) {
            return false;
        }
        String p = deps.get(currDep);
        printConsole("Do you want to install " + p + "? (yes,no)");
        needInput = true;
        currDep++;
        return true;
    }

    private void sendToDevice(String msg) throws IOException {
        if (ssh != null) {
            OutputStream out = ssh.getStdin();
            msg = msg + "\n";
            out.write(msg.getBytes("UTF-8"));
            out.flush();
        }
    }

    private void connectWithDevice() throws IOException {
        conn = new Connection(host);
        conn.connect();
        conn.authenticateWithPassword(user, pass);
        ssh = conn.openSession();
        scp = conn.createSCPClient();
        ssh.requestDumbPTY();
        ssh.startShell();
        reader = new Running() {

            @Override
            public void run() {
                InputStream stdout = ssh.getStdout();
                while (!state.equals(State.CLOSING) && !state.equals(State.FAILED)) {
                    try {
                        int i = stdout.read();
                        if (i == -1) {
                            break;
                        }
                        printConsole(i);
                    } catch (Exception e) {
                        e.printStackTrace();
                        printConsole("Error!");
                        break;
                    }
                }
                running = false;
            }
        };
        new Thread(reader).start();
    }

    private void close() {
        if (ssh != null) {
            ssh.waitForCondition(ChannelCondition.EOF, 5000);
            ssh.close();
        }
        if (conn != null) {
            conn.close();
        }
        state = State.CLOSING;
        if (reader != null) {
            while (reader.running) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                }
            }
        }
        printConsole("Connections closed!");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jTextField1 = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jButton1.setText("Close");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));

        jTextArea1.setBackground(new java.awt.Color(0, 0, 0));
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setForeground(new java.awt.Color(255, 255, 255));
        jTextArea1.setRows(5);
        jTextArea1.setBorder(null);
        jScrollPane1.setViewportView(jTextArea1);

        jTextField1.setBackground(new java.awt.Color(0, 0, 0));
        jTextField1.setForeground(new java.awt.Color(255, 255, 255));
        jTextField1.setBorder(null);
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE)
            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(382, 382, 382)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(411, Short.MAX_VALUE))
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        parseInput(evt.getActionCommand());
        jTextField1.setText("");
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        close();
    }//GEN-LAST:event_formWindowClosing
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables

    private enum State {

        ENTER_HOST,
        ENTER_USER,
        CONNECTING,
        ASKING,
        INSTALLING,
        FAILED,
        FINISHED,
        CLOSING;
    }
}
