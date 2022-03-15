package zone.greggle.gregbot.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.scheduling.task.PresenceTask;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

@Component
public class MissionStartScheduler {
    private static final Logger logger = LoggerFactory.getLogger(MissionEndScheduler.class);

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    JDAContainer jdaContainer;

    List<PresenceTask> allTasks = new ArrayList<>();

    Timer timer = new Timer();

    public void registerExistingTasks() {
        List<Mission> publishedMissions = missionRepository.findByPublishedIs(true);
        logger.info("Registering all stored presence tasks");
        for (Mission mission: publishedMissions) {
            schedulePresence(mission);
        }
    }

    public void schedulePresence(Mission mission) {
        Date missionTime = Date.from(mission.getMissionDate().toInstant(ZoneOffset.UTC));

        if (!missionRepository.findById(mission.getID()).isPresent()) {
            logger.error("Cannot find mission to register presence");
            return;
        }

        unregisterTask(mission);

        if (!missionTime.before(new Date())) {
            PresenceTask task = new PresenceTask(mission.getID());
            timer.schedule(task, missionTime);
            allTasks.add(task);
            logger.debug("Set presence time for mission #" + mission.getShortID() + " to " + missionTime.toString());
        } else {
            logger.warn("Specified presence time is in the past (#" + mission.getShortID() + ")");
        }
    }

    public void unregisterTask(Mission mission) {
        for (int i = allTasks.size() - 1; i >= 0; i--) {
            PresenceTask existingTask = allTasks.get(i);
            if (existingTask.getMissionID().equals(mission.getID())) {
                existingTask.cancel();
                allTasks.remove(i);
                return;
            }
        }
    }
}
