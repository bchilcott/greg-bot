package zone.greggle.gregbot.entity;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriberRepository extends MongoRepository<Subscriber, String> {

    Subscriber findByDiscordID(Long discordID);
    void deleteByDiscordID(Long discordID);
}
