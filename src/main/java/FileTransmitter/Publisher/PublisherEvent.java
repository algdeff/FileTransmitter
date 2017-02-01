package FileTransmitter.Publisher;

import FileTransmitter.Facade;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;

import java.io.Serializable;

public class PublisherEvent implements IPublisherEvent, Serializable {

    private String _name, _type, _serverCommand, _listenerRegName;
    private Object _body;
    private Object[] _args;

    private static final Object[] NULL_ARRAY = new Object[0];


    public PublisherEvent() {
        this(null);
    }
    public PublisherEvent(String name) {
        this(name, null);
    }
    public PublisherEvent(String name, Object body) {
        this(name, body, NULL_ARRAY);
    }
    public PublisherEvent(String name, Object body, Object ...args) {
        _name = name;
        _body = body;
        _args = (args == null) ? NULL_ARRAY : args;
        _type = name != null ? Facade.EVENT_TYPE_SUBSCRIBE : Facade.EVENT_TYPE_GROUP;
        _serverCommand = null;
        _listenerRegName = null;
    }

    public String getName() {
        return _name;
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
    public int getNumArgs() {
        return _args.length;
    }

    public String getType() {
        return _type;
    }
    public void setType(String type) {
        _type = type;
    }

    public String getServerCommand() {
        return _serverCommand;
    }
    public void setServerCommand(String serverCommand) {
        _serverCommand = serverCommand;
    }

    public String getListenerRegName() {
        return _listenerRegName;
    }
    public void setListenerRegName(String listenerRegName) {
        _listenerRegName = listenerRegName;
    }

    public PublisherEvent toServerCommand() {
        setType(Facade.EVENT_TYPE_SERVERGROUP_CMD);
        setServerCommand(_name);
        return this;
    }

    public PublisherEvent toGenericEvent() {
        setServerCommand(null);
        return this;
    }

    public PublisherEvent addServerCommand(String serverCommand) {
        setServerCommand(serverCommand);
        return this;
    }
}
