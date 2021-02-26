package zone.greggle.gregbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JDAContainer {

    private static final Logger logger = LoggerFactory.getLogger(JDAContainer.class);

    @Value("${guild.id}")
    private long guildID;

    private JDA jda;

    public Guild getGuild() {
        Guild guild = jda.getGuildById(guildID);
        if (guild != null) {
            return guild;
        } else {
            logger.error("Failed to get Guild from JDA - Fatal Error");
            return null;
        }
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }
    public JDA getJDA() {
        return this.jda;
    }

}
