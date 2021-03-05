package zone.greggle.gregbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.command.DMMessageListener;
import zone.greggle.gregbot.command.GuildCommandListener;
import zone.greggle.gregbot.command.GuildReactionListener;
import zone.greggle.gregbot.mission.MissionManagerUtil;
import zone.greggle.gregbot.scheduling.AlertScheduler;
import zone.greggle.gregbot.scheduling.DeleteScheduler;

@Component
public class GregBot {

    private static final Logger logger = LoggerFactory.getLogger(GregBot.class);

    @Value("${jda.discord.token}")
    private String SECRET_TOKEN;

    @Autowired
    JDAContainer jdaContainer;

    @Autowired
    GuildCommandListener guildCommandListener;

    @Autowired
    GuildReactionListener guildReactionListener;

    @Autowired
    DMMessageListener dmMessageListener;

    @Autowired
    AlertScheduler alertScheduler;

    @Autowired
    DeleteScheduler deleteScheduler;

    @Autowired
    MissionManagerUtil missionManagerUtil;

    public void startBot() {
        try {
            JDA jda = JDABuilder.createLight(SECRET_TOKEN)
                    .addEventListeners(guildCommandListener)
                    .addEventListeners(guildReactionListener)
                    .addEventListeners(dmMessageListener)
                    .build()
                    .awaitReady();
            jdaContainer.setJDA(jda);
            logger.info("Connected to discord as " + jdaContainer.getJDA().getSelfUser().getName());
            missionManagerUtil.updateManagerMessage(jdaContainer.getGuild());
            alertScheduler.registerStoredAlerts();
            deleteScheduler.registerExistingDeletes();
        } catch (Exception e) {
            logger.error("Failed to connect to Discord: " + e.getMessage());
        }
    }


}
