package FileTransmitter.Publisher;

import FileTransmitter.Publisher.Interfaces.IListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListenerContext {

    private IListener _listener;
    private String _groupName, _className;
    private List<String> _listenerInterests;

    public ListenerContext(IListener listener) {
        this(listener, null);
    }
    public ListenerContext(IListener listener, String group_name) {
        _listener = listener;
        _groupName = group_name;
        _listenerInterests = new ArrayList<String>(Arrays.asList(listener.listenerInterests()));
        _className = listener.toString();
    }

    public void setListener(IListener listener) {
        _listener = listener;
    }
    public IListener getListener() {
        return _listener;
    }

    public void setGroupName(String groupName) {
        _groupName = groupName;
    }
    public String getGroupName() {
        return _groupName;
    }

    public void setListenerInterests(ArrayList<String> listenerInterests) {
        _listenerInterests = listenerInterests;
    }
    public ArrayList<String> getListenerInterests() {
        return (ArrayList<String>) _listenerInterests;
    }
    public Boolean addListenerInterests(String[] listenerInterests) {
        Boolean result = false;
        for (String listenerInterest : listenerInterests) {
            if (!_listenerInterests.contains(listenerInterest)) {
                _listenerInterests.add(listenerInterest);
                result = true;
            }
        }
        return result;
    }
    public Boolean removeListenerInterests(String[] listenerInterests) {
        Boolean result = false;
        for (String listenerInterest : listenerInterests) {
            if (_listenerInterests.contains(listenerInterest)) {
                _listenerInterests.remove(listenerInterest);
                result = true;
            }
        }
        return result;
    }

    public void setClassName(String className) {
        _className = className;
    }
    public String getClassName() {
        return _className;
    }


}
