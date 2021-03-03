package zone.greggle.gregbot.scheduling.task;

import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.greggle.gregbot.BeanUtil;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionMember;
import zone.greggle.gregbot.entity.MissionRepository;

import java.util.TimerTask;

public class AlertTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(AlertTask.class);

    JDAContainer jdaContainer;

    MissionRepository missionRepository;

    String missionID;

    int warningMinutes;

    public AlertTask(String missionID, int warningMinutes) {
        this.jdaContainer = BeanUtil.getBean(JDAContainer.class);
        this.missionRepository = BeanUtil.getBean(MissionRepository.class);
        this.missionID = missionID;
        this.warningMinutes = warningMinutes;
    }

    @Override
    public void run() {
        Guild guild = this.jdaContainer.getGuild();
        Mission mission = missionRepository.findMissionById(missionID);

        if (mission != null) {
            for (MissionMember missionMember: mission.getMembers()) {
                guild.retrieveMemberById(missionMember.getDiscordID()).queue(guildMember -> {
                    guildMember.getUser().openPrivateChannel().queue(dm -> {
                        dm.sendMessage(mission.getName() + " is starting in " + warningMinutes +" minutes!").queue();
                    });
                });
            }
        } else {
            logger.error("Attempted to send alerts for deleted mission");
        }
    }
}
