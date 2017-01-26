package XmlMonitor.Publisher.Interfaces;

public interface IListener {
    void registerOnPublisher();
    String[] listenerInterests();
    void listenerHandler(IPublisherEvent publisherEvent);
}
