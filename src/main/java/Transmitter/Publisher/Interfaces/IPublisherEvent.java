package Transmitter.Publisher.Interfaces;

public interface IPublisherEvent {

    String getInterestName();
    void setInterestName(String interestName);

    Object getBody();
    void setBody(Object body);

    Object[] getArgs();
    void setArgs(Object ...args);
    int getNumArgs();

    String getType();
    void setType(String type);

    String getGroupName();
    void setGroupName(String groupName);

    String getServerCommand();
    void setServerCommand(String serverCommand);

    String getPrivateSubscriberName();
    void setPrivateSubscriberName(String privateSubscriberName);

}
