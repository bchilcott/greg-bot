package zone.greggle.gregbot.scheduling;

import net.dv8tion.jda.api.entities.Guild;
import zone.greggle.gregbot.BeanUtil;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionMember;
import zone.greggle.gregbot.entity.MissionRepository;

import java.util.TimerTask;

public class AlertTask extends TimerTask {

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

        for (MissionMember missionMember: mission.getMembers()) {
            guild.retrieveMemberById(missionMember.getDiscordID()).queue(guildMember -> {
                guildMember.getUser().openPrivateChannel().queue(dm -> {
                    dm.sendMessage(mission.getName() + " is starting in " + warningMinutes +" minutes!").queue();
                });
            });
        }
    }
}
