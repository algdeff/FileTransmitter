package FileTransmitter.Publisher;

import FileTransmitter.Facade;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;

import java.io.Serializable;

public class PublisherEvent implements IPublisherEvent, Serializable {

    private String _name, _type, _groupName, _className;
    private Object _body;
    private Object[] _args;

    private static final Object[] NULL_ARRAY = new Object[0];


    public PublisherEvent(String name, Object body, Object[] args) {
        _name = name;
        _body = body;
        _args = (args == null) ? NULL_ARRAY : args;
        _type = Facade.EVENT_TYPE_GENERIC;
        _groupName = null;
        _className = null;
    }
    public PublisherEvent(String name, Object body) {
        this(name, body, NULL_ARRAY);
    }
    public PublisherEvent(String name) {
        this(name, null, NULL_ARRAY);
    }

    public String getName() {
        return (_name != null) ? _name : "";
    }
    public void setName(String name) {
        _name = name;
    }

    public Object getBody() {
        return _body;
    }
    public void setBody(Object body) {
        _body = body;
    }

    public Object[] getArgs() {
        return _args;
    }
    public void setArgs(Object ...args) {
        _args = args;
    }
    public int numArgs() {
        return _args.length;
    }

    public String getType() {
        return _type;
    }
    public void setType(String type) {
        _type = type;
    }

    public String getGroupName() {
        return _groupName;
    }
    public void setGroupName(String groupName) {
        _groupName = groupName;
    }
    public String getClassName() {
        return _className;
    }
    public void setClassName(String className) {
        _className = className;
    }

    public PublisherEvent toServerCommand() {
        setType(Facade.EVENT_TYPE_SERVERGROUP_CMD);
        setGroupName(_name);
        return this;
    }
}
