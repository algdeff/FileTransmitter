package Transmitter.Publisher.Interfaces;

public interface ISubscriber {
    void registerOnPublisher();
    String[] subscriberInterests();
    void listenerHandler(IPublisherEvent publisherEvent);
}
