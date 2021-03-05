package zone.greggle.gregbot.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;
import zone.greggle.gregbot.scheduling.task.DeleteTask;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

@Component
public class DeleteScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DeleteScheduler.class);

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    MissionEditorUtil missionEditorUtil;

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
//        Date deleteTime = Date.from(mission.getDateCreated().plusSeconds(10)
//                .toInstant(ZoneOffset.UTC));
        Date deleteTime = Date.from(mission.getMissionDate().plusHours(8)
                .toInstant(ZoneOffset.UTC));

        if (missionRepository.findByShortID(mission.getShortID()) == null) {
            logger.error("Cannot find mission to register delete");
            return;
        }

        unregisterDelete(mission);

        if (!deleteTime.before(new Date())) {
            DeleteTask task = new DeleteTask(mission.getID());
            timer.schedule(task, deleteTime);
            allTasks.add(task);
            logger.info("Set delete time for mission #" + mission.getShortID() + " to " + deleteTime.toString());
        } else {
            logger.warn("Specified delete time is in the past (#" + mission.getShortID() + ")");
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
