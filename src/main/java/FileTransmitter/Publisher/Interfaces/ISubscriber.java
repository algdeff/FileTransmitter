package FileTransmitter.Publisher.Interfaces;

public interface ISubscriber {
    void registerOnPublisher();
    String[] subscriberInterests();
    void listenerHandler(IPublisherEvent publisherEvent);
}
