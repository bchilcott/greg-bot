package zone.greggle.gregbot.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;
import zone.greggle.gregbot.scheduling.task.AlertTask;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

@Component
public class AlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AlertScheduler.class);

    @Value("${alert.warning.minutes}")
    int warningMinutes;

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    MissionEditorUtil missionEditorUtil;

    List<AlertTask> allTasks = new ArrayList<>();

    Timer timer = new Timer();

    public void registerStoredAlerts() {

        List<Mission> publishedMissions = missionRepository.findByPublishedIs(true);
        logger.info("Registering all stored alerts");
        for (Mission mission: publishedMissions) {
            registerNewAlert(mission);
        }
    }

    public void registerNewAlert(Mission mission) {
//        Date alertTime = Date.from(LocalDateTime.now().plusSeconds(10).toInstant(ZoneOffset.UTC));
        Date alertTime = Date.from(mission.getMissionDate().minusMinutes(warningMinutes)
                .toInstant(ZoneOffset.UTC));

        if (!missionRepository.findById(mission.getID()).isPresent()) {
            logger.error("Cannot find mission to register alert");
            return;
        }

        unregisterAlert(mission);

        if (!alertTime.before(new Date())) {
            AlertTask task = new AlertTask(mission.getID(), warningMinutes);
            timer.schedule(task, alertTime);
            allTasks.add(task);
            logger.debug("Set alert for mission #" + mission.getShortID() + " at " + alertTime.toString());
        } else {
            logger.warn("Specified alert time is in the past (#" + mission.getShortID() + ")");
        }
    }

    public void unregisterAlert(Mission mission) {
        for (int i = allTasks.size() - 1; i >= 0; i--) {
            AlertTask existingTask = allTasks.get(i);
            if (existingTask.getMissionID().equals(mission.getID())) {
                existingTask.cancel();
                allTasks.remove(i);
                return;
            }
        }
    }

}
