package zone.greggle.gregbot.command;

import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.summary.MissionSummaryUtil;

@Component
public class DMMessageListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DMMessageListener.class);

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    MissionSummaryUtil missionSummaryUtil;

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        for (Mission mission : missionRepository.findMissionsByMemberDiscordID(event.getAuthor().getIdLong())) {
            if (mission.getMemberByID(event.getAuthor().getIdLong()).isSelectingRole()) {
                for (String roleName : mission.getAvailableRoles()) {
                    if (event.getMessage().getContentRaw().equalsIgnoreCase(roleName)) {
                        mission.updateMemberRole(event.getAuthor().getIdLong(), roleName);
                        event.getChannel().sendMessage("Successfully selected role: " + roleName).queue();
                        missionRepository.save(mission);
                        missionSummaryUtil.updateSummary(mission);
                        logger.debug(event.getAuthor().getName() + " selected role: " + roleName);
                        return;
                    }
                }
                event.getChannel().sendMessage(event.getMessage().getContentRaw() +
                        " is not a valid role for this mission!").queue();
                logger.info(event.getAuthor().getName() + " sent invalid role name: " + event.getMessage().getContentRaw());
            }
        }
    }
}
