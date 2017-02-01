package FileTransmitter.Publisher;

import FileTransmitter.Publisher.Interfaces.IListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListenerContext {

    private IListener _listener;
    private String _listenerGroupName, _listenerRegName;
    private List<String> _listenerInterests;

    public ListenerContext(IListener listener) {
        this(listener, null);
    }
    public ListenerContext(IListener listener, String listenerGroupName) {
        this(listener, listenerGroupName, null);
    }
    public ListenerContext(IListener listener, String listenerGroupName, String listenerRegName) {
        _listener = listener;
        _listenerGroupName = listenerGroupName;
        _listenerRegName = listenerRegName != null ? listenerRegName : listener.toString();
        _listenerInterests = new ArrayList<>(Arrays.asList(listener.listenerInterests()));
    }

//    public void sendPublisherEventToListener(PublisherEvent publisherEvent) {
//        publisherEvent.setGroupName(_groupName);
//        publisherEvent.setListenerRegName(_listenerRegName);
//        getListener().listenerHandler(publisherEvent);
//
//    }

    public boolean addListenerInterests(String[] listenerInterests) {
        boolean result = false;
        for (String listenerInterest : listenerInterests) {
            if (!_listenerInterests.contains(listenerInterest)) {
                _listenerInterests.add(listenerInterest);
                result = true;
            }
        }
        return result;
    }
    public boolean removeListenerInterests(String[] listenerInterests) {
        boolean result = false;
        for (String listenerInterest : listenerInterests) {
            if (_listenerInterests.contains(listenerInterest)) {
                _listenerInterests.remove(listenerInterest);
                result = true;
            }
        }
        return result;
    }

//    public void setListener(IListener listener) {
//        _listener = listener;
//    }
    public IListener getListener() {
        return _listener;
    }

    public void setListenerGroupName(String listenerGroupName) {
        _listenerGroupName = listenerGroupName;
    }
    public String getListenerGroupName() {
        return _listenerGroupName;
    }

    public void setListenerInterests(ArrayList<String> listenerInterests) {
        _listenerInterests.clear();
        _listenerInterests.addAll(listenerInterests);
    }
    public ArrayList<String> getListenerInterests() {
        return new ArrayList<>(_listenerInterests);
    }

    public void setListenerRegName(String className) {
        _listenerRegName = className;
    }
    public String getListenerRegName() {
        return _listenerRegName;
    }

}
