package FileTransmitter.Publisher.Interfaces;

public interface IPublisherEvent {

    public String getInterestName();
    public void setInterestName(String interestName);

    public Object getBody();
    public void setBody(Object body);

    public Object[] getArgs();
    public void setArgs(Object ...args);
    public int getNumArgs();

    public String getType();
    public void setType(String type);

    public String getGroupName();
    public void setGroupName(String groupName);

    public String getServerCommand();
    public void setServerCommand(String serverCommand);

    public String getPrivateSubscriberName();
    public void setPrivateSubscriberName(String privateSubscriberName);

}
