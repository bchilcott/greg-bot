package zone.greggle.gregbot.scheduling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.greggle.gregbot.BeanUtil;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.MissionUtil;

import java.util.TimerTask;

public class DeleteTask extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(DeleteTask.class);

    MissionRepository missionRepository;

    MissionUtil missionUtil;

    final String missionID;

    public DeleteTask(String missionID) {
        this.missionRepository = BeanUtil.getBean(MissionRepository.class);
        this.missionUtil = BeanUtil.getBean(MissionUtil.class);
        this.missionID = missionID;
    }

    @Override
    public void run() {
        try {
            missionUtil.deleteMission(missionRepository.findMissionById(missionID));
        } catch (NullPointerException e) {
            logger.error("Attempted to delete a deleted mission");
        }
    }

    public String getMissionID() {
        return missionID;
    }
}
