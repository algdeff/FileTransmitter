package Transmitter.Publisher;

import Transmitter.Facade;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Publisher {

    /**
     *                          _registeredSubscribers
     *          Key = registered subscriber Name - default, Class name from object.toString,
     *          Values = subscriber object included subscriber class link
     *
     *                      _subscribersEventMap
     *          Key = eventName,
     *          Value = List of subscribers names;
     */
    private static ConcurrentMap<String, SubscriberContext> _registeredSubscribers;
    private static ConcurrentMap<String, List<String>> _subscribersEventMap;
    private static ConcurrentMap<String, SubscriberContext> _registeredRemoteUsers;
    private static volatile Publisher instance;

//    private static BlockingQueue<PublisherEvent> _transitionEventsQueue;

    private Publisher() {
        _registeredSubscribers = new ConcurrentHashMap<>();
        _subscribersEventMap = new ConcurrentHashMap<>();
        _registeredRemoteUsers = new ConcurrentHashMap<>();
//        _transitionEventsQueue = new LinkedBlockingQueue<>();

    }

    public static Publisher getInstance() {
        if (instance == null) {
            synchronized (Publisher.class) {
                if (instance == null) {
                    instance = new Publisher();
                }
            }
        }
        return instance;
    }


    /**
     *  register new Subscriber (Subscriber)
     */

    public boolean registerNewSubscriber(ISubscriber subscriber) {
        return registerNewSubscriber(subscriber, null);
    }
    public boolean registerNewSubscriber(ISubscriber subscriber, String subscriberGroupName) {
        return registerNewSubscriber(subscriber, subscriberGroupName, null);
    }
    public boolean registerNewSubscriber(ISubscriber subscriber, String subscriberGroupName, String subscriberRegName) {
        String subscriberName = subscriberRegName != null ? subscriberRegName : subscriber.toString();
        if (_registeredSubscribers.containsKey(subscriber.toString()) || _registeredSubscribers.containsKey(subscriberName)) {
            return false;
        }

        SubscriberContext context = new SubscriberContext(subscriber, subscriberGroupName, subscriberName);
        _registeredSubscribers.put(subscriberName, context);
        updateSubscribersEventMap();

        return true;
    }


    public boolean registerRemoteUser(ISubscriber remoteUserHandler, String User_ID) {
        return registerRemoteUser(remoteUserHandler, User_ID, null);
    }
    public boolean registerRemoteUser(ISubscriber remoteUserHandler, String User_ID, String userGroup) {
        if (_registeredRemoteUsers.containsKey(User_ID)) {
            System.err.println("User (" + User_ID + ") already registered!");
            return false;
        }

        SubscriberContext context = new SubscriberContext(remoteUserHandler, userGroup);
        context.setSubscriberPrivateName(User_ID);
        _registeredRemoteUsers.put(User_ID, context);
//        System.err.println("User (" + User_ID + ") registered!");
        return true;
    }

    public void unregisterRemoteUser(String User_ID) {
        if (!_registeredRemoteUsers.containsKey(User_ID)) {
            System.err.println("User (" + User_ID + ") is not registered!");
            return;
        }
        _registeredRemoteUsers.remove(User_ID);
//        System.err.println("[Publisher] User (" + User_ID + ") unregistered!");
    }

    public boolean removeListaner(ISubscriber subscriber) {
        boolean success = false;
        for (String subscriberName : _registeredSubscribers.keySet()) {
            if (_registeredSubscribers.get(subscriberName).getSubscriberInstance().equals(subscriber)) {
                _registeredSubscribers.remove(subscriberName);
                success = true;
            }
        }
        updateSubscribersEventMap();

        return success;
    }
    public boolean removeSubscriber(String subscriberRegName) {
        if (!_registeredSubscribers.containsKey(subscriberRegName)) {
            return false;
        }

        _registeredSubscribers.remove(subscriberRegName);
        updateSubscribersEventMap();

        return true;
    }


    public void addEventsToSubscriber(String subscriberRegName, String[] newInterests) {
        _registeredSubscribers.get(subscriberRegName).addSubscriberInterests(newInterests);
        updateSubscribersEventMap();
    }
    public void removeEventsFromSubscriber(String subscriberRegName, String[] interests) {
        _registeredSubscribers.get(subscriberRegName).removeSubscriberInterests(interests);
        updateSubscribersEventMap();
    }

    public void addEventsToSubscribersGroup(String groupName, String[] newInterests) {
        for (SubscriberContext context : _registeredSubscribers.values()) {
            if (context.getSubscriberGroupName().equals(groupName)) {
                context.addSubscriberInterests(newInterests);
            }
        }
        updateSubscribersEventMap();
    }
    public void removeEventsFromSubscribersGroup(String groupName, String[] interests) {
        for (SubscriberContext context : _registeredSubscribers.values()) {
            if (context.getSubscriberGroupName().equals(groupName)) {
                context.removeSubscriberInterests(interests);
            }
        }
        updateSubscribersEventMap();
    }

    private void updateSubscribersEventMap() {
//        if (true) return;
//        _subscribersEventMap.clear();
//        for (String subscriber : _registeredSubscribers.keySet()) {
//            for (String eventName : _registeredSubscribers.get(subscriber).getSubscriberInterests()) {
//                List<String> subscribers = _subscribersEventMap.get(eventName);
//                if (subscribers == null) {
//                    subscribers = new ArrayList<>();
//                    _subscribersEventMap.put(eventName, subscribers);
//                }
//                subscribers.add(subscriber);
//            }
//        }
    }
    public void updateSubscribersEventMap(ArrayList<String> events) {
        System.out.println(_subscribersEventMap.size());

        for (String eventName : events) {
            List<String> subscribers = _subscribersEventMap.get(eventName);
            for (String subscriber : subscribers) {
                if (!_registeredSubscribers.containsKey(subscriber)) {
                    subscribers.remove(subscriber);
                }
            }
            if (subscribers.isEmpty()) {
                _subscribersEventMap.remove(eventName);
            } else {
                _subscribersEventMap.replace(eventName, subscribers);
            }
        }

        System.out.println(_subscribersEventMap.size());

    }

    public boolean subscriberIsRegistered(ISubscriber subscriber) {
        boolean success = false;
        for (String subscriberName : _registeredSubscribers.keySet()) {
            if (_registeredSubscribers.get(subscriberName).getSubscriberInstance().equals(subscriber)) {
                _registeredSubscribers.remove(subscriberName);
                success = true;
            }
        }
        return success;
    }
    public boolean isSubscriberRegistered(String privateName) {
        return _registeredSubscribers.containsKey(privateName);
    }
    public boolean isUserRegistered(String User_ID) {
        return _registeredRemoteUsers.containsKey(User_ID);
    }
    public boolean isEventRegistered(String eventName) {
        return _subscribersEventMap.containsKey(eventName);
    }


    public void sendTransitionEvent(IPublisherEvent publisherEvent) {
        sendTransitionEvent(publisherEvent, null, null);
    }
    public void sendTransitionEvent(IPublisherEvent publisherEvent, String targetClientID) {
        sendTransitionEvent(publisherEvent, targetClientID, null);
    }
    public void sendTransitionEvent(IPublisherEvent publisherEvent, String targetClientID, String groupName) {
        publisherEvent.setServerCommand(Facade.SERVER_TRANSITION_EVENT);
        if (groupName != null) {
            for (SubscriberContext remoteClientsContext : _registeredRemoteUsers.values()) {
                if (remoteClientsContext.getSubscriberGroupName().equals(groupName)) {
                    System.out.println("SendTransitionEventToClientsGroup: " + groupName);
                    remoteClientsContext.getSubscriberInstance().listenerHandler(publisherEvent);
                }
            }
            return;
        }
        if (targetClientID != null) {
            for (SubscriberContext remoteClientsContext : _registeredRemoteUsers.values()) {
                if (remoteClientsContext.getSubscriberPrivateName().equals(targetClientID)) {
                    System.out.println("Send Transition Event to private client: " + remoteClientsContext.getSubscriberPrivateName());
                    remoteClientsContext.getSubscriberInstance().listenerHandler(publisherEvent);
                }
            }
            return;
        }
        for (SubscriberContext subscriberContext : _registeredSubscribers.values()) {
            if (subscriberContext.getSubscriberGroupName().equals(Facade.TRANSITION_EVENT_GROUP_CLIENT)) {
                System.out.println("Send Transition Event to server");
                subscriberContext.getSubscriberInstance().listenerHandler(publisherEvent);
            }
        }

    }

    private Object blankObject() {
        return new Object();
    }

    public void sendPublisherEvent(String event_name) {
        sendPublisherEvent(new PublisherEvent(event_name, blankObject()));
    }
    public void sendPublisherEvent(String event_name, Object body) {
        sendPublisherEvent(new PublisherEvent(event_name, body, null));
    }
    public void sendPublisherEvent(String event_name, Object body, Object ...args) {
        sendPublisherEvent(new PublisherEvent(event_name, body, args));
    }
    public void sendPublisherEvent(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType() == null) {
            System.err.println("Event type not found!");
            return;
        }

//        System.out.println("SPE"+publisherEvent.getInterestName()+publisherEvent.getType()+publisherEvent.getBody().toString());

        switch (publisherEvent.getType()) {
            case Facade.EVENT_TYPE_SUBSCRIBE: {
                sendEventToSubscribers(publisherEvent);
                return;
            }
            case Facade.EVENT_TYPE_GROUP: {
                sendEventToGroup(publisherEvent);
                return;
            }
            case Facade.EVENT_TYPE_PRIVATE: {
                sendEventToPrivateSubscriberName(publisherEvent);
                return;
            }
            case Facade.EVENT_TYPE_BROADCAST: {
                sendBroadcastEvent(publisherEvent);
                return;
            }

        }
        System.err.println("Incorrect event type: " + publisherEvent.getType());

    }

    private void sendEventToSubscribers(IPublisherEvent publisherEvent) {
        for (String subscriberRegName : _registeredSubscribers.keySet()) {
            SubscriberContext subscriberContext = _registeredSubscribers.get(subscriberRegName);
            if (subscriberContext.getSubscriberInterests().contains(publisherEvent.getInterestName())) {
//                System.out.println("SendEventToSubscribers: " + subscriberRegName + ":" + subscriberContext.getSubscriberPrivateName());
                subscriberContext.getSubscriberInstance().listenerHandler(publisherEvent);
            }
        }
    }


    public void sendEventToPrivateSubscriberName(String subscriberRegName, Object body) {
        sendEventToPrivateSubscriberName(new PublisherEvent(subscriberRegName, body));
    }
    private void sendEventToPrivateSubscriberName(IPublisherEvent publisherEvent) {
        publisherEvent.setType(Facade.EVENT_TYPE_PRIVATE);
        for (SubscriberContext subscriberContext : _registeredSubscribers.values()) {
            if (subscriberContext.getSubscriberPrivateName().equals(publisherEvent.getPrivateSubscriberName())) {
                System.out.println("sendEventToSpecificSubscriberName: " + subscriberContext.getSubscriberPrivateName());
                subscriberContext.getSubscriberInstance().listenerHandler(publisherEvent);
            }
        }

    }

    public void sendEventToGroup(String targetGroup, Object body) {
        sendEventToGroup(new PublisherEvent(targetGroup, body));
    }
    public void sendEventToGroup(String targetGroup, Object body, Object ...args) {
        sendEventToGroup(new PublisherEvent(targetGroup, body, args));
    }
    private void sendEventToGroup(IPublisherEvent publisherEvent) {
        publisherEvent.setType(Facade.EVENT_TYPE_GROUP);
        for (SubscriberContext subscriberContext : _registeredSubscribers.values()) {
            if (subscriberContext.getSubscriberGroupName().equals(publisherEvent.getGroupName())) {
                System.out.println("sendEventToGroup: " + subscriberContext.getSubscriberPrivateName());
                subscriberContext.getSubscriberInstance().listenerHandler(publisherEvent);
            }
        }
    }

    private void sendBroadcastEvent(IPublisherEvent publisherEvent) {
        publisherEvent.setType(Facade.EVENT_TYPE_BROADCAST);
        for (SubscriberContext subscriberContext : _registeredSubscribers.values()) {
            System.out.println("sendBroadcastEvent: " + subscriberContext.getSubscriberPrivateName());
            subscriberContext.getSubscriberInstance().listenerHandler(publisherEvent);
        }
    }

}
