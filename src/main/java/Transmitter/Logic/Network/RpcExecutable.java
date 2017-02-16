package Transmitter.Logic.Network;

import java.io.Serializable;

import java.util.concurrent.TimeUnit;

public class RpcExecutable implements Runnable, Serializable {

    private String _clientID;

    @Override
    public void run() {
        remoteProcedure();
    }

    public RpcExecutable(String client_ID) {
        _clientID = client_ID;
    }

    private void remoteProcedure() {
        System.out.println("Remote procedure from client: " + _clientID);
        try {
            for (int i = 0; i < 20; i++) {
                System.out.println(i);
                TimeUnit.SECONDS.sleep(1);
            }

        } catch (InterruptedException e) {
            System.out.println("INTERRUPTED");
        }

        System.out.println("Remote procedure end");

    }

}
