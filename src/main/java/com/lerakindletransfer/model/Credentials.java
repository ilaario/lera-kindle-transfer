package com.lerakindletransfer.model;

public final class Credentials {
    private final String password;
    private final String privateKeyPassphrase;

    public Credentials(String password, String privateKeyPassphrase) {
        this.password = password == null ? "" : password;
        this.privateKeyPassphrase = privateKeyPassphrase == null ? "" : privateKeyPassphrase;
    }

    public String password() {
        return password;
    }

    public String privateKeyPassphrase() {
        return privateKeyPassphrase;
    }
}
