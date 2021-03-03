package zone.greggle.gregbot.entity;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import zone.greggle.gregbot.data.EditMode;

import java.util.List;

@Repository
public interface MissionRepository extends MongoRepository<Mission, String> {

    Mission findMissionById(String id);

    Mission findByShortID(String shortID);

    Mission findByMissionChannelID(long missionChannelID);

    void deleteByShortID(String shortID);

    void deleteByMissionChannelID(long missionChannelID);

    List<Mission> findByPublishedIsFalseAndEditModeIsNot(EditMode editMode);

    List<Mission> findByPublishedIs(boolean isPublished);
}
