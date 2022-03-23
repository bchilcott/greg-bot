package zone.greggle.gregbot.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.MissionUtil;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;
import zone.greggle.gregbot.scheduling.task.DeleteTask;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

@Component
public class MissionEndScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MissionEndScheduler.class);

    @Value("${delete.time.hours}")
    int deleteHours;

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    MissionEditorUtil missionEditorUtil;

    @Autowired
    MissionUtil missionUtil;

    List<DeleteTask> allTasks = new ArrayList<>();

    Timer timer = new Timer();

    public void registerExistingDeletes() {
        List<Mission> publishedMissions = missionRepository.findByPublishedIs(true);
        logger.info("Registering all stored deletes");
        for (Mission mission: publishedMissions) {
            scheduleDelete(mission);
        }
    }

    public void scheduleDelete(Mission mission) {
        Date deleteTime = Date.from(mission.getMissionDate().plusHours(deleteHours)
                .toInstant(ZoneOffset.UTC));

        if (!missionRepository.findById(mission.getID()).isPresent()) {
            logger.error("Cannot find mission to register delete");
            return;
        }

        unregisterDelete(mission);

        if (!deleteTime.before(new Date())) {
            DeleteTask task = new DeleteTask(mission.getID());
            timer.schedule(task, deleteTime);
            allTasks.add(task);
            logger.debug("Set delete time for " + mission.getName() + " to " + deleteTime);
        } else {
            try {
                logger.info("Deleting mission that has already ended: " + mission.getName());
                missionUtil.deleteMission(mission);
            } catch (NullPointerException e) {
                logger.error("Attempted to delete a deleted mission");
            }
        }
    }

    public void unregisterDelete(Mission mission) {
        for (int i = allTasks.size() - 1; i >= 0; i--) {
            DeleteTask existingTask = allTasks.get(i);
            if (existingTask.getMissionID().equals(mission.getID())) {
                existingTask.cancel();
                allTasks.remove(i);
                return;
            }
        }
    }

}
