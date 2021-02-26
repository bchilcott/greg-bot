package zone.greggle.gregbot.entity;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Subscriber {

    public Long discordID;

    public Subscriber(Long discordID) {
        this.discordID = discordID;
    }

    public Long getDiscordID() {
        return discordID;
    }
    public void setDiscordID(Long discordID) {
        this.discordID = discordID;
    }

}
