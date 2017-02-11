package Transmitter.Publisher;

import static Transmitter.Facade.*;
import Transmitter.Publisher.Interfaces.IPublisherEvent;

import java.io.Serializable;

public class PublisherEvent implements IPublisherEvent, Serializable {

    private String _interestName, _type, _groupName, _serverCommand, _privateSubscriberName;
    private Object _body;
    private Object[] _args;

    private static final Object[] NULL_ARRAY = new Object[0];


    public PublisherEvent() {
        this(null);
    }
    public PublisherEvent(String interestName) {
        this(interestName, null);
    }
    public PublisherEvent(String interestName, Object body) {
        this(interestName, body, NULL_ARRAY);
    }
    public PublisherEvent(String interestName, Object body, Object ...args) {
        _interestName = interestName;
        _body = body;
        _args = (args == null) ? NULL_ARRAY : args;
        _type = interestName != null ? EVENT_TYPE_SUBSCRIBE : EVENT_TYPE_GROUP;

        _groupName = "";
        _serverCommand = "";
        _privateSubscriberName = "";
    }

    public String getInterestName() {
        return _interestName;
    }
    public void setInterestName(String interestName) {
        _interestName = interestName;
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

    public String getGroupName() {
        return _groupName;
    }
    public void setGroupName(String groupName) {
        _groupName = groupName;
    }

    public String getServerCommand() {
        return _serverCommand;
    }
    public void setServerCommand(String serverCommand) {
        _serverCommand = serverCommand;
    }

    public String getPrivateSubscriberName() {
        return _privateSubscriberName;
    }
    public void setPrivateSubscriberName(String privateSubscriberName) {
        _privateSubscriberName = privateSubscriberName;
    }

    public PublisherEvent toSubscribeEvent(String interestName) {
        setInterestName(interestName);
        setType(EVENT_TYPE_SUBSCRIBE);
        return this;
    }

    public PublisherEvent toPrivateEvent(String privateSubscriberName) {
        setType(EVENT_TYPE_PRIVATE);
        setPrivateSubscriberName(privateSubscriberName);
        return this;

    }

    public PublisherEvent toGroupEvent(String groupName) {
        setType(EVENT_TYPE_PRIVATE);
        setGroupName(groupName);
        return this;
    }


    public PublisherEvent toGenericEvent() {
        setServerCommand(null);
        return this;
    }

    public PublisherEvent toServerCommand() {
        setServerCommand(getInterestName());
        return this;
    }

    public PublisherEvent addServerCommand(String serverCommand) {
        setServerCommand(serverCommand);
        return this;
    }
}
