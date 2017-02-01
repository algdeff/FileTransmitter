package FileTransmitter.Publisher.Interfaces;

public interface IPublisherEvent {

    public String getName();
    public void setName(String name);

    public Object getBody();
    public void setBody(Object body);

    public Object[] getArgs();
    public void setArgs(Object ...args);
    public int getNumArgs();

    public String getType();
    public void setType(String type);

    public String getServerCommand();
    public void setServerCommand(String serverCommand);

    public String getListenerRegName();
    public void setListenerRegName(String listenerRegName);

}
