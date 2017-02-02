package FileTransmitter.Publisher;

import FileTransmitter.Facade;
import FileTransmitter.Publisher.Interfaces.IListener;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class Publisher {

    /**
     *                          _registeredListeners
     *          Key = registered listener Name - default, Class name from object.toString,
     *          Values = listener object included listener class link
     *
     *                      _listenersEventMap
     *          Key = eventName,
     *          Value = List of listeners names;
     */
    private static ConcurrentMap<String, ListenerContext> _registeredListeners;
    private static ConcurrentMap<String, List<String>> _listenersEventMap;
    private static volatile Publisher instance;

    private static BlockingQueue<PublisherEvent> _transitionEventsQueue;

    private Publisher() {
        _registeredListeners = new ConcurrentHashMap<>();
        _listenersEventMap = new ConcurrentHashMap<>();
        _transitionEventsQueue = new LinkedBlockingQueue<>();

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
     *  register new Listener (Subscriber)
     */

    public boolean registerNewListener(IListener listener) {
        return registerNewListener(listener, null);
    }
    public boolean registerNewListener(IListener listener, String listenerGroupName) {
        return registerNewListener(listener, listenerGroupName, null);
    }
    public boolean registerNewListener(IListener listener, String listenerGroupName, String listenerRegName) {
        String listenerName = listenerRegName != null ? listenerRegName : listener.toString();
        if (_registeredListeners.containsKey(listener.toString()) || _registeredListeners.containsKey(listenerName)) {
            return false;
        }

        ListenerContext context = new ListenerContext(listener, listenerGroupName, listenerName);
        _registeredListeners.put(listenerName, context);
        updateListenersEventMap();

        return true;
    }

    public boolean removeListaner(IListener listener) {
        boolean success = false;
        for (String listenerName : _registeredListeners.keySet()) {
            if (_registeredListeners.get(listenerName).getListener().equals(listener)) {
                _registeredListeners.remove(listenerName);
                success = true;
            }
        }
        updateListenersEventMap();

        return success;
    }
    public boolean removeListener(String listenerRegName) {
        if (!_registeredListeners.containsKey(listenerRegName)) {
            return false;
        }

        _registeredListeners.remove(listenerRegName);
        updateListenersEventMap();

        return true;
    }


    public void addEventsToListener(String listenerRegName, String[] newInterests) {
        _registeredListeners.get(listenerRegName).addListenerInterests(newInterests);
        updateListenersEventMap();
    }
    public void removeEventsFromListener(String listenerRegName, String[] interests) {
        _registeredListeners.get(listenerRegName).removeListenerInterests(interests);
        updateListenersEventMap();
    }

    public void addEventsToListenersGroup(String groupName, String[] newInterests) {
        for (ListenerContext context : _registeredListeners.values()) {
            if (context.getListenerGroupName().equals(groupName)) {
                context.addListenerInterests(newInterests);
            }
        }
        updateListenersEventMap();
    }
    public void removeEventsFromListenersGroup(String groupName, String[] interests) {
        for (ListenerContext context : _registeredListeners.values()) {
            if (context.getListenerGroupName().equals(groupName)) {
                context.removeListenerInterests(interests);
            }
        }
        updateListenersEventMap();
    }

    private void updateListenersEventMap() {
//        if (true) return;
//        _listenersEventMap.clear();
//        for (String listener : _registeredListeners.keySet()) {
//            for (String eventName : _registeredListeners.get(listener).getListenerInterests()) {
//                List<String> listeners = _listenersEventMap.get(eventName);
//                if (listeners == null) {
//                    listeners = new ArrayList<>();
//                    _listenersEventMap.put(eventName, listeners);
//                }
//                listeners.add(listener);
//            }
//        }
    }
    public void updateListenersEventMap(ArrayList<String> events) {
        System.out.println(_listenersEventMap.size());

        for (String eventName : events) {
            List<String> listeners = _listenersEventMap.get(eventName);
            for (String listener : listeners) {
                if (!_registeredListeners.containsKey(listener)) {
                    listeners.remove(listener);
                }
            }
            if (listeners.isEmpty()) {
                _listenersEventMap.remove(eventName);
            } else {
                _listenersEventMap.replace(eventName, listeners);
            }
        }

        System.out.println(_listenersEventMap.size());

    }

    public boolean listenerIsRegistered(IListener listener) {
        boolean success = false;
        for (String listenerName : _registeredListeners.keySet()) {
            if (_registeredListeners.get(listenerName).getListener().equals(listener)) {
                _registeredListeners.remove(listenerName);
                success = true;
            }
        }
        return success;
    }
    public boolean listenerIsRegistered(String uniqueName) {
        return _registeredListeners.containsKey(uniqueName);
    }

    public boolean eventIsRegistered(String eventName) {
        return _listenersEventMap.containsKey(eventName);
//        for (ListenerContext context : _registeredListeners.values()) {
//            for (String interestName : context.getListenerInterests()) {
//                if (interestName.equals(eventName)) {
//                    return true;
//                }
//            }
//        }
//        return false;
    }

    public void sendTransitionEvent(String eventName) {
        PublisherEvent transitionEvent = new PublisherEvent(eventName, null);
        sendTransitionEvent(transitionEvent);
    }
    public void sendTransitionEvent(String eventName, Object body) {
        PublisherEvent transitionEvent = new PublisherEvent(eventName, body);
        sendTransitionEvent(transitionEvent);
    }
    public void sendTransitionEvent(PublisherEvent publisherEvent) {
        try {
            _transitionEventsQueue.put(publisherEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public PublisherEvent getTransitionEvent() {
        PublisherEvent publisherEvent = null;
        try {
            publisherEvent = _transitionEventsQueue.take();
        } catch (InterruptedException e) {
            System.err.println("PUBLISHER: Transition event interrupted!");
        }
        return publisherEvent;

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

//        System.out.println("SPE"+publisherEvent.getName()+publisherEvent.getType()+publisherEvent.getBody().toString());

        switch (publisherEvent.getType()) {
            case Facade.EVENT_TYPE_SUBSCRIBE: {
                sendEventToSubscribers(publisherEvent);
                return;
            }
            case Facade.EVENT_TYPE_GROUP: {
                sendEventToGroup(publisherEvent);
                return;
            }
            case Facade.EVENT_TYPE_SPECIFIC: {
                sendEventToSpecificListenerName(publisherEvent);
                return;
            }
            case Facade.EVENT_TYPE_BROADCAST: {
                sendBroadcastEvent(publisherEvent);
                return;
            }

        }

        System.err.println("Incorrect event type: " + publisherEvent.getType());


//        String eventName = publisherEvent.getName();
//        List<String> listeners = _listenersEventMap.get(eventName);
//
//        if (listeners == null || listeners.size() == 0) {
//            System.out.println("Publisher: this event is not registered");
//            return;
//        }
//
//        for (String listenerName : listeners) {
//            ListenerContext context = _registeredListeners.get(listenerName);
//            if (context == null) {
//                System.out.println("sendPublisherEvent ERROR");
//                continue;
//            }
//            publisherEvent.setGroupName(context.getGroupName());
//            publisherEvent.setListenerRegName(context.getListenerRegName());
//            context.getListener().listenerHandler(publisherEvent);
//        }
    }

    private void sendEventToSubscribers(IPublisherEvent publisherEvent) {
//        if (!publisherEvent.getType().equals(Facade.EVENT_TYPE_SUBSCRIBE)) {
//            System.err.println("Incorrect event type: " + publisherEvent.getType());
//            return;
//        }
//        System.out.println("sets"+_registeredListeners.size());

        for (String listenerRegName : _registeredListeners.keySet()) {
            ListenerContext listenerContext = _registeredListeners.get(listenerRegName);
            if (listenerContext.getListenerInterests().contains(publisherEvent.getName())) {
//                System.out.println("SendEventToSubscribers: " + listenerRegName + ":" + listenerContext.getListenerRegName());
                publisherEvent.setListenerRegName(listenerContext.getListenerRegName());
                listenerContext.getListener().listenerHandler(publisherEvent);
            }
        }
    }


    public void sendEventToSpecificListenerName(String listenerRegName, Object body) {
        PublisherEvent publisherEvent = new PublisherEvent(listenerRegName, body);
        publisherEvent.setType(Facade.EVENT_TYPE_SPECIFIC);
        sendEventToSpecificListenerName(publisherEvent);
    }
    private void sendEventToSpecificListenerName(IPublisherEvent publisherEvent) {
        for (ListenerContext listenerContext : _registeredListeners.values()) {
            if (listenerContext.getListenerRegName().equals(publisherEvent.getName())) {
                System.out.println("sendEventToSpecificListenerName: " + listenerContext.getListenerRegName());
                publisherEvent.setListenerRegName(listenerContext.getListenerRegName());
                listenerContext.getListener().listenerHandler(publisherEvent);
            }
        }

    }

    public void sendEventToGroup(String targetGroup, Object body) {
        PublisherEvent publisherEvent = new PublisherEvent(targetGroup, body);
        publisherEvent.setType(Facade.EVENT_TYPE_GROUP);
        sendEventToGroup(publisherEvent);
    }
    public void sendEventToGroup(String targetGroup, Object body, Object ...args) {
        PublisherEvent publisherEvent = new PublisherEvent(targetGroup, body, args);
        publisherEvent.setType(Facade.EVENT_TYPE_GROUP);
        sendEventToGroup(publisherEvent);
    }
    private void sendEventToGroup(IPublisherEvent publisherEvent) {
        for (ListenerContext listenerContext : _registeredListeners.values()) {
            if (listenerContext.getListenerGroupName().equals(publisherEvent.getName())) {
                System.out.println("sendEventToGroup: " + listenerContext.getListenerRegName());
                publisherEvent.setListenerRegName(listenerContext.getListenerRegName());
                listenerContext.getListener().listenerHandler(publisherEvent);
            }
        }
    }

    private void sendBroadcastEvent(IPublisherEvent publisherEvent) {
        for (ListenerContext listenerContext : _registeredListeners.values()) {
            System.out.println("sendBroadcastEvent: " + listenerContext.getListenerRegName());
            publisherEvent.setListenerRegName(listenerContext.getListenerRegName());
            listenerContext.getListener().listenerHandler(publisherEvent);
        }
    }

}
