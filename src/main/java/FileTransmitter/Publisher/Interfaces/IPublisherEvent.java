package FileTransmitter.Publisher.Interfaces;

public interface IPublisherEvent {
    public String getName();
    public void setName(String name);

    public Object getBody();
    public void setBody(Object body);

    public Object[] getArgs();
    public void setArgs(Object ...args);
    public int numArgs();

    public String getType();
    public void setType(String type);
    public String getGroupName();
    public void setGroupName(String groupName);
    public String getClassName();
    public void setClassName(String className);

}
