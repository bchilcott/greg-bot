package zone.greggle.gregbot.scheduling.task;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.managers.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.greggle.gregbot.BeanUtil;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;

import java.util.TimerTask;

public class PresenceTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(PresenceTask.class);

    JDAContainer jdaContainer;

    MissionRepository missionRepository;

    final String missionID;

    public PresenceTask(String missionID) {
        this.jdaContainer = BeanUtil.getBean(JDAContainer.class);
        this.missionRepository = BeanUtil.getBean(MissionRepository.class);
        this.missionID = missionID;
    }

    @Override
    public void run() {
        Presence presence = jdaContainer.getJDA().getPresence();
        Mission mission = missionRepository.findMissionById(missionID);

        if (mission == null) return;
        logger.debug("Setting presence for mission: " + mission.getName());
        presence.setActivity(Activity.playing(mission.getName()));

    }

    public String getMissionID() {
        return missionID;
    }
}
