/* BEGIN COPYRIGHT BLOCK
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Copyright (C) 2017 Red Hat, Inc.
 * All rights reserved.
 * END COPYRIGHT BLOCK */

package org.apache.tomcat.util.net.jss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.mozilla.jss.CryptoManager;
import org.mozilla.jss.crypto.AlreadyInitializedException;
import org.mozilla.jss.crypto.CryptoToken;
import org.mozilla.jss.ssl.SSLAlertEvent;
import org.mozilla.jss.ssl.SSLHandshakeCompletedEvent;
import org.mozilla.jss.ssl.SSLSocketListener;

public class TomcatJSS implements SSLSocketListener {

    final static Logger logger = Logger.getLogger(TomcatJSS.class.getName());

    public final static TomcatJSS INSTANCE = new TomcatJSS();

    public static TomcatJSS getInstance() { return INSTANCE; }

    String certdbDir;
    String passwordClass;
    String passwordFile;
    String serverCertNickFile;

    IPasswordStore passwordStore;

    Collection<SSLSocketListener> socketListeners = new ArrayList<SSLSocketListener>();

    public String getCertdbDir() {
        return certdbDir;
    }

    public void setCertdbDir(String certdbDir) {
        this.certdbDir = certdbDir;
    }

    public String getPasswordClass() {
        return passwordClass;
    }

    public void setPasswordClass(String passwordClass) {
        this.passwordClass = passwordClass;
    }

    public String getPasswordFile() {
        return passwordFile;
    }

    public void setPasswordFile(String passwordFile) {
        this.passwordFile = passwordFile;
    }

    public String getServerCertNickFile() {
        return serverCertNickFile;
    }

    public void setServerCertNickFile(String serverCertNickFile) {
        this.serverCertNickFile = serverCertNickFile;
    }

    public IPasswordStore getPasswordStore() {
        return passwordStore;
    }

    public void setPasswordStore(IPasswordStore passwordStore) {
        this.passwordStore = passwordStore;
    }

    public void init() throws Exception {

        logger.info("TomcatJSS: initialization");

        logger.fine("certdbDir: " + certdbDir);
        logger.fine("passwordClass: " + passwordClass);
        logger.fine("passwordFile: " + passwordFile);

        if (certdbDir == null) {
            throw new Exception("Missing certdbDir");
        }

        if (passwordClass == null) {
            throw new Exception("Missing passwordClass");
        }

        CryptoManager.InitializationValues vals = new CryptoManager.InitializationValues(
                certdbDir, "", "", "secmod.db");

        vals.removeSunProvider = false;
        vals.installJSSProvider = true;

        try {
            CryptoManager.initialize(vals);

        } catch (AlreadyInitializedException e) {
            logger.warning("TomcatJSS: " + e);
        }

        passwordStore = (IPasswordStore) Class.forName(passwordClass).newInstance();
        passwordStore.init(passwordFile);
    }

    public CryptoToken getToken(String tag) throws Exception {

        CryptoManager manager = CryptoManager.getInstance();

        if (tag.equals("internal")) {
            return manager.getInternalKeyStorageToken();
        }

        if (tag.startsWith("hardware-")) {
            String tokenName = tag.substring(9);
            return manager.getTokenByName(tokenName);
        }

        // non-token password entry
        return null;
    }

    public void addSocketListener(SSLSocketListener listener) {
        socketListeners.add(listener);
    }

    public void removeSocketListener(SSLSocketListener listener) {
        socketListeners.remove(listener);
    }

    public Collection<SSLSocketListener> getSocketListeners() {
        return socketListeners;
    }

    @Override
    public void alertReceived(SSLAlertEvent event) {
        for (SSLSocketListener listener : socketListeners) {
            listener.alertReceived(event);
        }
    }

    @Override
    public void alertSent(SSLAlertEvent event) {
        for (SSLSocketListener listener : socketListeners) {
            listener.alertSent(event);
        }
    }

    @Override
    public void handshakeCompleted(SSLHandshakeCompletedEvent event) {
        for (SSLSocketListener listener : socketListeners) {
            listener.handshakeCompleted(event);
        }
    }
}
