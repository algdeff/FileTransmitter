package FileTransmitter.Publisher;

import FileTransmitter.Publisher.Interfaces.ISubscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubscriberContext {

    private ISubscriber _subscriberInstance;
    private String _subscriberGroupName, _subscriberPrivateName;
    private List<String> _subscriberInterests;

    public SubscriberContext(ISubscriber subscriberInstance) {
        this(subscriberInstance, null);
    }
    public SubscriberContext(ISubscriber subscriberInstance, String subscriberGroupName) {
        this(subscriberInstance, subscriberGroupName, null);
    }
    public SubscriberContext(ISubscriber subscriberInstance, String subscriberGroupName, String subscriberPrivateName) {
        _subscriberInstance = subscriberInstance;
        _subscriberGroupName = subscriberGroupName;
        _subscriberPrivateName = subscriberPrivateName != null ? subscriberPrivateName : subscriberInstance.toString();

        _subscriberInterests = new ArrayList<>(Arrays.asList(subscriberInstance.subscriberInterests()));
    }

    public boolean addSubscriberInterests(String[] subscriberInterests) {
        boolean result = false;
        for (String subscriberInterest : subscriberInterests) {
            if (!_subscriberInterests.contains(subscriberInterest)) {
                _subscriberInterests.add(subscriberInterest);
                result = true;
            }
        }
        return result;
    }
    public boolean removeSubscriberInterests(String[] subscriberInterests) {
        boolean result = false;
        for (String subscriberInterest : subscriberInterests) {
            if (_subscriberInterests.contains(subscriberInterest)) {
                _subscriberInterests.remove(subscriberInterest);
                result = true;
            }
        }
        return result;
    }

//    public void setSubscriberInstance(ISubscriber subscriberInstance) {
//        _subscriberInstance = subscriberInstance;
//    }
    public ISubscriber getSubscriberInstance() {
        return _subscriberInstance;
    }

    public void setSubscriberGroupName(String subscriberGroupName) {
        _subscriberGroupName = subscriberGroupName;
    }
    public String getSubscriberGroupName() {
        return _subscriberGroupName;
    }

    public void setSubscriberInterests(ArrayList<String> subscriberInterests) {
        _subscriberInterests.clear();
        _subscriberInterests.addAll(subscriberInterests);
    }
    public ArrayList<String> getSubscriberInterests() {
        return new ArrayList<>(_subscriberInterests);
    }

    public void setSubscriberPrivateName(String className) {
        _subscriberPrivateName = className;
    }
    public String getSubscriberPrivateName() {
        return _subscriberPrivateName;
    }

}
