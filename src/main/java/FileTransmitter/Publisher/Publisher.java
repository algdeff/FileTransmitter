package XmlMonitor.Publisher;

import XmlMonitor.Publisher.Interfaces.IListener;
import XmlMonitor.Publisher.Interfaces.IPublisherEvent;

import java.util.*;

public final class Publisher {

    private Map<String, ListenerContext> _listeners; //Key = registered listener Class name from object.toString, Values = listener object included listener class link
    private Map<String, List<String>> _listenersEventMap; //Key = eventName, Value = listener Class name;
    private static volatile Publisher instance;

    public static final String LISTEN_ALL_EVENTS = "listen_all_events";

    public static final String EVENT_TYPE_BROADCAST =   "broadcast_event";
    public static final String EVENT_TYPE_GROUP =       "group_event";
    public static final String EVENT_TYPE_GENERIC =     "generic_event";

    private Publisher() {
        _listeners = new HashMap<>();
        _listenersEventMap = new HashMap<>();

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

    //register new Subscriber
    public void registerNewListener(IListener listener) {
        registerNewListener(listener, null);
    }
    public Boolean registerNewListener(IListener listener, String group_name) {
        String listenerName = listener.toString();
        if (_listeners.containsKey(listenerName)) {
            return false;
        }
        ListenerContext context = new ListenerContext(listener, group_name);
        _listeners.put(listenerName, context);
        updateListenersEventMap();

        return true;
    }

    public Boolean removeListaner(IListener listener) {
        String listenerName = listener.toString();
        if (!_listeners.containsKey(listenerName)) {
            return false;
        }

        _listeners.remove(listenerName);
        updateListenersEventMap();

        return true;
    }

    public void addEventsToListener(String listenerName, String[] newInterests) {
        _listeners.get(listenerName).addListenerInterests(newInterests);
        updateListenersEventMap();
    }
    public void removeEventsFromListener(String listenerName, String[] interests) {
        _listeners.get(listenerName).removeListenerInterests(interests);
        updateListenersEventMap();
    }

    public void addEventsToListenersGroup(String groupName, String[] newInterests) {
        for (ListenerContext context : _listeners.values()) {
            if (context.getGroupName().equals(groupName)) {
                context.addListenerInterests(newInterests);
            }
        }
        updateListenersEventMap();
    }
    public void removeEventsFromListenersGroup(String groupName, String[] interests) {
        for (ListenerContext context : _listeners.values()) {
            if (context.getGroupName().equals(groupName)) {
                context.removeListenerInterests(interests);
            }
        }
        updateListenersEventMap();
    }

    private void updateListenersEventMap() {
        _listenersEventMap.clear();
        for (String listener : _listeners.keySet()) {
            for (String eventName : _listeners.get(listener).getListenerInterests()) {
                List<String> listeners = _listenersEventMap.get(eventName);
                if (listeners == null) {
                    listeners = new ArrayList<>();
                    _listenersEventMap.put(eventName, listeners);
                }
                listeners.add(listener);
            }
        }
    }
    public void updateListenersEventMap(ArrayList<String> events) {
        System.out.println(_listenersEventMap.size());

        for (String eventName : events) {
            List<String> listeners = _listenersEventMap.get(eventName);
            for (String listener : listeners) {
                if (!_listeners.containsKey(listener)) {
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

    public Boolean isRegistered(IListener listener) {
        return _listeners.containsKey(listener.toString());
    }
    public Boolean isRegistered(String eventName) {
        return _listenersEventMap.containsKey(eventName);
//        for (ListenerContext context : _listeners.values()) {
//            for (String interestName : context.getListenerInterests()) {
//                if (interestName.equals(eventName)) {
//                    return true;
//                }
//            }
//        }
//        return false;
    }


    public void sendPublisherEvent(String event_name, Object body) {
        sendPublisherEvent(new PublisherEvent(event_name, body));
    }
    public void sendPublisherEvent(IPublisherEvent publisherEvent) {
        String eventName = publisherEvent.getName();
        List<String> listeners = _listenersEventMap.get(eventName);

        if (listeners == null) {
            System.out.println("This event is not registered");
            return;
        }

        for (String listenerName : listeners) {
            ListenerContext context = _listeners.get(listenerName);
            if (context == null) {
                System.out.println("sendPublisherEvent ERROR");
                continue;
            }
            publisherEvent.setGroupName(context.getGroupName());
            publisherEvent.setClassName(context.getClassName());
            context.getListener().listenerHandler(publisherEvent);
        }
    }

    public void sendGroupEvent(IPublisherEvent publisherEvent, String target_group) {
        for (ListenerContext context : _listeners.values()) {
            if (context.getGroupName().equals(target_group)) {
                publisherEvent.setType(EVENT_TYPE_GROUP);
                publisherEvent.setGroupName(target_group);
                publisherEvent.setClassName(context.getClassName());
                context.getListener().listenerHandler(publisherEvent);
            }
        }
    }

    public void sendBroadcastEvent(IPublisherEvent publisherEvent) {
        for (ListenerContext context : _listeners.values()) {
            publisherEvent.setType(EVENT_TYPE_BROADCAST);
            publisherEvent.setGroupName(context.getGroupName());
            publisherEvent.setClassName(context.getClassName());
            context.getListener().listenerHandler(publisherEvent);
        }
    }

}
